package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("online_users")
public class OnlineUsers {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID（同一用户只保留最新会话）
    @TableField("user_id")
    private Long user_id;

    // 用户名
    @TableField("username")
    private String username;

    // 会话ID
    @TableField("session_id")
    private String session_id;

    // 登录时间
    @TableField("login_time")
    private LocalDateTime login_time;

    // 最后活跃时间
    @TableField("last_active_time")
    private LocalDateTime last_active_time;

    // IP地址
    @TableField("ip_address")
    private String ip_address;

    // 浏览器
    @TableField("browser")
    private String browser;

    // 操作系统
    @TableField("os")
    private String os;

    // 设备类型
    @TableField("device")
    private String device;

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