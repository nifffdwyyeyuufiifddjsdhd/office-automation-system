package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("worklog_categories")
public class WorklogCategories {

    // 类别ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 类别名称
    @TableField("category_name")
    private String category_name;

    // 类别编码
    @TableField("category_code")
    private String category_code;

    // 类别描述
    @TableField("description")
    private String description;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 创建人ID
    @TableField("creator_id")
    private Long creator_id;

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