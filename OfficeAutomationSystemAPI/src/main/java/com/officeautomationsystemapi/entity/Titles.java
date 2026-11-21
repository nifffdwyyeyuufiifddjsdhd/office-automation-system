package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("titles")
public class Titles {

    // 职称ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 职称名称
    @TableField("title_name")
    private String title_name;

    // 职称编号
    @TableField("code")
    private String code;

    // 职称等级：1初级 2中级 3高级 4正高
    @TableField("title_level")
    private Integer title_level;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 备注说明
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