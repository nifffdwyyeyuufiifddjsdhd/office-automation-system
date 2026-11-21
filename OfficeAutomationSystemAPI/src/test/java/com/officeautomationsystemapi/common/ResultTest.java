package com.officeautomationsystemapi.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 覆盖统一响应结构：成功码文案、失败码、自定义码、timestamp 每次生成
class ResultTest {

    @Test
    void successWrapsDataWithDefaultMessage() {
        Result<String> result = Result.success("ok");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("ok", result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void successWithoutDataAndWithCustomMessage() {
        Result<Void> empty = Result.success();
        assertEquals(200, empty.getCode());
        assertNull(empty.getData());

        Result<Integer> custom = Result.success("已保存", 3);
        assertEquals(200, custom.getCode());
        assertEquals("已保存", custom.getMessage());
        assertEquals(3, custom.getData());
    }

    @Test
    void errorUsesFiveHundredByDefault() {
        Result<Void> result = Result.error("金额不合法");
        assertEquals(500, result.getCode());
        assertEquals("金额不合法", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void errorKeepsBusinessCode() {
        Result<Void> result = Result.error(ErrorCodes.USER_BAD_CREDENTIALS, "用户名或密码错误");
        assertEquals(1001, result.getCode());
        assertEquals("用户名或密码错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void timestampIsFreshOnEveryCall() throws Exception {
        long before = System.currentTimeMillis();
        Result<String> first = Result.success("a");
        Thread.sleep(5);
        Result<String> second = Result.success("b");
        long after = System.currentTimeMillis();

        assertTrue(first.getTimestamp() >= before && first.getTimestamp() <= after);
        assertTrue(second.getTimestamp() > first.getTimestamp());
    }
}
