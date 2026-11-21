package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_steps")
public class FlowSteps {

    // 步骤ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 流程定义ID
    @TableField("definition_id")
    private Long definition_id;

    // 步骤名称
    @TableField("step_name")
    private String step_name;

    // 步骤编码
    @TableField("step_code")
    private String step_code;

    // 前置步骤ID（0表示第一步）
    @TableField("prev_step_id")
    private Long prev_step_id;

    // 步骤类型：1审批节点 2抄送节点 3条件网关节点 4开始节点 5结束节点
    @TableField("step_type")
    private Integer step_type;

    // 审批人类型：1指定用户 2指定角色 3部门主管 4职位匹配 5发起人自选
    @TableField("approver_type")
    private Integer approver_type;

    // 审批人配置（JSON格式，根据approver_type存储不同内容）
    @TableField("approver_config")
    private String approver_config;

    // 步骤表单配置（JSON格式，审批时需要填写的字段）
    @TableField("step_form")
    private String step_form;

    // 超时时间（小时，0表示不超时）
    @TableField("timeout_hours")
    private Integer timeout_hours;

    // 超时处理：0不处理 1自动催办 2自动通过 3自动驳回
    @TableField("timeout_action")
    private Integer timeout_action;

    // 步骤顺序号
    @TableField("sort_order")
    private Integer sort_order;

    // 会签类型：1或签（任意一人通过） 2会签（全部通过） 3按比例通过
    @TableField("sign_type")
    private Integer sign_type;

    // 会签通过比例（sign_type=3时使用，如80表示80%）
    @TableField("sign_ratio")
    private Integer sign_ratio;

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