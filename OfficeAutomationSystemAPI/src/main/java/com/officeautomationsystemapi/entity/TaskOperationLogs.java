package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_operation_logs")
public class TaskOperationLogs {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 任务ID
    @TableField("task_id")
    private Long task_id;

    // 操作人ID
    @TableField("operator_id")
    private Long operator_id;

    // 操作类型（如 create, assign, accept, submit, approve, reject, update_status）
    @TableField("action")
    private String action;

    // 旧值（JSON格式，记录变更前的字段）
    @TableField("old_value")
    private String old_value;

    // 新值（JSON格式，记录变更后的字段）
    @TableField("new_value")
    private String new_value;

    // 操作备注
    @TableField("remark")
    private String remark;

    // 操作时间
    @TableField("operation_time")
    private LocalDateTime operation_time;

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