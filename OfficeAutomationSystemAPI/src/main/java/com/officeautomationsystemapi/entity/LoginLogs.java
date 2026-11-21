package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_logs")
public class LoginLogs {

    // 日志ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID（登录成功时记录）
    @TableField("user_id")
    private Long user_id;

    // 登录用户名
    @TableField("username")
    private String username;

    // 登录时间
    @TableField("login_time")
    private LocalDateTime login_time;

    // 登录IP
    @TableField("ip_address")
    private String ip_address;

    // 登录地点（IP解析）
    @TableField("location")
    private String location;

    // 设备类型（如 Windows, iOS, Android）
    @TableField("device")
    private String device;

    // 浏览器信息
    @TableField("browser")
    private String browser;

    // 操作系统
    @TableField("os")
    private String os;

    // 登录状态：0失败 1成功
    @TableField("status")
    private Integer status;

    // 失败原因（如密码错误、账号锁定）
    @TableField("fail_reason")
    private String fail_reason;

    // 登出时间
    @TableField("logout_time")
    private LocalDateTime logout_time;

    // 会话ID
    @TableField("session_id")
    private String session_id;

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
}