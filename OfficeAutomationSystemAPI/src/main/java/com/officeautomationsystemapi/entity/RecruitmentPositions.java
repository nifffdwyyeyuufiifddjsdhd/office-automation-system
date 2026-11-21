package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 招聘岗位：人事发布的在招职位，候选人按岗位投递
@Data
@TableName("recruitment_positions")
public class RecruitmentPositions {

    // 岗位ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 招聘岗位名称
    @TableField("position_name")
    private String position_name;

    // 用人部门
    @TableField("dept_id")
    private Long dept_id;

    // 关联职位表，用于定级
    @TableField("position_id")
    private Long position_id;

    // 招聘人数
    @TableField("headcount")
    private Integer headcount;

    // 学历要求
    @TableField("education")
    private String education;

    // 经验要求（年）
    @TableField("experience_years")
    private Integer experience_years;

    // 薪资范围
    @TableField("salary_range")
    private String salary_range;

    // 工作地点
    @TableField("work_place")
    private String work_place;

    // 岗位职责
    @TableField("duty")
    private String duty;

    // 任职要求
    @TableField("requirement")
    private String requirement;

    // 状态：0已停招 1在招 2已结束
    @TableField("status")
    private Integer status;

    // 发布时间
    @TableField("publish_time")
    private LocalDateTime publish_time;

    // 截止日期
    @TableField("deadline")
    private LocalDate deadline;

    // 创建人
    @TableField("creator_id")
    private Long creator_id;

    // 备注
    @TableField("remark")
    private String remark;

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
