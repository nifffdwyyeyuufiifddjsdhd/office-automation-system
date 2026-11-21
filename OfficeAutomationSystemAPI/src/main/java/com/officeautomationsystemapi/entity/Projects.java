package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("projects")
public class Projects {

    // 项目ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 项目编号（如 PRJ-2026-001）
    @TableField("project_no")
    private String project_no;

    // 项目名称
    @TableField("name")
    private String name;

    // 项目描述
    @TableField("description")
    private String description;

    // 项目类型：1内部项目 2客户项目 3研发项目
    @TableField("project_type")
    private Integer project_type;

    // 状态：0未启动 1进行中 2已暂停 3已完成 4已取消
    @TableField("status")
    private Integer status;

    // 优先级：1低 2中 3高 4紧急
    @TableField("priority")
    private Integer priority;

    // 项目经理ID
    @TableField("manager_id")
    private Long manager_id;

    // 创建人ID
    @TableField("creator_id")
    private Long creator_id;

    // 计划开始日期
    @TableField("plan_start_date")
    private LocalDate plan_start_date;

    // 计划结束日期
    @TableField("plan_end_date")
    private LocalDate plan_end_date;

    // 实际开始日期
    @TableField("actual_start_date")
    private LocalDate actual_start_date;

    // 实际结束日期
    @TableField("actual_end_date")
    private LocalDate actual_end_date;

    // 进度百分比（0-100）
    @TableField("progress")
    private Integer progress;

    // 项目标识颜色（看板视图使用）
    @TableField("color")
    private String color;

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
