package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_makeup")
public class AttendanceMakeup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long user_id;

    private LocalDate attendance_date;

    // 1补签到 2补签退 3两者都补
    private Integer type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime check_in_time;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime check_out_time;

    private String reason;

    // 0待审批 1已通过 2已驳回
    private Integer status;

    // 当前审批阶段（status=0 时有效）：1部门主管初审 2人事终审
    private Integer current_stage;

    private Long approver_id;

    // 部门主管初审留痕
    private Long first_approver_id;

    private LocalDateTime first_approve_time;

    private String first_approve_remark;

    private LocalDateTime approve_time;

    private String approve_remark;

    private LocalDateTime created_at;

    private LocalDateTime updated_at;

    @TableLogic
    private LocalDateTime deleted_at;

    // 联表查询字段（非数据库列）
    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String real_name;

    @TableField(exist = false)
    private String approver_name;

    @TableField(exist = false)
    private String first_approver_name;
}
