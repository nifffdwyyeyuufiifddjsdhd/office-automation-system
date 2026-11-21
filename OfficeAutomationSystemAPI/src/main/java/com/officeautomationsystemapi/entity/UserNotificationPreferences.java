package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification_preferences")
public class UserNotificationPreferences {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    @TableField("user_id")
    private Long user_id;

    // 系统通知开关（type=1）：0关 1开
    @TableField("pref_system")
    private Integer pref_system;

    // 审批待办开关（type=2）
    @TableField("pref_flow")
    private Integer pref_flow;

    // 任务通知开关（type=3）
    @TableField("pref_task")
    private Integer pref_task;

    // 出勤提醒开关（type=4）
    @TableField("pref_attendance")
    private Integer pref_attendance;

    // 日志提醒开关（type=5）
    @TableField("pref_worklog")
    private Integer pref_worklog;

    // 新消息声音/标题提醒开关
    @TableField("pref_sound")
    private Integer pref_sound;

    @TableField("created_at")
    private LocalDateTime created_at;

    @TableField("updated_at")
    private LocalDateTime updated_at;
}
