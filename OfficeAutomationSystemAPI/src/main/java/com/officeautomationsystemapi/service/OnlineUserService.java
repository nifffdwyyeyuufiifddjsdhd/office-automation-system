package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.OnlineUsers;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.OnlineUsersRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// 在线用户：30 分钟内有过请求算在线，一个用户只保留一条记录
@Service
public class OnlineUserService {

    private static final Logger log = LoggerFactory.getLogger(OnlineUserService.class);

    private static final long ONLINE_TIMEOUT_MIN = 30;

    @Autowired
    private OnlineUsersRepository onlineRepo;

    @Autowired
    private UsersRepository usersRepo;

    @Autowired
    private JdbcTemplate jdbc;

    // 登录成功先删旧记录再插入，一个账号只留一条在线数据
    @Transactional
    public void recordLogin(Long userId, String ip, String userAgent) {
        Users user = usersRepo.selectById(userId);
        if (user == null) {
            return;
        }
        String displayName = user.getReal_name() != null && !user.getReal_name().isBlank()
                ? user.getReal_name() : user.getUsername();

        onlineRepo.physicalDeleteByUserId(userId);

        LocalDateTime now = LocalDateTime.now();
        OnlineUsers record = new OnlineUsers();
        record.setUser_id(userId);
        record.setUsername(displayName);
        record.setLogin_time(now);
        record.setLast_active_time(now);
        record.setIp_address(ip);
        record.setSession_id(UUID.randomUUID().toString().replace("-", ""));
        parseUserAgent(userAgent, record);
        try {
            onlineRepo.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发首次登录撞唯一键，说明已有记录，直接更新
            // 直接按 user_id 更新，避免先查再改读不到对方刚提交的行
            onlineRepo.update(record, new QueryWrapper<OnlineUsers>().eq("user_id", userId));
        }
    }

    // 拦截器每次放行前调用，刷新最后活跃时间，记录没了就跳过
    @Transactional
    public void refreshActive(Long userId) {
        OnlineUsers record = onlineRepo.findByUserId(userId);
        if (record == null) {
            return;
        }
        record.setLast_active_time(LocalDateTime.now());
        onlineRepo.updateById(record);
    }

    // 主动退出时删掉在线记录，右上角人数立即减一
    @Transactional
    public void logout(Long userId) {
        onlineRepo.physicalDeleteByUserId(userId);
    }

    // 在线人数，先清掉超时记录再统计
    public long count() {
        cleanInactive();
        return onlineRepo.countOnlineSince(LocalDateTime.now().minusMinutes(ONLINE_TIMEOUT_MIN));
    }

