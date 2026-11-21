package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.service.DataScopeService;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.ProjectScopeService;
import com.officeautomationsystemapi.service.SensitiveDataService;
import com.officeautomationsystemapi.service.UserPermissionService;
import com.officeautomationsystemapi.service.UserHandoverService;
import com.officeautomationsystemapi.service.UserService;
import com.officeautomationsystemapi.vo.ImportResultVO;
import com.officeautomationsystemapi.vo.OrgNodeVO;
import com.officeautomationsystemapi.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 用户管理：个人中心、账号维护、导入导出
@RestController
@RequestMapping("/api/users")
public class UsersController {

    // 头像最大 2MB，只接受常见图片后缀
    private static final long AVATAR_MAX_SIZE = 2L * 1024 * 1024;
    private static final List<String> AVATAR_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    // 单页最多 500 条
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserHandoverService userHandoverService;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private SensitiveDataService sensitiveDataService;

    @Autowired
    private UserPermissionService userPermissionService;

    @Autowired
    private ProjectScopeService projectScopeService;

    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    // 当前登录人的资料
    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        UserVO vo = userService.getMyProfile(RequestUser.id(request));
        return Result.success(vo);
    }

    // 只能改姓名、性别、手机、邮箱、头像，其它字段忽略
    @PutMapping("/me")
    @RequirePermission("user:profile:edit")
    public Result<Void> updateMe(@RequestBody UserVO vo, HttpServletRequest request) {
        userService.updateMyProfile(RequestUser.id(request), vo);
        return Result.success(null);
    }

    // 头像按年月分目录，文件名用 UUID 防重名
    @PostMapping("/me/avatar")
    @RequirePermission("user:profile:edit")
    public Result<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        Long uid = RequestUser.id(request);
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            return Result.error("头像不能超过2MB");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!AVATAR_EXTENSIONS.contains(ext)) {
            return Result.error("仅支持 jpg/jpeg/png/gif/webp 格式");
        }

        String monthDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Path.of(uploadDir, "avatar", monthDir);
        try {
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName));
            }
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }

        String relPath = "avatar/" + monthDir + "/" + fileName;
        UserVO vo = new UserVO();
        vo.setAvatar(relPath);
        userService.updateMyProfile(uid, vo);

        Map<String, Object> result = new HashMap<>();
        result.put("avatar", relPath);
        return Result.success(result);
    }

    // 只读 uploads/avatar 下的文件，防止改字段读任意文件
    @GetMapping("/me/avatar")
    public void myAvatar(HttpServletRequest request, HttpServletResponse response) {
        UserVO vo = userService.getMyProfile(RequestUser.id(request));
        String avatar = vo.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path avatarRoot = root.resolve("avatar").normalize();
        Path file = root.resolve(avatar).normalize();
        if (!file.startsWith(avatarRoot) || !Files.isRegularFile(file)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String lower = file.getFileName().toString().toLowerCase();
        String contentType = "image/jpeg";
        if (lower.endsWith(".png")) {
            contentType = "image/png";
        } else if (lower.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        }
        try {
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "no-cache");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // 分页参数先处理 null 和负数，再限制最大条数
    @GetMapping("/page")
    @RequirePermission("user:list")
    public Result<Page<UserVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) Long orgId,
                                     HttpServletRequest request) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        // 部门主管只看本部门，人事主管看本部门及下级
        List<Long> orgScope = dataScopeService.visibleOrgIds(RequestUser.id(request));
        Page<UserVO> result = userService.pageUsers(current, size, keyword, status, orgId, orgScope);
        // 手机号、邮箱按规则脱敏，本人和有查看权的不脱敏
        sensitiveDataService.maskUsers(result.getRecords(), RequestUser.id(request));
        return Result.success(result);
    }

    // 指派和提到人用的精简列表
    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options(HttpServletRequest request) {
        QueryWrapper<Users> wrapper = new QueryWrapper<Users>()
                .select("id", "username", "real_name")
                .eq("status", 1)
                .orderByAsc("id");
        // 下拉也按数据范围过滤
        List<Long> visibleUsers = dataScopeService.visibleUserIds(RequestUser.id(request));
        if (visibleUsers != null) {
            if (visibleUsers.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            wrapper.in("id", visibleUsers);
        }
        List<Users> users = userService.list(wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Users user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("real_name", user.getReal_name());
            list.add(item);
        }
        return Result.success(list);
    }

    // 用户列表左侧的部门树
    @GetMapping("/org-tree")
    @RequirePermission("user:list")
    public Result<List<OrgNodeVO>> orgTree(HttpServletRequest request) {
        return Result.success(userService.orgTree(dataScopeService.visibleOrgIds(RequestUser.id(request))));
    }

    // Excel 批量导入，校验和入库在 service 里
    @PostMapping("/import")
    @RequirePermission("user:add")
    public Result<ImportResultVO> importUsers(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.importUsers(file));
    }

    // 启用中的角色，新增和编辑时选
    @GetMapping("/roles")
    @RequirePermission("user:list")
    public Result<List<Roles>> roles() {
        return Result.success(userService.listEnabledRoles());
    }

    // 用户详情，数据范围外的不给看
    @GetMapping("/{id}")
    @RequirePermission("user:list")
    public Result<UserVO> detail(@PathVariable Long id, HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), id)) {
            return Result.error(403, "无权查看该用户：不在您的数据范围内");
        }
        UserVO vo = userService.getUserDetail(id);
        // 详情同样按规则脱敏
        sensitiveDataService.maskUser(vo, RequestUser.id(request));
        return Result.success(vo);
    }

    // 新增用户，同时分配角色和初始密码
    @PostMapping
    @RequirePermission("user:add")
    public Result<Long> add(@RequestBody UserVO vo, HttpServletRequest request) {
        // 没有 user:salary:view 的人不能写薪资
        sensitiveDataService.stripSalaryIfForbidden(vo, RequestUser.id(request));
        return Result.success(userService.createUserByAdmin(vo));
    }

    // 编辑用户，用户名和密码不在这里改
    @PutMapping
    @RequirePermission("user:edit")
    public Result<Void> update(@RequestBody UserVO vo, HttpServletRequest request) {
        if (vo.getId() != null && !dataScopeService.canOperateUser(RequestUser.id(request), vo.getId())) {
            return Result.error(403, "无权修改该用户：不在您的数据范围内");
        }
        if (!permissionService.isAdmin(RequestUser.id(request)) && touchesAdminRole(vo)) {
            return Result.error(403, "只有管理员可以分配管理员角色");
        }
        sensitiveDataService.stripSalaryIfForbidden(vo, RequestUser.id(request));
        userService.updateUserByAdmin(vo);
        return Result.success(null);
    }

    // 非管理员不能分配 admin 角色，也不能动管理员账号
    private boolean touchesAdminRole(UserVO vo) {
        if (vo.getId() != null && permissionService.isAdmin(vo.getId())) {
            return true;
        }
        if (vo.getRoleIds() == null || vo.getRoleIds().isEmpty()) {
            return false;
        }
        Set<Long> adminRoleIds = userService.listEnabledRoles().stream()
                .filter(role -> "admin".equals(role.getRole_code()))
                .map(Roles::getId)
                .collect(Collectors.toSet());
        for (Long roleId : vo.getRoleIds()) {
            // id=1 是内置管理员，和 role_code 一起拦
            if (roleId != null && (roleId == 1L || adminRoleIds.contains(roleId))) {
                return true;
            }
        }
        return false;
    }

    // 删除用户，只标记删除并解绑角色
    @DeleteMapping("/{id}")
    @RequirePermission("user:delete")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), id)) {
            return Result.error(403, "无权删除该用户：不在您的数据范围内");
        }
        userService.deleteUserByAdmin(RequestUser.id(request), id);
        return Result.success(null);
    }

    // 重置密码，下次登录要改，限在自己数据范围内
    @PutMapping("/{id}/reset-password")
    @RequirePermission("user:reset")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), id)) {
            return Result.error(403, "无权重置该用户密码：不在您的数据范围内");
        }
        userService.resetPasswordByAdmin(id, body.get("newPassword"));
        return Result.success(null);
    }

    // 启用或停用账号
    @PutMapping("/{id}/status")
    @RequirePermission("user:status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status,
                                     @RequestParam(defaultValue = "false") boolean force,
                                     HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), id)) {
            return Result.error(403, "无权修改该用户状态：不在您的数据范围内");
        }
        userService.changeUserStatus(RequestUser.id(request), id, status, force);
        return Result.success(null);
    }

    // 手动解除登录锁定
    @PostMapping("/unlock/{userId}")
    @RequirePermission("user:unlock")
    public Result<Void> unlock(@PathVariable Long userId, HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), userId)) {
            return Result.error(403, "无权解锁该用户：不在您的数据范围内");
        }
        userService.unlock(userId);
        return Result.success(null);
    }

    // 按当前筛选条件导出，文件名和表头由 service 生成
    @GetMapping("/export")
    @RequirePermission("user:export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Long orgId,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        userService.exportUsers(RequestUser.id(request), request.getRemoteAddr(),
                request.getHeader("User-Agent"), keyword, status, orgId, response);
    }

    // 入职办理，开账号并分配部门、职位、角色，密码留空由系统生成
    @PostMapping("/onboard")
    @RequirePermission("user:onboard")
    public Result<Map<String, Object>> onboard(@RequestBody UserVO vo, HttpServletRequest request) {
        if (vo.getOrg_id() == null || vo.getOrg_id() == 0L) {
            return Result.error("请选择入职部门");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), vo.getOrg_id())) {
            return Result.error(403, "无权在该部门下开通账号：不在您的数据范围内");
        }
        return Result.success(userService.onboard(vo));
    }

    // 离职交接预览，看待办数和在审流程数
    @GetMapping("/{id}/handover-preview")
    @RequirePermission("user:transfer")
    public Result<Map<String, Object>> handoverPreview(@PathVariable Long id, HttpServletRequest request) {
        if (!dataScopeService.canOperateUser(RequestUser.id(request), id)) {
            return Result.error(403, "无权办理该用户的交接：不在您的数据范围内");
        }
        return Result.success(userHandoverService.preview(id));
    }

    // 离职交接，待办转给接收人、停用账号、写交接记录
    @PostMapping("/{id}/handover")
    @RequirePermission("user:transfer")
    public Result<Map<String, Object>> handover(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        Long actorId = RequestUser.id(request);
        if (!dataScopeService.canOperateUser(actorId, id)) {
            return Result.error(403, "无权办理该用户的交接：不在您的数据范围内");
        }
        // mode：transfer 指定接收人，keep 保留待办，auto 转直属上级
        String mode = body.get("mode") == null ? "transfer" : String.valueOf(body.get("mode")).trim().toLowerCase();
        Long receiverId = toLong(body.get("receiverId"));
        if ("transfer".equals(mode)) {
            if (receiverId == null) {
                return Result.error("请选择交接接收人");
            }
            // 接收人也要在数据范围内
            if (!dataScopeService.canOperateUser(actorId, receiverId)) {
                return Result.error(403, "接收人不在您的数据范围内，请选择本部门同事");
            }
        } else if (!"keep".equals(mode) && !"auto".equals(mode)) {
            return Result.error("处理方式只能是 transfer、keep 或 auto");
        }
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        LocalDate effectiveDate = toDate(body.get("effectiveDate"));
        boolean transferRunning = body.get("transferRunning") == null
                || Boolean.parseBoolean(String.valueOf(body.get("transferRunning")));
        return Result.success(userHandoverService.handover(actorId, id, receiverId, reason, effectiveDate,
                transferRunning, mode));
    }

    // 交接记录，可按离职人筛
    @GetMapping("/handovers")
    @RequirePermission("user:transfer")
    public Result<Map<String, Object>> handovers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) Long leaverId) {
        return Result.success(userHandoverService.page(pageNum, pageSize, leaverId));
    }

    // 任务中心访问权，角色之外的单独授权

    // 查看额外权限，管理员看全部，组长只看项目成员
    @GetMapping("/{id}/extra-permissions")
    public Result<Map<String, Object>> extraPermissions(HttpServletRequest request, @PathVariable Long id) {
        Long operatorId = RequestUser.id(request);
        if (!canGrantTaskAccess(operatorId)) {
            return Result.error(403, "无权限查看任务权限（仅管理员或已获授权的项目经理）");
        }
        if (!canTargetUser(operatorId, id)) {
            return Result.error(403, "无权操作该用户：不在您的项目或部门范围内");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", id);
        data.put("items", userPermissionService.listOf(id));
        data.put("taskAccess", userPermissionService.has(id, "task:access"));
        // 只有管理员能把发放权限的权限再发出去
        data.put("canGrantPermission", permissionService.isAdmin(operatorId));
        return Result.success(data);
    }

    // 授予任务中心访问权，task:grant 只有管理员能发
    @PostMapping("/{id}/extra-permissions")
    public Result<Map<String, Object>> grantExtraPermission(HttpServletRequest request, @PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        Long operatorId = RequestUser.id(request);
        String menuCode = body.get("menu_code") == null ? "task:access" : String.valueOf(body.get("menu_code")).trim();
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason")).trim();
        if (!canGrantTaskAccess(operatorId)) {
            return Result.error(403, "无权限发放任务权限（仅管理员或已获授权的项目经理）");
        }
        if (!canTargetUser(operatorId, id)) {
            return Result.error(403, "无权操作该用户：不在您的项目或部门范围内");
        }
        if (!"task:access".equals(menuCode) && !"task:grant".equals(menuCode)) {
            return Result.error(400, "只支持发放任务中心访问权或发放权限本身");
        }
        if ("task:grant".equals(menuCode) && !permissionService.isAdmin(operatorId)) {
            return Result.error(403, "只有管理员可以把「发放任务权限」的权限授予他人");
        }
        boolean added = userPermissionService.grantIfAbsent(id, menuCode, operatorId, 1,
                reason == null || reason.isEmpty() ? "管理员/项目经理手动授予" : reason);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("added", added);
        data.put("menuCode", menuCode);
        data.put("items", userPermissionService.listOf(id));
        return Result.success(added ? "已开通" : "该用户已有此权限", data);
    }

    // 回收额外权限
    @DeleteMapping("/{id}/extra-permissions/{menuId}")
    public Result<Map<String, Object>> revokeExtraPermission(HttpServletRequest request, @PathVariable Long id,
                                                             @PathVariable Long menuId) {
        Long operatorId = RequestUser.id(request);
        if (!canGrantTaskAccess(operatorId)) {
            return Result.error(403, "无权限回收任务权限（仅管理员或已获授权的项目经理）");
        }
        if (!canTargetUser(operatorId, id)) {
            return Result.error(403, "无权操作该用户：不在您的项目或部门范围内");
        }
        // task:grant 只允许管理员回收
        boolean isGrantCode = userPermissionService.listOf(id).stream()
                .anyMatch(it -> String.valueOf(it.get("menuId")).equals(String.valueOf(menuId))
                        && "task:grant".equals(String.valueOf(it.get("menuCode"))));
        if (isGrantCode && !permissionService.isAdmin(operatorId)) {
            return Result.error(403, "只有管理员可以回收「发放任务权限」");
        }
        boolean removed = userPermissionService.revoke(id, menuId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("removed", removed);
        data.put("items", userPermissionService.listOf(id));
        return Result.success(removed ? "已回收" : "该用户没有这条权限", data);
    }

    // 能发 task:grant 的人：管理员或被单独授予的人
    private boolean canGrantTaskAccess(Long operatorId) {
        if (operatorId == null) return false;
        if (permissionService.isAdmin(operatorId)) return true;
        return permissionService.getUserPermissions(operatorId).contains("task:grant");
    }

    // 管理员不限，组长只能动自己项目成员，部门主管动本部门及下级
    private boolean canTargetUser(Long operatorId, Long targetId) {
        if (operatorId == null || targetId == null) return false;
        if (permissionService.isAdmin(operatorId)) return true;
        if (operatorId.equals(targetId)) return true;
        for (Long projectId : projectScopeService.managedProjectIds(operatorId)) {
            if (projectScopeService.projectMemberIds(projectId).contains(targetId)) return true;
        }
        return dataScopeService.canOperateUser(operatorId, targetId);
    }
    // id 可能是数字或字符串，统一转 Long
    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isEmpty()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 离职日期可不填，service 默认今天
    private LocalDate toDate(Object value) {
        if (value == null || String.valueOf(value).isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
