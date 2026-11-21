package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 覆盖敏感字段脱敏：手机号/邮箱打码、正文里的手机号替换、本人与有权限的人看明文
class SensitiveDataServiceTest {

    private SensitiveDataService service;

    // 极简权限桩：admin 用户 ID 固定为 1，其它用户按给定权限码返回
    private static class StubPermissionService implements PermissionService {
        private final Set<String> perms;

        StubPermissionService(Set<String> perms) {
            this.perms = perms;
        }

        @Override
        public Set<String> getUserPermissions(Long userId) {
            return perms;
        }

        @Override
        public List<String> getUserRoleCodes(Long userId) {
            return List.of("employee");
        }

        @Override
        public boolean isAdmin(Long userId) {
            return userId != null && userId == 1L;
        }
    }

    @BeforeEach
    void setUp() {
        service = new SensitiveDataService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "permissionService",
                new StubPermissionService(Set.of()));
    }

    @Test
    void maskPhoneKeepsHeadAndTail() {
        assertEquals("138****8000", service.maskPhone("13800138000"));
        assertEquals("123", service.maskPhone("123"));
        assertEquals("", service.maskPhone(null));
    }

    @Test
    void maskEmailKeepsDomain() {
        assertEquals("h***@oa.com", service.maskEmail("hr@oa.com"));
        assertEquals("a***@x.cn", service.maskEmail("ab@x.cn"));
        assertEquals("", service.maskEmail(null));
    }

    @Test
    void maskTextReplacesPhoneInsideLog() {
        String masked = service.maskText("{\"phone\":\"13800138000\",\"note\":\"联系 13912345678 处理\"}");
        assertTrue(masked.contains("138****8000"));
        assertTrue(masked.contains("139****5678"));
        assertFalse(masked.contains("13800138000"));
    }

    @Test
    void permissionControlsPlainText() {
        UserVO vo = new UserVO();
        vo.setId(5L);
        vo.setPhone("13800138000");
        vo.setEmail("hr@oa.com");

        // 普通访客：脱敏
        UserVO masked = new UserVO();
        masked.setId(5L);
        masked.setPhone(vo.getPhone());
        masked.setEmail(vo.getEmail());
        service.maskUser(masked, 4L);
        assertEquals("138****8000", masked.getPhone());

        // 本人：保持明文
        UserVO self = new UserVO();
        self.setId(5L);
        self.setPhone(vo.getPhone());
        self.setEmail(vo.getEmail());
        service.maskUser(self, 5L);
        assertEquals("13800138000", self.getPhone());

        // 管理员：保持明文
        UserVO adminView = new UserVO();
        adminView.setId(5L);
        adminView.setPhone(vo.getPhone());
        adminView.setEmail(vo.getEmail());
        service.maskUser(adminView, 1L);
        assertEquals("hr@oa.com", adminView.getEmail());
    }

    @Test
    void salaryStrippedWithoutPermission() {
        UserVO vo = new UserVO();
        vo.setSalary(new java.math.BigDecimal("8800.00"));
        service.stripSalaryIfForbidden(vo, 4L);
        assertNull(vo.getSalary());

        UserVO admin = new UserVO();
        admin.setSalary(new java.math.BigDecimal("8800.00"));
        service.stripSalaryIfForbidden(admin, 1L);
        assertEquals(new java.math.BigDecimal("8800.00"), admin.getSalary());
    }
}
