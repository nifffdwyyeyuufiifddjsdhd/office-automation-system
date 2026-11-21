package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("page_controls")
public class PageControls {

    // 控件ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 控件编码（前端控件唯一标识，如 user_list_add_btn）
    @TableField("control_code")
    private String control_code;

    // 控件名称（如：新增用户按钮：）
    @TableField("control_name")
    private String control_name;

    // 控件类型：1按钮 2链接 3输入框 4表格列 5其他
    @TableField("control_type")
    private Integer control_type;

    // 所属页面/菜单ID（关联menus表）
    @TableField("page_id")
    private Long page_id;

    // 父控件ID（用于控件层级）
    @TableField("parent_control_id")
    private Long parent_control_id;

    // 父控件类别
    @TableField("parent_control_type")
    private String parent_control_type;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

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
