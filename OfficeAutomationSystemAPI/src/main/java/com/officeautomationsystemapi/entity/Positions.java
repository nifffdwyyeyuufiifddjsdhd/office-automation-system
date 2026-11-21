package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("positions")
public class Positions {

    // 职位ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 职位名称
    @TableField("position_name")
    private String position_name;

    // 职位编码
    @TableField("position_code")
    private String position_code;

    // 职位级别（1-10，用于审批流自动匹配）
    @TableField("level")
    private Integer level;

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