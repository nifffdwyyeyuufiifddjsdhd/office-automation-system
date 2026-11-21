package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 第三方账号绑定：一个用户在每个平台只留一条
@Data
@TableName("user_third_party_bindings")
public class UserThirdPartyBindings {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long user_id;

    // wecom=企业微信 dingtalk=钉钉
    @TableField("platform")
    private String platform;

    // 第三方账号标识，企业微信是 UserID，钉钉是 userId
    @TableField("external_id")
    private String external_id;

    // 第三方显示名，方便本人核对绑定对不对
    @TableField("external_name")
    private String external_name;

    // 1已绑定 0已解绑
    @TableField("status")
    private Integer status;

    @TableField("bind_time")
    private LocalDateTime bind_time;

    @TableField("created_at")
    private LocalDateTime created_at;

    @TableField("updated_at")
    private LocalDateTime updated_at;

    @TableLogic
    @TableField("deleted_at")
    private LocalDateTime deleted_at;
}
