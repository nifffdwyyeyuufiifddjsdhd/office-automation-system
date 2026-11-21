package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_templates")
public class NotificationTemplates {

    // 模板ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 模板编码（如 flow_arrive）
    @TableField("template_code")
    private String template_code;

    // 模板名称
    @TableField("template_name")
    private String template_name;

    // 业务类型（flow_instance / task / attendance 等）
    @TableField("biz_type")
    private String biz_type;

    // 标题模板，支持 {变量} 占位
    @TableField("title_template")
    private String title_template;

    // 内容模板
    @TableField("content_template")
    private String content_template;

    // 推送渠道（逗号分隔：system,wecom,dingtalk）
    @TableField("channels")
    private String channels;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 备注
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
