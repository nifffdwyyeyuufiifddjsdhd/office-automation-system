package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("attendance_appeals")
public class AttendanceAppeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("record_id")
    private Long record_id;
    @TableField("user_id")
    private Long user_id;
    private String reason;
    private Integer status;
    @TableField("approver_id")
    private Long approver_id;
    @TableField("approve_time")
    private LocalDateTime approve_time;
    @TableField("approve_remark")
    private String approve_remark;
    @TableField("created_at")
    private LocalDateTime created_at;
    @TableField("updated_at")
    private LocalDateTime updated_at;
    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deleted_at;
}
