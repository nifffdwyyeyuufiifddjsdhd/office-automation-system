package com.officeautomationsystemapi.task;

import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 定时清理历史通知，防止通知表一直变大
@Component
public class NotificationCleanupTask {

    @Autowired
    private NotificationRecordsRepository repo;

    // 每天凌晨 3 点删除 30 天前的已读通知，未读保留
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        repo.physicalDeleteReadBefore(cutoff);
    }
}
