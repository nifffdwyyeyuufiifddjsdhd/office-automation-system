package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_step_transitions")
public class FlowStepTransitions {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 当前步骤ID
    @TableField("step_id")
    private Long step_id;

    // 目标步骤ID
    @TableField("target_step_id")
    private Long target_step_id;

    // 跳转条件表达式（如 amount>1000 && type==‘expense‘）
    @TableField("condition_expr")
    private String condition_expr;

    // 条件名称（如：金额大于1000）
    @TableField("condition_name")
    private String condition_name;

    // 条件优先级（数字越小越优先，先匹配先走）
    @TableField("priority")
    private Integer priority;

    // 是否默认路径：0否 1是（所有条件不匹配时走默认）
    @TableField("is_default")
    private Integer is_default;

    // 跳转类型：1正常通过 2驳回 3转交 4撤回
    @TableField("transition_type")
    private Integer transition_type;

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