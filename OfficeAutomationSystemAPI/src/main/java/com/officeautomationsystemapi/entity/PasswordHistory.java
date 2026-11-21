package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("password_history")
public class PasswordHistory {

    @TableId(type= IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long user_id;

    @TableField("password")
    private String password;

    @TableField("created_at")
    private LocalDateTime created_at;
}
