package com.officeautomationsystemapi.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 覆盖密码规则：长度 8-20、大小写字母/数字/特殊字符至少三类、不能包含用户名或姓名
class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abc12345!", "zhangsan", "张三"));
    }

    @Test
    void rejectsNullOrTooShortOrTooLong() {
        assertEquals("密码长度需8-20位",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate(null, "u", "n")).getMessage());
        assertEquals("密码长度需8-20位",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("Ab1!", "u", "n")).getMessage());
        assertEquals("密码长度需8-20位",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("Abc12345!Abc12345!Abc1", "u", "n")).getMessage());
    }

    @Test
    void requiresAtLeastThreeCharacterKinds() {
        // 只有小写字母与数字两种，缺第三类
        assertEquals("密码需包含大小写字母，数字，特殊字符中至少3种",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("abc12345", "u", "n")).getMessage());
        // 小写 + 大写 + 数字三种，可以过
        assertDoesNotThrow(() -> PasswordPolicy.validate("abcDEF12", "u", "n"));
    }

    @Test
    void rejectsPasswordContainingUsernameIgnoringCase() {
        assertEquals("密码不能包含用户名",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("Zhangsan1!", "ZhangSan", "李四")).getMessage());
    }

    @Test
    void rejectsPasswordContainingRealName() {
        assertEquals("密码不能包含姓名",
                assertThrows(RuntimeException.class, () -> PasswordPolicy.validate("ZhangSan1!", "lisi", "zhangsan")).getMessage());
    }

    @Test
    void blankUsernameOrRealNameDoesNotBlockPassword() {
        // 空串调用 contains("") 永远为真，这里确认空值与全空格都被跳过
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abc12345!", null, null));
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abc12345!", "", "  "));
    }
}
