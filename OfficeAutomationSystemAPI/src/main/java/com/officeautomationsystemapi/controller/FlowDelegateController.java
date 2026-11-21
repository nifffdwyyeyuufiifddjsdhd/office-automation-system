package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.FlowDelegates;
import com.officeautomationsystemapi.service.FlowDelegateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 审批委托，审核人出差时把待办临时交给同事
@RestController
@RequestMapping("/api/flow/delegates")
public class FlowDelegateController {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private FlowDelegateService flowDelegateService;

    // 我发起的委托和委托给我的
    @GetMapping
    public Result<Map<String, Object>> list(HttpServletRequest request) {
        return Result.success(flowDelegateService.list(RequestUser.id(request)));
    }

    // 可选代理人，任意在职用户都行，不限部门范围
    @GetMapping("/agent-options")
    public Result<List<Map<String, Object>>> agentOptions() {
        return Result.success(flowDelegateService.agentOptions());
    }

    // 新建委托，body 传 agentId、startTime、endTime、reason
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long agentId = toLong(body.get("agentId"));
        if (agentId == null) {
            return Result.error("请选择代理人");
        }
        LocalDateTime start = toDateTime(body.get("startTime"));
        LocalDateTime end = toDateTime(body.get("endTime"));
        FlowDelegates item = flowDelegateService.create(RequestUser.id(request), agentId, start, end,
                body.get("reason") == null ? null : String.valueOf(body.get("reason")));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("startTime", item.getStart_time());
        data.put("endTime", item.getEnd_time());
        return Result.success(data);
    }

    // 取消委托
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        flowDelegateService.cancel(RequestUser.id(request), id);
        return Result.success(null);
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 兼容 2026-10-23T09:00:00 和 2026-10-23 09:00:00 两种格式
    private LocalDateTime toDateTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        String text = String.valueOf(value).trim().replace('T', ' ');
        try {
            if (text.length() == 10) {
                return LocalDateTime.parse(text + " 00:00:00", DATE_TIME_FMT);
            }
            if (text.length() == 16) {
                text = text + ":00";
            }
            return LocalDateTime.parse(text, DATE_TIME_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
