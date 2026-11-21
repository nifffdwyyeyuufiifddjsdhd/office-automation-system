package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_members")
public class TaskMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long task_id;
    private Long user_id;
    private Integer role;
    private LocalDateTime join_time;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    @TableLogic
    private LocalDateTime deleted_at;
}
