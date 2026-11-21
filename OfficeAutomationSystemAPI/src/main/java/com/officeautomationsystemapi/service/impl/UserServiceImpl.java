package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.officeautomationsystemapi.utilt.ExcelSafe;
import com.officeautomationsystemapi.common.PasswordPolicy;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.entity.LoginLogs;
import com.officeautomationsystemapi.entity.OperationLogs;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.PasswordHistory;
import com.officeautomationsystemapi.entity.Positions;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.Titles;
import com.officeautomationsystemapi.entity.UserRoles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.LoginLogsRepository;
import com.officeautomationsystemapi.repository.OperationLogsRepository;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.PasswordHistoryRepository;
import com.officeautomationsystemapi.repository.PositionsRepository;
import com.officeautomationsystemapi.repository.RolesRepository;
import com.officeautomationsystemapi.repository.TitlesRepository;
import com.officeautomationsystemapi.repository.UserRolesRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.OrganizationService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.UserService;
import com.officeautomationsystemapi.vo.ImportResultVO;
import com.officeautomationsystemapi.vo.OrgNodeVO;
import com.officeautomationsystemapi.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 用户：登录锁定、改密、增删改查、角色绑定、Excel 导入导出
@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UsersRepository, Users> implements UserService {

    // 连续输错 5 次锁 30 分钟
    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_MINUTES = 30;

    // 导入时没填密码就用这个默认密码
    private static final String DEFAULT_PASSWORD = "Admin@123";

    // users.username 是 VARCHAR(50)，软删除改名时要截断原名给后缀留位置
    private static final int USERNAME_MAX_LENGTH = 50;

    // 这几个角色只有管理员能分配和收回，这类账号也只让他动
    private static final Set<String> ADMIN_ONLY_ROLE_CODES = Set.of("admin", "gm", "hr", "hr_manager");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private LoginLogsRepository loginLogsRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private OperationLogsRepository operationLogsRepository;

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private TitlesRepository titlesRepository;

    // 停用前统计待办与未结束任务，直接查表比走各业务 service 更省依赖
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private OrganizationService organizationService;

    public void createUser(Users user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // 登录：看锁定、校密码，失败累加次数，成功清空并记登录信息
    @Override
    public Users login(String username, String rawPassword, String ip) {
        Users user = findUserByUserName(username);
        if (user == null) {
            saveLoginLog(null, username, ip, 0, "用户名或密码错误");
            throw new RuntimeException("用户名或密码错误");
        }

        // 停用或已删除的账号密码对也拒绝，不写登录时间和成功日志
        if (user.getDeleted_at() != null || user.getStatus() == null || user.getStatus() != 1) {
            saveLoginLog(user, ip, 0, "账号已停用或删除，拒绝登录");
            throw new RuntimeException("账号已停用或删除，请联系管理员");
        }

        if (user.getLock_time() != null) {
            long lockedMinutes = Duration.between(user.getLock_time(), LocalDateTime.now()).toMinutes();
            if (lockedMinutes < LOCK_MINUTES) {
                long remain = LOCK_MINUTES - lockedMinutes;
                saveLoginLog(user, ip, 0, "账号锁定中，剩余" + remain + "分钟");
                throw new RuntimeException("账号已锁定，请" + remain + "分钟后重试或联系管理员解锁");
            }
            user.setLock_time(null);
            user.setLogin_fail_count(0);
            updateById(user);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            int failCount = (user.getLogin_fail_count() == null ? 0 : user.getLogin_fail_count()) + 1;
            user.setLogin_fail_count(failCount);
            if (failCount >= MAX_FAIL_COUNT) {
                user.setLock_time(LocalDateTime.now());
                updateById(user);
                saveLoginLog(user, ip, 0, "连续" + failCount + "次密码错误，账号锁定" + LOCK_MINUTES + "分钟");
                throw new RuntimeException("密码错误已达" + MAX_FAIL_COUNT + "次，账号已锁定，请" + LOCK_MINUTES + "分钟后重试或联系管理员解锁");
            }
            updateById(user);
            saveLoginLog(user, ip, 0, "密码错误(第" + failCount + "次)");
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLogin_fail_count(0);
        user.setLock_time(null);
        user.setLast_login_time(LocalDateTime.now());
        user.setLast_login_ip(ip);
        updateById(user);
        saveLoginLog(user, ip, 1, null);
        return user;
    }

    // 本人改密：强度校验 + 原密码校验 + 不能和最近三次重复
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (oldPassword == null) {
            throw new RuntimeException("原密码不正确");
        }
        PasswordPolicy.validate(newPassword, user.getUsername(), user.getReal_name());

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与最近三次密码相同");
        }
        for (PasswordHistory history : passwordHistoryRepository.findRecent3(userId)) {
            if (passwordEncoder.matches(newPassword, history.getPassword())) {
                throw new RuntimeException("新密码不能与最近3次使用过的密码相同");
            }
        }

        PasswordHistory record = new PasswordHistory();
        record.setUser_id(userId);
        record.setPassword(user.getPassword());
        passwordHistoryRepository.insert(record);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMust_change_password(0);
        updateById(user);
        return true;
    }

    @Override
    public void unlock(Long userId) {
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setLogin_fail_count(0);
        user.setLock_time(null);
        updateById(user);
        saveLoginLog(user, null, 1, "管理员手动解锁");
    }

    @Override
    public void logout(Long userId) {
        Users user = getById(userId);
        if (user == null) {
            return;
        }
        user.setLast_logout_time(LocalDateTime.now());
        updateById(user);
    }

    private record ExcelRow(int lineNo, String[] cells) {
    }

    // 批量导入逐行处理，某行出错不影响其他行，所以不加事务
    @Override
    public ImportResultVO importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要导入的 Excel 文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new RuntimeException("仅支持 .xlsx / .xls 文件");
        }

        List<ExcelRow> rows = parseExcel(file);
        if (rows.isEmpty()) {
            throw new RuntimeException("文件中没有数据行");
        }

        ImportResultVO result = new ImportResultVO();
        Map<String, Long> orgMap = loadOrgNameMap();
        Map<String, Long> roleMap = loadRoleNameMap();
        for (ExcelRow row : rows) {
            try {
                createUserByAdmin(buildVoFromRow(row.cells(), orgMap, roleMap));
                result.setSuccess(result.getSuccess() + 1);
            } catch (RuntimeException e) {
                result.getErrors().add("第" + row.lineNo() + "行：" + e.getMessage());
            }
        }
        return result;
    }

    // 从第 2 行起读数据
    // 列顺序：用户名、姓名、性别、手机、邮箱、部门、角色、密码、入职日期
    private List<ExcelRow> parseExcel(MultipartFile file) {
        List<ExcelRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String[] cells = new String[9];
                for (int c = 0; c < cells.length; c++) {
                    Cell cell = row.getCell(c);
                    cells[c] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                }
                if (cells[0].isEmpty() && cells[1].isEmpty()) {
                    continue;
                }
                rows.add(new ExcelRow(i + 1, cells));
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败：" + e.getMessage());
        }
        return rows;
    }

    private UserVO buildVoFromRow(String[] cells, Map<String, Long> orgMap, Map<String, Long> roleMap) {
        UserVO vo = new UserVO();
        vo.setUsername(cells[0]);
        vo.setReal_name(cells[1]);

        String gender = cells[2];
        if (gender.contains("男")) {
            vo.setGender(1);
        } else if (gender.contains("女")) {
            vo.setGender(2);
        } else {
            vo.setGender(0);
        }

        vo.setPhone(cells[3].isEmpty() ? null : cells[3]);
        vo.setEmail(cells[4].isEmpty() ? null : cells[4]);

        if (!cells[5].isEmpty()) {
            Long orgId = orgMap.get(cells[5]);
            if (orgId == null) {
                throw new RuntimeException("部门「" + cells[5] + "」不存在");
            }
            vo.setOrg_id(orgId);
        }
        if (!cells[6].isEmpty()) {
            Long roleId = roleMap.get(cells[6]);
            if (roleId == null) {
                throw new RuntimeException("角色「" + cells[6] + "」不存在");
            }
            vo.setRoleIds(Collections.singletonList(roleId));
        }

        vo.setPassword(cells[7].isEmpty() ? DEFAULT_PASSWORD : cells[7]);
        if (!cells[8].isEmpty()) {
            try {
                vo.setEntry_date(LocalDate.parse(cells[8]));
            } catch (Exception e) {
                throw new RuntimeException("入职时间格式应为 yyyy-MM-dd");
            }
        }
        return vo;
    }

    private Map<String, Long> loadOrgNameMap() {
        QueryWrapper<Organizations> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return organizationsRepository.selectList(wrapper).stream()
                .collect(Collectors.toMap(Organizations::getOrg_name, Organizations::getId, (a, b) -> a));
    }

    private Map<String, Long> loadRoleNameMap() {
        QueryWrapper<Roles> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        return rolesRepository.selectList(wrapper).stream()
                .collect(Collectors.toMap(Roles::getRole_name, Roles::getId, (a, b) -> a));
    }

    private void saveLoginLog(Users user, String ip, Integer status, String reason) {
        saveLoginLog(user, null, ip, status, reason);
    }

    // 登录日志：user 为空时说明用户名都不存在，只能记下填的用户名
    private void saveLoginLog(Users user, String username, String ip, Integer status, String reason) {
        LoginLogs log = new LoginLogs();
        if (user != null) {
            log.setUser_id(user.getId());
            log.setUsername(user.getUsername());
        } else {
            log.setUsername(username);
        }
        log.setIp_address(ip);
        log.setStatus(status);
        log.setFail_reason(reason);
        log.setLogin_time(LocalDateTime.now());
        log.setCreated_at(LocalDateTime.now());
        loginLogsRepository.insert(log);
    }

    @Override
    public Users findUserByUserName(String username) {
        return baseMapper.findUserByUserName(username);
    }

    // 用户分页：关键字匹配用户名或姓名，再按状态和部门过滤
    // orgScope 是调用者的可见部门，null 不限，范围外直接返回空页
    @Override
    public Page<UserVO> pageUsers(Integer pageNum, Integer pageSize, String keyword, Integer status, Long orgId, List<Long> orgScope) {
        if (orgScope != null && (orgScope.isEmpty() || (orgId != null && !orgScope.contains(orgId)))) {
            return new Page<>(pageNum, pageSize, 0);
        }
        QueryWrapper<Users> wrapper = userQuery(keyword, status, orgId);
        if (orgScope != null && orgId == null) {
            wrapper.in("org_id", orgScope);
        }
        wrapper.orderByDesc("created_at");
        Page<Users> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, String> orgNames = loadOrgNames();
        List<UserVO> records = page.getRecords().stream()
                .map(u -> toUserVo(u, orgNames))
                .collect(Collectors.toList());
        fillRoles(records);

        Page<UserVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVO vo = toUserVo(user, loadOrgNames());
        fillRoles(List.of(vo));
        return vo;
    }

    // 个人中心比详情多职位和职称名称
    @Override
    public UserVO getMyProfile(Long userId) {
        UserVO vo = getUserDetail(userId);
        if (vo.getPosition_id() != null) {
            Positions position = positionsRepository.selectById(vo.getPosition_id());
            if (position != null) {
                vo.setPosition_name(position.getPosition_name());
            }
        }
        if (vo.getTitle_id() != null) {
            Titles title = titlesRepository.selectById(vo.getTitle_id());
            if (title != null) {
                vo.setTitle_name(title.getTitle_name());
            }
        }
        return vo;
    }

    // 本人能改用户名、姓名、性别、手机、邮箱、头像，其余归管理员
    @Override
    public void updateMyProfile(Long userId, UserVO vo) {
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 用户名自己要能改，要满足格式且唯一，下次登录就换新名
        if (vo.getUsername() != null && !vo.getUsername().trim().isEmpty()) {
            String username = vo.getUsername().trim();
            if (!username.equals(user.getUsername())) {
                if (!username.matches("^[A-Za-z][A-Za-z0-9_]{2,19}$")) {
                    throw new RuntimeException("用户名需字母开头，3-20 位字母、数字或下划线");
                }
                ensureUsernameUnique(username);
                user.setUsername(username);
            }
        }

        if (vo.getReal_name() != null && !vo.getReal_name().trim().isEmpty()) {
            user.setReal_name(vo.getReal_name().trim());
        }

        if (vo.getGender() != null) {
            if (vo.getGender() < 0 || vo.getGender() > 2) {
                throw new RuntimeException("性别取值不合法");
            }
            user.setGender(vo.getGender());
        }

        if (vo.getPhone() != null) {
            String phone = vo.getPhone().trim();
            if (phone.isEmpty()) {
                user.setPhone(null);
            } else {
                ensurePhoneUnique(phone, userId);
                user.setPhone(phone);
            }
        }

        if (vo.getEmail() != null) {
            String email = vo.getEmail().trim();
            if (email.isEmpty()) {
                user.setEmail(null);
            } else {
                ensureEmailUnique(email, userId);
                user.setEmail(email);
            }
        }

        if (vo.getAvatar() != null) {
            user.setAvatar(vo.getAvatar());
        }
        user.setUpdated_at(LocalDateTime.now());
        updateById(user);
    }

    // 管理员新增用户：用户名、手机、邮箱都要唯一，初始密码 8 到 20 位
    @Override
    @Transactional
    public Long createUserByAdmin(UserVO vo) {
        ensureUsernameUnique(vo.getUsername());
        if (vo.getPhone() != null && !vo.getPhone().isEmpty()) {
            ensurePhoneUnique(vo.getPhone(), null);
        }
        if (vo.getEmail() != null && !vo.getEmail().isEmpty()) {
            ensureEmailUnique(vo.getEmail(), null);
        }

        String rawPassword = vo.getPassword();
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 20) {
            throw new RuntimeException("初始密码长度需为 8-20 位");
        }

        Users user = new Users();
        BeanUtils.copyProperties(vo, user);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        user.setMust_change_password(1);
        user.setOrg_id(vo.getOrg_id() == null ? 0L : vo.getOrg_id());
        user.setCreated_at(LocalDateTime.now());
        user.setUpdated_at(LocalDateTime.now());
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            // 软删除的账号仍占着用户名，唯一键冲突要转成业务提示
            throw new RuntimeException("该用户名已被使用（可能属于已删除的账号）");
        }

        saveUserRoles(user.getId(), vo.getRoleIds());
        return user.getId();
    }

    // 入职办理：没填初始密码就随机生成，首次登录强制改
    @Override
    @Transactional
    public Map<String, Object> onboard(UserVO vo) {
        if (vo.getUsername() == null || vo.getUsername().trim().isEmpty()) {
            throw new RuntimeException("请填写登录用户名");
        }
        if (vo.getReal_name() == null || vo.getReal_name().trim().isEmpty()) {
            throw new RuntimeException("请填写员工姓名");
        }
        if (vo.getOrg_id() == null || vo.getOrg_id() == 0L) {
            throw new RuntimeException("请选择入职部门");
        }

        // 初始密码：人事填了就用填的，没填自动生成 10 位
        String initPassword = vo.getPassword();
        if (initPassword == null || initPassword.trim().isEmpty()) {
            initPassword = randomPassword(10);
        } else if (initPassword.length() < 8 || initPassword.length() > 20) {
            throw new RuntimeException("初始密码长度需为 8-20 位");
        }
        vo.setPassword(initPassword);
        if (vo.getEntry_date() == null) {
            vo.setEntry_date(LocalDate.now());
        }
        if (vo.getStatus() == null) {
            vo.setStatus(1);
        }

        // 没指定角色就默认给普通员工，入职后由主管再调岗
        if (vo.getRoleIds() == null || vo.getRoleIds().isEmpty()) {
            QueryWrapper<Roles> wrapper = new QueryWrapper<>();
            wrapper.eq("role_code", "employee");
            wrapper.eq("status", 1);
            Roles employeeRole = rolesRepository.selectOne(wrapper);
            if (employeeRole != null) {
                vo.setRoleIds(List.of(employeeRole.getId()));
            }
        }

        Long userId = createUserByAdmin(vo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", userId);
        result.put("username", vo.getUsername());
        result.put("realName", vo.getReal_name());
        result.put("initPassword", initPassword);
        return result;
    }

    // 生成随机初始密码，去掉 0/O、1/l 这类容易看错的字符
    private String randomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 管理员改用户：用户名、密码、登录状态等不允许在这里改
    @Override
    @Transactional
    public void updateUserByAdmin(UserVO vo) {
        Users user = getById(vo.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        ensureCanOperateTarget(vo.getId());
        if (vo.getPhone() != null && !vo.getPhone().isEmpty()) {
            ensurePhoneUnique(vo.getPhone(), vo.getId());
        }
        if (vo.getEmail() != null && !vo.getEmail().isEmpty()) {
            ensureEmailUnique(vo.getEmail(), vo.getId());
        }

        // 启用停用走单独接口，这里不能顺带改，防止越权停用
        BigDecimal oldSalary = user.getSalary();
        BeanUtils.copyProperties(vo, user, "id", "username", "password", "status", "created_at", "deleted_at",
                "login_fail_count", "lock_time", "last_login_time", "last_login_ip", "must_change_password");
        // 薪资字段没传（含没权限被抹掉）时保持原值，不能被 BeanUtils 用 null 覆盖
        if (vo.getSalary() == null) {
            user.setSalary(oldSalary);
        }
        user.setUpdated_at(LocalDateTime.now());
        updateById(user);

        saveUserRoles(user.getId(), vo.getRoleIds());
    }

    @Override
    @Transactional
    public void deleteUserByAdmin(Long operatorId, Long userId) {
        if (operatorId.equals(userId)) {
            throw new RuntimeException("不能删除当前登录账号");
        }
        Users target = getById(userId);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        ensureCanOperateTarget(userId);

        // 用户名、手机、邮箱是唯一索引，软删除后仍占着这三个值
        // 重建同名账号会撞唯一键，删前把用户名拼成原名加 #del 和 id
        String suffix = "#del" + userId;
        String username = target.getUsername() == null ? "" : target.getUsername();
        if (!username.endsWith(suffix)) {
            int keep = Math.max(1, USERNAME_MAX_LENGTH - suffix.length());
            if (username.length() > keep) {
                username = username.substring(0, keep);
            }
            username = username + suffix;
        }
        // updateById 会跳过 null 字段，这里必须用 UpdateWrapper 显式把手机、邮箱置空
        UpdateWrapper<Users> release = new UpdateWrapper<>();
        release.eq("id", userId);
        release.set("username", username);
        release.set("phone", null);
        release.set("email", null);
        release.set("updated_at", LocalDateTime.now());
        update(release);

        removeById(userId);

        QueryWrapper<UserRoles> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        userRolesRepository.delete(wrapper);
    }

    // 管理员重置密码：直接写新密码，下次登录要再改一次
    @Override
    @Transactional
    public void resetPasswordByAdmin(Long userId, String newPassword) {
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 人事持有 user:reset，不加这一步就能重置管理员密码再登进去
        ensureCanOperateTarget(userId);
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度需为 8-20 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMust_change_password(1);
        user.setLogin_fail_count(0);
        user.setLock_time(null);
        user.setUpdated_at(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void changeUserStatus(Long operatorId, Long userId, Integer status, boolean force) {
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("非法状态值");
        }
        if (operatorId.equals(userId)) {
            throw new RuntimeException("不能操作自己的账号");
        }
        Users user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        ensureCanOperateTarget(userId);
        // 停用前先看手上还有多少没走完的单子
        if (status == 0 && !force) {
            Long todos = jdbc.queryForObject("SELECT COUNT(*) FROM flow_instances"
                    + " WHERE current_auditor_id = ? AND status = 1 AND deleted_at IS NULL", Long.class, userId);
            Long tasks = jdbc.queryForObject("SELECT COUNT(*) FROM tasks"
                    + " WHERE assignee_id = ? AND deleted_at IS NULL AND status NOT IN (4,5,6)", Long.class, userId);
            long todoCount = todos == null ? 0L : todos;
            long taskCount = tasks == null ? 0L : tasks;
            if (todoCount + taskCount > 0) {
                throw new RuntimeException("该用户还有 " + todoCount + " 条待办流程、" + taskCount
                        + " 个未结束任务，请先到「用户管理 - 离职交接」办理转交，确认要直接停用请选择强制停用");
            }
        }
        user.setStatus(status);
        user.setUpdated_at(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public List<Roles> listEnabledRoles() {
        QueryWrapper<Roles> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order");
        return rolesRepository.selectList(wrapper);
    }

    // 部门树复用部门服务的启用树，再按数据范围剪掉
    @Override
    public List<OrgNodeVO> orgTree(List<Long> visibleOrgIds) {
        List<OrgNodeVO> tree = organizationService.orgTreeEnabled();
        if (visibleOrgIds == null) {
            return tree;
        }
        return filterOrgTree(tree, new HashSet<>(visibleOrgIds));
    }

    // 自身不可见时把子节点提上来，保证树不断层
    private List<OrgNodeVO> filterOrgTree(List<OrgNodeVO> nodes, Set<Long> visible) {
        List<OrgNodeVO> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (OrgNodeVO node : nodes) {
            List<OrgNodeVO> children = filterOrgTree(node.getChildren(), visible);
            if (visible.contains(node.getId())) {
                OrgNodeVO copy = new OrgNodeVO();
                copy.setId(node.getId());
                copy.setLabel(node.getLabel());
                copy.setChildren(children);
                result.add(copy);
            } else {
                result.addAll(children);
            }
        }
        return result;
    }

    // 部门 ID 到名称的映射，停用部门的用户也要显示名称
    private Map<Long, String> loadOrgNames() {
        return organizationsRepository.selectList(null).stream()
                .collect(Collectors.toMap(Organizations::getId, Organizations::getOrg_name, (a, b) -> a));
    }

    private UserVO toUserVo(Users user, Map<Long, String> orgNames) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setOrg_name(orgNames.get(user.getOrg_id()));
        // 属性拷贝会把密码散列带进 VO，所有查询出口都走这里，统一置空
        vo.setPassword(null);
        return vo;
    }

    // 批量填角色，避免每个用户各查两次库
    private void fillRoles(List<UserVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> userIds = vos.stream().map(UserVO::getId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        QueryWrapper<UserRoles> wrapper = new QueryWrapper<>();
        wrapper.in("user_id", userIds);
        Map<Long, List<Long>> roleIdsByUser = userRolesRepository.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(UserRoles::getUser_id,
                        Collectors.mapping(UserRoles::getRole_id, Collectors.toList())));
        Map<Long, String> roleNames = rolesRepository.selectList(null).stream()
                .collect(Collectors.toMap(Roles::getId, Roles::getRole_name, (a, b) -> a));

        for (UserVO vo : vos) {
            List<Long> roleIds = roleIdsByUser.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roleIds.stream().map(roleNames::get).filter(Objects::nonNull).collect(Collectors.toList()));
        }
    }

    // 重建用户角色：不传表示不动现有角色，显式传空数组才是清空
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        // 新增、导入、编辑三条路径都会走到这里，统一在这里拦提权
        ensureCanAssignRoles(roleIds);
        QueryWrapper<UserRoles> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        userRolesRepository.delete(wrapper);
        for (Long roleId : roleIds) {
            UserRoles relation = new UserRoles();
            relation.setUser_id(userId);
            relation.setRole_id(roleId);
            relation.setGrant_type(1);
            relation.setCreated_at(LocalDateTime.now());
            relation.setUpdated_at(LocalDateTime.now());
            userRolesRepository.insert(relation);
        }
    }

    // 管理员、总经理、人事这三个角色只有管理员能给，防提权
    private void ensureCanAssignRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long operatorId = currentOperatorId();
        if (operatorId == null || permissionService.isAdmin(operatorId)) {
            // 非 HTTP 上下文（定时任务、初始化脚本）没有登录人，不拦
            return;
        }
        for (Long roleId : roleIds) {
            // id=1 是建表脚本里内置管理员的固定 id，库里改了编码也照样拦
            if (roleId == null) {
                continue;
            }
            if (roleId == 1L) {
                throw new RuntimeException("只有管理员可以分配管理员、总经理或人事角色");
            }
            Roles role = rolesRepository.selectById(roleId);
            if (role != null && ADMIN_ONLY_ROLE_CODES.contains(role.getRole_code())) {
                throw new RuntimeException("只有管理员可以分配管理员、总经理或人事角色");
            }
        }
    }

    // 目标账号是否挂着管理员、总经理、人事这类高权限角色
    private boolean hasProtectedRole(Long userId) {
        if (userId == null) {
            return false;
        }
        QueryWrapper<UserRoles> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        List<Long> roleIds = userRolesRepository.selectList(qw).stream()
                .map(UserRoles::getRole_id)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        QueryWrapper<Roles> roleQw = new QueryWrapper<>();
        roleQw.in("id", roleIds);
        for (Roles role : rolesRepository.selectList(roleQw)) {
            if (role.getRole_code() != null && ADMIN_ONLY_ROLE_CODES.contains(role.getRole_code())) {
                return true;
            }
        }
        return false;
    }

    // 非管理员不能动挂着高权限角色的账号，防止人事给自己提权
    private void ensureCanOperateTarget(Long targetUserId) {
        Long operatorId = currentOperatorId();
        if (operatorId == null || permissionService.isAdmin(operatorId)) {
            return;
        }
        if (hasProtectedRole(targetUserId)) {
            throw new RuntimeException("管理员、总经理或人事账号只能由管理员操作");
        }
    }

    // 从当前请求里取登录人，取不到就返回 null
    private Long currentOperatorId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        try {
            return RequestUser.id(request);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private QueryWrapper<Users> userQuery(String keyword, Integer status, Long orgId) {
        QueryWrapper<Users> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("username", keyword).or().like("real_name", keyword));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (orgId != null) {
            wrapper.eq("org_id", orgId);
        }
        return wrapper;
    }

    private void ensureUsernameUnique(String username) {
        QueryWrapper<Users> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
    }

    // 手机号唯一，excludeUserId 不为空时排除自己
    private void ensurePhoneUnique(String phone, Long excludeUserId) {
        QueryWrapper<Users> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        if (excludeUserId != null) {
            wrapper.ne("id", excludeUserId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("手机号已被其他用户使用");
        }
    }

    private void ensureEmailUnique(String email, Long excludeUserId) {
        QueryWrapper<Users> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        if (excludeUserId != null) {
            wrapper.ne("id", excludeUserId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("邮箱已被其他用户使用");
        }
    }

    // 导出用户列表，筛选和分页接口一致，最多一万条，记操作日志
    @Override
    public void exportUsers(Long operatorId, String ip, String userAgent,
                            String keyword, Integer status, Long orgId,
                            HttpServletResponse response) {
        long start = System.currentTimeMillis();
        int exported = 0;
        int resultStatus = 1;
        String errorMessage = null;
        try {
            QueryWrapper<Users> wrapper = userQuery(keyword, status, orgId);
            wrapper.orderByDesc("created_at");
            wrapper.last("limit 10000");

            Map<Long, String> orgNames = loadOrgNames();
            List<UserVO> voList = baseMapper.selectList(wrapper).stream()
                    .map(u -> toUserVo(u, orgNames))
                    .collect(Collectors.toList());
            fillRoles(voList);
            exported = voList.size();

            String[] headers = {"ID", "用户名", "姓名", "性别", "部门", "角色",
                    "手机号(脱敏)", "邮箱(脱敏)", "入职日期", "状态", "最后登录时间"};
            int[] columnWidths = {8, 14, 12, 8, 20, 20, 16, 24, 14, 8, 20};

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream out = response.getOutputStream()) {
                Sheet sheet = workbook.createSheet("用户列表");
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                    sheet.setColumnWidth(i, columnWidths[i] * 256);
                }

                int rowIndex = 1;
                for (UserVO vo : voList) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(vo.getId() == null ? "" : String.valueOf(vo.getId()));
                    row.createCell(1).setCellValue(vo.getUsername() == null ? "" : vo.getUsername());
                    row.createCell(2).setCellValue(vo.getReal_name() == null ? "" : vo.getReal_name());
                    row.createCell(3).setCellValue(genderText(vo.getGender()));
                    row.createCell(4).setCellValue(vo.getOrg_name() == null ? "" : vo.getOrg_name());
                    row.createCell(5).setCellValue(vo.getRoleNames() == null ? "" : String.join(",", vo.getRoleNames()));
                    row.createCell(6).setCellValue(maskPhone(vo.getPhone()));
                    row.createCell(7).setCellValue(maskEmail(vo.getEmail()));
                    row.createCell(8).setCellValue(vo.getEntry_date() == null ? "" : vo.getEntry_date().toString());
                    row.createCell(9).setCellValue(vo.getStatus() != null && vo.getStatus() == 1 ? "启用" : "禁用");
                    row.createCell(10).setCellValue(vo.getLast_login_time() == null ? "" : vo.getLast_login_time().toString());
                }

                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setCharacterEncoding("UTF-8");
                String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replace("+", "%20");
                response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");
                ExcelSafe.sanitize(workbook);
            workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            resultStatus = 0;
            errorMessage = e.getMessage();
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        } finally {
            saveExportLog(operatorId, ip, userAgent, keyword, status, orgId,
                    exported, resultStatus, errorMessage, (int) (System.currentTimeMillis() - start));
        }
    }

    // 导出日志写失败不影响导出结果
    private void saveExportLog(Long operatorId, String ip, String userAgent,
                               String keyword, Integer status, Long orgId,
                               int exported, int resultStatus, String errorMessage, int costTime) {
        try {
            Users operator = getById(operatorId);
            OperationLogs log = new OperationLogs();
            log.setUser_id(operatorId);
            log.setUsername(operator == null ? null : operator.getUsername());
            log.setModule("user");
            log.setAction("export");
            log.setMethod("GET");
            log.setUrl("/api/users/export");
            log.setIp_address(ip);
            log.setUser_agent(userAgent);
            log.setTarget_type("user");
            log.setRequest_params("{\"keyword\":" + (keyword == null ? "null" : "\"" + keyword + "\"")
                    + ",\"status\":" + (status == null ? "null" : String.valueOf(status))
                    + ",\"orgId\":" + (orgId == null ? "null" : String.valueOf(orgId)) + "}");
            log.setResponse_result("导出" + exported + "条");
            log.setStatus(resultStatus);
            log.setError_msg(errorMessage);
            log.setCost_time(costTime);
            log.setCreated_at(LocalDateTime.now());
            log.setUpdated_at(LocalDateTime.now());
            operationLogsRepository.insert(log);
        } catch (Exception ignore) {
            // 日志失败不影响导出
        }
    }

    // 手机号脱敏，保留前 3 位后 4 位
    private String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    // 邮箱脱敏，保留首字符和域名
    private String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        String local = value.substring(0, at);
        String head = local.length() <= 1 ? "*" : local.substring(0, 1);
        return head + "***" + value.substring(at);
    }

    private String genderText(Integer gender) {
        if (gender == null) {
            return "";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }
}
