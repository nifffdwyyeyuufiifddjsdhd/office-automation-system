package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 招聘候选人：一人对应一个在招岗位，状态走到入职或淘汰
@Data
@TableName("recruitment_candidates")
public class RecruitmentCandidates {

    // 候选人ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 应聘岗位
    @TableField("position_id")
    private Long position_id;

    // 姓名
    @TableField("candidate_name")
    private String candidate_name;

    // 性别：0未知 1男 2女
    @TableField("gender")
    private Integer gender;

    // 手机号
    @TableField("phone")
    private String phone;

    // 邮箱
    @TableField("email")
    private String email;

    // 学历
    @TableField("education")
    private String education;

    // 工作年限
    @TableField("experience_years")
    private Integer experience_years;

    // 来源：简历投递/内推/校招/猎头
    @TableField("source")
    private String source;

    // 简历附件路径
    @TableField("resume_path")
    private String resume_path;

    // 状态：1待筛选 2待面试 3面试通过 4已入职 5已淘汰
    @TableField("status")
    private Integer status;

    // 面试时间
    @TableField("interview_time")
    private LocalDateTime interview_time;

    // 面试官
    @TableField("interviewer")
    private String interviewer;

    // 面试评价
    @TableField("evaluation")
    private String evaluation;

    // 期望薪资
    @TableField("expected_salary")
    private BigDecimal expected_salary;

    // 录入人
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
