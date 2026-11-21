package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tasks")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String task_no;
    private String title;
    private String description;
    private Long project_id;
    private Integer task_type;
    private Integer priority;
    private Integer status;
    private Integer progress;
    private Long creator_id;
    private Long assignee_id;
    private LocalDateTime plan_start_time;
    private LocalDateTime plan_end_time;
    private LocalDateTime actual_start_time;
    private LocalDateTime actual_end_time;
    private BigDecimal plan_work_hours;
    private BigDecimal actual_work_hours;
    private Long attachment_id;
    private String tags;
    private LocalDateTime accept_time;
    private LocalDateTime submit_time;
    private String reject_reason;
    private String complete_note;
    private Integer is_overdue;
    private Integer due_reminded;
    private Integer archive_status;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    @TableLogic
    private LocalDateTime deleted_at;
}
