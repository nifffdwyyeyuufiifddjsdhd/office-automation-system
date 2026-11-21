package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.entity.FlowApprovals;
import com.officeautomationsystemapi.entity.FlowInstances;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.ProjectMember;
import com.officeautomationsystemapi.entity.Task;
import com.officeautomationsystemapi.entity.UserHandovers;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.FlowApprovalsRepository;
import com.officeautomationsystemapi.repository.FlowInstancesRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.ProjectMemberRepository;
import com.officeautomationsystemapi.repository.TaskRepository;
import com.officeautomationsystemapi.repository.UserHandoversRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.UserHandoverService;
import com.officeautomationsystemapi.service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 离职交接：待办流程、未结束任务、项目成员一起交给接收人
@Service
@Transactional
public class UserHandoverServiceImpl implements UserHandoverService {

    // 已结束的任务状态 4 完成、5 撤销、6 拒绝，保留原负责人
    private static final String CLOSED_TASK_STATUS = "4,5,6";

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private FlowInstancesRepository flowInstancesRepository;

    @Autowired
    private FlowApprovalsRepository flowApprovalsRepository;

    @Autowired
    private UserHandoversRepository userHandoversRepository;

    @Autowired
    private NotificationRecordsRepository notificationRecordsRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private UserPermissionService userPermissionService;

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public Map<String, Object> preview(Long leaverId) {
        Users leaver = usersRepository.selectById(leaverId);
        if (leaver == null) {
            throw new RuntimeException("用户不存在");
        }
        // 待办：他是当前审核人且流程还在审批中
        List<FlowInstances> todos = flowInstancesRepository.selectList(new QueryWrapper<FlowInstances>()
                .eq("current_auditor_id", leaverId)
                .eq("status", 1)
                .orderByDesc("submit_time"));
        // 他发起、还在审批中的流程，交接后由接收人继续跟进
        List<FlowInstances> running = flowInstancesRepository.selectList(new QueryWrapper<FlowInstances>()
                .eq("creator_id", leaverId)
                .eq("status", 1)
                .orderByDesc("submit_time"));
        // 已办只统计不转交，历史审批记录要保留原始审批人
        int done = doneCountOf(leaverId);
        // 他手上还没结束的任务，不转走就会永远压在停用账号上
        List<Task> tasks = pendingTasksOf(leaverId);
        List<Long> projectIds = projectIdsOf(leaverId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", leaver.getId());
        result.put("username", leaver.getUsername());
        result.put("realName", leaver.getReal_name());
        result.put("status", leaver.getStatus());
        result.put("orgName", orgNameOf(leaver.getOrg_id()));
        result.put("todoCount", todos.size());
        result.put("runningCount", running.size());
        result.put("doneCount", done);
        result.put("taskCount", tasks.size());
        result.put("projectCount", projectIds.size());
        result.put("todoList", flowBrief(todos));
        result.put("runningList", flowBrief(running));
        result.put("taskList", taskBrief(tasks));
        // 自动流转模式要先给前端预览接手人是谁
        Long autoReceiverId = resolveAutoReceiver(leaverId);
        result.put("autoReceiverId", autoReceiverId);
        Users autoReceiver = autoReceiverId == null ? null : usersRepository.selectById(autoReceiverId);
        result.put("autoReceiverName", autoReceiver == null ? null : autoReceiver.getReal_name());
        return result;
    }

    @Override
    public Map<String, Object> handover(Long operatorId, Long leaverId, Long receiverId, String reason,
                                        LocalDate effectiveDate, Boolean transferRunning, String mode) {
        if (leaverId == null) {
            throw new RuntimeException("请选择离职人");
        }
        String handleMode = normalizeMode(mode);
        Users leaver = usersRepository.selectById(leaverId);
        if (leaver == null) {
            throw new RuntimeException("离职人不存在");
        }
        // 自动模式找直属上级，找不到给管理员；保留模式不转交
        if ("auto".equals(handleMode) && receiverId == null) {
            receiverId = resolveAutoReceiver(leaverId);
            if (receiverId == null) {
                throw new RuntimeException("没找到直属上级，也没有可用的系统管理员，请改用手动指定接收人");
            }
        }
        if ("keep".equals(handleMode)) {
            receiverId = null;
        }
        Users receiver = null;
        if (receiverId != null) {
            if (leaverId.equals(receiverId)) {
                throw new RuntimeException("接收人不能是离职人自己");
            }
            receiver = usersRepository.selectById(receiverId);
            if (receiver == null) {
                throw new RuntimeException("接收人不存在");
            }
            if (receiver.getStatus() == null || receiver.getStatus() != 1) {
                throw new RuntimeException("接收人账号已停用，请先启用或换一个接收人");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new LinkedHashMap<>();
        // 先把要交接的数量统计出来，保留模式要靠它说明还剩多少
        List<FlowInstances> todos = flowInstancesRepository.selectList(new QueryWrapper<FlowInstances>()
                .eq("current_auditor_id", leaverId)
                .eq("status", 1));
        List<FlowInstances> runningList = flowInstancesRepository.selectList(new QueryWrapper<FlowInstances>()
                .eq("creator_id", leaverId)
                .eq("status", 1));
        List<Task> tasks = pendingTasksOf(leaverId);
        List<ProjectMember> memberships = projectMemberRepository.selectList(new QueryWrapper<ProjectMember>()
                .eq("user_id", leaverId));

        int todoCount = todos.size();
        int runningCount = runningList.size();
        int taskCount = 0;
        int projectCount = 0;

        if (receiver != null) {
            // 待办流程换审核人：只动还在审批中的，已完成的保持原样
            todoCount = flowInstancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                    .eq("current_auditor_id", leaverId)
                    .eq("status", 1)
                    .set("current_auditor_id", receiverId)
                    .set("updated_at", now));

            // 他发起、还在审批中的流程改由接收人跟进
            runningCount = 0;
            if (transferRunning == null || transferRunning) {
                runningCount = flowInstancesRepository.update(null, new UpdateWrapper<FlowInstances>()
                        .eq("creator_id", leaverId)
                        .eq("status", 1)
                        .set("creator_id", receiverId)
                        .set("updated_at", now));
            }

            // 未结束任务的负责人换成接收人，任务不随账号消失
            for (Task task : tasks) {
                jdbc.update("UPDATE tasks SET assignee_id = ?, updated_at = ? WHERE id = ?",
                        receiverId, now, task.getId());
                // task_members 有 (task_id, user_id) 唯一键，接收人以前被移出过时要把软删记录恢复回来
                int restored = jdbc.update(
                        "UPDATE task_members SET role = 1, deleted_at = NULL, updated_at = ? WHERE task_id = ? AND user_id = ?",
                        now, task.getId(), receiverId);
                if (restored == 0) {
                    jdbc.update("INSERT INTO task_members (task_id, user_id, role, created_at, updated_at) VALUES (?, ?, 1, ?, ?)",
                            task.getId(), receiverId, now, now);
                }
                // 离职人的负责人身份摘掉，避免一个任务挂两个负责人
                jdbc.update("UPDATE task_members SET deleted_at = ? WHERE task_id = ? AND user_id = ? AND role = 1",
                        now, task.getId(), leaverId);
                taskCount++;
            }

            // 项目成员交给接收人，他负责的项目也一并换负责人
            for (ProjectMember member : memberships) {
                Long projectId = member.getProject_id();
                int role = member.getRole() == null ? 2 : member.getRole();
                int restored = jdbc.update(
                        "UPDATE project_members SET role = ?, deleted_at = NULL, updated_at = ? WHERE project_id = ? AND user_id = ?",
                        role, now, projectId, receiverId);
                if (restored == 0) {
                    jdbc.update("INSERT INTO project_members (project_id, user_id, role, join_time, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                            projectId, receiverId, role, now, now, now);
                }
                if (role == 1) {
                    jdbc.update("UPDATE projects SET manager_id = ?, updated_at = ? WHERE id = ?", receiverId, now, projectId);
                }
                projectMemberRepository.deleteById(member.getId());
                projectCount++;
            }
        }

        // 回收离职人的自动授权，管理员手工授的保留
        jdbc.update("UPDATE user_permissions SET deleted_at = ? WHERE user_id = ? AND source = 2 AND deleted_at IS NULL",
                now, leaverId);
        // 接收人手上多了任务，必须能进任务中心
        if (taskCount > 0 && receiverId != null) {
            userPermissionService.grantIfAbsent(receiverId, "task:access", operatorId, 2, "离职交接接手任务");
        }

        // 历史已办只统计数量，审批记录里的审批人保持原样
        int done = doneCountOf(leaverId);

        // 停用离职人账号，不删数据，档案与历史流程保留
        Users update = new Users();
        update.setId(leaverId);
        update.setStatus(0);
        update.setUpdated_at(now);
        usersRepository.updateById(update);

        // 写交接记录，保留模式没有接收人
        UserHandovers record = new UserHandovers();
        record.setUser_id(leaverId);
        record.setReceiver_id(receiverId);
        record.setHandle_mode(handleMode);
        record.setOperator_id(operatorId);
        record.setReason(reason);
        record.setEffective_date(effectiveDate == null ? LocalDate.now() : effectiveDate);
        record.setTodo_count(todoCount);
        record.setRunning_count(runningCount);
        record.setTask_count(taskCount);
        record.setProject_count(projectCount);
        record.setDone_count(done);
        record.setItem_summary(buildSummary(leaver, receiver, handleMode, todos.size(), runningList.size(),
                tasks.size(), memberships.size(), todoCount, runningCount, taskCount, projectCount, done));
        record.setCreated_at(now);
        record.setUpdated_at(now);
        userHandoversRepository.insert(record);

        // 通知接收人，保留模式下没人接手就不发
        if (receiver != null) {
            try {
                NotificationRecords notice = new NotificationRecords();
                notice.setUser_id(receiverId);
                notice.setTitle("离职交接已完成");
                notice.setContent(leaver.getReal_name() + " 的 " + todoCount + " 条待办流程、"
                        + taskCount + " 个任务已转交给您" + (projectCount > 0 ? "，并接管了 " + projectCount + " 个项目" : "")
                        + "，请及时处理");
                notice.setType(1);
                notice.setBiz_type("user_handover");
                notice.setBiz_id(record.getId());
                notice.setJump_url(taskCount > 0 ? "/home/task" : "/home/flow/todo");
                notice.setIs_read(0);
                notice.setCreated_at(now);
                notice.setUpdated_at(now);
                notificationRecordsRepository.insert(notice);
            } catch (Exception ignored) {
                // 通知失败不影响交接结果
            }
        }

        result.put("handoverId", record.getId());
        result.put("handleMode", handleMode);
        result.put("todoCount", todoCount);
        result.put("runningCount", runningCount);
        result.put("taskCount", taskCount);
        result.put("projectCount", projectCount);
        result.put("pendingTodo", todos.size());
        result.put("pendingTask", tasks.size());
        result.put("doneCount", record.getDone_count());
        result.put("receiverName", receiver == null ? null : receiver.getReal_name());
        result.put("itemSummary", record.getItem_summary());
        return result;
    }

    @Override
    public Map<String, Object> page(int pageNum, int pageSize, Long leaverId) {
        QueryWrapper<UserHandovers> wrapper = new QueryWrapper<>();
        if (leaverId != null) {
            wrapper.eq("user_id", leaverId);
        }
        wrapper.orderByDesc("id");
        Page<UserHandovers> page = userHandoversRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, Users> users = new HashMap<>();
        List<UserHandovers> records = page.getRecords();
        for (UserHandovers record : records) {
            for (Long id : new Long[]{record.getUser_id(), record.getReceiver_id(), record.getOperator_id()}) {
                if (id != null && !users.containsKey(id)) {
                    Users user = usersRepository.selectById(id);
                    if (user != null) {
                        users.put(id, user);
                    }
                }
            }
            record.setUser_name(nameOf(users.get(record.getUser_id())));
            record.setReceiver_name(nameOf(users.get(record.getReceiver_id())));
            record.setOperator_name(nameOf(users.get(record.getOperator_id())));
            Users leaver = users.get(record.getUser_id());
            record.setOrg_name(leaver == null ? null : orgNameOf(leaver.getOrg_id()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        return result;
    }

    // 只转未结束的任务，已完成/撤销/拒绝的保留原负责人当作历史
    private List<Task> pendingTasksOf(Long leaverId) {
        return taskRepository.selectList(new QueryWrapper<Task>()
                .eq("assignee_id", leaverId)
                .apply("status NOT IN (" + CLOSED_TASK_STATUS + ")")
                .orderByDesc("created_at"));
    }

    // 他参与的项目，交接后由接收人接管
    private List<Long> projectIdsOf(Long leaverId) {
        List<Long> ids = new ArrayList<>();
        for (ProjectMember member : projectMemberRepository.selectList(new QueryWrapper<ProjectMember>()
                .eq("user_id", leaverId))) {
            ids.add(member.getProject_id());
        }
        return ids;
    }

    // 流程列表只给前端看标题和当前环节，不返回 business_data
    private List<Map<String, Object>> flowBrief(List<FlowInstances> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FlowInstances instance : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", instance.getId());
            row.put("instanceNo", instance.getInstance_no());
            row.put("title", instance.getTitle());
            row.put("currentStepName", instance.getCurrent_step_name());
            row.put("submitTime", instance.getSubmit_time());
            rows.add(row);
        }
        return rows;
    }

    // 任务清单同样只给必要的几列，交接弹窗里够用
    private List<Map<String, Object>> taskBrief(List<Task> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Task task : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", task.getId());
            row.put("taskNo", task.getTask_no());
            row.put("title", task.getTitle());
            row.put("status", task.getStatus());
            row.put("projectId", task.getProject_id());
            rows.add(row);
        }
        return rows;
    }

    // 已办按审批过的流程数算，提交、抄送、自动跳过不算
    private int doneCountOf(Long userId) {
        List<FlowApprovals> aps = flowApprovalsRepository.selectList(new QueryWrapper<FlowApprovals>()
                .eq("approver_id", userId).in("action", Arrays.asList(1, 2, 3)));
        if (aps.isEmpty()) return 0;
        List<Long> ids = new ArrayList<>();
        for (FlowApprovals a : aps) {
            if (isNotApproveRecord(a)) continue;
            if (!ids.contains(a.getInstance_id())) ids.add(a.getInstance_id());
        }
        if (ids.isEmpty()) return 0;
        Map<Long, FlowInstances> instMap = new HashMap<>();
        for (FlowInstances i : flowInstancesRepository.selectBatchIds(ids)) instMap.put(i.getId(), i);
        int count = 0;
        for (Long id : ids) {
            if (instMap.containsKey(id)) count++;
        }
        return count;
    }

    private boolean isNotApproveRecord(FlowApprovals a) {
        if (a.getSign_status() != null && a.getSign_status() == 1) return true;
        String comment = a.getComment();
        return comment != null && comment.startsWith("该步骤无有效审批人");
    }

    private String buildSummary(Users leaver, Users receiver, String mode, int pendingTodo, int pendingRunning,
                                int pendingTask, int pendingProject, int todoCount, int runningCount,
                                int taskCount, int projectCount, int doneCount) {
        String how;
        if ("keep".equals(mode)) {
            how = "处理方式：保留待办（" + pendingTodo + " 条待办流程、" + pendingTask + " 个任务、"
                    + pendingProject + " 个项目身份仍在原账号名下，需管理员后续转交）；";
        } else if ("auto".equals(mode)) {
            how = "处理方式：自动转直属上级（接手人 " + nameOf(receiver) + "）；";
        } else {
            how = "处理方式：手动指定接收人（" + nameOf(receiver) + "）；";
        }
        return how
                + "待办流程转交 " + todoCount + " 条；"
                + "审批中发起流程转交 " + runningCount + " 条；"
                + "未结束任务转交 " + taskCount + " 个；"
                + "项目成员身份接管 " + projectCount + " 个；"
                + "历史已办流程 " + doneCount + " 条留档；"
                + "账号 " + nameOf(leaver) + " 已停用，档案与历史数据保留";
    }

    // 三种处理方式：默认批量转交，非法值按转交处理
    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "transfer";
        String m = mode.trim().toLowerCase();
        if (!"transfer".equals(m) && !"keep".equals(m) && !"auto".equals(m)) return "transfer";
        return m;
    }

    // 找直属上级：本人部门主管，没有就沿 parent_id 往上
    private Long resolveAutoReceiver(Long leaverId) {
        Long orgId = null;
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT org_id FROM users WHERE id = ?", leaverId);
        if (!rows.isEmpty() && rows.get(0).get("org_id") != null) {
            orgId = ((Number) rows.get(0).get("org_id")).longValue();
        }
        int guard = 0;
        while (orgId != null && guard++ < 10) {
            List<Map<String, Object>> org = jdbc.queryForList(
                    "SELECT leader_id, parent_id FROM organizations WHERE id = ? AND deleted_at IS NULL", orgId);
            if (org.isEmpty()) break;
            Object leader = org.get(0).get("leader_id");
            if (leader != null) {
                Long leaderId = ((Number) leader).longValue();
                if (!leaderId.equals(leaverId)) return leaderId;
            }
            Object parent = org.get(0).get("parent_id");
            orgId = parent == null ? null : ((Number) parent).longValue();
            if (orgId != null && orgId == 0L) orgId = null;
        }
        List<Map<String, Object>> admins = jdbc.queryForList(
                "SELECT u.id FROM users u"
                        + " JOIN user_roles ur ON ur.user_id = u.id AND ur.deleted_at IS NULL"
                        + " JOIN roles r ON r.id = ur.role_id"
                        + " WHERE r.role_code = 'admin' AND u.deleted_at IS NULL AND u.status = 1"
                        + " ORDER BY u.id LIMIT 1");
        if (admins.isEmpty()) return null;
        Object adminId = admins.get(0).get("id");
        if (adminId == null) return null;
        Long id = ((Number) adminId).longValue();
        return id.equals(leaverId) ? null : id;
    }

    private String nameOf(Users user) {
        if (user == null) {
            return null;
        }
        return user.getReal_name() == null || user.getReal_name().isEmpty() ? user.getUsername() : user.getReal_name();
    }

    private String orgNameOf(Long orgId) {
        if (orgId == null) {
            return null;
        }
        Organizations org = organizationsRepository.selectById(orgId);
        return org == null ? null : org.getOrg_name();
    }
}
