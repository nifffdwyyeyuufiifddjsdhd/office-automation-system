package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 审批委托：委托人按时段把待办交给代理人
@Data
@TableName("flow_delegates")
public class FlowDelegates {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 委托人，待办原本属于他
    @TableField("delegator_id")
    private Long delegator_id;

    // 代理人，代委托人审批
    @TableField("agent_id")
    private Long agent_id;

    // 生效时间
    @TableField("start_time")
    private LocalDateTime start_time;

    // 失效时间
    @TableField("end_time")
    private LocalDateTime end_time;

    // 委托原因
    @TableField("reason")
    private String reason;

    // 1生效 0已取消
    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime created_at;

    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deleted_at;

    // 下面几个只在列表展示，不对应数据库字段
    @TableField(exist = false)
    private String delegator_name;

    @TableField(exist = false)
    private String agent_name;

    @TableField(exist = false)
    private String delegator_org;

    @TableField(exist = false)
    private String agent_org;
}
