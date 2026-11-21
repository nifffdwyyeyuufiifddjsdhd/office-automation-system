package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorklogCommentVO {
    private Long id;
    private Long worklog_id;
    private Long user_id;
    private String username;
    private String real_name;
    private String avatar;
    private Long parent_id;
    private String content;
    private LocalDateTime comment_time;
    // 删除后显示"该评论已删除"（软删除标记由查询层补）
    private Boolean deleted;
}
