package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 业务办完后把它挂的通知一起清掉，避免办完还提示
@Service
public class NotificationBizService {

    @Autowired
    private NotificationRecordsRepository notificationRecordsRepository;

    // 清掉某条业务的全部通知，调用方清完再补发结果通知
    public void clear(String bizType, Long bizId) {
        if (bizType == null || bizId == null) return;
        try {
            notificationRecordsRepository.delete(new QueryWrapper<NotificationRecords>()
                    .eq("biz_type", bizType).eq("biz_id", bizId));
        } catch (Exception ignore) {
            // 清理失败不影响主流程，最多多留一条已过期的通知
        }
    }
}
