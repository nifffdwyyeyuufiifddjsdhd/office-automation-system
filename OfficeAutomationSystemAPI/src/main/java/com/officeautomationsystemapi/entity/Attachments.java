package com.officeautomationsystemapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attachments")
public class Attachments {

    // 附件ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 原始文件名
    @TableField("file_name")
    private String file_name;

    // 文件存储路径
    @TableField("file_path")
    private String file_path;

    // 文件大小（字节）
    @TableField("file_size")
    private Long file_size;

    // 文件类型（MIME类型）
    @TableField("file_type")
    private String file_type;

    // 文件扩展名
    @TableField("file_ext")
    private String file_ext;

    // 关联业务类型（如 worklog, flow_instance, task, announcement）
    @TableField("target_type")
    private String target_type;

    // 关联业务ID
    @TableField("target_id")
    private Long target_id;

    // 上传人ID
    @TableField("uploader_id")
    private Long uploader_id;

    // 存储方式：1本地存储 2云存储OSS 3MinIO
    @TableField("storage_type")
    private Integer storage_type;

    // 文件MD5（用于秒传和去重）
    @TableField("md5")
    private String md5;

    // 下载次数
    @TableField("download_count")
    private Integer download_count;

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