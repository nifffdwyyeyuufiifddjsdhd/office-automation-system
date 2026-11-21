package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.officeautomationsystemapi.entity.FlowDelegates;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.FlowDelegatesRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.FlowDelegateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 审批委托代理：只影响审批权，不改变流程本身的审核人字段
@Service
@Transactional
public class FlowDelegateServiceImpl implements FlowDelegateService {

    // 委托最多设三个月，避免有人一次性把审批权长期交出去
    private static final int MAX_DAYS = 90;

    @Autowired
    private FlowDelegatesRepository delegatesRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private NotificationRecordsRepository notificationRecordsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // 现在生效的委托：状态开启、时间落在区间内、没被软删
    private QueryWrapper<FlowDelegates> activeWrapper() {
        LocalDateTime now = LocalDateTime.now();
        return new QueryWrapper<FlowDelegates>()
                .eq("status", 1)
                .le("start_time", now)
                .ge("end_time", now);
    }

    @Override
    public List<Long> activeDelegators(Long agentId) {
        List<Long> result = new ArrayList<>();
        if (agentId == null) {
            return result;
        }
        for (FlowDelegates d : delegatesRepository.selectList(activeWrapper().eq("agent_id", agentId))) {
            if (d.getDelegator_id() != null && !result.contains(d.getDelegator_id())) {
                result.add(d.getDelegator_id());
            }
        }
        return result;
    }

    @Override
    public boolean canHandle(Long agentId, Long auditorId) {
        if (agentId == null || auditorId == null || agentId.equals(auditorId)) {
            return false;
        }
        Long count = delegatesRepository.selectCount(activeWrapper()
                .eq("agent_id", agentId)
                .eq("delegator_id", auditorId));
        return count != null && count > 0;
    }

    @Override
    public Map<String, Object> list(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new LinkedHashMap<>();
        // 我委托出去的：包含已过期和已取消的，方便自己回看
        List<FlowDelegates> mine = delegatesRepository.selectList(new QueryWrapper<FlowDelegates>()
                .eq("delegator_id", userId)
                .orderByDesc("created_at"));
        // 别人委托给我的
        List<FlowDelegates> toMe = delegatesRepository.selectList(new QueryWrapper<FlowDelegates>()
                .eq("agent_id", userId)
                .orderByDesc("created_at"));
        result.put("mine", rows(mine, now, true));
        result.put("toMe", rows(toMe, now, false));
        return result;
    }

    @Override
    public FlowDelegates create(Long ownerId, Long agentId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        if (agentId == null) {
            throw new RuntimeException("请选择代理人");
        }
        if (agentId.equals(ownerId)) {
            throw new RuntimeException("不能把待办委托给自己");
        }
        Users agent = usersRepository.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("代理人不存在");
        }
        if (agent.getStatus() == null || agent.getStatus() != 1) {
            throw new RuntimeException("代理人账号已停用，请换一个人");
        }
        LocalDateTime start = startTime == null ? LocalDateTime.now() : startTime;
        LocalDateTime end = endTime;
        if (end == null) {
            throw new RuntimeException("请选择委托结束时间");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
        if (end.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("委托结束时间不能早于当前时间");
        }
        if (end.isAfter(start.plusDays(MAX_DAYS))) {
            throw new RuntimeException("单次委托最长 " + MAX_DAYS + " 天，请缩短时间范围");
        }
        // 同一对委托人的时间段不能重叠，否则一条待办两个代理
        Long overlap = delegatesRepository.selectCount(new QueryWrapper<FlowDelegates>()
                .eq("delegator_id", ownerId)
                .eq("agent_id", agentId)
                .eq("status", 1)
                .lt("start_time", end)
                .gt("end_time", start));
        if (overlap != null && overlap > 0) {
            throw new RuntimeException("该时间段内已经有相同的委托，不用重复设置");
        }

        LocalDateTime now = LocalDateTime.now();
        FlowDelegates item = new FlowDelegates();
        item.setDelegator_id(ownerId);
        item.setAgent_id(agentId);
        item.setStart_time(start);
        item.setEnd_time(end);
        item.setReason(reason);
        item.setStatus(1);
        item.setCreated_at(now);
        item.setUpdated_at(now);
        delegatesRepository.insert(item);

        // 通知代理人，让他知道待办会多出来
        try {
            NotificationRecords notice = new NotificationRecords();
            notice.setUser_id(agentId);
            notice.setTitle("有人把审批待办委托给您");
            Users owner = usersRepository.selectById(ownerId);
            notice.setContent((owner == null ? "同事" : owner.getReal_name()) + " 在 " + start.toLocalDate()
                    + " 到 " + end.toLocalDate() + " 期间把审批待办委托给您，请及时处理"
                    + (reason == null || reason.isBlank() ? "" : "（原因：" + reason + "）"));
            notice.setType(1);
            notice.setBiz_type("flow_delegate");
            notice.setBiz_id(item.getId());
            notice.setJump_url("/home/flow-todo");
            notice.setIs_read(0);
            notice.setCreated_at(now);
            notice.setUpdated_at(now);
            notificationRecordsRepository.insert(notice);
        } catch (Exception ignored) {
            // 通知失败不影响委托本身
        }
        return item;
    }

