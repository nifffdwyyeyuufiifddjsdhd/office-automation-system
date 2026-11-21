package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_comments")
public class TaskComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long task_id;
    private Long user_id;
    private Long parent_id;
    private String content;
    private String mention_users;
    private Long attachment_id;
    private LocalDateTime comment_time;
    private Integer is_edited;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    @TableLogic
    private LocalDateTime deleted_at;
}
