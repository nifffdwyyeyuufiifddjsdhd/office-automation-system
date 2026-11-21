package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String real_name;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private Long org_id;
    private String org_name;    // 所属部门名称
    private Long position_id;
    private String position_name;   // 职位名称
    private Long title_id;
    private String title_name;      // 职称名称
    private LocalDate entry_date;
    private String emergency_contact;   // 紧急联系人
    private String emergency_phone;     // 紧急联系电话
    private String bank_account;        // 银行卡号
    private String bank_name;           // 开户行
    private java.math.BigDecimal salary; // 月薪，无 user:salary:view 时后端置空
    private Integer status;
    private LocalDateTime last_login_time;
    private LocalDateTime lock_time;
    private LocalDateTime created_at;
    private String password;        // 仅新增/重置密码时使用，列表不返回
    private List<Long> roleIds;     // 用户绑定的角色ID
    private List<String> roleNames; // 用户绑定的角色名称
}
