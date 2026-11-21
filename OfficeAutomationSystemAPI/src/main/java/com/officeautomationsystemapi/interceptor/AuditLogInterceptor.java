package com.officeautomationsystemapi.interceptor;

import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

// 业务没单独记日志的 POST/PUT/PATCH/DELETE 在这里补一条
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "AUDIT_START_MS";

    // 登录登出有自己的日志，这里跳过
    private static final Set<String> SKIP_PREFIX = Set.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/captcha",
            "/api/auth/register",
            "/api/auth/forgot-password");

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!writeMethod(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return true;
        }
        for (String prefix : SKIP_PREFIX) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (!(start instanceof Long began)) {
            return;
        }
        // 业务层已写过详细日志，这里不再记
        if (Boolean.TRUE.equals(request.getAttribute(AuditLogService.RECORDED_ATTR))) {
            return;
        }
        long spent = Math.max(0L, System.currentTimeMillis() - began);
        int cost = (int) Math.min(Integer.MAX_VALUE, spent);
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String url = query == null || query.isEmpty() ? uri : uri + "?" + query;
        boolean success = ex == null && response.getStatus() < 400;
        String error = ex != null ? ex.getMessage() : (success ? null : "HTTP " + response.getStatus());
        try {
            auditLogService.record(RequestUser.id(request), moduleOf(uri), actionOf(request.getMethod(), uri),
                    request.getMethod(), url, clientIp(request), request.getHeader("User-Agent"),
                    query, null, success ? 1 : 0, error, cost);
        } catch (Exception ignore) {
            // 审计出错不能影响响应
        }
    }

    private boolean writeMethod(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
    }

    // api/users/12 取 user，模块名用单数
    private String moduleOf(String uri) {
        String[] parts = uri.split("/");
        String segment = parts.length > 2 ? parts[2] : "api";
        return switch (segment) {
            case "users" -> "user";
            case "roles" -> "role";
            case "tasks" -> "task";
            case "projects" -> "project";
            case "worklogs" -> "worklog";
            case "announcements" -> "announcement";
            case "notifications" -> "notification";
            case "recruitment" -> "recruitment";
            case "finance" -> "finance";
            case "attendance" -> "attendance";
            case "flow" -> "flow";
            case "system" -> "system";
            case "profile" -> "profile";
            case "groups" -> "group";
            case "positions" -> "position";
            case "titles" -> "title";
            case "organizations" -> "organization";
            case "online" -> "online";
            case "public" -> "public";
            default -> segment;
        };
    }

    // 先看路径里的动作词，没有就按 HTTP 方法归类
    private String actionOf(String method, String uri) {
        String path = uri.toLowerCase();
        String[][] keywords = {
                {"/batch-approve", "batch-approve"},
                {"/export", "export"},
                {"/approve", "approve"},
                {"/reject", "reject"},
                {"/resubmit", "resubmit"},
                {"/withdraw", "withdraw"},
                {"/transfer", "transfer"},
                {"/urge", "urge"},
                {"/handover", "handover"},
                {"/onboard", "onboard"},
                {"/reset-password", "reset-password"},
                {"/pay", "pay"},
                {"/void", "void"},
                {"/move", "move"},
                {"/archive", "archive"},
                {"/unarchive", "unarchive"},
                {"/members", "member"},
                {"/permissions", "permission"},
                {"/status", "status"},
                {"/read", "read"},
                {"/resolve", "resolve"},
                {"/bindings", "binding"}
        };
        for (String[] keyword : keywords) {
            if (path.contains(keyword[0])) {
                return keyword[1];
            }
        }
        return switch (method.toUpperCase()) {
            case "POST" -> "create";
            case "PUT", "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> method.toLowerCase();
        };
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
