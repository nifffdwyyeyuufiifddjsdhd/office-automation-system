package com.officeautomationsystemapi.config;

import com.alibaba.fastjson2.JSON;
import com.officeautomationsystemapi.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 按来源 IP 限流，一分钟窗口，登录接口单独收紧
// 单机内存实现，多实例部署要换成 Redis
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // 登录、找回密码这类接口每分钟最多 20 次
    private static final int AUTH_LIMIT = 20;
    // 其它接口每分钟 600 次
    private static final int API_LIMIT = 600;
    // 统计窗口，单位毫秒
    private static final long WINDOW_MS = 60_000L;
    // 桶数超过这个值就清理一次，防止内存涨满
    private static final int MAX_BUCKETS = 20_000;

    private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检不占配额
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        boolean authApi = uri.startsWith("/api/auth/login")
                || uri.startsWith("/api/auth/register")
                || uri.startsWith("/api/auth/forgot-password");
        int limit = authApi ? AUTH_LIMIT : API_LIMIT;
        String key = clientIp(request) + (authApi ? "|auth" : "|api");

        long now = System.currentTimeMillis();
        Deque<Long> window = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                return writeTooMany(response, authApi);
            }
            window.addLast(now);
        }
        if (buckets.size() > MAX_BUCKETS) {
            cleanExpired(now);
        }
        return true;
    }

    // 取来源 IP，有反向代理时用 X-Forwarded-For 第一段
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

    // 丢掉过期的桶，防止 Map 一直变大
    private void cleanExpired(long now) {
        buckets.entrySet().removeIf(entry -> {
            Deque<Long> window = entry.getValue();
            synchronized (window) {
                return window.isEmpty() || now - window.peekLast() > WINDOW_MS;
            }
        });
    }

    // 超过阈值返回 429
    private boolean writeTooMany(HttpServletResponse response, boolean authApi) throws Exception {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json;charset=UTF-8");
        String message = authApi ? "登录尝试过于频繁，请一分钟后再试" : "请求过于频繁，请稍后再试";
        response.getWriter().write(JSON.toJSONString(Result.error(429, message)));
        return false;
    }
}
