package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_roles")
public class UserRoles {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long user_id;

    // 角色ID
    @TableField("role_id")
    private Long role_id;

    // 授权类型：1直接授权 2继承授权
    @TableField("grant_type")
    private Integer grant_type;

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