package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.ProjectMember;
import com.officeautomationsystemapi.entity.Projects;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.ProjectMemberRepository;
import com.officeautomationsystemapi.repository.ProjectsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.ProjectProgressService;
import com.officeautomationsystemapi.service.ProjectScopeService;
import com.officeautomationsystemapi.service.UserPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// 项目管理：立项、成员、任务汇总、验收结项
// 数据范围：管理员和总经理看全部，其他人只看自己负责或参与的
@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    @Autowired
    private ProjectsRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private NotificationRecordsRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectScopeService projectScopeService;

    @Autowired
    private UserPermissionService userPermissionService;

    @Autowired
    private ProjectProgressService projectProgressService;

    // 项目状态：0 未启动 1 进行中 2 已暂停 3 已完成 4 已取消
    private static final int PS_DRAFT = 0;
    private static final int PS_RUNNING = 1;
    private static final int PS_PAUSED = 2;
    private static final int PS_DONE = 3;
    private static final int PS_CANCELED = 4;

    // 项目内角色：1 项目经理 2 成员 3 关注人
    private static final int PM_ROLE_OWNER = 1;
    private static final int PM_ROLE_MEMBER = 2;

    // 任务状态 4 完成 5 撤销 6 拒绝，都算已结束的任务
    private static final String CLOSED_TASK_STATUS = "4,5,6";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 项目列表：项目经理只看自己负责或参与的，管理员和总经理看全部
    @GetMapping
    @RequirePermission("project:list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Long managerId,
            HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        boolean global = isGlobal(userId);

        QueryWrapper<Projects> wrapper = new QueryWrapper<>();
        if (!global) {
            // 没参与任何项目时直接返回空页，避免拼出 IN () 报错
            List<Long> visible = projectScopeService.visibleProjectIds(userId);
            if (visible.isEmpty()) return Result.success(emptyPage(page, size));
            wrapper.in("id", visible);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("name", kw).or().like("project_no", kw));
        }
        if (status != null) wrapper.eq("status", status);
        if (priority != null) wrapper.eq("priority", priority);
        if (managerId != null) wrapper.eq("manager_id", managerId);
        wrapper.orderByDesc("id");
        Page<Projects> result = projectRepository.selectPage(new Page<>(page, size), wrapper);

        List<Long> ids = new ArrayList<>();
        Set<Long> userIds = new HashSet<>();
        for (Projects p : result.getRecords()) {
            ids.add(p.getId());
            userIds.add(p.getManager_id());
            userIds.add(p.getCreator_id());
        }
        Map<Long, int[]> stats = taskStats(ids);
        Map<Long, Integer> memberCounts = memberCounts(ids);
        Map<Long, String> names = userNames(userIds);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Projects p : result.getRecords()) {
            // 进度现算现写，免得任务改了进度列表还是旧数
            p.setProgress(projectProgressService.refresh(p.getId()));
            Map<String, Object> row = projectRow(p, names, stats, memberCounts);
            row.put("can_manage", canManageProject(p, userId));
            rows.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", rows);
        data.put("total", result.getTotal());
        data.put("pageNum", page);
        data.put("pageSize", size);
        return Result.success(data);
    }

    // 下拉选项：建任务时选所属项目，只给能看到的项目
    @GetMapping("/options")
    @RequirePermission({"project:list", "task:add"})
    public Result<List<Map<String, Object>>> options(HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (isGlobal(userId)) {
            List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT id, project_no, name, status FROM projects WHERE deleted_at IS NULL AND status IN (0,1,2)"
                    + " ORDER BY id DESC LIMIT 200");
            for (Map<String, Object> r : all) {
                rows.add(optionRow(r));
            }
            return Result.success(rows);
        }
        List<Long> visible = projectScopeService.visibleProjectIds(userId);
        if (visible.isEmpty()) return Result.success(rows);
        List<Map<String, Object>> mine = jdbc.queryForList(
            "SELECT id, project_no, name, status FROM projects WHERE deleted_at IS NULL AND status IN (0,1,2)"
                + " AND id IN (" + placeholders(visible.size()) + ") ORDER BY id DESC", visible.toArray());
        for (Map<String, Object> r : mine) {
            rows.add(optionRow(r));
        }
        return Result.success(rows);
    }

    // 加成员时的候选人下拉，项目经理可以从全公司挑人
    @GetMapping("/member-options")
    @RequirePermission({"project:member:manage", "project:add"})
    public Result<List<Map<String, Object>>> memberOptions() {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> users = jdbc.queryForList(
            "SELECT u.id, u.username, u.real_name, o.org_name FROM users u"
                + " LEFT JOIN organizations o ON o.id = u.org_id"
                + " WHERE u.status = 1 AND u.deleted_at IS NULL ORDER BY u.id");
        for (Map<String, Object> u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", ((Number) u.get("id")).longValue());
            row.put("username", u.get("username"));
            row.put("real_name", u.get("real_name"));
            row.put("org_name", u.get("org_name"));
            rows.add(row);
        }
        return Result.success(rows);
    }

    // 成员的任务中心权限，项目经理据此开通或回收，角色自带的也会返回
    @GetMapping("/{id}/member-task-access")
    @RequirePermission({"project:member:manage", "task:grant", "project:list"})
    public Result<List<Map<String, Object>>> memberTaskAccess(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) {
            return Result.error(403, "无权限查看成员任务权限：只有项目经理或管理员可以");
        }
        // 角色自带 task:access 的人：管理员、部门主管、总经理、项目经理
        Set<Long> granted = new HashSet<>(jdbc.queryForList(
            "SELECT DISTINCT ur.user_id FROM user_roles ur"
                + " JOIN role_permissions rp ON rp.role_id = ur.role_id AND rp.deleted_at IS NULL"
                + " JOIN menus m ON m.id = rp.menu_id AND m.deleted_at IS NULL"
                + " WHERE m.menu_code = 'task:access' AND ur.deleted_at IS NULL", Long.class));
        // 单独发给某个人的，多为派任务时自动开通
        granted.addAll(jdbc.queryForList(
            "SELECT up.user_id FROM user_permissions up"
                + " JOIN menus m ON m.id = up.menu_id AND m.deleted_at IS NULL"
                + " WHERE m.menu_code = 'task:access' AND up.deleted_at IS NULL", Long.class));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> m : memberRows(id)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user_id", m.get("user_id"));
            row.put("real_name", m.get("real_name"));
            row.put("role", m.get("role"));
            row.put("task_access", m.get("user_id") instanceof Number n && granted.contains(n.longValue()));
            rows.add(row);
        }
        return Result.success(rows);
    }
    // 项目详情：概览、成员、任务清单、统计一起返回
    @GetMapping("/{id}")
    @RequirePermission("project:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!isGlobal(userId) && !projectScopeService.canSeeProject(id, userId)) {
            return Result.error(403, "无权查看该项目：不在您的项目范围内");
        }
        p.setProgress(projectProgressService.refresh(p.getId()));

        Set<Long> userIds = new HashSet<>();
        userIds.add(p.getManager_id());
        userIds.add(p.getCreator_id());
        List<Map<String, Object>> members = memberRows(id);
        for (Map<String, Object> m : members) {
            if (m.get("user_id") instanceof Number n) userIds.add(n.longValue());
        }
        Map<Long, String> names = userNames(userIds);
        List<Long> ids = List.of(id);
        Map<String, Object> data = projectRow(p, names, taskStats(ids), memberCounts(ids));
        data.put("manager_name", p.getManager_id() == null ? null : names.get(p.getManager_id()));
        data.put("creator_name", p.getCreator_id() == null ? null : names.get(p.getCreator_id()));
        data.put("description", p.getDescription());
        data.put("actual_start_date", p.getActual_start_date());
        data.put("actual_end_date", p.getActual_end_date());
        data.put("can_manage", canManageProject(p, userId));
        data.put("members", members);
        data.put("tasks", taskRows(id));
        data.put("statistics", statisticsOf(id));
        return Result.success(data);
    }

    // 立项：项目经理默认是自己，管理员可以指定他人
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:add")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        String name = text(body.get("name"));
        if (name == null || name.trim().isEmpty()) return Result.error(400, "项目名称不能为空");
        if (name.trim().length() > 200) return Result.error(400, "项目名称不能超过 200 个字");

        Long managerId = parseLong(body.get("manager_id"));
        if (managerId == null) managerId = userId;
        if (!Objects.equals(managerId, userId) && !isGlobal(userId)) {
            return Result.error(403, "只有管理员可以指定其他人担任项目经理");
        }
        if (usersRepository.selectById(managerId) == null) return Result.error(400, "项目经理不存在");

        LocalDate planStart = parseDate(body.get("plan_start_date"));
        LocalDate planEnd = parseDate(body.get("plan_end_date"));
        if (planStart != null && planEnd != null && planEnd.isBefore(planStart)) {
            return Result.error(400, "计划结束日期不能早于开始日期");
        }

        Projects p = new Projects();
        p.setProject_no(genProjectNo());
        p.setName(name.trim());
        p.setDescription(text(body.get("description")));
        p.setProject_type(body.get("project_type") == null ? 1 : Integer.valueOf(body.get("project_type").toString()));
        p.setPriority(body.get("priority") == null ? 2 : Integer.valueOf(body.get("priority").toString()));
        p.setStatus(PS_DRAFT);
        p.setProgress(0);
        p.setManager_id(managerId);
        p.setCreator_id(userId);
        p.setPlan_start_date(planStart);
        p.setPlan_end_date(planEnd);
        p.setColor(text(body.get("color")));
        projectRepository.insert(p);

        addMember(p.getId(), managerId, PM_ROLE_OWNER, userId);
        if (!Objects.equals(userId, managerId)) addMember(p.getId(), userId, PM_ROLE_MEMBER, userId);

        Set<Long> extra = new HashSet<>(parseIdList(body.get("member_ids")));
        extra.remove(managerId);
        extra.remove(userId);
        for (Long mid : extra) {
            Users member = usersRepository.selectById(mid);
            if (member == null || member.getStatus() == null || member.getStatus() != 1) continue;
            addMember(p.getId(), mid, PM_ROLE_MEMBER, userId);
            notifyUser(mid, "您被加入项目：" + p.getName(), "/home/project?id=" + p.getId());
        }
        if (!Objects.equals(userId, managerId)) {
            notifyUser(managerId, "您被指派为项目经理：" + p.getName(), "/home/project?id=" + p.getId());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", p.getId());
        data.put("project_no", p.getProject_no());
        return Result.success("立项成功", data);
    }

    // 只有项目经理本人和管理员能改，已完成或已取消的只读
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:edit")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) return Result.error(403, "无权编辑该项目：仅项目经理或管理员可操作");
        int st = statusOf(p);
        if (st == PS_DONE || st == PS_CANCELED) return Result.error(400, "已完成 / 已取消的项目不可编辑");
        try {
            if (body.containsKey("name")) {
                String name = text(body.get("name"));
                if (name == null || name.trim().isEmpty()) return Result.error(400, "项目名称不能为空");
                p.setName(name.trim());
            }
            if (body.containsKey("description")) p.setDescription(text(body.get("description")));
            if (body.get("project_type") != null) p.setProject_type(Integer.valueOf(body.get("project_type").toString()));
            if (body.get("priority") != null) p.setPriority(Integer.valueOf(body.get("priority").toString()));
            if (body.containsKey("plan_start_date")) p.setPlan_start_date(parseDate(body.get("plan_start_date")));
            if (body.containsKey("plan_end_date")) p.setPlan_end_date(parseDate(body.get("plan_end_date")));
            if (p.getPlan_start_date() != null && p.getPlan_end_date() != null
                && p.getPlan_end_date().isBefore(p.getPlan_start_date())) {
                return Result.error(400, "计划结束日期不能早于开始日期");
            }
            if (body.get("manager_id") != null) {
                Long newManager = parseLong(body.get("manager_id"));
                if (newManager != null && !Objects.equals(newManager, p.getManager_id())) {
                    if (!isGlobal(userId)) return Result.error(403, "只有管理员可以更换项目经理");
                    Users newManagerUser = usersRepository.selectById(newManager);
                    if (newManagerUser == null) return Result.error(400, "项目经理不存在");
                    if (newManagerUser.getStatus() == null || newManagerUser.getStatus() != 1) {
                        return Result.error(400, "该账号已停用，不能担任项目经理");
                    }
                    p.setManager_id(newManager);
                    addMember(id, newManager, PM_ROLE_OWNER, userId);
                    notifyUser(newManager, "您被指派为项目经理：" + p.getName(), "/home/project?id=" + id);
                }
            }
            if (body.containsKey("color")) p.setColor(text(body.get("color")));
            p.setUpdated_at(LocalDateTime.now());
            projectRepository.updateById(p);

            // 传了 member_ids 才同步成员，项目经理始终留在项目里
            if (body.containsKey("member_ids")) {
                Set<Long> keep = new HashSet<>(parseIdList(body.get("member_ids")));
                keep.add(p.getManager_id());
                keep.add(p.getCreator_id());
                List<ProjectMember> current = memberRepository.selectList(
                    new QueryWrapper<ProjectMember>().eq("project_id", id));
                for (ProjectMember m : current) {
                    if (!keep.contains(m.getUser_id())) {
                        memberRepository.deleteById(m.getId());
                    }
                }
                for (Long mid : keep) {
                    Users member = usersRepository.selectById(mid);
                    // 停用账号不回填成成员，历史关系留给交接流程
                    if (member == null || member.getStatus() == null || member.getStatus() != 1) continue;
                    addMember(id, mid, Objects.equals(mid, p.getManager_id()) ? PM_ROLE_OWNER : PM_ROLE_MEMBER, userId);
                }
            }
            return Result.success("已保存", Map.of("id", id));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // 还有未结束的任务时不能删项目，免得任务没有归属
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:delete")
    public Result<Map<String, Object>> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) return Result.error(403, "无权删除该项目：仅项目经理或管理员可操作");
        Integer unfinished = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tasks WHERE project_id = ? AND deleted_at IS NULL AND archive_status = 0"
                + " AND status NOT IN (" + CLOSED_TASK_STATUS + ")", Integer.class, id);
        if (unfinished != null && unfinished > 0) {
            return Result.error(400, "项目下还有 " + unfinished + " 个未结束的任务，请先完成或撤销后再删除");
        }
        p.setUpdated_at(LocalDateTime.now());
        projectRepository.deleteById(id);
        for (ProjectMember m : memberRepository.selectList(
            new QueryWrapper<ProjectMember>().eq("project_id", id))) {
            memberRepository.deleteById(m.getId());
        }
        return Result.success("项目已删除", Map.of("id", id));
    }

    // 项目状态流转：开始、暂停、恢复、结项、取消
    @PostMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:edit")
    public Result<Map<String, Object>> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) return Result.error(403, "无权操作该项目：仅项目经理或管理员可操作");
        String action = text(body.get("action"));
        if (action == null) return Result.error(400, "缺少操作类型");
        int cur = statusOf(p);
        switch (action) {
            case "start" -> {
                if (cur != PS_DRAFT) return Result.error(400, "只有未启动的项目可以开始");
                p.setStatus(PS_RUNNING);
                p.setActual_start_date(LocalDate.now());
            }
            case "pause" -> {
                if (cur != PS_RUNNING) return Result.error(400, "只有进行中的项目可以暂停");
                p.setStatus(PS_PAUSED);
            }
            case "resume" -> {
                if (cur != PS_PAUSED) return Result.error(400, "只有已暂停的项目可以恢复");
                p.setStatus(PS_RUNNING);
                if (p.getActual_start_date() == null) p.setActual_start_date(LocalDate.now());
            }
            case "complete" -> {
                if (cur != PS_RUNNING && cur != PS_DRAFT && cur != PS_PAUSED) {
                    return Result.error(400, "当前状态不能结项");
                }
                Integer unfinished = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE project_id = ? AND deleted_at IS NULL AND archive_status = 0"
                        + " AND status NOT IN (" + CLOSED_TASK_STATUS + ")", Integer.class, id);
                if (unfinished != null && unfinished > 0) {
                    return Result.error(400, "项目下还有 " + unfinished + " 个任务没有完成或撤销，先处理完再结项");
                }
                p.setStatus(PS_DONE);
                p.setProgress(100);
                p.setActual_end_date(LocalDate.now());
                if (p.getActual_start_date() == null) p.setActual_start_date(LocalDate.now());
            }
            case "cancel" -> {
                if (cur != PS_DRAFT && cur != PS_RUNNING && cur != PS_PAUSED) {
                    return Result.error(400, "当前状态不能取消");
                }
                p.setStatus(PS_CANCELED);
                p.setActual_end_date(LocalDate.now());
            }
            default -> {
                return Result.error(400, "不支持的操作：" + action);
            }
        }
        p.setUpdated_at(LocalDateTime.now());
        projectRepository.updateById(p);
        // 状态变化通知项目成员
        for (Long mid : projectScopeService.projectMemberIds(id)) {
            if (Objects.equals(mid, userId)) continue;
            notifyUser(mid, "项目状态更新：" + p.getName() + " → " + statusText(p.getStatus()),
                "/home/project?id=" + id);
        }
        return Result.success("操作成功", Map.of("id", id, "status", p.getStatus()));
    }

    // 添加项目成员，项目经理或管理员
    @PostMapping("/{id}/members")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:member:manage")
    public Result<Map<String, Object>> addMembers(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) return Result.error(403, "无权管理成员：仅项目经理或管理员可操作");
        if (statusOf(p) == PS_DONE || statusOf(p) == PS_CANCELED) {
            return Result.error(400, "已结束的项目不能再加成员");
        }
        List<Long> ids = parseIdList(body.get("user_ids"));
        if (ids.isEmpty()) {
            Long single = parseLong(body.get("user_id"));
            if (single != null) ids.add(single);
        }
        if (ids.isEmpty()) return Result.error(400, "请选择成员");
        int added = 0;
        for (Long mid : ids) {
            Users member = usersRepository.selectById(mid);
            if (member == null) continue;
            // 停用账号不能进项目
            if (member.getStatus() == null || member.getStatus() != 1) continue;
            if (projectScopeService.isProjectMember(mid, id)) {
                // 已经是成员也再开通一次，补历史漏发的权限
                grantTaskAccess(mid, userId);
                continue;
            }
            addMember(id, mid, PM_ROLE_MEMBER, userId);
            notifyUser(mid, "您被加入项目：" + p.getName(), "/home/project?id=" + id);
            added++;
        }
        return Result.success("已添加 " + added + " 名成员", Map.of("added", added));
    }

    // 移除项目成员，项目经理不能被移除
    @DeleteMapping("/{id}/members/{memberUserId}")
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission("project:member:manage")
    public Result<Map<String, Object>> removeMember(@PathVariable Long id,
                                                     @PathVariable Long memberUserId,
                                                     HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!canManageProject(p, userId)) return Result.error(403, "无权管理成员：仅项目经理或管理员可操作");
        if (Objects.equals(memberUserId, p.getManager_id())) return Result.error(400, "项目经理不能移除，请先更换项目经理");
        ProjectMember m = memberRepository.selectOne(new QueryWrapper<ProjectMember>()
            .eq("project_id", id).eq("user_id", memberUserId));
        if (m == null) return Result.error(404, "该成员不在项目中");
        memberRepository.deleteById(m.getId());
        revokeTaskAccessIfIdle(memberUserId);
        notifyUser(memberUserId, "您已被移出项目：" + p.getName(), "/home/project");
        return Result.success("已移除成员", Map.of("removed", memberUserId));
    }

    // 项目统计：完成率、逾期率、工时偏差，按负责人、状态、优先级分布
    @GetMapping("/{id}/statistics")
    @RequirePermission({"project:stat", "project:list"})
    public Result<Map<String, Object>> statistics(@PathVariable Long id, HttpServletRequest request) {
        Long userId = RequestUser.id(request);
        Projects p = projectRepository.selectById(id);
        if (p == null) return Result.error(404, "项目不存在");
        if (!isGlobal(userId) && !projectScopeService.canSeeProject(id, userId)) {
            return Result.error(403, "无权查看该项目统计：不在您的项目范围内");
        }
        return Result.success(statisticsOf(id));
    }

    // 数据范围判断

    // 管理员和总经理看全部，其余按项目范围
    private boolean isGlobal(Long userId) {
        if (userId == null) return false;
        if (permissionService.isAdmin(userId)) return true;
        return permissionService.getUserRoleCodes(userId).contains("gm");
    }

    // 项目经理本人、创建人、管理员可以操作项目
    private boolean canManageProject(Projects p, Long userId) {
        if (p == null || userId == null) return false;
        if (isGlobal(userId) && permissionService.getUserRoleCodes(userId).contains("admin")) return true;
        return Objects.equals(userId, p.getManager_id()) || Objects.equals(userId, p.getCreator_id());
    }

    private int statusOf(Projects p) {
        return p.getStatus() == null ? PS_DRAFT : p.getStatus();
    }

    private String statusText(Integer status) {
        if (status == null) return "未启动";
        return switch (status) {
            case PS_DRAFT -> "未启动";
            case PS_RUNNING -> "进行中";
            case PS_PAUSED -> "已暂停";
            case PS_DONE -> "已完成";
            case PS_CANCELED -> "已取消";
            default -> "未知";
        };
    }

    private void addMember(Long projectId, Long userId, int role, Long operatorId) {
        // 加入项目就开通任务中心，不用再单独点开通
        grantTaskAccess(userId, operatorId);
        ProjectMember exist = memberRepository.selectOne(new QueryWrapper<ProjectMember>()
            .eq("project_id", projectId).eq("user_id", userId));
        if (exist != null) {
            if (role == PM_ROLE_OWNER && !Objects.equals(exist.getRole(), PM_ROLE_OWNER)) {
                exist.setRole(PM_ROLE_OWNER);
                memberRepository.updateById(exist);
            }
            return;
        }
        // 被移出过的成员有软删除记录，唯一键会挡住插入，直接恢复那一行
        Integer archived = jdbc.queryForObject(
            "SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?",
            Integer.class, projectId, userId);
        if (archived != null && archived > 0) {
            jdbc.update("UPDATE project_members SET deleted_at = NULL, role = ?, join_time = ? "
                + "WHERE project_id = ? AND user_id = ?", role, LocalDateTime.now(), projectId, userId);
            return;
        }
        ProjectMember m = new ProjectMember();
        m.setProject_id(projectId);
        m.setUser_id(userId);
        m.setRole(role);
        m.setJoin_time(LocalDateTime.now());
        memberRepository.insert(m);
    }

    // 移出后既不在别的项目也没任务，就回收自动开通的任务中心权限
    // 只回收 source=2 自动开通的记录，手工授予的不动
    private void revokeTaskAccessIfIdle(Long userId) {
        if (userId == null) return;
        Integer projects = jdbc.queryForObject(
            "SELECT COUNT(*) FROM project_members WHERE user_id = ? AND deleted_at IS NULL", Integer.class, userId);
        Integer tasks = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted_at IS NULL AND status NOT IN (4, 5, 6)",
            Integer.class, userId);
        if ((projects != null && projects > 0) || (tasks != null && tasks > 0)) return;
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT up.id FROM user_permissions up JOIN menus m ON m.id = up.menu_id "
                + "WHERE up.user_id = ? AND m.menu_code = 'task:access' AND up.source = 2 AND up.deleted_at IS NULL",
            userId);
        if (rows.isEmpty()) return;
        jdbc.update("DELETE FROM user_permissions WHERE id = ?", rows.get(0).get("id"));
    }

    // 角色自带 task:access 的不再写用户级权限
    private void grantTaskAccess(Long userId, Long operatorId) {
        if (userId == null) return;
        if (permissionService.getUserPermissions(userId).contains("task:access")) return;
        userPermissionService.grantIfAbsent(userId, "task:access", operatorId, 2, "加入项目自动开通");
    }

    private Map<String, Object> projectRow(Projects p, Map<Long, String> names,
                                           Map<Long, int[]> stats, Map<Long, Integer> memberCounts) {
        Map<String, Object> row = new LinkedHashMap<>();
        int[] s = stats.get(p.getId());
        row.put("id", p.getId());
        row.put("project_no", p.getProject_no());
        row.put("name", p.getName());
        row.put("project_type", p.getProject_type());
        row.put("status", p.getStatus());
        row.put("status_text", statusText(p.getStatus()));
        row.put("priority", p.getPriority());
        row.put("progress", p.getProgress());
        row.put("manager_id", p.getManager_id());
        row.put("manager_name", names.get(p.getManager_id()));
        row.put("creator_id", p.getCreator_id());
        row.put("plan_start_date", p.getPlan_start_date() == null ? null : p.getPlan_start_date().format(DATE_FMT));
        row.put("plan_end_date", p.getPlan_end_date() == null ? null : p.getPlan_end_date().format(DATE_FMT));
        row.put("task_total", s == null ? 0 : s[0]);
        row.put("task_done", s == null ? 0 : s[1]);
        row.put("task_running", s == null ? 0 : s[2]);
        row.put("task_overdue", s == null ? 0 : s[3]);
        row.put("member_count", memberCounts.getOrDefault(p.getId(), 0));
        row.put("created_at", p.getCreated_at());
        return row;
    }

    private Map<String, Object> optionRow(Map<String, Object> r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ((Number) r.get("id")).longValue());
        row.put("project_no", r.get("project_no"));
        row.put("name", r.get("name"));
        row.put("status", r.get("status"));
        return row;
    }

    // 任务完成情况按项目一次查出，避免逐行 count
    private Map<Long, int[]> taskStats(List<Long> projectIds) {
        Map<Long, int[]> map = new HashMap<>();
        if (projectIds == null || projectIds.isEmpty()) return map;
        String sql = "SELECT project_id, COUNT(*) AS total,"
            + " SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) AS done,"
            + " SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS running,"
            + " SUM(CASE WHEN status NOT IN (" + CLOSED_TASK_STATUS + ") AND plan_end_time IS NOT NULL"
            + " AND plan_end_time < NOW() THEN 1 ELSE 0 END) AS overdue"
            + " FROM tasks WHERE deleted_at IS NULL AND archive_status = 0"
            + " AND project_id IN (" + placeholders(projectIds.size()) + ") GROUP BY project_id";
        for (Map<String, Object> row : jdbc.queryForList(sql, projectIds.toArray())) {
            Long pid = ((Number) row.get("project_id")).longValue();
            map.put(pid, new int[]{num(row.get("total")), num(row.get("done")),
                num(row.get("running")), num(row.get("overdue"))});
        }
        return map;
    }

    private Map<Long, Integer> memberCounts(List<Long> projectIds) {
        Map<Long, Integer> map = new HashMap<>();
        if (projectIds == null || projectIds.isEmpty()) return map;
        String sql = "SELECT project_id, COUNT(*) AS cnt FROM project_members WHERE deleted_at IS NULL"
            + " AND project_id IN (" + placeholders(projectIds.size()) + ") GROUP BY project_id";
        for (Map<String, Object> row : jdbc.queryForList(sql, projectIds.toArray())) {
            map.put(((Number) row.get("project_id")).longValue(), num(row.get("cnt")));
        }
        return map;
    }

    private List<Map<String, Object>> memberRows(Long projectId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT pm.user_id, pm.role, pm.join_time, u.username, u.real_name, o.org_name"
            + " FROM project_members pm JOIN users u ON u.id = pm.user_id"
            + " LEFT JOIN organizations o ON o.id = u.org_id"
            + " WHERE pm.project_id = ? AND pm.deleted_at IS NULL AND u.deleted_at IS NULL"
            + " ORDER BY pm.role, pm.id";
        for (Map<String, Object> r : jdbc.queryForList(sql, projectId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user_id", ((Number) r.get("user_id")).longValue());
            row.put("username", r.get("username"));
            row.put("real_name", r.get("real_name"));
            row.put("org_name", r.get("org_name"));
            row.put("role", r.get("role"));
            row.put("join_time", r.get("join_time"));
            rows.add(row);
        }
        return rows;
    }

    // 项目下的任务清单，详情页直接展示
    private List<Map<String, Object>> taskRows(Long projectId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT t.id, t.task_no, t.title, t.status, t.priority, t.progress,"
            + " t.assignee_id, t.plan_end_time, u.real_name AS assignee_name"
            + " FROM tasks t LEFT JOIN users u ON u.id = t.assignee_id"
            + " WHERE t.project_id = ? AND t.deleted_at IS NULL AND t.archive_status = 0"
            + " ORDER BY t.created_at DESC LIMIT 100";
        for (Map<String, Object> r : jdbc.queryForList(sql, projectId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", ((Number) r.get("id")).longValue());
            row.put("task_no", r.get("task_no"));
            row.put("title", r.get("title"));
            row.put("status", r.get("status"));
            row.put("priority", r.get("priority"));
            row.put("progress", r.get("progress"));
            row.put("assignee_id", r.get("assignee_id"));
            row.put("assignee_name", r.get("assignee_name"));
            row.put("plan_end_time", r.get("plan_end_time"));
            rows.add(row);
        }
        return rows;
    }

    // 统计口径：认领任务不含已撤销和已拒绝
    private Map<String, Object> statisticsOf(Long projectId) {
        List<Map<String, Object>> all = jdbc.queryForList(
            "SELECT status, priority, progress, plan_work_hours, actual_work_hours, plan_end_time, assignee_id"
                + " FROM tasks WHERE project_id = ? AND deleted_at IS NULL AND archive_status = 0", projectId);
        int total = all.size();
        int done = 0, running = 0, waiting = 0, overdue = 0, planCnt = 0, actualCnt = 0, diffCnt = 0;
        BigDecimal planSum = BigDecimal.ZERO, actualSum = BigDecimal.ZERO, diffSum = BigDecimal.ZERO;
        Map<Integer, Integer> statusCount = new LinkedHashMap<>();
        Map<Integer, Integer> priorityCount = new LinkedHashMap<>();
        Map<Long, int[]> byAssignee = new LinkedHashMap<>();
        for (Map<String, Object> r : all) {
            int st = r.get("status") == null ? 0 : ((Number) r.get("status")).intValue();
            statusCount.merge(st, 1, Integer::sum);
            int pr = r.get("priority") == null ? 2 : ((Number) r.get("priority")).intValue();
            priorityCount.merge(pr, 1, Integer::sum);
            if (st == 4) done++;
            if (st == 2) running++;
            if (st == 0 || st == 1) waiting++;
            Object planEnd = r.get("plan_end_time");
            boolean isOverdue = st != 4 && st != 5 && st != 6 && planEnd instanceof LocalDateTime t
                && t.isBefore(LocalDateTime.now());
            if (isOverdue) overdue++;
            BigDecimal plan = toDecimal(r.get("plan_work_hours"));
            BigDecimal actual = toDecimal(r.get("actual_work_hours"));
            if (plan != null) {
                planSum = planSum.add(plan);
                planCnt++;
            }
            if (actual != null) {
                actualSum = actualSum.add(actual);
                actualCnt++;
                if (plan != null) {
                    diffSum = diffSum.add(actual.subtract(plan));
                    diffCnt++;
                }
            }
            Object assignee = r.get("assignee_id");
            if (assignee instanceof Number n) {
                int[] counter = byAssignee.computeIfAbsent(n.longValue(), k -> new int[2]);
                counter[0]++;
                if (st == 4) counter[1]++;
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("done", done);
        data.put("running", running);
        data.put("waiting", waiting);
        data.put("overdue", overdue);
        data.put("completion_rate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        data.put("overdue_rate", total == 0 ? 0 : Math.round(overdue * 1000.0 / total) / 10.0);
        data.put("avg_plan_hours", planCnt == 0 ? BigDecimal.ZERO
            : planSum.divide(BigDecimal.valueOf(planCnt), 2, RoundingMode.HALF_UP));
        data.put("avg_actual_hours", actualCnt == 0 ? BigDecimal.ZERO
            : actualSum.divide(BigDecimal.valueOf(actualCnt), 2, RoundingMode.HALF_UP));
        data.put("avg_hour_diff", diffCnt == 0 ? BigDecimal.ZERO
            : diffSum.divide(BigDecimal.valueOf(diffCnt), 2, RoundingMode.HALF_UP));
        data.put("status_count", statusCount);
        data.put("priority_count", priorityCount);

        Set<Long> userIds = new HashSet<>(byAssignee.keySet());
        Map<Long, String> names = userNames(userIds);
        List<Map<String, Object>> assignees = new ArrayList<>();
        byAssignee.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
            .forEach(e -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("user_id", e.getKey());
                row.put("real_name", names.get(e.getKey()));
                row.put("total", e.getValue()[0]);
                row.put("done", e.getValue()[1]);
                assignees.add(row);
            });
        data.put("by_assignee", assignees);
        return data;
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        List<Long> valid = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && id > 0) valid.add(id);
        }
        if (valid.isEmpty()) return map;
        String sql = "SELECT id, real_name FROM users WHERE deleted_at IS NULL AND id IN ("
            + placeholders(valid.size()) + ")";
        for (Map<String, Object> r : jdbc.queryForList(sql, valid.toArray())) {
            map.put(((Number) r.get("id")).longValue(), (String) r.get("real_name"));
        }
        return map;
    }

    private void notifyUser(Long userId, String content, String url) {
        if (userId == null) return;
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle("项目通知");
            n.setContent(content);
            n.setType(2);
            n.setBiz_type("project");
            n.setJump_url(url);
            n.setIs_read(0);
            n.setCreated_at(LocalDateTime.now());
            notificationRepository.insert(n);
        } catch (Exception ignored) {
        }
    }

    private String genProjectNo() {
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer cnt = jdbc.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE DATE(created_at) = CURDATE()", Integer.class);
        int seq = (cnt == null ? 0 : cnt) + 1;
        return "PRJ-" + day + "-" + String.format("%03d", seq);
    }

    private String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private int num(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private BigDecimal toDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String text(Object o) {
        return o == null ? null : o.toString();
    }

    private Long parseLong(Object o) {
        if (o == null || o.toString().trim().isEmpty()) return null;
        try {
            return Long.valueOf(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Long> parseIdList(Object o) {
        List<Long> ids = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object item : list) {
                Long id = parseLong(item);
                if (id != null) ids.add(id);
            }
        } else {
            Long single = parseLong(o);
            if (single != null) ids.add(single);
        }
        return ids;
    }

    private LocalDate parseDate(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式不正确：" + s);
        }
    }

    private Map<String, Object> emptyPage(int page, int size) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", new ArrayList<>());
        data.put("total", 0);
        data.put("pageNum", page);
        data.put("pageSize", size);
        return data;
    }
}
