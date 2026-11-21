package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_records")
public class PaymentRecords {

    // 付款记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 付款单号（PAY-20261019-0001）
    @TableField("pay_no")
    private String pay_no;

    // 关联的流程实例ID
    @TableField("instance_id")
    private Long instance_id;

    // 流程编号（冗余字段，列表直接展示）
    @TableField("instance_no")
    private String instance_no;

    // 流程定义ID
    @TableField("definition_id")
    private Long definition_id;

    // 流程名称（冗余字段）
    @TableField("flow_name")
    private String flow_name;

    // 流程标题
    @TableField("title")
    private String title;

    // 申请人ID
    @TableField("applicant_id")
    private Long applicant_id;

    // 费用类别ID：付款时由财务确认，未指定则按关键词给默认值
    @TableField("category_id")
    private Long category_id;

    // 费用类别名称：冗余存一份，类别改名不影响历史单据
    @TableField("category_name")
    private String category_name;

    // 付款金额
    @TableField("amount")
    private BigDecimal amount;

    // 收款人
    @TableField("payee_name")
    private String payee_name;

    // 收款账号
    @TableField("payee_account")
    private String payee_account;

    // 付款方式：1银行转账 2现金 3微信 4支付宝 5支票
    @TableField("pay_method")
    private Integer pay_method;

    // 付款时间
    @TableField("pay_time")
    private LocalDateTime pay_time;

    // 状态：1已付款 2已作废
    @TableField("status")
    private Integer status;

    // 付款操作人ID
    @TableField("operator_id")
    private Long operator_id;

    // 付款备注
    @TableField("remark")
    private String remark;

    // 作废原因
    @TableField("void_reason")
    private String void_reason;

    // 作废操作人ID
    @TableField("void_by")
    private Long void_by;

    // 作废时间
    @TableField("void_time")
    private LocalDateTime void_time;

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
