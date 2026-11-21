package com.officeautomationsystemapi.service.impl;

import com.officeautomationsystemapi.common.PasswordPolicy;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// 忘记密码：邮箱验证码校验后重置为临时密码
@Service
public class ForgetPasswordService {

    // 验证码有效期 5 分钟
    private static final long CODE_TTL_MILLIS = 5 * 60 * 1000;

    // 同一验证码最多输错 5 次，超过就作废，避免 6 位码被逐个试出来
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 验证码放在内存里，key 是用户名，value 里带有效期和已输错次数
    private final Map<String, CodeEntry> codeMap = new ConcurrentHashMap<>();

    // 验证码、过期时间、已输错次数绑在一起，避免拆成多个 Map 后对不上
    private static final class CodeEntry {

        private final String code;
        private final long expireAt;
        private int attempts;

        private CodeEntry(String code) {
            this.code = code;
            this.expireAt = System.currentTimeMillis() + CODE_TTL_MILLIS;
        }

        private boolean expired() {
            return expireAt < System.currentTimeMillis();
        }

        // 输错一次累加一次，加同步保证并发请求下次数不会算少
        private synchronized int addAttempt() {
            attempts++;
            return attempts;
        }
    }

    // 第一步：校验用户名和邮箱是否匹配，然后发送 6 位验证码
    public void sendCode(String username, String email) {
        Users user = usersRepository.findUserByUserName(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }
        // 停用或已删除的账号不发验证码，避免给不能登录的号改密
        if (user.getDeleted_at() != null || user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已停用或删除，请联系管理员");
        }
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("该账号未绑定邮箱，请联系管理员重置密码");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        // 每次重新获取都是一份全新的验证码，次数从 0 开始
        codeMap.put(username, new CodeEntry(code));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("OA办公系统找回密码");
        message.setText("您好，" + user.getReal_name() + "：\n\n"
                + "您的验证码是：" + code + "\n"
                + "5 分钟内有效，请勿泄露给他人。\n\n"
                + "--OA办公系统");
        mailSender.send(message);
    }

    // 第二步：校验验证码和邮箱后重置密码，登录后仍要求改密
    public void resetPassword(String username, String email, String code, String newPassword) {
        CodeEntry entry = codeMap.get(username);
        if (entry == null) {
            throw new RuntimeException("请先获取验证码");
        }
        if (entry.expired()) {
            codeMap.remove(username);
            throw new RuntimeException("验证码已过期，请先获取");
        }
        if (!entry.code.equals(code)) {
            int attempts = entry.addAttempt();
            // 试满次数就作废，必须重新获取验证码
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                codeMap.remove(username);
                throw new RuntimeException("验证码错误次数过多，验证码已失效，请重新获取");
            }
            throw new RuntimeException("验证码错误，还可以尝试" + (MAX_VERIFY_ATTEMPTS - attempts) + "次");
        }

        Users user = usersRepository.findUserByUserName(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("邮箱与注册邮箱不一样");
        }

        PasswordPolicy.validate(newPassword, user.getUsername(), user.getReal_name());

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMust_change_password(1);
        usersRepository.updateById(user);
        codeMap.remove(username);
    }
}
