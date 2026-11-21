package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 用户级额外权限：角色之外单独授予，如任务中心访问权
@Data
@TableName("user_permissions")
public class UserPermissions {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 被授权的用户
    @TableField("user_id")
    private Long user_id;

    // 权限（menus.id）
    @TableField("menu_id")
    private Long menu_id;

    // 授权人
    @TableField("granted_by")
    private Long granted_by;

    // 来源：1 管理员授予 2 派发任务自动开通
    @TableField("source")
    private Integer source;

    // 授予原因
    @TableField("reason")
    private String reason;

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
