package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exception_logs")
public class ExceptionLogs {

    // 异常ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 操作用户ID
    @TableField("user_id")
    private Long user_id;

    // 操作用户名
    @TableField("username")
    private String username;

    // 请求URL
    @TableField("url")
    private String url;

    // 请求方法
    @TableField("method")
    private String method;

    // 请求IP
    @TableField("ip_address")
    private String ip_address;

    // 浏览器信息
    @TableField("browser")
    private String browser;

    // 操作系统
    @TableField("os")
    private String os;

    // 屏幕分辨率
    @TableField("screen_resolution")
    private String screen_resolution;

    // 异常类型（如 NullPointerException, SQLException）
    @TableField("exception_type")
    private String exception_type;

    // 异常信息
    @TableField("error_msg")
    private String error_msg;

    // 异常堆栈
    @TableField("stack_trace")
    private String stack_trace;

    // 请求参数（JSON格式，敏感字段脱敏）
    @TableField("request_params")
    private String request_params;

    // 处理状态：0未解决 1已解决
    @TableField("status")
    private Integer status;

    // 是否已解决：0否 1是
    @TableField("resolved")
    private Integer resolved;

    // 处理人ID
    @TableField("resolved_by")
    private Long resolved_by;

    // 处理时间
    @TableField("resolved_time")
    private LocalDateTime resolved_time;

    // 处理说明
    @TableField("resolve_note")
    private String resolve_note;

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