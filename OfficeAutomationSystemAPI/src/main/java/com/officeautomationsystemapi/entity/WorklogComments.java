package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("worklog_comments")
public class WorklogComments {

    // 评论ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 日志ID
    @TableField("worklog_id")
    private Long worklog_id;

    // 评论人ID
    @TableField("user_id")
    private Long user_id;

    // 父评论ID（0表示顶级评论）
    @TableField("parent_id")
    private Long parent_id;

    // 评论内容
    @TableField("content")
    private String content;

    // 评论时间
    @TableField("comment_time")
    private LocalDateTime comment_time;

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