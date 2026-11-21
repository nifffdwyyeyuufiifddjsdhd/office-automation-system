package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organizations")
public class Organizations {

    // 组织ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父组织ID（0表示顶级）
    @TableField("parent_id")
    private Long parent_id;

    // 组织名称
    @TableField("org_name")
    private String org_name;

    // 组织编码
    @TableField("org_code")
    private String org_code;

    // 组织类型：1公司 2部门 3小组
    @TableField("org_type")
    private Integer org_type;

    // 负责人用户ID
    @TableField("leader_id")
    private Long leader_id;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

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