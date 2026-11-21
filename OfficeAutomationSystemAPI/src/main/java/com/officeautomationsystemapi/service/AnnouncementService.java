package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.vo.AnnouncementCategoryVO;
import com.officeautomationsystemapi.vo.AnnouncementVO;

import java.util.List;
import java.util.Map;

public interface AnnouncementService {

    // 类别管理，需要 announcement:category:manage 权限
    List<AnnouncementCategoryVO> listCategoryTree(Long userId);              // 树形列表，按权限过滤可见类别
    Long addCategory(AnnouncementCategoryVO vo);                            // 新增类别
    void updateCategory(AnnouncementCategoryVO vo);                         // 修改类别
    void deleteCategory(Long categoryId);                                   // 删除类别，有子类别或公告时拒绝
    void toggleCategoryStatus(Long categoryId, Integer status);             // 停用或启用

    // 公告管理
    Page<AnnouncementVO> pageAnnouncements(Long userId, Integer pageNum, Integer pageSize,
                                           Long categoryId, String keyword, Integer status);
    AnnouncementVO getAnnouncementDetail(Long userId, Long announcementId); // 详情，标记已读并让浏览数加一
    Long addAnnouncement(Long userId, AnnouncementVO vo);                   // 新增，存为草稿
    void updateAnnouncement(Long userId, AnnouncementVO vo);                // 修改
    void deleteAnnouncement(Long announcementId);                           // 软删除
    void publishAnnouncement(Long announcementId);                          // 发布
    void cancelPublishAnnouncement(Long announcementId);                    // 下线
    void batchPublish(List<Long> ids);                                      // 批量发布
    void toggleTop(Long announcementId, Integer isTop);                     // 置顶或取消置顶
    Page<Map<String, Object>> pageReadUsers(Long announcementId, Integer pageNum, Integer pageSize); // 已读人列表
}
