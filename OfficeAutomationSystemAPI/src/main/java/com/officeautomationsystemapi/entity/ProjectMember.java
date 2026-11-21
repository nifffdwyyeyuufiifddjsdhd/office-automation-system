package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 项目成员：项目经理 1、普通成员 2、关注人 3
@Data
@TableName("project_members")
public class ProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long project_id;

    @TableField("user_id")
    private Long user_id;

    @TableField("role")
    private Integer role;

    @TableField("join_time")
    private LocalDateTime join_time;

    @TableField("created_at")
    private LocalDateTime created_at;

    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deleted_at;
}
