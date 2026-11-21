package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("menus")
public class Menus {

    // 菜单ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父菜单ID（0表示顶级）
    @TableField("parent_id")
    private Long parent_id;

    // 菜单名称
    @TableField("menu_name")
    private String menu_name;

    // 菜单编码（权限标识，如 system:user:list）
    @TableField("menu_code")
    private String menu_code;

    // 类型：1目录 2菜单 3按钮
    @TableField("menu_type")
    private Integer menu_type;

    // 路由路径
    @TableField("path")
    private String path;

    // 前端组件路径
    @TableField("component")
    private String component;

    // 图标
    @TableField("icon")
    private String icon;

    // 是否在导航栏显示：0否 1是
    @TableField("is_nav")
    private Integer is_nav;

    // 页面地址（前端路由路径）
    @TableField("page_url")
    private String page_url;

    // 导航图标
    @TableField("nav_icon")
    private String nav_icon;

    // 打开方式：1当前窗口 2新窗口
    @TableField("open_type")
    private Integer open_type;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 是否可见：0隐藏 1显示
    @TableField("visible")
    private Integer visible;

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