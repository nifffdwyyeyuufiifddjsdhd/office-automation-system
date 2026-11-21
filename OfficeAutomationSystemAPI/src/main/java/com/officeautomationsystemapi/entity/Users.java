package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class Users {

    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户名（登录账号）
    @TableField("username")
    private String username;

    // 密码（BCrypt加密）
    @TableField("password")
    private String password;

    // 真实姓名
    @TableField("real_name")
    private String real_name;

    // 性别：0未知 1男 2女
    @TableField("gender")
    private Integer gender;

    // 手机号
    @TableField("phone")
    private String phone;

    // 邮箱
    @TableField("email")
    private String email;

    // 头像URL
    @TableField("avatar")
    private String avatar;

    // 所属组织/部门ID
    @TableField("org_id")
    private Long org_id;

    // 职位ID
    @TableField("position_id")
    private Long position_id;

    // 职称ID
    @TableField("title_id")
    private Long title_id;

    // 入职日期
    @TableField("entry_date")
    private LocalDate entry_date;

    // 状态：0禁用 1正常 2锁定
    @TableField("status")
    private Integer status;

    // 最后登录时间
    @TableField("last_login_time")
    private LocalDateTime last_login_time;

    // 最后登录IP
    @TableField("last_login_ip")
    private String last_login_ip;

    // 最后退出时间
    @TableField("last_logout_time")
    private LocalDateTime last_logout_time;

    // 连续登录失败次数
    @TableField("login_fail_count")
    private Integer login_fail_count;

    // 账号锁定时间
    @TableField("lock_time")
    private LocalDateTime lock_time;

    // 企业微信/钉钉用户ID（用于推送绑定）
    @TableField("wechat_id")
    private String wechat_id;

    // 紧急联系人
    @TableField("emergency_contact")
    private String emergency_contact;

    // 紧急联系电话
    @TableField("emergency_phone")
    private String emergency_phone;

    // 工资卡/报销收款卡号
    @TableField("bank_account")
    private String bank_account;

    // 开户行
    @TableField("bank_name")
    private String bank_name;

    // 月薪，只有 HR/财务/总经理/管理员能看到
    @TableField("salary")
    private java.math.BigDecimal salary;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime created_at;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    // 删除时间（软删除）
    @TableField("deleted_at")
    private LocalDateTime deleted_at;

    @TableField("must_change_password")
    private Integer must_change_password;
}