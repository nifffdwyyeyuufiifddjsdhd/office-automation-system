package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("flow_definitions")
public class FlowDefinitions {

    // 流程定义ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 流程名称
    @TableField("flow_name")
    private String flow_name;

    // 流程编码（如 leave, expense, overtime）
    @TableField("code")
    private String code;

    // 流程类型：1审批流程 2业务流程
    @TableField("flow_type")
    private Integer flow_type;

    // 流程描述
    @TableField("description")
    private String description;

    // 所属部门，NULL=全员可用
    @TableField("dept_id")
    private Long dept_id;

    // 流程图标
    @TableField("icon")
    private String icon;

    // 表单配置（JSON格式，定义申请时的表单字段）
    @TableField("form_config")
    private String form_config;

    // 是否需要财务付款：0不需要 1审批通过后进入财务待付款池
    @TableField("use_scope")
    private Integer use_scope;

    @TableField("use_scope_ids")
    private String use_scope_ids;

    @TableField("need_payment")
    private Integer need_payment;

    // 版本号
    @TableField("version")
    private Integer version;

    // 状态：0草稿 1已发布 2已停用
    @TableField("status")
    private Integer status;

    // 被哪个新版本替代，历史单据仍然指向旧版本这一行
    @TableField("superseded_by")
    private Long superseded_by;

    // 审核状态：0待审核 1通过 2拒绝（部门自建模板需admin审核）
    @TableField("audit_status")
    private Integer audit_status;

    // 创建人ID
    @TableField("creator_id")
    private Long creator_id;

    // 发布时间
    @TableField("publish_time")
    private LocalDateTime publish_time;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

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