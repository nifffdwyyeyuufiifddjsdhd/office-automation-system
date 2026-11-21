package com.officeautomationsystemapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// 只留密码加密器，接口鉴权由 LoginInterceptor 负责
@Configuration
public class SecurityConfig {

    // 登录和改密都用 BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 强度 10
        return new BCryptPasswordEncoder(10);
    }

    // 关掉 CSRF 和 session，请求全放行，token 由拦截器校验
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
