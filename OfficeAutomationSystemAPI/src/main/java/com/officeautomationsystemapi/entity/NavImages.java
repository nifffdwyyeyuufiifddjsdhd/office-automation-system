package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nav_images")
public class NavImages {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 图片标题（alt文本）
    @TableField("title")
    private String title;

    // 图片URL
    @TableField("image_url")
    private String image_url;

    // 点击跳转链接
    @TableField("link_url")
    private String link_url;

    // 展示位置：banner首页轮播, nav导航栏, sidebar侧边栏
    @TableField("position")
    private String position;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0停用 1启用
    @TableField("status")
    private Integer status;

    // 展示开始时间
    @TableField("start_time")
    private LocalDateTime start_time;

    // 展示结束时间
    @TableField("end_time")
    private LocalDateTime end_time;

    // 点击次数
    @TableField("click_count")
    private Integer click_count;

    // 上传人ID
    @TableField("uploader_id")
    private Long uploader_id;

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