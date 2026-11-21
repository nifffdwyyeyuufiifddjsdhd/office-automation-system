package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement_reads")
public class AnnouncementReads {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 公告ID
    @TableField("announcement_id")
    private Long announcement_id;

    // 用户ID
    @TableField("user_id")
    private Long user_id;

    // 阅读时间
    @TableField("read_time")
    private LocalDateTime read_time;

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