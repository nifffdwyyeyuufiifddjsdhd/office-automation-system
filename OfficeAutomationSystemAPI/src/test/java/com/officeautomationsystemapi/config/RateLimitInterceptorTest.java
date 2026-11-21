package com.officeautomationsystemapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 覆盖限流：登录接口第 21 次请求被挡，普通接口前 3 次正常，OPTIONS 预检不占配额
class RateLimitInterceptorTest {

    @Test
    void loginIsRateLimited() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor();
        for (int i = 0; i < 20; i++) {
            assertTrue(interceptor.preHandle(request("POST", "/api/auth/login", "10.0.0.9"),
                    new MockHttpServletResponse(), new Object()));
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("POST", "/api/auth/login", "10.0.0.9"), blocked, new Object()));
        assertEquals(429, blocked.getStatus());
    }

    @Test
    void normalApiAllowsBurst() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor();
        for (int i = 0; i < 3; i++) {
            assertTrue(interceptor.preHandle(request("GET", "/api/tasks", "10.0.0.10"),
                    new MockHttpServletResponse(), new Object()));
        }
    }

    @Test
    void optionsRequestSkipped() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor();
        assertTrue(interceptor.preHandle(request("OPTIONS", "/api/auth/login", "10.0.0.11"),
                new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request(String method, String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        return request;
    }
}
