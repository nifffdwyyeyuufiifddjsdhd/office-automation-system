package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementVO {
    private Long id;
    private String title;
    private Long category_id;
    private String category_name;
    private String summary;
    private String cover_image;
    private String theme;            // 正文主题，缺省 standard
    private String content;          // 仅详情返回，列表不返回
    private Long author_id;
    private String author_name;      // 发布人姓名
    private Integer status;
    private Integer is_top;
    private LocalDateTime publish_time;
    private Integer view_count;      // 查看次数（PV）
    private Long read_count;         // 实际已读人数（去重）
    private Boolean is_read;         // 当前用户是否已读
}