    @Override
    public void cancel(Long operatorId, Long id) {
        FlowDelegates item = delegatesRepository.selectById(id);
        if (item == null) {
            throw new RuntimeException("委托不存在");
        }
        // 委托人和代理人都能取消，管理员也能操作
        boolean admin = isAdmin(operatorId);
        if (!admin && !operatorId.equals(item.getDelegator_id()) && !operatorId.equals(item.getAgent_id())) {
            throw new RuntimeException("只能取消与自己相关的委托");
        }
        delegatesRepository.update(null, new UpdateWrapper<FlowDelegates>()
                .eq("id", id)
                .set("status", 0)
                .set("updated_at", LocalDateTime.now()));
    }

    @Override
    public List<Map<String, Object>> agentOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT u.id, u.username, u.real_name, o.org_name FROM users u"
                        + " LEFT JOIN organizations o ON o.id = u.org_id"
                        + " WHERE u.deleted_at IS NULL AND u.status = 1 ORDER BY u.org_id, u.id");
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("username", row.get("username"));
            item.put("realName", row.get("real_name"));
            item.put("orgName", row.get("org_name"));
            result.add(item);
        }
        return result;
    }

    // 拼列表展示字段，同时算出生效状态文案
    private List<Map<String, Object>> rows(List<FlowDelegates> list, LocalDateTime now, boolean mine) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FlowDelegates d : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", d.getId());
            row.put("delegatorId", d.getDelegator_id());
            row.put("agentId", d.getAgent_id());
            row.put("delegatorName", nameOf(d.getDelegator_id()));
            row.put("agentName", nameOf(d.getAgent_id()));
            row.put("delegatorOrg", orgOf(d.getDelegator_id()));
            row.put("agentOrg", orgOf(d.getAgent_id()));
            row.put("startTime", d.getStart_time());
            row.put("endTime", d.getEnd_time());
            row.put("reason", d.getReason());
            row.put("status", d.getStatus());
            row.put("active", isActive(d, now));
            row.put("statusText", statusText(d, now));
            row.put("mine", mine);
            result.add(row);
        }
        return result;
    }

    private boolean isActive(FlowDelegates d, LocalDateTime now) {
        return d.getStatus() != null && d.getStatus() == 1
                && d.getStart_time() != null && d.getEnd_time() != null
                && !now.isBefore(d.getStart_time()) && !now.isAfter(d.getEnd_time());
    }

    private String statusText(FlowDelegates d, LocalDateTime now) {
        if (d.getStatus() == null || d.getStatus() != 1) {
            return "已取消";
        }
        if (d.getStart_time() != null && now.isBefore(d.getStart_time())) {
            return "未生效";
        }
        if (d.getEnd_time() != null && now.isAfter(d.getEnd_time())) {
            return "已过期";
        }
        return "生效中";
    }

    private String nameOf(Long userId) {
        if (userId == null) return null;
        Users u = usersRepository.selectById(userId);
        return u == null ? null : u.getReal_name();
    }

    private String orgOf(Long userId) {
        if (userId == null) return null;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT o.org_name FROM users u LEFT JOIN organizations o ON o.id = u.org_id WHERE u.id = ?", userId);
        if (rows.isEmpty()) return null;
        Object name = rows.get(0).get("org_name");
        return name == null ? null : String.valueOf(name);
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) return false;
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM user_roles ur"
                + " JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND r.role_code = 'admin'"
                + " AND ur.deleted_at IS NULL", Long.class, userId);
        return count != null && count > 0;
    }
}
