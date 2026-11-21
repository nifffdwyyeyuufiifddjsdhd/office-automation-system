package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.vo.WorklogCategoryVO;
import com.officeautomationsystemapi.vo.WorklogCommentVO;
import com.officeautomationsystemapi.vo.WorklogGrantVO;
import com.officeautomationsystemapi.vo.WorklogStatsVO;
import com.officeautomationsystemapi.vo.WorklogVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface WorklogService {

    // 我的日志
    Page<WorklogVO> myPage(Long userId, int pageNum, int pageSize, String logDate, Long categoryId, String keyword);
    Long create(Long userId, WorklogVO vo);
    void update(Long userId, WorklogVO vo);
    void myDelete(Long userId, Long id);
    WorklogVO myDetail(Long userId, Long id);

    // 日志查看，按可见范围
    Page<WorklogVO> viewPage(Long userId, int pageNum, int pageSize, String keyword, Long categoryId, Long orgId);
    WorklogVO viewDetail(Long userId, Long id);
    List<WorklogCommentVO> listComments(Long userId, Long worklogId);
    void addComment(Long userId, Long worklogId, String content, Long parentId);
    void deleteComment(Long userId, Long commentId);

    // 日志管理，管理员用
    Page<WorklogVO> adminPage(Long viewerId, int pageNum, int pageSize, String keyword, Integer status, Long categoryId, Long orgId, String startDate, String endDate);
    Page<WorklogVO> recyclePage(Long viewerId, int pageNum, int pageSize, String keyword);
    void adminDelete(Long id);
    void adminRestore(Long id);

    // 类别管理，管理员用
    List<WorklogCategoryVO> listCategories();
    List<WorklogCategoryVO> adminCategories();
    void addCategory(Long userId, WorklogCategoryVO vo);
    void updateCategory(WorklogCategoryVO vo);
    void deleteCategory(Long id);

    // 查看权限配置，管理员用
    Page<WorklogGrantVO> grantPage(int pageNum, int pageSize, String keyword);
    void addGrant(Long granterId, WorklogGrantVO vo);
    void deleteGrant(Long id);

    // 统计
    WorklogStatsVO stats(Long userId, Long orgId);

    // 导出
    void exportExcel(Long userId, boolean mineOnly, String keyword, Long categoryId, Long orgId,
                     String startDate, String endDate, HttpServletResponse response);

    // 下载附件，先校验查看权限，下载数加一并记操作日志
    void downloadAttachment(Long userId, Long aid, HttpServletResponse response);

    // 定时任务调用
    void sendUnwrittenReminders();   // 工作日下午五点提醒当天没写日志的人
    void sendWeeklySummary();        // 周一上午九点把上周完成率发给部门主管
    void physicalCleanupDeleted();   // 清理超过180天的已删除日志
}
