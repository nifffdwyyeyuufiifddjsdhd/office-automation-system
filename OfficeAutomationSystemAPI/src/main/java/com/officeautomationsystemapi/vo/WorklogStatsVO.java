package com.officeautomationsystemapi.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorklogStatsVO {
    // 我的日志：本月/本周条数、有日志天数、累计被查看数、累计评论数
    private Long myMonthCount;
    private Long myMonthDays;
    private Integer myViewCount;
    private Integer myCommentCount;
    // 部门排名（按用户聚合，含姓名/条数）
    private List<Map<String, Object>> deptRank;
    // 统计增强（需求文档 13.3 日志统计维度）
    private Integer myCompletionRate;                 // 本月完成率（%）：实际撰写天数 / 本月工作日天数
    private List<Map<String, Object>> timeDist;      // 时段分布 [{period,count}]：上午/下午/晚上（按提交时间）
    private Map<String, Object> wordStats;           // {totalWords 总字数, avgWords 平均字数}（当月我的日志）
    private List<Map<String, Object>> commentTop;    // 评论最多的日志 top5 [{id,title,comment_count}]
}
