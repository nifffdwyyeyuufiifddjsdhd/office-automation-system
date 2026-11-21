package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 敏感字段处理：手机号、邮箱脱敏，日志正文里的手机号也换掉
// 能否看明文由 user:sensitive:view 决定，管理员豁免
@Service
public class SensitiveDataService {

    // 查看完整手机号、邮箱的权限码
    public static final String PERM_VIEW_SENSITIVE = "user:sensitive:view";

    // 查看薪资的权限码，薪资比联系方式更敏感，单独控制
    public static final String PERM_VIEW_SALARY = "user:salary:view";

    // 11 位手机号，前后都不能是数字，避免把长串单号误伤
    private static final Pattern PHONE_IN_TEXT = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");

    @Autowired
    private PermissionService permissionService;

    // 是否允许看到明文敏感字段
    public boolean canViewSensitive(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionService.isAdmin(userId)) {
            return true;
        }
        return permissionService.getUserPermissions(userId).contains(PERM_VIEW_SENSITIVE);
    }

    // 是否能看薪资：只有 HR、财务、总经理和管理员
    public boolean canViewSalary(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionService.isAdmin(userId)) {
            return true;
        }
        return permissionService.getUserPermissions(userId).contains(PERM_VIEW_SALARY);
    }

    // 手机号保留前 3 位和后 4 位
    public String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    // 邮箱保留首字符和域名，例如 l***@oa.com
    public String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return value;
        }
        String local = value.substring(0, at);
        String head = local.length() <= 1 ? "*" : local.substring(0, 1);
        return head + "***" + value.substring(at);
    }

    // 把文本（一般是请求参数 JSON）里的手机号换成脱敏值
    public String maskText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = PHONE_IN_TEXT.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String phone = m.group(1);
            String masked = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
            // 用 quoteReplacement 防止手机号字符被当成转义
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // 列表批量脱敏，本人的联系方式保持明文
    // 薪资和联系方式分开判断权限，没权限的置空
    public void maskUsers(List<UserVO> list, Long viewerId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        boolean canContact = canViewSensitive(viewerId);
        boolean canSalary = canViewSalary(viewerId);
        if (canContact && canSalary) {
            return;
        }
        for (UserVO vo : list) {
            if (vo == null) {
                continue;
            }
            boolean self = vo.getId() != null && vo.getId().equals(viewerId);
            if (!canContact && !self) {
                vo.setPhone(maskPhone(vo.getPhone()));
                vo.setEmail(maskEmail(vo.getEmail()));
            }
            if (!canSalary) {
                vo.setSalary(null);
            }
        }
    }

    // 单条脱敏，详情和导出共用
    public void maskUser(UserVO vo, Long viewerId) {
        if (vo == null) {
            return;
        }
        boolean self = vo.getId() != null && vo.getId().equals(viewerId);
        if (!canViewSensitive(viewerId) && !self) {
            vo.setPhone(maskPhone(vo.getPhone()));
            vo.setEmail(maskEmail(vo.getEmail()));
        }
        if (!canViewSalary(viewerId)) {
            vo.setSalary(null);
        }
    }

    // 保存用户时抹掉没权限的人传上来的薪资
    public void stripSalaryIfForbidden(UserVO vo, Long operatorId) {
        if (vo != null && !canViewSalary(operatorId)) {
            vo.setSalary(null);
        }
    }
}
