package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_permissions")
public class RolePermissions {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 角色ID
    @TableField("role_id")
    private Long role_id;

    // 菜单/权限ID
    @TableField("menu_id")
    private Long menu_id;

    // 权限类型：1菜单 2按钮
    @TableField("permission_type")
    private Integer permission_type;

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