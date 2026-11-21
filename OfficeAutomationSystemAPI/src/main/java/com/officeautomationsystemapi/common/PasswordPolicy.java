package com.officeautomationsystemapi.common;

// 密码 8 到 20 位，大写、小写、数字、特殊字符至少三类，不含用户名或姓名
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword, String username, String realName) {
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 20) {
            throw new RuntimeException("密码长度需8-20位");
        }

        int kinds = 0;
        if (rawPassword.matches(".*[a-z].*")) {
            kinds++;
        }
        if (rawPassword.matches(".*[A-Z].*")) {
            kinds++;
        }
        if (rawPassword.matches(".*\\d.*")) {
            kinds++;
        }
        if (rawPassword.matches(".*[^a-zA-Z0-9].*")) {
            kinds++;
        }
        if (kinds < 3) {
            throw new RuntimeException("密码需包含大小写字母，数字，特殊字符中至少3种");
        }

        String lower = rawPassword.toLowerCase();
        // 空串调 contains 恒为真，先排除空值
        if (username != null && !username.isBlank() && lower.contains(username.toLowerCase())) {
            throw new RuntimeException("密码不能包含用户名");
        }
        if (realName != null && !realName.isBlank() && lower.contains(realName.toLowerCase())) {
            throw new RuntimeException("密码不能包含姓名");
        }
    }
}