    // 在线明细，可按部门筛选、按姓名或账号搜，已删除用户跳过
    public List<Map<String, Object>> list(Long deptId, String keyword) {
        cleanInactive();
        List<OnlineUsers> onlineList = onlineRepo.findOnlineSince(LocalDateTime.now().minusMinutes(ONLINE_TIMEOUT_MIN));
        if (onlineList.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次把涉及的用户查出来，避免循环里逐条查库
        List<Long> userIds = onlineList.stream().map(OnlineUsers::getUser_id).distinct().toList();
        Map<Long, Users> userMap = usersRepo.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, Function.identity()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (OnlineUsers record : onlineList) {
            Users user = userMap.get(record.getUser_id());
            if (user == null) {
                continue;
            }
            // 按部门筛选时，没设部门的用户也要排除，不能漏进来
            if (deptId != null && !deptId.equals(user.getOrg_id())) {
                continue;
            }
            if (keyword != null && !keyword.isEmpty()) {
                String name = (user.getReal_name() == null ? "" : user.getReal_name())
                        + (user.getUsername() == null ? "" : user.getUsername());
                if (!name.toLowerCase().contains(keyword.toLowerCase())) {
                    continue;
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("userId", record.getUser_id());
            row.put("username", record.getUsername());
            row.put("loginTime", record.getLogin_time());
            row.put("lastActiveTime", record.getLast_active_time());
            row.put("ip", record.getIp_address());
            row.put("browser", record.getBrowser());
            row.put("os", record.getOs());
            row.put("device", record.getDevice());
            row.put("deptId", user.getOrg_id());
            result.add(row);
        }
        return result;
    }

    // 单人详情：资料、在线状态、最近 10 条操作，需 system:online
    public Map<String, Object> detail(Long userId) {
        Users user = usersRepo.selectById(userId);
        if (user == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getReal_name());
        data.put("orgName", orgNameOf(user.getOrg_id()));
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("status", user.getStatus());
        data.put("lastLoginTime", user.getLast_login_time());
        data.put("lastLoginIp", user.getLast_login_ip());
        data.put("lastLogoutTime", user.getLast_logout_time());

        OnlineUsers record = onlineRepo.findByUserId(userId);
        boolean online = record != null && record.getLast_active_time() != null
                && record.getLast_active_time().isAfter(LocalDateTime.now().minusMinutes(ONLINE_TIMEOUT_MIN));
        data.put("online", online);
        data.put("ip", record == null ? null : record.getIp_address());
        data.put("browser", record == null ? null : record.getBrowser());
        data.put("os", record == null ? null : record.getOs());
        data.put("device", record == null ? null : record.getDevice());

        // 最近操作和最近登录分开查，一个查失败不影响另一个
        List<Map<String, Object>> logs = new ArrayList<>();
        try {
            logs = jdbc.queryForList(
                    "SELECT module, action, method, url, status, ip_address AS ip, created_at AS createdAt "
                            + "FROM operation_logs WHERE user_id=? AND deleted_at IS NULL ORDER BY id DESC LIMIT 10",
                    userId);
        } catch (Exception e) {
            log.warn("查询在线用户最近操作失败 userId={}", userId, e);
        }
        data.put("recentOperations", logs);

        List<Map<String, Object>> logins = new ArrayList<>();
        try {
            // 登录日志的成功标记叫 status，这里起别名 result
            logins = jdbc.queryForList(
                    "SELECT login_time AS loginTime, ip_address AS ip, status AS result, fail_reason AS failReason "
                            + "FROM login_logs WHERE user_id=? AND deleted_at IS NULL ORDER BY id DESC LIMIT 5",
                    userId);
        } catch (Exception e) {
            log.warn("查询在线用户最近登录失败 userId={}", userId, e);
        }
        data.put("recentLogins", logins);
        return data;
    }

    // 普通员工看到的在线名单：只有姓名、部门、登录和活跃时间
    // 明细里的 IP、浏览器、设备只有 system:online 能看
    public List<Map<String, Object>> directory(Long deptId, String keyword) {
        List<Map<String, Object>> rows = list(deptId, keyword);
        for (Map<String, Object> row : rows) {
            row.remove("ip");
            row.remove("browser");
            row.remove("os");
            row.remove("device");
            Object deptIdValue = row.get("deptId");
            row.put("orgName", deptIdValue == null ? null : orgNameOf(Long.valueOf(deptIdValue.toString())));
        }
        return rows;
    }

    // 部门 id 换部门名，查不到就留空，不影响名单展示
    private String orgNameOf(Long orgId) {
        if (orgId == null) {
            return null;
        }
        try {
            List<String> names = jdbc.queryForList(
                    "SELECT org_name FROM organizations WHERE id=? AND deleted_at IS NULL", String.class, orgId);
            return names.isEmpty() ? null : names.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    // 删掉超过 30 分钟没有活动的在线记录
    @Transactional
    public int cleanInactive() {
        return onlineRepo.deleteInactive(LocalDateTime.now().minusMinutes(ONLINE_TIMEOUT_MIN));
    }

    // 从 UA 里粗略识别浏览器、系统和设备类型，识别不出就留未知
    private void parseUserAgent(String userAgent, OnlineUsers record) {
        String ua = userAgent == null ? "" : userAgent;

        String browser = "未知";
        if (ua.contains("Edg/")) {
            browser = "Edge";
        } else if (ua.contains("Chrome/")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("Safari/")) {
            browser = "Safari";
        }

        // iPhone 和 iPad 的 UA 也带 Mac OS X，要先判断 iOS
        String os = "未知";
        if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("iPhone") || ua.contains("iPad")) {
            os = "iOS";
        } else if (ua.contains("Mac OS")) {
            os = "macOS";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        }

        record.setBrowser(browser);
        record.setOs(os);
        record.setDevice(ua.contains("Mobile") ? "手机" : "电脑");
    }
}
