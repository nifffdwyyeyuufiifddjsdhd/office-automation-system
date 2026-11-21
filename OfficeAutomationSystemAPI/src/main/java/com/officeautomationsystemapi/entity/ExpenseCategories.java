package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 费用类别字典：用 keywords 把报销单自动归类
@Data
@TableName("expense_categories")
public class ExpenseCategories {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 类别名称
    @TableField("category_name")
    private String category_name;

    // 类别编码，唯一
    @TableField("category_code")
    private String category_code;

    // 识别关键词，逗号分隔
    @TableField("keywords")
    private String keywords;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 备注
    @TableField("remark")
    private String remark;

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
