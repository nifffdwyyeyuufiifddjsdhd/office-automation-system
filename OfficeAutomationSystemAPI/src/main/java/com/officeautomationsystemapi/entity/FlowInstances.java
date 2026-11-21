package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_instances")
public class FlowInstances {

    // 流程实例ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 流程定义ID
    @TableField("definition_id")
    private Long definition_id;

    // 流程编号（如 FLOW-20260911-0001）
    @TableField("instance_no")
    private String instance_no;

    // 流程标题（如：张三的请假申请）
    @TableField("title")
    private String title;

    // 发起人ID
    @TableField("creator_id")
    private Long creator_id;

    // 当前步骤ID
    @TableField("current_step_id")
    private Long current_step_id;

    // 当前步骤名称（冗余字段）
    @TableField("current_step_name")
    private String current_step_name;

    // 当前审核人ID（冗余字段，便于待办查询）
    @TableField("current_auditor_id")
    private Long current_auditor_id;

    // 状态：0草稿 1审批中 2已通过 3已驳回 4已撤回 5已取消
    @TableField("status")
    private Integer status;

    // 业务数据（JSON格式，存储申请表单数据）
    @TableField("business_data")
    private String business_data;

    // 附件ID
    @TableField("attachment_id")
    private Long attachment_id;

    // 紧急程度：0普通 1紧急
    @TableField("urgency")
    private Integer urgency;

    // 提交时间
    @TableField("submit_time")
    private LocalDateTime submit_time;

    // 完成时间（通过/驳回/撤回时间）
    @TableField("finish_time")
    private LocalDateTime finish_time;

    // 审批耗时（分钟，从提交到完成）
    @TableField("approve_duration")
    private Integer approve_duration;

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