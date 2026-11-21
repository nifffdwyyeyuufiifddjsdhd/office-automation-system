package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_approvals")
public class FlowApprovals {

    // 审批记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 流程实例ID
    @TableField("instance_id")
    private Long instance_id;

    // 审批步骤ID
    @TableField("step_id")
    private Long step_id;

    // 审批人ID
    @TableField("approver_id")
    private Long approver_id;

    // 操作：1通过 2驳回 3转交 4撤回 5催办 6加签
    @TableField("action")
    private Integer action;

    // 审批意见
    @TableField("comment")
    private String comment;

    // 审批时间
    @TableField("approve_time")
    private LocalDateTime approve_time;

    // 目标用户ID（转交/加签时的目标用户）
    @TableField("target_user_id")
    private Long target_user_id;

    // 会签状态：1待审批 2已通过 3已驳回（会签场景使用）
    @TableField("sign_status")
    private Integer sign_status;

    // 查看时间（用于统计审批响应时长）
    @TableField("read_time")
    private LocalDateTime read_time;

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