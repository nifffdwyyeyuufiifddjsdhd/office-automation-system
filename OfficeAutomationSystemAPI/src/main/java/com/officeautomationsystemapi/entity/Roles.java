package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("roles")
public class Roles {

    // 角色ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 角色名称
    @TableField("role_name")
    private String role_name;

    // 角色编码（如 admin, employee, dept_manager）
    @TableField("role_code")
    private String role_code;

    // 角色类型：1系统角色 2业务角色 3动态角色
    @TableField("role_type")
    private Integer role_type;

    // 角色描述
    @TableField("description")
    private String description;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 是否系统内置：0否 1是（内置不可删除）
    @TableField("is_system")
    private Integer is_system;

    // 数据权限范围：1全部 2本部门 3本部门及以下 4本人 5自定义
    @TableField("data_scope")
    private Integer data_scope;

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