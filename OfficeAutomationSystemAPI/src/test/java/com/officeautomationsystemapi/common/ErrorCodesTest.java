package com.officeautomationsystemapi.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 覆盖业务错误码约定：四位数字、按模块分段、互不重复，避免前后端判定口径漂移
class ErrorCodesTest {

    // 反射取出全部 public static final int 常量，新增错误码会自动纳入检查
    private Map<String, Integer> codes() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        for (Field field : ErrorCodes.class.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (field.getType() == int.class && Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
                map.put(field.getName(), field.getInt(null));
            }
        }
        return map;
    }

    @Test
    void everyCodeIsFourDigitsAndUnique() throws Exception {
        Map<String, Integer> codes = codes();
        assertTrue(codes.size() >= 11, "错误码数量少于预期");

        List<Integer> seen = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : codes.entrySet()) {
            int value = entry.getValue();
            assertTrue(value >= 1000 && value <= 9999, entry.getKey() + " 不是四位码：" + value);
            assertTrue(!seen.contains(value), entry.getKey() + " 与其他错误码重复：" + value);
            seen.add(value);
        }
    }

    @Test
    void codesFollowModulePrefix() throws Exception {
        Map<String, Integer> codes = codes();
        assertEquals(1001, codes.get("USER_BAD_CREDENTIALS"));
        assertEquals(1004, codes.get("USER_NOT_FOUND"));
        assertEquals(2001, codes.get("FLOW_NOT_YOUR_TODO"));
        assertEquals(2003, codes.get("FLOW_NO_APPROVER"));
        assertEquals(3002, codes.get("ATTENDANCE_MAKEUP_LIMIT"));
        assertEquals(4002, codes.get("TASK_ARCHIVED"));

        // 用户段 1xxx、流程段 2xxx、出勤段 3xxx、任务段 4xxx
        for (String name : new String[]{"USER_BAD_CREDENTIALS", "USER_DISABLED", "USER_LOCKED", "USER_NOT_FOUND"}) {
            assertTrue(codes.get(name) / 1000 == 1, name + " 应落在 1xxx 用户段");
        }
        for (String name : new String[]{"FLOW_NOT_YOUR_TODO", "FLOW_ILLEGAL_STATE", "FLOW_NO_APPROVER"}) {
            assertTrue(codes.get(name) / 1000 == 2, name + " 应落在 2xxx 流程段");
        }
        assertTrue(codes.get("ATTENDANCE_ILLEGAL_STATE") / 1000 == 3);
        assertTrue(codes.get("TASK_ILLEGAL_STATE") / 1000 == 4);
    }
}
