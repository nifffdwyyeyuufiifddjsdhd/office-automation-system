package com.officeautomationsystemapi.utilt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 覆盖 token 签发与解析：subject 存用户 ID，过期时间按配置小时数
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "jingtian2026nian9yue18haoxingqiwuqing");
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 24L);
    }

    @Test
    void generateAndParseToken() {
        String token = jwtUtil.generateToken(7L, "gm01");
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("7", claims.getSubject());
        assertEquals("gm01", claims.get("username"));
    }

    @Test
    void parseTokenRejectsTamperedToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(Exception.class, () -> jwtUtil.parseToken(tampered));
    }
}
