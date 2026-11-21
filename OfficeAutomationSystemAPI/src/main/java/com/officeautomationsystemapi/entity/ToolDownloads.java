package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tool_downloads")
public class ToolDownloads {

    // 主键
    @TableId(type = IdType.AUTO)
    private Long id;

    // 工具名称（如：VPN客户端：：Git安装包：：项目模板：）
    @TableField("name")
    private String name;

    // 工具描述（功能说明、使用方法、版本信息）
    @TableField("description")
    private String description;

    // 分类ID
    @TableField("category_id")
    private Long category_id;

    // 附件ID（关联attachments表）
    @TableField("file_id")
    private Long file_id;

    // 外部下载链接（如官方下载页）
    @TableField("download_url")
    private String download_url;

    // 版本号
    @TableField("version")
    private String version;

    // 文件大小（字节）
    @TableField("file_size")
    private Long file_size;

    // 适用平台：Windows, macOS, Linux, All
    @TableField("platform")
    private String platform;

    // 下载次数
    @TableField("download_count")
    private Integer download_count;

    // 排序号
    @TableField("sort_order")
    private Integer sort_order;

    // 状态：0下架 1上架
    @TableField("status")
    private Integer status;

    // 上传人ID
    @TableField("uploader_id")
    private Long uploader_id;

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