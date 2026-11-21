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
@TableName("worklogs")
public class Worklogs {

    // 日志ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 撰写人ID
    @TableField("user_id")
    private Long user_id;

    // 日志日期
    @TableField("log_date")
    private LocalDate log_date;

    // 日志类别ID
    @TableField("category_id")
    private Long category_id;

    // 日志标题
    @TableField("title")
    private String title;

    // 日志内容（富文本）
    @TableField("content")
    private String content;

    // 工作总结
    @TableField("work_summary")
    private String work_summary;

    // 明日计划
    @TableField("next_plan")
    private String next_plan;

    // 附件ID
    @TableField("attachment_id")
    private Long attachment_id;

    // 状态：0草稿 1已提交 2已撤回
    @TableField("status")
    private Integer status;

    // 是否补写：0正常 1补写
    @TableField("is_supplement")
    private Integer is_supplement;

    // 提交时间
    @TableField("submit_time")
    private LocalDateTime submit_time;

    // 被查看次数
    @TableField("view_count")
    private Integer view_count;

    // 评论数
    @TableField("comment_count")
    private Integer comment_count;

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