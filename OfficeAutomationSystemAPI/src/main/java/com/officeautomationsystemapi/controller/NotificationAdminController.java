package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.NotificationTemplates;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.repository.NotificationTemplatesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 通知管理，模板维护和推送记录查看
@RestController
@RequestMapping("/api/notifications")
public class NotificationAdminController {

    @Autowired
    private NotificationTemplatesRepository templatesRepository;

    @Autowired
    private NotificationRecordsRepository recordsRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // 占位符写成 {变量}，这里只校验花括号成对
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^{}]*\\}");

    // 通知模板

    @GetMapping("/templates")
    @RequirePermission("notify:template:manage")
    public Result<List<Map<String, Object>>> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bizType) {
        QueryWrapper<NotificationTemplates> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("template_name", kw).or().like("template_code", kw));
        }
        if (bizType != null && !bizType.trim().isEmpty()) {
            wrapper.eq("biz_type", bizType.trim());
        }
        wrapper.orderByAsc("biz_type").orderByAsc("id");
        List<NotificationTemplates> list = templatesRepository.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NotificationTemplates t : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("templateCode", t.getTemplate_code());
            row.put("templateName", t.getTemplate_name());
            row.put("bizType", t.getBiz_type());
            row.put("titleTemplate", t.getTitle_template());
            row.put("contentTemplate", t.getContent_template());
            row.put("channels", t.getChannels());
            row.put("status", t.getStatus());
            row.put("remark", t.getRemark());
            row.put("updatedAt", t.getUpdated_at());
            rows.add(row);
        }
        return Result.success(rows);
    }

    @PostMapping("/templates")
    @RequirePermission("notify:template:manage")
    public Result<Map<String, Object>> createTemplate(@RequestBody NotificationTemplates body) {
        String code = body.getTemplate_code() == null ? "" : body.getTemplate_code().trim();
        String name = body.getTemplate_name() == null ? "" : body.getTemplate_name().trim();
        if (code.isEmpty()) {
            return Result.error("模板编码不能为空");
        }
        if (name.isEmpty()) {
            return Result.error("模板名称不能为空");
        }
        String title = body.getTitle_template() == null ? "" : body.getTitle_template().trim();
        if (title.isEmpty()) {
            return Result.error("标题模板不能为空");
        }
        if (!bracketsBalanced(title) || !bracketsBalanced(body.getContent_template())) {
            return Result.error("模板里的 { } 占位符没有成对闭合，请检查");
        }
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_templates WHERE template_code = ? AND deleted_at IS NULL", Long.class, code);
        if (exists != null && exists > 0) {
            return Result.error("模板编码 " + code + " 已存在");
        }
        body.setTemplate_code(code);
        body.setTemplate_name(name);
        body.setTitle_template(title);
        body.setChannels(body.getChannels() == null || body.getChannels().trim().isEmpty() ? "system" : body.getChannels().trim());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        body.setCreated_at(LocalDateTime.now());
        body.setUpdated_at(LocalDateTime.now());
        templatesRepository.insert(body);
        Map<String, Object> data = new HashMap<>();
        data.put("id", body.getId());
        return Result.success(data);
    }

    @PutMapping("/templates/{id}")
    @RequirePermission("notify:template:manage")
    public Result<?> updateTemplate(@PathVariable Long id, @RequestBody NotificationTemplates body) {
        NotificationTemplates t = templatesRepository.selectById(id);
        if (t == null) {
            return Result.error(404, "模板不存在或已删除");
        }
        if (body.getTemplate_name() != null) {
            t.setTemplate_name(body.getTemplate_name());
        }
        if (body.getBiz_type() != null) {
            t.setBiz_type(body.getBiz_type());
        }
        if (body.getTitle_template() != null) {
            if (!bracketsBalanced(body.getTitle_template())) {
                return Result.error("标题模板里的 { } 占位符没有成对闭合");
            }
            t.setTitle_template(body.getTitle_template());
        }
        if (body.getContent_template() != null) {
            if (!bracketsBalanced(body.getContent_template())) {
                return Result.error("内容模板里的 { } 占位符没有成对闭合");
            }
            t.setContent_template(body.getContent_template());
        }
        if (body.getChannels() != null) {
            t.setChannels(body.getChannels());
        }
        if (body.getStatus() != null) {
            t.setStatus(body.getStatus());
        }
        if (body.getRemark() != null) {
            t.setRemark(body.getRemark());
        }
        t.setUpdated_at(LocalDateTime.now());
        templatesRepository.updateById(t);
        return Result.success(null);
    }

    @DeleteMapping("/templates/{id}")
    @RequirePermission("notify:template:manage")
    public Result<?> deleteTemplate(@PathVariable Long id) {
        if (templatesRepository.selectById(id) == null) {
            return Result.error(404, "模板不存在或已删除");
        }
        templatesRepository.deleteById(id);
        return Result.success(null);
    }

    // 推送记录

    // 推送记录，列出每条通知的渠道、结果和失败原因
    @GetMapping("/logs")
    @RequirePermission("notify:log:view")
    public Result<Map<String, Object>> pushLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer pushStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        pageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        QueryWrapper<NotificationRecords> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("title", kw).or().like("content", kw));
        }
        if (bizType != null && !bizType.trim().isEmpty()) {
            wrapper.eq("biz_type", bizType.trim());
        }
        if (pushStatus != null) {
            wrapper.eq("push_status", pushStatus);
        }
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start != null) {
            wrapper.ge("created_at", start.atStartOfDay());
        }
        if (end != null) {
            wrapper.le("created_at", end.atTime(23, 59, 59));
        }
        wrapper.orderByDesc("id");
        Page<NotificationRecords> page = recordsRepository.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> users = userNames(page.getRecords());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (NotificationRecords n : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getId());
            row.put("userId", n.getUser_id());
            row.put("userName", users.getOrDefault(n.getUser_id(), ""));
            row.put("title", n.getTitle());
            row.put("bizType", n.getBiz_type());
            row.put("bizId", n.getBiz_id());
            row.put("isRead", n.getIs_read());
            row.put("pushStatus", n.getPush_status());
            row.put("pushStatusText", pushStatusText(n.getPush_status()));
            row.put("pushTime", n.getPush_time());
            row.put("pushChannel", n.getPush_channel());
            row.put("pushFailReason", n.getPush_fail_reason());
            row.put("createdAt", n.getCreated_at());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }

    // 推送失败的通知可以重排，push_status 归零后由推送任务重试
    @PostMapping("/logs/{id}/resend")
    @RequirePermission("notify:log:view")
    public Result<?> resend(@PathVariable Long id) {
        NotificationRecords record = recordsRepository.selectById(id);
        if (record == null) {
            return Result.error(404, "通知记录不存在或已删除");
        }
        NotificationRecords update = new NotificationRecords();
        update.setId(id);
        update.setPush_status(0);
        update.setPush_fail_reason(null);
        update.setPush_time(null);
        recordsRepository.updateById(update);
        return Result.success(null);
    }

    private Map<Long, String> userNames(List<NotificationRecords> list) {
        Map<Long, String> map = new HashMap<>();
        if (list.isEmpty()) {
            return map;
        }
        List<Long> ids = new ArrayList<>();
        for (NotificationRecords n : list) {
            if (n.getUser_id() != null && !ids.contains(n.getUser_id())) {
                ids.add(n.getUser_id());
            }
        }
        StringBuilder sql = new StringBuilder("SELECT id, username, real_name FROM users WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")");
        List<Map<String, Object>> users = jdbc.queryForList(sql.toString(), ids.toArray());
        for (Map<String, Object> u : users) {
            Long id = ((Number) u.get("id")).longValue();
            Object realName = u.get("real_name");
            map.put(id, realName == null ? String.valueOf(u.get("username")) : realName.toString());
        }
        return map;
    }

    private String pushStatusText(Integer status) {
        if (status == null) {
            return "未推送";
        }
        return switch (status) {
            case 1 -> "推送成功";
            case 2 -> "推送失败";
            default -> "未推送";
        };
    }

    // 只校验花括号成对，模板内容可以为空
    private boolean bracketsBalanced(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        int open = 0;
        for (char c : text.toCharArray()) {
            if (c == '{') {
                open++;
            } else if (c == '}') {
                open--;
                if (open < 0) {
                    return false;
                }
            }
        }
        return open == 0;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
