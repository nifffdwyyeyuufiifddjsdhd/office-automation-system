package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement_categories")
public class AnnouncementCategories {

    // 类别ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父类别ID
    @TableField("parent_id")
    private Long parent_id;

    // 类别名称
    @TableField("category_name")
    private String category_name;

    // 类别编码
    @TableField("category_code")
    private String category_code;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 查看权限：1全部可见 2指定角色可见
    @TableField("view_permission_type")
    private Integer view_permission_type;

    // 图标
    @TableField("icon")
    private String icon;

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