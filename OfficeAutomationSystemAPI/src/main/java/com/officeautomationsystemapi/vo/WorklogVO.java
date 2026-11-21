package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WorklogVO {
    private Long id;
    // 撰写人
    private Long user_id;
    private String username;
    private String real_name;
    private Long org_id;
    private String org_name;
    // 类别
    private Long category_id;
    private String category_name;
    // 日志内容
    private LocalDate log_date;
    private String title;
    private String content;
    private String work_summary;
    private String next_plan;
    // 状态：0草稿 1已提交 2已撤回
    private Integer status;
    // 是否补写
    private Integer is_supplement;
    private LocalDateTime submit_time;
    private Integer view_count;
    private Integer comment_count;
    private LocalDateTime created_at;
    // 是否自己的日志（前端区分操作）
    private Boolean mine;
    // 附件（需求文档 13.3.6：每篇日志1个附件）
    private Long attachment_id;
    private String attachment_file_name;
    private Long attachment_file_size;
    private String attachment_download_url;
}
