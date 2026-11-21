package com.officeautomationsystemapi.config;

import com.officeautomationsystemapi.interceptor.AuditLogInterceptor;
import com.officeautomationsystemapi.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 注册限流和登录拦截器，放开跨域
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private AuditLogInterceptor auditLogInterceptor;

    // 跨域来源白名单，逗号分隔，线上换成自己的域名
    @Value("${cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    // 除登录、注册、验证码、找回密码、公开公告外，/api 都要带 token
    // 限流在登录校验之前，没登录的请求也会被限
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(                       //白名单
                        "/api/auth/login",                  //登录接口不需要拦截
                        "/api/auth/register",               //注册接口
                        "/api/auth/captcha",                //获取验证码
                        "/api/auth/forgot-password/**",     //找回密码所有相关的接口
                        // 公开公告，guest 和门户页免登录
                        "/api/public/**",
                        "/error",                           //Spring Boot默认的错误页
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                )
                .order(1);
        // 审计在登录之后，能拿到 userId
        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/api/**")
                .order(2);
    }

    // 前后端不同源，允许带凭证跨域，只放行用到的方法
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
