package com.officeautomationsystemapi.utilt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 签发和解析 JWT，有效期 24 小时
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // 密钥从配置读，没配就用默认值
    @Value("${jwt.secret:jingtian2026nian9yue18haoxingqiwuqing}")
    private String secret;

    private static final String DEFAULT_SECRET = "jingtian2026nian9yue18haoxingqiwuqing";

    // 有效期小时数，可用 jwt.expire-hours 配置
    @Value("${jwt.expire-hours:24}")
    private long expireHours;

    @jakarta.annotation.PostConstruct
    public void checkSecret() {
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("JWT 正在使用内置默认密钥");
        }
        if (expireHours <= 0 || expireHours > 24 * 30) {
            log.warn("jwt.expire-hours 配置为 {}，超出 1-720 小时，按 24 小时处理", expireHours);
            expireHours = 24;
        }
    }

    // 密钥只构建一次，签名和解析共用
    private SecretKey key;

    // 注入完成后再按需构建密钥
    private SecretKey getKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }

    // 用户 ID 放 subject，用户名放自定义 claim
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)        //自定字段
                .issuedAt(now)                         //设置签发时间
                //设置过期时间，当前时间+（小时数*60*60*1000ms）
                .expiration(new Date(now.getTime() + expireHours * 60 * 60 * 1000L))
                .signWith(getKey())                     //使用密钥进行签名，防止篡改
                .compact();                             //压缩并生成最终的字符串
    }

    // 校验签名和过期时间，失败抛异常
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())               //用相同的的密钥验签
                .build()                            //构建解析器
                .parseSignedClaims(token)           //解析传进来的Token，假如过期/篡改 则会抛出异常
                .getPayload();                      //获取Claims对象
    }
}
