package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("worklog_view_permissions")
public class WorklogViewPermissions {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 被查看人ID（谁的日志）
    @TableField("target_id")
    private Long target_id;

    // 查看人ID（0表示按群组/部门授权）
    @TableField("viewer_id")
    private Long viewer_id;

    // 用户群组ID（按群组授权时使用）
    @TableField("group_id")
    private Long group_id;

    // 部门ID（按部门授权时使用，扩展）
    @TableField("dept_id")
    private Long dept_id;

    // 授权类型：1按用户 2按群组 3按部门
    @TableField("permission_type")
    private Integer permission_type;

    // 授权时间
    @TableField("grant_time")
    private LocalDateTime grant_time;

    // 授权人ID
    @TableField("granter_id")
    private Long granter_id;

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