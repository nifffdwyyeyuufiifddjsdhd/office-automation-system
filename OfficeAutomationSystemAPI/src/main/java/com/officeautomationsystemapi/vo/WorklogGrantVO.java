package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorklogGrantVO {
    private Long id;
    // 被查看人（谁的日志）
    private Long target_id;
    private String target_name;
    // 查看人
    private Long viewer_id;
    private String viewer_name;
    // 群组/部门授权
    private Long group_id;
    private String group_name;
    private Long dept_id;
    private String dept_name;
    // 授权类型：1按用户 2按群组 3按部门
    private Integer permission_type;
    private LocalDateTime grant_time;
    private Long granter_id;
    private String granter_name;
}
