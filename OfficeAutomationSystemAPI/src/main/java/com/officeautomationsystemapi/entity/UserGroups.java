package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_groups")
public class UserGroups {

    // 群组ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 群组名称
    @TableField("group_name")
    private String group_name;

    // 群组编码
    @TableField("group_code")
    private String group_code;

    // 群组描述
    @TableField("description")
    private String description;

    // 创建人ID
    @TableField("creator_id")
    private Long creator_id;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

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