package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_records")
public class NotificationRecords {

    // 通知类型：1系统通知 2审批待办 3任务通知 4出勤提醒 5日志提醒
    @TableId(type = IdType.AUTO)
    private Long id;

    // 接收用户ID
    @TableField("user_id")
    private Long user_id;

    // 通知标题
    @TableField("title")
    private String title;

    // 通知内容
    @TableField("content")
    private String content;

    // 通知类型：1系统通知 2审批待办 3任务通知 4出勤提醒 5日志提醒
    @TableField("type")
    private Integer type;

    // 关联业务类型（如 flow_instance, task）
    @TableField("biz_type")
    private String biz_type;

    // 关联业务ID
    @TableField("biz_id")
    private Long biz_id;

    // 跳转链接
    @TableField("jump_url")
    private String jump_url;

    // 是否已读：0未读 1已读
    @TableField("is_read")
    private Integer is_read;

    // 是否置顶：0否 1是
    @TableField("is_top")
    private Integer is_top;

    // 阅读时间
    @TableField("read_time")
    private LocalDateTime read_time;

    // 推送状态：0未推送 1推送成功 2推送失败
    @TableField("push_status")
    private Integer push_status;

    // 推送时间
    @TableField("push_time")
    private LocalDateTime push_time;

    // 推送渠道：wechat_work, dingtalk, email
    @TableField("push_channel")
    private String push_channel;

    // 推送失败原因
    @TableField("push_fail_reason")
    private String push_fail_reason;

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