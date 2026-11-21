package com.officeautomationsystemapi.service;

import java.time.LocalDate;
import java.util.Map;

// 离职交接：先预览要交接的流程，再批量转交并留档
public interface UserHandoverService {

    // 预览：离职人的待办流程、审批中的发起流程、历史已办数量
    Map<String, Object> preview(Long leaverId);

    // 执行交接：transfer 批量转交 / keep 保留待办 / auto 转给上级
    Map<String, Object> handover(Long operatorId, Long leaverId, Long receiverId, String reason,
                                 LocalDate effectiveDate, Boolean transferRunning, String mode);

    // 交接记录分页，leaverId 为空查全部
    Map<String, Object> page(int pageNum, int pageSize, Long leaverId);
}
