package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcements")
public class Announcements {

    // 公告ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 公告标题
    @TableField("title")
    private String title;

    // 所属类别ID
    @TableField("category_id")
    private Long category_id;

    // 公告内容（富文本）
    @TableField("content")
    private String content;

    // 内容摘要
    @TableField("summary")
    private String summary;

    // 封面图片URL
    @TableField("cover_image")
    private String cover_image;

    // 正文主题（standard/notice/academic/finance），只存名字，样式在前端统一定义
    @TableField("theme")
    private String theme;

    // 发布人ID
    @TableField("author_id")
    private Long author_id;

    // 状态：0草稿 1已发布 2已下线
    @TableField("status")
    private Integer status;

    // 是否置顶：0否 1是
    @TableField("is_top")
    private Integer is_top;

    // 发布时间
    @TableField("publish_time")
    private LocalDateTime publish_time;

    // 下线时间
    @TableField("offline_time")
    private LocalDateTime offline_time;

    // 查看次数
    @TableField("view_count")
    private Integer view_count;

    // 点赞数
    @TableField("like_count")
    private Integer like_count;

    // 是否允许评论：0否 1是
    @TableField("allow_comment")
    private Integer allow_comment;

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