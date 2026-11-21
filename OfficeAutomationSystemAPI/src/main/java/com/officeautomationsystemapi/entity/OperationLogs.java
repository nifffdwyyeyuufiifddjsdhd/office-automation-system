package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_logs")
public class OperationLogs {

    // 日志ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 操作用户ID
    @TableField("user_id")
    private Long user_id;

    // 操作用户名（冗余）
    @TableField("username")
    private String username;

    // 操作模块（如 user, role, announcement）
    @TableField("module")
    private String module;

    // 操作类型（如 create, update, delete, login, export）
    @TableField("action")
    private String action;

    // 请求方法（GET, POST, PUT, DELETE）
    @TableField("method")
    private String method;

    // 请求URL
    @TableField("url")
    private String url;

    // 操作IP
    @TableField("ip_address")
    private String ip_address;

    // 浏览器 User-Agent
    @TableField("user_agent")
    private String user_agent;

    // 操作对象类型（如 user, role）
    @TableField("target_type")
    private String target_type;

    // 操作对象ID
    @TableField("target_id")
    private Long target_id;

    // 请求参数（JSON格式，敏感字段脱敏）
    @TableField("request_params")
    private String request_params;

    // 响应结果摘要
    @TableField("response_result")
    private String response_result;

    // 操作状态：0失败 1成功
    @TableField("status")
    private Integer status;

    // 错误信息（失败时记录）
    @TableField("error_msg")
    private String error_msg;

    // 耗时（毫秒）
    @TableField("cost_time")
    private Integer cost_time;

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