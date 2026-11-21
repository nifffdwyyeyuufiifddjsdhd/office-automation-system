package com.officeautomationsystemapi.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.*;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.ProjectScopeService;
import com.officeautomationsystemapi.service.ProjectProgressService;
import com.officeautomationsystemapi.service.UserPermissionService;
import com.officeautomationsystemapi.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

// 任务管理：增删改查、状态流转、成员、评论、附件、看板与统计
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskMemberRepository taskMemberRepository;
    @Autowired private TaskCommentRepository taskCommentRepository;
    @Autowired private TaskOperationLogsRepository logRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private NotificationRecordsRepository notificationRepository;
    // 指派类通知的标题正文走通知模板，运营改文案不用改代码
    @Autowired private com.officeautomationsystemapi.service.NotificationTemplateService notificationTemplateService;
    @Autowired private AttachmentsRepository attachmentsRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PermissionService permissionService;
    @Autowired private ProjectScopeService projectScopeService;
    @Autowired private com.officeautomationsystemapi.service.NotificationBizService notificationBizService;
    @Autowired private UserPermissionService userPermissionService;
    @Autowired private ProjectProgressService projectProgressService;

    // 任务状态或进度变了，就同步到所属项目进度，项目不存在或已结项跳过
    private void touchProject(Long projectId) {
        if (projectId == null || projectId <= 0) return;
        try {
            projectProgressService.refresh(projectId);
        } catch (Exception ignore) {
            // 项目进度只是派生数据，不该影响任务主流程
        }
    }

    // 附件保存的根目录
    @Value("${oa.upload-dir:./uploads}")
    private String uploadDir;

    // 每个任务最多 3 个附件
    private static final int MAX_TASK_ATTACH = 3;

    // 允许上传的文件后缀
    private static final Set<String> ALLOWED_EXTS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md",
            "jpg", "jpeg", "png", "gif", "bmp", "zip", "rar", "java", "js", "ts", "py");

    // 任务状态：0 待分配 1 待接受 2 进行中 3 待验收 4 已完成 5 已撤销 6 已拒绝
    private static final int ST_PENDING = 0;
    private static final int ST_WAIT_ACCEPT = 1;
    private static final int ST_IN_PROGRESS = 2;
    private static final int ST_WAIT_APPROVE = 3;
    private static final int ST_COMPLETED = 4;
    private static final int ST_REVOKED = 5;
    private static final int ST_REJECTED = 6;

    // 成员角色：1 负责人 2 参与人 3 关注人 4 验收人
    private static final int ROLE_OWNER = 1;
    private static final int ROLE_MEMBER = 2;
    private static final int ROLE_WATCHER = 3;
    private static final int ROLE_CHECKER = 4;

    // 当前登录用户 ID
    private Long uid(HttpServletRequest req) {
        return RequestUser.id(req);
    }

    // 是否系统管理员，写操作都要求管理员或部门主管
    private boolean isAdmin(Long userId) {
        // 统一走权限服务按角色编码判断
        return userId != null && permissionService.isAdmin(userId);
    }

    // 是否全局查看者：管理员、总经理，或持有任务全景查看权限的角色
    private boolean isGlobalViewer(Long userId) {
        if (userId == null) return false;
        List<String> roleCodes = permissionService.getUserRoleCodes(userId);
        if (roleCodes.contains("admin") || roleCodes.contains("gm")) return true;
        // 其他角色靠权限码获得全景查看
        return permissionService.getUserPermissions(userId).contains("task:view-all");
    }

    // 是否是任务成员
    private boolean isMember(Long taskId, Long userId) {
        if (userId == null) return false;
        Long cnt = taskMemberRepository.selectCount(
            new QueryWrapper<TaskMember>().eq("task_id", taskId).eq("user_id", userId));
        return cnt != null && cnt > 0;
    }

    // 能否查看任务：全局查看者、创建人、负责人、成员、待分配的公开任务、部门主管
    private boolean canView(Task t, Long userId) {
        if (t == null || userId == null) return false;
        if (isGlobalViewer(userId)
            || Objects.equals(userId, t.getCreator_id())
            || Objects.equals(userId, t.getAssignee_id())
            || isMember(t.getId(), userId)) {
            return true;
        }
        // 项目相关任务：项目经理和项目成员都能看
        if (t.getProject_id() != null && t.getProject_id() > 0
            && projectScopeService.canSeeProject(t.getProject_id(), userId)) {
            return true;
        }
        // 待分配的公开任务只对本部门或自己管的部门开放
        if (statusOf(t) == ST_PENDING) {
            return isGlobalViewer(userId) || isDeptManagerOf(userId, t.getCreator_id()) || sameOrg(userId, t.getCreator_id());
        }
        return isDeptManagerOf(userId, t.getCreator_id()) || isDeptManagerOf(userId, t.getAssignee_id());
    }

    // 能否评论、传附件：管理员和任务相关人员，gm 只读
    private boolean canCollaborate(Task t, Long userId) {
        if (t == null || userId == null) return false;
        return isAdmin(userId)
            || Objects.equals(userId, t.getCreator_id())
            || Objects.equals(userId, t.getAssignee_id())
            || isMember(t.getId(), userId);
    }

    // managerId 是否是 targetUserId 所在部门含下级的负责人
    private boolean isDeptManagerOf(Long managerId, Long targetUserId) {
        if (managerId == null || targetUserId == null || managerId.equals(targetUserId)) return false;
        return managedDeptUserIds(managerId).contains(targetUserId);
    }

    // 两人是否同部门，用于判断能否查看和认领本部门的公开任务
    private boolean sameOrg(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) return false;
        if (userId.equals(targetUserId)) return true;
        try {
            Long mine = orgOf(userId);
            Long theirs = orgOf(targetUserId);
            return mine != null && mine.equals(theirs);
        } catch (Exception e) {
            return false;
        }
    }

    // 取用户所属部门 ID
    private Long orgOf(Long userId) {
        List<Long> orgIds = jdbc.queryForList(
            "SELECT org_id FROM users WHERE id=? AND deleted_at IS NULL", Long.class, userId);
        return orgIds.isEmpty() ? null : orgIds.get(0);
    }

    // 是否部门主管，至少负责一个部门
    private boolean isDeptManager(Long userId) {
        if (userId == null) return false;
        try {
            Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE leader_id=? AND deleted_at IS NULL",
                Integer.class, userId);
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 能否管理任务：管理员、创建人、创建人或负责人所在部门的主管
    private boolean canManageTask(Task t, Long userId) {
        if (t == null || userId == null) return false;
        if (isAdmin(userId) || Objects.equals(userId, t.getCreator_id())) return true;
        // 项目经理负责这个项目，就能分派、改派、验收项目下全部任务
        if (t.getProject_id() != null && t.getProject_id() > 0
            && projectScopeService.isProjectManager(userId, t.getProject_id())) {
            return true;
        }
        return isDeptManagerOf(userId, t.getCreator_id()) || isDeptManagerOf(userId, t.getAssignee_id());
    }

    // 主管负责的部门含下级里的全部用户 ID
    private List<Long> managedDeptUserIds(Long managerId) {
        try {
            List<Long> leadOrgIds = jdbc.queryForList(
                "SELECT id FROM organizations WHERE leader_id=? AND deleted_at IS NULL", Long.class, managerId);
            if (leadOrgIds.isEmpty()) return new ArrayList<>();
            List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT id,parent_id FROM organizations WHERE deleted_at IS NULL");
            // 先把父子关系整理成 map，避免每层全表扫一遍
            Map<Long, List<Long>> children = new HashMap<>();
            for (Map<String, Object> r : all) {
                Object pid = r.get("parent_id");
                if (pid == null) continue;
                children.computeIfAbsent(((Number) pid).longValue(), k -> new ArrayList<>())
                    .add(((Number) r.get("id")).longValue());
            }
            Set<Long> scope = new HashSet<>();
            Deque<Long> stack = new ArrayDeque<>(leadOrgIds);
            while (!stack.isEmpty()) {
                Long o = stack.pop();
                if (!scope.add(o)) continue;
                stack.addAll(children.getOrDefault(o, Collections.emptyList()));
            }
            if (scope.isEmpty()) return new ArrayList<>();
            String placeholders = scope.stream().map(x -> "?").collect(Collectors.joining(","));
            return jdbc.queryForList(
                "SELECT id FROM users WHERE org_id IN (" + placeholders + ") AND deleted_at IS NULL",
                Long.class, scope.toArray());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 任务是否已归档，归档后只读
    private boolean archived(Task t) {
        return t.getArchive_status() != null && t.getArchive_status() == 1;
    }

    // 任务状态，空值按 -1 处理，避免拆箱空指针
    private int statusOf(Task t) {
        return t.getStatus() == null ? -1 : t.getStatus();
    }

    // 失败响应
    private Map<String, Object> err(int code, String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", msg);
        return m;
    }

    // 转成字符串，null 还是 null
    private String text(Object v) {
        return v == null ? null : v.toString();
    }

    // 兼容带 Z 的 UTC 时间和普通本地时间
    private LocalDateTime parseDateTime(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            if (s.endsWith("Z")) {
                return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式不正确：" + s);
        }
    }

    // 解析小数工时
    private BigDecimal parseDecimal(Object v, String field) {
        if (v == null || v.toString().trim().isEmpty()) return null;
        try {
            return new BigDecimal(v.toString().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(field + "必须是数字");
        }
    }

    // 解析 ID 列表，兼容 JSON 数组和逗号分隔字符串，去重并忽略空值
    private List<Long> parseIdList(Object v) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        if (v instanceof List) {
            for (Object o : (List<?>) v) {
                if (o == null || o.toString().trim().isEmpty()) continue;
                try { set.add(Long.valueOf(o.toString().trim())); } catch (Exception ignored) {}
            }
        } else if (v != null && !v.toString().trim().isEmpty()) {
            for (String s : v.toString().split(",")) {
                if (s.trim().isEmpty()) continue;
                try { set.add(Long.valueOf(s.trim())); } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(set);
    }

    // 解析可选 ID，空值返回 null
    private Long parseLongOrNull(Object v) {
        if (v == null || v.toString().trim().isEmpty()) return null;
        return Long.valueOf(v.toString().trim());
    }

    // 解析 ID，空值按 0 处理
    private Long parseIdOrZero(Object v) {
        if (v == null || v.toString().trim().isEmpty()) return 0L;
        return Long.valueOf(v.toString().trim());
    }

    // 逾期判断：计划结束时间已过，还停在待接受、进行中、待验收
    private boolean calcOverdue(Task t) {
        if (t.getIs_overdue() != null && t.getIs_overdue() == 1) return true;
        int st = statusOf(t);
        if (st != ST_WAIT_ACCEPT && st != ST_IN_PROGRESS && st != ST_WAIT_APPROVE) return false;
        return t.getPlan_end_time() != null && t.getPlan_end_time().isBefore(LocalDateTime.now());
    }

    // 生成任务编号，重复了就重试几次
    private String genTaskNo() {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        for (int i = 0; i < 5; i++) {
            String no = "TASK-" + date + "-" + time + (ThreadLocalRandom.current().nextInt(900) + 100);
            Long c = taskRepository.selectCount(new QueryWrapper<Task>().eq("task_no", no));
            if (c == null || c == 0) return no;
        }
        return "TASK-" + date + "-" + System.currentTimeMillis();
    }

    // 被派任务的人要有任务中心权限，已有时不重复插入，自己派给自己也照补
    private void grantTaskAccess(Long targetUserId, Long operatorId, String reason) {
        if (targetUserId == null) return;
        userPermissionService.grantIfAbsent(targetUserId, "task:access", operatorId, 2, reason);
    }
    // 加入成员并去重，之前被标记删除的记录直接恢复，避免唯一键冲突
    private void addMember(Long taskId, Long userId, Integer role) {
        if (userId == null) return;
        // 用原生 SQL 统计，把标记删除的记录也算进来
        Integer cnt = jdbc.queryForObject(
            "SELECT COUNT(*) FROM task_members WHERE task_id=? AND user_id=?",
            Integer.class, taskId, userId);
        if (cnt != null && cnt > 0) {
            jdbc.update(
                "UPDATE task_members SET deleted_at=NULL, role=?, join_time=?, updated_at=NOW() WHERE task_id=? AND user_id=?",
                role, LocalDateTime.now(), taskId, userId);
            return;
        }
        TaskMember m = new TaskMember();
        m.setTask_id(taskId);
        m.setUser_id(userId);
        m.setRole(role);
        m.setJoin_time(LocalDateTime.now());
        taskMemberRepository.insert(m);
    }

    // 写操作日志，失败不影响主流程
    private void log(Long taskId, Long operatorId, String action, String remark, String oldVal, String newVal) {
        try {
            TaskOperationLogs l = new TaskOperationLogs();
            l.setTask_id(taskId);
            l.setOperator_id(operatorId);
            l.setAction(action);
            l.setRemark(remark);
            l.setOld_value(oldVal);
            l.setNew_value(newVal);
            l.setOperation_time(LocalDateTime.now());
            logRepository.insert(l);
        } catch (Exception ignored) {
        }
    }

    // 批量查用户姓名，返回 ID 到姓名的映射
    private Map<Long, String> userNames(Collection<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Users u : usersRepository.selectBatchIds(ids)) map.put(u.getId(), u.getReal_name());
        return map;
    }

    // 收集非空的用户 ID
    private void collectUserIds(Set<Long> ids, Long... userIds) {
        for (Long id : userIds) {
            if (id != null) ids.add(id);
        }
    }

    // 列表和看板只要创建人、负责人的姓名
    private Map<Long, String> namesOfTasks(List<Task> tasks) {
        Set<Long> ids = new HashSet<>();
        for (Task t : tasks) collectUserIds(ids, t.getCreator_id(), t.getAssignee_id());
        return userNames(ids);
    }

    // 取用户名，查不到时用默认值
    private String nameOf(Map<Long, String> names, Long userId, String def) {
        if (userId == null) return def;
        return names.containsKey(userId) ? names.get(userId) : def;
    }

    // 非全局查看者只能看自己创建、负责、参与的任务，以及待分配的公开任务
    private void applyVisibleScope(QueryWrapper<Task> qw, Long userId, boolean admin, boolean includePendingPool) {
        if (admin) return;
        List<Long> related = new ArrayList<>();
        related.add(-1L);
        taskMemberRepository.selectList(new QueryWrapper<TaskMember>().eq("user_id", userId))
            .forEach(m -> related.add(m.getTask_id()));
        List<Long> deptUserIds = managedDeptUserIds(userId);
        // 我负责或参与的项目下的任务全部可见
        List<Long> projectIds = projectScopeService.visibleProjectIds(userId);
        qw.and(w -> {
            w.eq("creator_id", userId).or().eq("assignee_id", userId).or().in("id", related);
            if (includePendingPool) w.or().eq("status", ST_PENDING);
            if (!deptUserIds.isEmpty()) {
                w.or(o -> o.in("creator_id", deptUserIds).or().in("assignee_id", deptUserIds));
            }
            if (!projectIds.isEmpty()) {
                w.or(o -> o.in("project_id", projectIds));
            }
        });
    }

    // 任务列表，view 可选 created、assigned、joined，空表示全部
    @GetMapping
    public Map<String, Object> list(HttpServletRequest req,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "") String view,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer priority,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Long projectId,
                                    @RequestParam(required = false) Long assigneeId,
                                    @RequestParam(required = false) Boolean archived) {
        Long userId = uid(req);
        boolean admin = isGlobalViewer(userId);
        QueryWrapper<Task> qw = new QueryWrapper<>();

        // 默认只看未归档，管理员可以查已归档
        if (admin && Boolean.TRUE.equals(archived)) qw.eq("archive_status", 1);
        else qw.eq("archive_status", 0);

        applyVisibleScope(qw, userId, admin, true);

        if ("created".equals(view)) qw.eq("creator_id", userId);
        if ("assigned".equals(view)) qw.eq("assignee_id", userId);
        if ("joined".equals(view)) {
            // 我以参与人、关注人身份加入的任务，不含自己建的和自己负责的
            List<Long> joinedIds = new ArrayList<>();
            joinedIds.add(-1L);
            taskMemberRepository.selectList(new QueryWrapper<TaskMember>()
                    .eq("user_id", userId).in("role", ROLE_MEMBER, ROLE_WATCHER))
                .forEach(m -> joinedIds.add(m.getTask_id()));
            qw.in("id", joinedIds).ne("creator_id", userId).ne("assignee_id", userId);
        }
        if (status != null) qw.eq("status", status);
        if (priority != null) qw.eq("priority", priority);
        if (projectId != null) qw.eq("project_id", projectId);
        if (assigneeId != null) qw.eq("assignee_id", assigneeId);
        if (keyword != null && !keyword.isEmpty()) qw.like("title", keyword);
        qw.orderByDesc("created_at");

        // 分页查询顺便带回总数，不用再单独 count
        Page<Task> page = taskRepository.selectPage(new Page<>(pageNum, pageSize), qw);
        List<Task> list = page.getRecords();
        Map<Long, String> names = namesOfTasks(list);
        Map<Long, String> projectNames = projectNamesOf(list);

        List<Map<String, Object>> rows = list.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("task_no", t.getTask_no());
            m.put("title", t.getTitle());
            m.put("priority", t.getPriority());
            m.put("status", t.getStatus());
            m.put("progress", t.getProgress());
            m.put("creator_id", t.getCreator_id());
            m.put("creator_name", nameOf(names, t.getCreator_id(), null));
            m.put("assignee_id", t.getAssignee_id());
            m.put("assignee_name", nameOf(names, t.getAssignee_id(), null));
            m.put("plan_start_time", t.getPlan_start_time());
            m.put("plan_end_time", t.getPlan_end_time());
            m.put("plan_work_hours", t.getPlan_work_hours());
            m.put("project_id", t.getProject_id());
            m.put("project_name", t.getProject_id() == null ? null : projectNames.get(t.getProject_id()));
            m.put("is_overdue", calcOverdue(t));
            m.put("dept_manageable", canManageTask(t, userId));
            m.put("created_at", t.getCreated_at());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", page.getTotal());
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    // 任务详情，成员、评论、附件、日志一起返回
    @GetMapping("/{id}")
    public Map<String, Object> detail(HttpServletRequest req, @PathVariable Long id) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canView(t, userId)) return err(403, "无权查看该任务");

        List<TaskMember> members = taskMemberRepository.selectList(
            new QueryWrapper<TaskMember>().eq("task_id", id));
        List<TaskComment> comments = taskCommentRepository.selectList(
            new QueryWrapper<TaskComment>().eq("task_id", id).orderByAsc("comment_time").orderByAsc("created_at"));
        List<Attachments> taskAtts = attachmentsRepository.selectList(
            new QueryWrapper<Attachments>().eq("target_type", "task").eq("target_id", id).orderByAsc("created_at"));
        List<TaskOperationLogs> logs = logRepository.selectList(
            new QueryWrapper<TaskOperationLogs>().eq("task_id", id).orderByDesc("operation_time").last("limit 50"));

        // 评论附件一次查完
        Set<Long> commentAttIds = new HashSet<>();
        for (TaskComment c : comments) {
            if (c.getAttachment_id() != null) commentAttIds.add(c.getAttachment_id());
        }
        Map<Long, Attachments> commentAttMap = new HashMap<>();
        if (!commentAttIds.isEmpty()) {
            for (Attachments a : attachmentsRepository.selectBatchIds(commentAttIds)) {
                commentAttMap.put(a.getId(), a);
            }
        }

        // 相关人员姓名一次查完，避免逐条查用户
        Set<Long> userIds = new HashSet<>();
        collectUserIds(userIds, t.getCreator_id(), t.getAssignee_id());
        for (TaskMember m : members) collectUserIds(userIds, m.getUser_id());
        for (TaskComment c : comments) collectUserIds(userIds, c.getUser_id());
        for (Attachments a : taskAtts) collectUserIds(userIds, a.getUploader_id());
        for (TaskOperationLogs l : logs) collectUserIds(userIds, l.getOperator_id());
        Map<Long, String> names = userNames(userIds);

        List<Map<String, Object>> memberList = members.stream().map(m -> {
            Map<String, Object> mm = new HashMap<>();
            mm.put("user_id", m.getUser_id());
            mm.put("real_name", nameOf(names, m.getUser_id(), "未知"));
            mm.put("member_role", m.getRole());
            return mm;
        }).collect(Collectors.toList());

        List<Map<String, Object>> commentList = comments.stream().map(c -> {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", c.getId());
            cm.put("user_id", c.getUser_id());
            cm.put("real_name", nameOf(names, c.getUser_id(), "未知"));
            cm.put("content", c.getContent());
            cm.put("parent_id", c.getParent_id() == null ? 0L : c.getParent_id());
            cm.put("mention_users", c.getMention_users());
            cm.put("comment_time", c.getComment_time());
            cm.put("created_at", c.getCreated_at());
            cm.put("attachment_id", c.getAttachment_id());
            Attachments ca = c.getAttachment_id() == null ? null : commentAttMap.get(c.getAttachment_id());
            if (ca != null) {
                Map<String, Object> cai = new HashMap<>();
                cai.put("id", ca.getId());
                cai.put("file_name", ca.getFile_name());
                cai.put("file_size", ca.getFile_size());
                cai.put("file_ext", ca.getFile_ext());
                cm.put("attachment", cai);
            }
            return cm;
        }).collect(Collectors.toList());

        List<Map<String, Object>> attachmentList = taskAtts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("file_name", a.getFile_name());
            m.put("file_size", a.getFile_size());
            m.put("file_ext", a.getFile_ext());
            m.put("created_at", a.getCreated_at());
            m.put("uploader_id", a.getUploader_id());
            m.put("uploader_name", nameOf(names, a.getUploader_id(), ""));
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> logList = logs.stream().map(l -> {
            Map<String, Object> lm = new HashMap<>();
            lm.put("action", l.getAction());
            lm.put("operator_name", nameOf(names, l.getOperator_id(), "系统"));
            lm.put("remark", l.getRemark());
            lm.put("operation_time", l.getOperation_time());
            return lm;
        }).collect(Collectors.toList());

        Map<String, Object> d = new HashMap<>();
        d.put("id", t.getId());
        d.put("task_no", t.getTask_no());
        d.put("title", t.getTitle());
        d.put("description", t.getDescription());
        d.put("task_type", t.getTask_type());
        d.put("priority", t.getPriority());
        d.put("status", t.getStatus());
        d.put("progress", t.getProgress());
        d.put("creator_id", t.getCreator_id());
        d.put("creator_name", nameOf(names, t.getCreator_id(), "未知"));
        d.put("assignee_id", t.getAssignee_id());
        d.put("assignee_name", nameOf(names, t.getAssignee_id(), "未指派"));
        d.put("project_id", t.getProject_id());
        d.put("project_name", t.getProject_id() == null || t.getProject_id() <= 0
            ? null : projectNamesOf(List.of(t)).get(t.getProject_id()));
        d.put("plan_start_time", t.getPlan_start_time());
        d.put("plan_end_time", t.getPlan_end_time());
        d.put("plan_work_hours", t.getPlan_work_hours());
        d.put("actual_start_time", t.getActual_start_time());
        d.put("actual_end_time", t.getActual_end_time());
        d.put("actual_work_hours", t.getActual_work_hours());
        d.put("accept_time", t.getAccept_time());
        d.put("submit_time", t.getSubmit_time());
        d.put("reject_reason", t.getReject_reason());
        d.put("complete_note", t.getComplete_note());
        d.put("tags", t.getTags());
        d.put("is_overdue", calcOverdue(t));
        d.put("archive_status", t.getArchive_status());
        d.put("dept_manageable", canManageTask(t, userId));
        d.put("members", memberList);
        d.put("comments", commentList);
        d.put("attachments", attachmentList);
        d.put("logs", logList);
        d.put("created_at", t.getCreated_at());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", d);
        return result;
    }

    // 创建任务，只有管理员或部门主管可以建
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        // 管理员、部门主管、持 task:add 的项目经理都能建任务
        if (!isAdmin(userId) && !isDeptManager(userId) && !hasPerm(userId, "task:add")) {
            return err(403, "仅管理员、部门主管或项目经理可创建任务");
        }
        try {
            String title = text(body.get("title"));
            title = title == null ? null : title.trim();
            if (title == null || title.isEmpty()) return err(400, "任务名称不能为空");

            Long assignee = parseLongOrNull(body.get("assignee_id"));
            List<Long> memberIds = parseIdList(body.get("member_ids"));
            // 停用账号接不了任务，派过去任务会一直卡着
            String activeErr = checkActive(assignee);
            if (activeErr != null) return err(400, activeErr);
            for (Long mid : memberIds) {
                String memberErr = checkActive(mid);
                if (memberErr != null) return err(400, "参与人" + memberErr);
            }

            Task t = new Task();
            t.setTask_no(genTaskNo());
            t.setTitle(title);
            t.setDescription(text(body.get("description")));
            // 只能建在自己负责或参与的项目下，这是项目经理的数据边界
            Long projectId = parseIdOrZero(body.get("project_id"));
            if (projectId != null && projectId > 0) {
                if (!projectScopeService.projectExists(projectId)) return err(400, "所选项目不存在或已删除");
                if (!isAdmin(userId) && !projectScopeService.canSeeProject(projectId, userId)) {
                    return err(403, "只能在自己负责或参与的项目下创建任务");
                }
            }
            t.setProject_id(projectId);
            t.setPriority(body.get("priority") == null ? 2 : Integer.valueOf(body.get("priority").toString()));
            t.setProgress(0);
            t.setIs_overdue(0);
            t.setArchive_status(0);
            t.setCreator_id(userId);
            t.setAssignee_id(assignee);
            // 有负责人是待接受，没负责人是待分配
            t.setStatus(assignee == null ? ST_PENDING : ST_WAIT_ACCEPT);
            t.setPlan_start_time(parseDateTime(body.get("plan_start_time")));
            t.setPlan_end_time(parseDateTime(body.get("plan_end_time")));
            if (t.getPlan_start_time() != null && t.getPlan_end_time() != null
                && t.getPlan_end_time().isBefore(t.getPlan_start_time())) {
                return err(400, "计划结束时间不能早于开始时间");
            }
            t.setPlan_work_hours(parseDecimal(body.get("plan_work_hours"), "计划工时"));

            // 部门主管只能派给本部门含下级的人；项目经理只能派给自己或项目成员；管理员不限
            if (!isAdmin(userId)) {
                List<Long> deptUserIds = managedDeptUserIds(userId);
                if (!deptUserIds.isEmpty()) {
                    if (assignee != null && !deptUserIds.contains(assignee)) {
                        return err(403, "只能给本部门（含下级）成员指派任务");
                    }
                    for (Long mid : memberIds) {
                        if (!mid.equals(userId) && !mid.equals(assignee) && !deptUserIds.contains(mid)) {
                            return err(403, "参与人须为本部门（含下级）成员");
                        }
                    }
                } else if (projectId != null && projectId > 0) {
                    List<Long> projectMembers = projectScopeService.projectMemberIds(projectId);
                    if (assignee != null && !assignee.equals(userId) && !projectMembers.contains(assignee)) {
                        return err(403, "项目经理只能把任务指派给自己或项目成员");
                    }
                    for (Long mid : memberIds) {
                        if (!mid.equals(userId) && !mid.equals(assignee) && !projectMembers.contains(mid)) {
                            return err(403, "参与人须为项目成员");
                        }
                    }
                }
            }

            taskRepository.insert(t);
            touchProject(projectId);

            if (assignee != null) addMember(t.getId(), assignee, ROLE_OWNER);
            // 创建人默认关注，自己当负责人时保留负责人身份
            if (!Objects.equals(userId, assignee)) addMember(t.getId(), userId, ROLE_WATCHER);

            for (Long mid : memberIds) {
                if (mid.equals(userId) || mid.equals(assignee)) continue;
                addMember(t.getId(), mid, ROLE_MEMBER);
                notify(mid, "您被加入任务：" + t.getTitle(), "/home/task?type=joined");
            }
            if (assignee != null) {
                // 派了任务就要能进任务中心，否则员工只看到通知打不开任务
                grantTaskAccess(assignee, userId, "被指派任务：" + t.getTitle());
                notifyTaskAssigned(assignee, t, userId);
            }
            for (Long mid : memberIds) {
                grantTaskAccess(mid, userId, "被加入任务：" + t.getTitle());
            }
            log(t.getId(), userId, "create", "创建任务", null, "状态=" + t.getStatus());

            Map<String, Object> d = new HashMap<>();
            d.put("id", t.getId());
            return Map.of("code", 200, "data", d);
        } catch (IllegalArgumentException e) {
            return err(400, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return err(500, "创建任务失败：" + e.getMessage());
        }
    }

    // 编辑任务，已完成、已撤销、已归档的不能改
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(HttpServletRequest req, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权编辑该任务（仅创建人、管理员或本部门主管可操作）");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        int st = statusOf(t);
        if (st == ST_COMPLETED || st == ST_REVOKED) return err(400, "已完成 / 已撤销的任务不可编辑");
        try {
            if (body.containsKey("title")) {
                String title = text(body.get("title"));
                title = title == null ? null : title.trim();
                if (title == null || title.isEmpty()) return err(400, "任务名称不能为空");
                t.setTitle(title);
            }
            if (body.containsKey("description")) t.setDescription(text(body.get("description")));
            if (body.get("priority") != null) t.setPriority(Integer.valueOf(body.get("priority").toString()));
            if (body.get("task_type") != null) t.setTask_type(Integer.valueOf(body.get("task_type").toString()));
            if (body.containsKey("tags")) t.setTags(text(body.get("tags")));
            if (body.containsKey("project_id")) t.setProject_id(parseIdOrZero(body.get("project_id")));
            if (body.containsKey("plan_start_time")) t.setPlan_start_time(parseDateTime(body.get("plan_start_time")));
            if (body.containsKey("plan_end_time")) t.setPlan_end_time(parseDateTime(body.get("plan_end_time")));
            if (t.getPlan_start_time() != null && t.getPlan_end_time() != null
                && t.getPlan_end_time().isBefore(t.getPlan_start_time())) {
                return err(400, "计划结束时间不能早于开始时间");
            }
            if (body.containsKey("plan_work_hours")) {
                t.setPlan_work_hours(parseDecimal(body.get("plan_work_hours"), "计划工时"));
            }
            taskRepository.updateById(t);

            // 传了 member_ids 才全量同步参与人
            if (body.containsKey("member_ids")) {
                syncMembers(id, t, parseIdList(body.get("member_ids")));
            }
            log(id, userId, "update", "编辑任务", null, null);
            return Map.of("code", 200, "message", "已保存");
        } catch (IllegalArgumentException e) {
            return err(400, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return err(500, "编辑任务失败：" + e.getMessage());
        }
    }

    // 全量同步参与人 role=2，不动负责人、创建人、关注人、验收人
    private void syncMembers(Long taskId, Task t, List<Long> targetIds) {
        Set<Long> target = new LinkedHashSet<>();
        for (Long mid : targetIds) {
            if (mid.equals(t.getCreator_id()) || mid.equals(t.getAssignee_id())) continue;
            target.add(mid);
        }
        List<TaskMember> existing = taskMemberRepository.selectList(
            new QueryWrapper<TaskMember>().eq("task_id", taskId).eq("role", ROLE_MEMBER));
        Set<Long> existingIds = new HashSet<>();
        for (TaskMember m : existing) existingIds.add(m.getUser_id());

        for (Long mid : target) {
            if (existingIds.contains(mid)) continue;
            addMember(taskId, mid, ROLE_MEMBER);
            grantTaskAccess(mid, t.getCreator_id(), "被加入任务：" + t.getTitle());
            notify(mid, "您被加入任务：" + t.getTitle(), "/home/task?type=joined");
        }
        for (TaskMember m : existing) {
            if (target.contains(m.getUser_id())) continue;
            taskMemberRepository.deleteById(m.getId());
            notify(m.getUser_id(), "您已被移出任务：" + t.getTitle(), "/home/task?type=joined");
        }
    }

    // 重新指派负责人，待分配、待接受、进行中、已拒绝时可以改派
    @PostMapping("/{id}/reassign")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reassign(HttpServletRequest req, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权指派该任务（仅创建人、管理员或本部门主管可操作）");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        int st = statusOf(t);
        if (st != ST_PENDING && st != ST_WAIT_ACCEPT && st != ST_IN_PROGRESS && st != ST_REJECTED) {
            return err(400, "当前状态不允许重新指派");
        }
        Long newAssignee = parseLongOrNull(body.get("assignee_id"));
        if (newAssignee == null) return err(400, "请选择负责人");
        String activeErr = checkActive(newAssignee);
        if (activeErr != null) return err(400, activeErr);
        if (!isAdmin(userId) && !managedDeptUserIds(userId).contains(newAssignee)) {
            return err(403, "只能改派给本部门（含下级）成员");
        }

        Integer oldStatus = t.getStatus();
        // 先清掉旧的负责人成员，再把新负责人加进来
        taskMemberRepository.delete(new QueryWrapper<TaskMember>().eq("task_id", id).eq("role", ROLE_OWNER));
        t.setAssignee_id(newAssignee);
        t.setStatus(ST_WAIT_ACCEPT);
        t.setReject_reason(null);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        addMember(id, newAssignee, ROLE_OWNER);

        grantTaskAccess(newAssignee, userId, "被改派任务：" + t.getTitle());
        notifyTaskAssigned(newAssignee, t, userId);
        log(id, userId, "assign", "重新指派负责人", String.valueOf(oldStatus), String.valueOf(ST_WAIT_ACCEPT));
        return Map.of("code", 200, "message", "已重新指派");
    }

    // 接受任务的写库动作，公开接口和看板拖动都调它，事务跟着调用方
    private Map<String, Object> applyAccept(Task t, Long userId) {
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(userId, t.getAssignee_id())) return err(403, "只能接受指派给您的任务");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_WAIT_ACCEPT) return err(400, "任务当前状态不可接受");

        Integer old = t.getStatus();
        LocalDateTime now = LocalDateTime.now();
        t.setStatus(ST_IN_PROGRESS);
        t.setAccept_time(now);
        if (t.getActual_start_time() == null) t.setActual_start_time(now);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        log(t.getId(), userId, "accept", "接受任务", String.valueOf(old), String.valueOf(ST_IN_PROGRESS));
        return Map.of("code", 200, "message", "已接受任务");
    }

    // 接受任务，待接受变成进行中
    @PostMapping("/{id}/accept")
    @RequirePermission("task:accept")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> accept(HttpServletRequest req, @PathVariable Long id) {
        return applyAccept(taskRepository.selectById(id), uid(req));
    }

    // 认领任务的写库动作
    private Map<String, Object> applyClaim(Task t, Long userId) {
        if (t == null) return err(404, "任务不存在");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_PENDING) return err(400, "任务已被认领或已指派");
        // 本部门的人才能认领，避免跨部门抢活
        if (!isGlobalViewer(userId) && !isDeptManagerOf(userId, t.getCreator_id()) && !sameOrg(userId, t.getCreator_id())) {
            return err(403, "只能认领本部门发布的任务");
        }

        Integer old = t.getStatus();
        LocalDateTime now = LocalDateTime.now();
        t.setAssignee_id(userId);
        t.setStatus(ST_IN_PROGRESS);
        t.setAccept_time(now);
        if (t.getActual_start_time() == null) t.setActual_start_time(now);
        // 带状态条件更新，避免两人同时认领互相覆盖
        int rows = taskRepository.update(t, new QueryWrapper<Task>().eq("id", t.getId()).eq("status", ST_PENDING));
        if (rows == 0) return err(400, "任务已被认领或已指派");
        touchProject(t.getProject_id());
        addMember(t.getId(), userId, ROLE_OWNER);
        if (t.getCreator_id() != null && !t.getCreator_id().equals(userId)) {
            notify(t.getCreator_id(), "任务被认领：" + t.getTitle(), "/home/task?type=created");
        }
        grantTaskAccess(userId, t.getCreator_id(), "认领任务：" + t.getTitle());
        log(t.getId(), userId, "claim", "认领任务", String.valueOf(old), String.valueOf(ST_IN_PROGRESS));
        return Map.of("code", 200, "message", "已认领任务");
    }

    // 认领任务，待分配的公开任务，认领人成为负责人
    @PostMapping("/{id}/claim")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("task:accept")
    public Map<String, Object> claim(HttpServletRequest req, @PathVariable Long id) {
        return applyClaim(taskRepository.selectById(id), uid(req));
    }

    // 拒绝任务，待接受变成已拒绝，同时把负责人摘掉
    @PostMapping("/{id}/reject")
    @RequirePermission("task:accept")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(HttpServletRequest req, @PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(userId, t.getAssignee_id())) return err(403, "只能拒绝指派给您的任务");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_WAIT_ACCEPT) return err(400, "任务当前状态不可拒绝");

        String reason = body == null ? null : body.get("reject_reason");
        // 兼容旧字段名 remark
        if ((reason == null || reason.trim().isEmpty()) && body != null) reason = body.get("remark");
        // 拒绝时必须写原因，否则交办人不知道问题在哪
        if (reason == null || reason.trim().isEmpty()) return err(400, "请填写拒绝原因");

        Integer old = t.getStatus();
        t.setStatus(ST_REJECTED);
        t.setAssignee_id(null);
        t.setReject_reason(reason);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        // 负责人成员一并移除，免得被拒后还能看到任务
        taskMemberRepository.delete(new QueryWrapper<TaskMember>().eq("task_id", id).eq("role", ROLE_OWNER));
        notify(t.getCreator_id(), "任务被拒绝：" + t.getTitle(), "/home/task?type=created");
        log(id, userId, "reject", "拒绝任务：" + (reason == null ? "" : reason),
            String.valueOf(old), String.valueOf(ST_REJECTED));
        return Map.of("code", 200, "message", "已拒绝");
    }

    // 更新进度，只有进行中的任务能改
    @PutMapping("/{id}/progress")
    @RequirePermission("task:update")
    public Map<String, Object> progress(HttpServletRequest req, @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(userId, t.getAssignee_id()) && !canManageTask(t, userId)) {
            return err(403, "无权限更新");
        }
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_IN_PROGRESS) return err(400, "仅进行中的任务可更新进度");
        if (body.get("progress") == null) return err(400, "请填写进度");
        int p;
        try {
            p = Integer.parseInt(body.get("progress").toString().trim());
        } catch (Exception e) {
            return err(400, "进度必须是数字");
        }
        if (p < 0 || p > 100) return err(400, "进度范围为 0-100");

        String workNote = body.get("work_note") == null ? null : body.get("work_note").toString().trim();
        Integer old = t.getProgress();
        t.setProgress(p);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        String remark = "更新进度为 " + p + "%";
        if (workNote != null && !workNote.isEmpty()) remark += "：" + workNote;
        log(id, userId, "update_progress", remark, String.valueOf(old), String.valueOf(p));
        return Map.of("code", 200, "message", "进度已更新");
    }

    // 提交完成的写库动作
    private Map<String, Object> applySubmit(Task t, Long userId, BigDecimal hours, String completeNote) {
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(userId, t.getAssignee_id())) return err(403, "只能提交自己负责的任务");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_IN_PROGRESS) return err(400, "仅进行中的任务可提交完成");
        if (completeNote == null || completeNote.isEmpty()) return err(400, "请填写完成说明");

        Integer old = t.getStatus();
        t.setStatus(ST_WAIT_APPROVE);
        t.setProgress(100);
        t.setSubmit_time(LocalDateTime.now());
        t.setComplete_note(completeNote);
        if (hours != null) t.setActual_work_hours(hours);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        notify(t.getCreator_id(), "任务待验收：" + t.getTitle(), "/home/task?type=created");
        log(t.getId(), userId, "submit", "提交验收", String.valueOf(old), String.valueOf(ST_WAIT_APPROVE));
        return Map.of("code", 200, "message", "已提交，等待验收");
    }

    // 提交完成，进行中变成待验收
    @PostMapping("/{id}/submit")
    @RequirePermission("task:submit")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(HttpServletRequest req, @PathVariable Long id,
                                      @RequestBody(required = false) Map<String, Object> body) {
        BigDecimal hours;
        try {
            hours = parseDecimal(body == null ? null : body.get("actual_work_hours"), "实际工时");
        } catch (IllegalArgumentException e) {
            return err(400, e.getMessage());
        }
        String completeNote = (body == null || body.get("complete_note") == null)
            ? null : body.get("complete_note").toString().trim();
        return applySubmit(taskRepository.selectById(id), uid(req), hours, completeNote);
    }

    // 验收通过的写库动作
    private Map<String, Object> applyApprove(Task t, Long userId) {
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权验收该任务（仅创建人、管理员或本部门主管可操作）");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_WAIT_APPROVE) return err(400, "仅待验收的任务可验收");

        Integer old = t.getStatus();
        t.setStatus(ST_COMPLETED);
        t.setProgress(100);
        t.setActual_end_time(LocalDateTime.now());
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        // 任务已完成，派发、提醒、逾期几条通知一起收掉，只留完成通知
        notificationBizService.clear("task", t.getId());
        if (t.getAssignee_id() != null) {
            notify(t.getAssignee_id(), "任务已验收通过：" + t.getTitle(), "/home/task?type=assigned");
        }
        log(t.getId(), userId, "approve", "验收通过", String.valueOf(old), String.valueOf(ST_COMPLETED));
        return Map.of("code", 200, "message", "已验收通过");
    }

    // 验收通过，待验收变成已完成
    @PostMapping("/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(HttpServletRequest req, @PathVariable Long id) {
        return applyApprove(taskRepository.selectById(id), uid(req));
    }

    // 验收驳回，待验收退回进行中
    @PostMapping("/{id}/reject-approve")
    public Map<String, Object> rejectApprove(HttpServletRequest req, @PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权验收该任务（仅创建人、管理员或本部门主管可操作）");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (statusOf(t) != ST_WAIT_APPROVE) return err(400, "仅待验收的任务可驳回");

        String reason = body == null ? null : body.get("reason");
        Integer old = t.getStatus();
        t.setStatus(ST_IN_PROGRESS);
        t.setProgress(99);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        if (t.getAssignee_id() != null) {
            notify(t.getAssignee_id(), "任务验收被驳回：" + t.getTitle()
                + (reason == null || reason.trim().isEmpty() ? "" : "（" + reason + "）"), "/home/task?type=assigned");
        }
        log(id, userId, "reject_approve", "验收驳回：" + (reason == null ? "" : reason),
            String.valueOf(old), String.valueOf(ST_IN_PROGRESS));
        return Map.of("code", 200, "message", "已驳回，任务退回进行中");
    }

    // 撤销任务的写库动作
    private Map<String, Object> applyRevoke(Task t, Long userId) {
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权撤销该任务（仅创建人、管理员或本部门主管可操作）");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        int st = statusOf(t);
        if (st == ST_COMPLETED || st == ST_REVOKED || st == ST_REJECTED) {
            return err(400, "已完成 / 已撤销 / 已拒绝的任务不可撤销");
        }

        Integer old = t.getStatus();
        t.setStatus(ST_REVOKED);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        if (t.getAssignee_id() != null) {
            notify(t.getAssignee_id(), "任务已撤销：" + t.getTitle(), "/home/task?type=assigned");
        }
        log(t.getId(), userId, "revoke", "撤销任务", String.valueOf(old), String.valueOf(ST_REVOKED));
        return Map.of("code", 200, "message", "任务已撤销");
    }

    // 撤销任务
    @PostMapping("/{id}/revoke")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revoke(HttpServletRequest req, @PathVariable Long id) {
        return applyRevoke(taskRepository.selectById(id), uid(req));
    }

    // 添加成员，负责人只能通过指派、改派设置
    @PostMapping("/{id}/members")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addTaskMember(HttpServletRequest req, @PathVariable Long id,
                                             @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(userId, t.getAssignee_id()) && !canManageTask(t, userId)) {
            return err(403, "无权限添加成员");
        }
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        int st = statusOf(t);
        if (st == ST_COMPLETED || st == ST_REVOKED) return err(400, "任务已结束，不可添加成员");
        Object u = body.get("user_id");
        if (u == null || u.toString().isEmpty()) return err(400, "请选择成员");
        Long targetUserId;
        try {
            targetUserId = Long.valueOf(u.toString());
        } catch (Exception e) {
            return err(400, "成员不合法");
        }
        Users target = usersRepository.selectById(targetUserId);
        if (target == null) return err(400, "用户不存在");
        String activeErr = checkActive(targetUserId);
        if (activeErr != null) return err(400, activeErr);
        int role = body.get("role") == null ? ROLE_MEMBER : Integer.valueOf(body.get("role").toString());
        if (role == ROLE_OWNER) return err(400, "负责人请通过指派/改派设置");
        if (role != ROLE_MEMBER && role != ROLE_WATCHER && role != ROLE_CHECKER) {
            return err(400, "成员角色不合法");
        }
        addMember(id, targetUserId, role);
        grantTaskAccess(targetUserId, userId, "被加入任务：" + t.getTitle());
        notify(targetUserId, "您被加入任务：" + t.getTitle(), "/home/task?type=joined");
        log(id, userId, "add_member", "添加成员", null, "用户=" + targetUserId + " 角色=" + role);
        return Map.of("code", 200, "message", "已添加成员");
    }

    // 移除成员，创建人和负责人不能这样移除
    // 删除任务：管理员可删任意任务，其他人只能删还没开始的（待分配、已撤销、已拒绝）
    @DeleteMapping("/{id}")
    @RequirePermission("task:delete")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = uid(request);
        Task task = taskRepository.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        boolean admin = isAdmin(userId);
        if (!admin && !canManageTask(task, userId)) {
            return Result.error(403, "无权删除该任务");
        }
        int status = task.getStatus() == null ? ST_PENDING : task.getStatus();
        if (!admin && status != ST_PENDING && status != ST_REVOKED && status != ST_REJECTED) {
            return Result.error("任务已开始执行，只有管理员可以删除");
        }
        // 成员和评论一起标记删除，避免详情里留下孤儿数据
        jdbc.update("UPDATE task_members SET deleted_at=NOW() WHERE task_id=? AND deleted_at IS NULL", id);
        jdbc.update("UPDATE task_comments SET deleted_at=NOW() WHERE task_id=? AND deleted_at IS NULL", id);
        taskRepository.deleteById(id);
        touchProject(task.getProject_id());
        return Result.success(null);
    }

    @DeleteMapping("/{id}/members/{memberUserId}")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeTaskMember(HttpServletRequest req, @PathVariable Long id,
                                                @PathVariable("memberUserId") Long memberUserId) {
        Long operatorId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!Objects.equals(operatorId, t.getAssignee_id()) && !canManageTask(t, operatorId)) {
            return err(403, "无权限移除成员");
        }
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (memberUserId.equals(t.getCreator_id())) return err(400, "不能移除任务创建人");
        TaskMember m = taskMemberRepository.selectOne(
            new QueryWrapper<TaskMember>().eq("task_id", id).eq("user_id", memberUserId));
        if (m == null) return err(404, "该成员不在任务中");
        if (m.getRole() != null && m.getRole() == ROLE_OWNER) return err(400, "负责人请通过改派更换");
        taskMemberRepository.deleteById(m.getId());
        notify(memberUserId, "您已被移出任务：" + t.getTitle(), "/home/task?type=joined");
        log(id, operatorId, "remove_member", "移除成员", null, "用户=" + memberUserId);
        return Map.of("code", 200, "message", "已移除成员");
    }

    // 看板，按状态分列返回
    @GetMapping("/board")
    public Map<String, Object> board(HttpServletRequest req,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer priority) {
        Long userId = uid(req);
        boolean admin = isGlobalViewer(userId);
        QueryWrapper<Task> qw = new QueryWrapper<>();
        qw.eq("archive_status", 0);
        applyVisibleScope(qw, userId, admin, true);
        qw.in("status", ST_PENDING, ST_WAIT_ACCEPT, ST_IN_PROGRESS, ST_WAIT_APPROVE, ST_COMPLETED);
        if (keyword != null && !keyword.isEmpty()) qw.like("title", keyword);
        if (priority != null) qw.eq("priority", priority);
        qw.orderByDesc("priority").orderByDesc("created_at");
        List<Task> all = taskRepository.selectList(qw);
        Map<Long, String> names = namesOfTasks(all);

        Map<String, List<Map<String, Object>>> columns = new LinkedHashMap<>();
        for (int s = 0; s <= 4; s++) columns.put(String.valueOf(s), new ArrayList<>());
        for (Task t : all) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("task_no", t.getTask_no());
            m.put("priority", t.getPriority());
            m.put("status", t.getStatus());
            m.put("progress", t.getProgress());
            m.put("creator_id", t.getCreator_id());
            m.put("creator_name", nameOf(names, t.getCreator_id(), null));
            m.put("assignee_id", t.getAssignee_id());
            m.put("assignee_name", nameOf(names, t.getAssignee_id(), null));
            m.put("plan_end_time", t.getPlan_end_time());
            m.put("is_overdue", calcOverdue(t));
            m.put("dept_manageable", canManageTask(t, userId));
            List<Map<String, Object>> col = columns.get(String.valueOf(t.getStatus()));
            if (col != null) col.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("columns", columns);
        return Map.of("code", 200, "data", data);
    }

    // 统计：总数、状态分布、完成率、逾期率、工时和偏差
    @GetMapping("/statistics")
    public Map<String, Object> statistics(HttpServletRequest req) {
        Long userId = uid(req);
        boolean admin = isGlobalViewer(userId);
        QueryWrapper<Task> qw = new QueryWrapper<>();
        qw.eq("archive_status", 0);
        // 统计不把待分配的任务算进来
        applyVisibleScope(qw, userId, admin, false);
        List<Task> all = taskRepository.selectList(qw);

        int total = all.size();
        int[] statusCount = new int[7];
        int finished = 0, overdue = 0, planCnt = 0, actualCnt = 0, diffCnt = 0;
        BigDecimal planSum = BigDecimal.ZERO, actualSum = BigDecimal.ZERO, diffSum = BigDecimal.ZERO;
        for (Task t : all) {
            int st = statusOf(t);
            if (st >= 0 && st <= 6) statusCount[st]++;
            if (st == ST_COMPLETED) finished++;
            if (calcOverdue(t)) overdue++;
            if (t.getPlan_work_hours() != null) {
                planSum = planSum.add(t.getPlan_work_hours());
                planCnt++;
            }
            if (t.getActual_work_hours() != null) {
                actualSum = actualSum.add(t.getActual_work_hours());
                actualCnt++;
                if (t.getPlan_work_hours() != null) {
                    diffSum = diffSum.add(t.getActual_work_hours().subtract(t.getPlan_work_hours()));
                    diffCnt++;
                }
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("status_count", statusCount);
        data.put("finished", finished);
        data.put("completion_rate", total == 0 ? 0 : Math.round(finished * 1000.0 / total) / 10.0);
        data.put("overdue", overdue);
        data.put("overdue_rate", total == 0 ? 0 : Math.round(overdue * 1000.0 / total) / 10.0);
        data.put("avg_plan_hours", planCnt == 0 ? BigDecimal.ZERO
            : planSum.divide(BigDecimal.valueOf(planCnt), 2, RoundingMode.HALF_UP));
        data.put("avg_actual_hours", actualCnt == 0 ? BigDecimal.ZERO
            : actualSum.divide(BigDecimal.valueOf(actualCnt), 2, RoundingMode.HALF_UP));
        data.put("avg_hour_diff", diffCnt == 0 ? BigDecimal.ZERO
            : diffSum.divide(BigDecimal.valueOf(diffCnt), 2, RoundingMode.HALF_UP));
        return Map.of("code", 200, "data", data);
    }

    // 看板拖拽自己带事务，直接调下面的私有方法
    // 不调公开状态接口，免得类内自调用绕过 @Transactional 代理
    @PostMapping("/{id}/move")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> move(HttpServletRequest req, @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (body == null || body.get("target_status") == null) return err(400, "缺少目标状态");
        int target;
        try {
            target = Integer.parseInt(body.get("target_status").toString());
        } catch (NumberFormatException e) {
            return err(400, "目标状态非法");
        }
        int cur = statusOf(t);
        if (target == cur) return Map.of("code", 200, "message", "状态未变化");

        if (target == ST_IN_PROGRESS) {
            if (cur == ST_PENDING) return applyClaim(t, userId);
            if (cur == ST_WAIT_ACCEPT) return applyAccept(t, userId);
            return err(400, "当前状态无法移动到进行中");
        }
        if (target == ST_WAIT_APPROVE) {
            if (cur != ST_IN_PROGRESS) return err(400, "仅进行中任务可提交验收");
            BigDecimal hours;
            try {
                hours = parseDecimal(body.get("actual_work_hours"), "实际工时");
            } catch (IllegalArgumentException e) {
                return err(400, e.getMessage());
            }
            Object note = body.get("complete_note");
            String completeNote = note == null ? null : note.toString().trim();
            return applySubmit(t, userId, hours, completeNote);
        }
        if (target == ST_COMPLETED) {
            if (cur != ST_WAIT_APPROVE) return err(400, "仅待验收任务可验收");
            return applyApprove(t, userId);
        }
        if (target == ST_REVOKED) return applyRevoke(t, userId);
        return err(400, "不支持的看板流转");
    }

    // 手动归档，只有已完成、已撤销、已拒绝的能归档
    @PostMapping("/{id}/archive")
    public Map<String, Object> archive(HttpServletRequest req, @PathVariable Long id) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权归档该任务（仅创建人、管理员或本部门主管可操作）");
        int st = statusOf(t);
        if (st != ST_COMPLETED && st != ST_REVOKED && st != ST_REJECTED) {
            return err(400, "仅已完成/已撤销/已拒绝的任务可归档");
        }
        if (archived(t)) return Map.of("code", 200, "message", "任务已归档");
        t.setArchive_status(1);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        log(id, userId, "archive", "任务归档", null, null);
        return Map.of("code", 200, "message", "任务已归档");
    }

    // 恢复归档，只有管理员能操作
    @PostMapping("/{id}/unarchive")
    public Map<String, Object> unarchive(HttpServletRequest req, @PathVariable Long id) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!isAdmin(userId)) return err(403, "仅管理员可恢复归档任务");
        if (!archived(t)) return Map.of("code", 200, "message", "任务未归档");
        t.setArchive_status(0);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        log(id, userId, "unarchive", "恢复归档任务", null, null);
        return Map.of("code", 200, "message", "任务已恢复");
    }

    // 撤销恢复，已撤销的任务重新激活，有负责人回待接受，没有回待分配
    @PostMapping("/{id}/restore")
    public Map<String, Object> restore(HttpServletRequest req, @PathVariable Long id) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canManageTask(t, userId)) return err(403, "无权恢复该任务（仅创建人、管理员或本部门主管可操作）");
        if (statusOf(t) != ST_REVOKED) return err(400, "仅已撤销任务可恢复");
        if (archived(t)) return err(400, "已归档任务请先恢复归档");
        int target = t.getAssignee_id() == null ? ST_PENDING : ST_WAIT_ACCEPT;
        t.setStatus(target);
        t.setReject_reason(null);
        taskRepository.updateById(t);
        touchProject(t.getProject_id());
        log(id, userId, "restore", "恢复已撤销任务", null, null);
        if (target == ST_WAIT_ACCEPT) {
            notify(t.getAssignee_id(), "任务「" + t.getTitle() + "」已恢复，待您接受", "/home/task?type=assigned");
        }
        return Map.of("code", 200, "message", "任务已恢复");
    }

    // 添加评论，评论附件上传时是游离的，这里绑定到评论上
    @PostMapping("/{id}/comments")
    @RequirePermission("task:comment")
    public Map<String, Object> addComment(HttpServletRequest req, @PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (archived(t)) return err(400, "任务已归档，为只读状态");
        if (!canCollaborate(t, userId)) return err(403, "仅任务相关人员可评论该任务");

        String content = body.get("content") == null ? null : body.get("content").toString().trim();
        if (content == null || content.isEmpty()) return err(400, "评论内容不能为空");

        TaskComment c = new TaskComment();
        c.setTask_id(id);
        c.setUser_id(userId);
        c.setParent_id(parseIdOrZero(body.get("parent_id")));
        c.setMention_users(body.get("mention_users") == null ? null : body.get("mention_users").toString());
        c.setContent(content);
        c.setComment_time(LocalDateTime.now());
        c.setIs_edited(0);
        taskCommentRepository.insert(c);

        // 绑定附件，只认自己上传的、还没绑定的评论附件
        Object attId = body.get("attachment_id");
        if (attId != null && !attId.toString().isEmpty()) {
            try {
                Attachments att = attachmentsRepository.selectById(Long.valueOf(attId.toString()));
                if (att != null && Objects.equals(userId, att.getUploader_id())
                    && "task_comment".equals(att.getTarget_type())) {
                    c.setAttachment_id(att.getId());
                    taskCommentRepository.updateById(c);
                    att.setTarget_id(c.getId());
                    attachmentsRepository.updateById(att);
                }
            } catch (Exception ignored) {
            }
        }

        // 给被 @ 的人发通知，自己跳过
        if (c.getMention_users() != null && !c.getMention_users().trim().isEmpty()) {
            for (String s : c.getMention_users().split(",")) {
                try {
                    Long mentioned = Long.valueOf(s.trim());
                    if (!mentioned.equals(userId)) {
                        notify(mentioned, "有人在任务「" + t.getTitle() + "」中提到您", "/home/task?type=assigned");
                    }
                } catch (Exception ignored) {
                }
            }
        }
        log(id, userId, "comment", "发表评论", null, null);
        return Map.of("code", 200, "message", "评论成功");
    }

    // 删除评论，管理员或评论本人
    @DeleteMapping("/{id}/comments/{cid}")
    public Map<String, Object> deleteComment(HttpServletRequest req, @PathVariable Long id, @PathVariable Long cid) {
        Long userId = uid(req);
        Task t = taskRepository.selectById(id);
        if (t == null) return err(404, "任务不存在");
        if (!canView(t, userId)) return err(403, "无权操作该任务");
        TaskComment c = taskCommentRepository.selectById(cid);
        if (c == null || !id.equals(c.getTask_id())) return err(404, "评论不存在");
        if (archived(t)) return err(400, "任务已归档，不可操作");
        if (!isAdmin(userId) && !Objects.equals(userId, c.getUser_id())) {
            return err(403, "仅管理员或评论本人可删除");
        }
        taskCommentRepository.deleteById(cid);
        log(id, userId, "delete_comment", "删除评论", null, null);
        return Map.of("code", 200, "message", "评论已删除");
    }

    // 上传附件，带 taskId 是任务附件最多 3 个，不带是评论附件，提交评论时再绑
    @PostMapping("/attachments/upload")
    @RequirePermission({"task:update", "task:comment"})
    public Map<String, Object> uploadAttachment(HttpServletRequest req,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "taskId", required = false) Long taskId) {
        Long userId = uid(req);
        if (file == null || file.isEmpty()) return err(400, "文件为空");
        if (file.getSize() > 20L * 1024 * 1024) return err(400, "文件大小超过20MB，请压缩后上传");

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTS.contains(ext)) return err(400, "该文件格式不允许上传");
        // 后缀能改，按文件头确认类型，防止可执行文件伪装
        if (!typeMatchesExt(file, ext)) return err(400, "文件类型与扩展名不匹配");

        Task bindTask = null;
        if (taskId != null) {
            bindTask = taskRepository.selectById(taskId);
            if (bindTask == null) return err(404, "任务不存在");
            if (!canCollaborate(bindTask, userId)) return err(403, "仅任务相关人员可上传该任务附件");
            int st = statusOf(bindTask);
            if (st == ST_COMPLETED || st == ST_REVOKED || st == ST_REJECTED) {
                return err(400, "任务已结束，不可再上传附件");
            }
            Long cnt = attachmentsRepository.selectCount(new QueryWrapper<Attachments>()
                .eq("target_type", "task").eq("target_id", taskId));
            if (cnt != null && cnt >= MAX_TASK_ATTACH) {
                return err(400, "每个任务最多上传 " + MAX_TASK_ATTACH + " 个附件");
            }
        }

        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(uploadDir + File.separator + "task" + File.separator + dateDir);
        if (!dir.exists()) dir.mkdirs();
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dest = new File(dir, storedName);
        try {
            Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            return err(400, "上传失败：" + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Attachments att = new Attachments();
        att.setFile_name(original);
        att.setFile_path(dest.getAbsolutePath());
        att.setFile_size(file.getSize());
        att.setFile_type(file.getContentType());
        att.setFile_ext("." + ext);
        att.setUploader_id(userId);
        att.setStorage_type(1);
        att.setDownload_count(0);
        if (bindTask != null) {
            att.setTarget_type("task");
            att.setTarget_id(taskId);
        } else {
            att.setTarget_type("task_comment");
        }
        att.setCreated_at(now);
        att.setUpdated_at(now);
        try {
            attachmentsRepository.insert(att);
        } catch (Exception e) {
            // 写库失败就把刚存的文件删掉
            if (dest.exists()) dest.delete();
            throw new RuntimeException("附件保存失败，请稍后重试");
        }

        Map<String, Object> r = new HashMap<>();
        r.put("id", att.getId());
        r.put("fileName", att.getFile_name());
        r.put("fileSize", att.getFile_size());
        r.put("fileExt", att.getFile_ext());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", r);
        return result;
    }

    // 按文件头判断类型，纯文本和代码没有文件头，直接放行
    private boolean typeMatchesExt(MultipartFile file, String ext) {
        if ("txt".equals(ext) || "md".equals(ext) || "java".equals(ext)
                || "js".equals(ext) || "ts".equals(ext) || "py".equals(ext)) {
            return true;
        }
        byte[] head = new byte[8];
        int len;
        try (InputStream in = file.getInputStream()) {
            len = in.read(head);
        } catch (IOException e) {
            return false;
        }
        if (len < 2) return false;
        switch (ext) {
            case "pdf":
                return len >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            case "docx":
            case "xlsx":
            case "pptx":
            case "zip":
                return head[0] == 'P' && head[1] == 'K';
            case "rar":
                return len >= 4 && head[0] == 'R' && head[1] == 'a' && head[2] == 'r';
            case "doc":
            case "xls":
            case "ppt":
                return len >= 4 && head[0] == (byte) 0xD0 && head[1] == (byte) 0xCF
                        && head[2] == (byte) 0x11 && head[3] == (byte) 0xE0;
            case "png":
                return len >= 4 && head[0] == (byte) 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "jpg":
            case "jpeg":
                return len >= 3 && head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF;
            case "gif":
                return len >= 4 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8';
            case "bmp":
                return head[0] == 'B' && head[1] == 'M';
            default:
                return false;
        }
    }

    // 下载失败输出 JSON 错误体，前端能弹提示
    private void writeDownloadError(HttpServletResponse response, int code, String message) {
        try {
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error(code, message)));
        } catch (Exception ignored) {
        }
    }

    // 下载附件，按附件所属任务判断能不能看
    @GetMapping("/attachments/{aid}/download")
    public void downloadAttachment(HttpServletRequest req, @PathVariable Long aid, HttpServletResponse response) {
        Long userId = uid(req);
        Attachments att = attachmentsRepository.selectById(aid);
        if (att == null) { writeDownloadError(response, 404, "附件不存在"); return; }
        String filePath = att.getFile_path();
        if (filePath == null || filePath.isBlank()) {
            writeDownloadError(response, 404, "附件文件路径为空，请联系管理员");
            return;
        }

        Long taskId = null;
        if ("task".equals(att.getTarget_type()) && att.getTarget_id() != null) {
            taskId = att.getTarget_id();
        } else if ("task_comment".equals(att.getTarget_type()) && att.getTarget_id() != null) {
            // 评论附件通过评论找到所属任务
            TaskComment c = taskCommentRepository.selectById(att.getTarget_id());
            if (c != null) taskId = c.getTask_id();
        }
        if (taskId != null) {
            Task t = taskRepository.selectById(taskId);
            if (t == null || !canView(t, userId)) { writeDownloadError(response, 403, "无权下载该附件"); return; }
        } else if (!Objects.equals(userId, att.getUploader_id())) {
            // 还没绑任务的游离附件只有上传人能下
            writeDownloadError(response, 403, "无权下载该附件");
            return;
        }

        File f = new File(filePath);
        // 文件和可读性在写响应头前判断，出错还能返回 JSON
        if (!f.isFile() || !f.canRead()) {
            writeDownloadError(response, 404, "文件已丢失或无法读取，请联系管理员");
            return;
        }
        try {
            att.setDownload_count((att.getDownload_count() == null ? 0 : att.getDownload_count()) + 1);
            attachmentsRepository.updateById(att);
        } catch (Exception e) {
            writeDownloadError(response, 500, "下载失败：" + e.getMessage());
            return;
        }
        try {
            response.setContentType("application/octet-stream");
            String enc = URLEncoder.encode(att.getFile_name() == null ? "file" : att.getFile_name(),
                StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + enc + "\"; filename*=UTF-8''" + enc);
            response.setContentLengthLong(f.length());
            Files.copy(f.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            // 响应头已发出、文件可能写了一半，这里拼不出 JSON，抛出去让全局异常标 500
            throw new RuntimeException("下载失败：" + e.getMessage());
        }
    }

    // 删除任务附件，创建人、上传人、管理员都能删
    @DeleteMapping("/attachments/{aid}")
    public Map<String, Object> deleteAttachment(HttpServletRequest req, @PathVariable Long aid) {
        Long userId = uid(req);
        Attachments att = attachmentsRepository.selectById(aid);
        if (att == null) return err(404, "附件不存在");
        if (!"task".equals(att.getTarget_type()) || att.getTarget_id() == null) {
            return err(400, "仅任务附件可在此删除");
        }
        Long taskId = att.getTarget_id();
        Task t = taskRepository.selectById(taskId);
        if (t == null) return err(404, "任务不存在");
        if (archived(t)) return err(400, "任务已归档，附件不可删除");
        if (!Objects.equals(userId, t.getCreator_id()) && !Objects.equals(userId, att.getUploader_id())
            && !isAdmin(userId)) {
            return err(403, "无权删除该附件");
        }
        attachmentsRepository.deleteById(aid);
        // 顺手删磁盘文件，删不掉不影响主流程
        try {
            String fp = att.getFile_path();
            if (fp != null && !fp.isBlank()) {
                File diskFile = new File(fp);
                if (diskFile.exists()) diskFile.delete();
            }
        } catch (Exception ignored) {
        }
        log(taskId, userId, "delete_attachment", "删除附件：" + att.getFile_name(), null, null);
        return Map.of("code", 200, "message", "附件已删除");
    }

    // 发任务通知，失败不影响主流程
    private void notify(Long userId, String content, String link) {
        notifyWithTitle(userId, "任务通知", content, link);
    }

    // 按指定标题与正文发通知，模板渲染结果从这里落库
    private void notifyWithTitle(Long userId, String title, String content, String link) {
        if (userId == null) return;
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle(title == null || title.isBlank() ? "任务通知" : title);
            n.setContent(content);
            n.setType(2);
            n.setBiz_type("task");
            n.setJump_url(link);
            n.setIs_read(0);
            n.setCreated_at(LocalDateTime.now());
            notificationRepository.insert(n);
        } catch (Exception ignored) {
        }
    }

    // 指派通知取 task_assign 模板，负责人能看到谁派的、什么时候要交
    private void notifyTaskAssigned(Long userId, Task task, Long assignerId) {
        if (userId == null || task == null) return;
        String assigner = displayName(assignerId);
        String planEnd = task.getPlan_end_time() == null ? "未设置"
                : task.getPlan_end_time().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Map<String, String> vars = new HashMap<>();
        if (task.getTitle() != null) vars.put("title", task.getTitle());
        if (assigner != null) vars.put("assigner", assigner);
        vars.put("planEndTime", planEnd);
        com.officeautomationsystemapi.service.NotificationTemplateService.Rendered message =
                notificationTemplateService.render("task_assign", vars,
                        "任务通知", "您有新任务：" + task.getTitle());
        notifyWithTitle(userId, message.title(), message.content(), "/home/task?type=assigned");
    }

    // 取用户显示名，取不到返回 null，模板会用自己的默认文案
    private String displayName(Long userId) {
        if (userId == null) return null;
        try {
            Users user = usersRepository.selectById(userId);
            if (user == null) return null;
            return user.getReal_name() == null || user.getReal_name().isBlank()
                    ? user.getUsername() : user.getReal_name();
        } catch (Exception e) {
            return null;
        }
    }

    // 判断用户是否持有某权限码，用于按权限码开放的操作
    private boolean hasPerm(Long userId, String code) {
        if (userId == null || code == null) return false;
        try {
            Set<String> perms = permissionService.getUserPermissions(userId);
            return perms != null && perms.contains(code);
        } catch (Exception e) {
            return false;
        }
    }

    // 负责人、参与人必须是启用中的账号
    private String checkActive(Long userId) {
        if (userId == null) return null;
        Users u = usersRepository.selectById(userId);
        if (u == null) return "账号不存在";
        if (u.getStatus() == null || u.getStatus() != 1) return "账号已停用，请选择启用中的员工";
        return null;
    }

    // 批量取任务所属项目名称，列表和详情都要显示属于哪个项目
    private Map<Long, String> projectNamesOf(List<Task> tasks) {
        Set<Long> ids = new HashSet<>();
        for (Task t : tasks) {
            if (t.getProject_id() != null && t.getProject_id() > 0) ids.add(t.getProject_id());
        }
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) return names;
        try {
            String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name FROM projects WHERE id IN (" + in + ")");
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                Object name = row.get("name");
                if (id instanceof Number) names.put(((Number) id).longValue(), name == null ? null : name.toString());
            }
        } catch (Exception ignored) {
        }
        return names;
    }
}
