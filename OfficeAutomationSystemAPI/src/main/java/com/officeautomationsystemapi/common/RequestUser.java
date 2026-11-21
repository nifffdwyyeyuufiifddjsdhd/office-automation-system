package com.officeautomationsystemapi.common;

import jakarta.servlet.http.HttpServletRequest;

// 取拦截器写进请求的用户 ID，省去强转
public final class RequestUser {

    private static final String ATTRIBUTE = "userId";

    //构造函数
    private RequestUser() {
    }

    // 没写入时返回 null
    public static Long id(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        if (value instanceof Long id) {
            return id;
        }
        if (value instanceof String text && !text.isEmpty()) {
            return Long.valueOf(text);
        }
        return null;
    }
}
