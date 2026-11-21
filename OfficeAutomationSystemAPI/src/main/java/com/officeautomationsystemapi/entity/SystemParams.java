package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_params")
public class SystemParams {

    // 参数ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 参数键（如 system.name, file.max_size）
    @TableField("param_key")
    private String param_key;

    // 参数值
    @TableField("param_value")
    private String param_value;

    // 参数名称
    @TableField("param_name")
    private String param_name;

    // 参数分组（如：基础配置：：通知配置：：出勤配置：：文件配置：）
    @TableField("param_group")
    private String param_group;

    // 参数类型：1字符串 2数字 3布尔 4JSON
    @TableField("param_type")
    private Integer param_type;

    // 同组内排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 参数说明
    @TableField("remark")
    private String remark;

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