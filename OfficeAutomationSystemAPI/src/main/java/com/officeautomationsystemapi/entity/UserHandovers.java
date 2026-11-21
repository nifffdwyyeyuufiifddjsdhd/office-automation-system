package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 离职交接记录：离职人把待办流程交给接收人，留档备查
@Data
@TableName("user_handovers")
public class UserHandovers {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 离职人
    @TableField("user_id")
    private Long user_id;

    // 接收人（接手待办的人），保留模式下为空
    @TableField("receiver_id")
    private Long receiver_id;

    // 处理方式：transfer 批量转交 / keep 保留待办 / auto 自动转直属上级
    @TableField("handle_mode")
    private String handle_mode;

    // 办理人（执行交接操作的人事）
    @TableField("operator_id")
    private Long operator_id;

    // 交接原因
    @TableField("reason")
    private String reason;

    // 离职生效日期
    @TableField("effective_date")
    private LocalDate effective_date;

    // 转交的待办流程数
    @TableField("todo_count")
    private Integer todo_count;

    // 转交的审批中发起流程数
    @TableField("running_count")
    private Integer running_count;

    // 转交的未结束任务数
    @TableField("task_count")
    private Integer task_count;

    // 接管的项目成员身份数
    @TableField("project_count")
    private Integer project_count;

    // 历史已办流程数（只统计，不转交）
    @TableField("done_count")
    private Integer done_count;

    // 交接内容清单
    @TableField("item_summary")
    private String item_summary;

    @TableField("created_at")
    private LocalDateTime created_at;

    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deleted_at;

    // 下面几个只在列表里展示，不对应数据库字段
    @TableField(exist = false)
    private String user_name;

    @TableField(exist = false)
    private String receiver_name;

    @TableField(exist = false)
    private String operator_name;

    @TableField(exist = false)
    private String org_name;
}
