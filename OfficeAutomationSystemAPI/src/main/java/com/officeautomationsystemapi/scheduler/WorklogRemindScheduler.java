package com.officeautomationsystemapi.scheduler;

import com.officeautomationsystemapi.service.WorklogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 工作日下午 5 点提醒当天没写日志的人，周一 9 点把上周情况发给主管
@Component
public class WorklogRemindScheduler {

    @Autowired
    private WorklogService worklogService;

    @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void remindUnwritten() {
        try {
            worklogService.sendUnwrittenReminders();
        } catch (Exception e) {
            System.out.println("[工作日志提醒] 异常：" + e.getMessage());
        }
    }

    // 凌晨 3 点清理超过 180 天的已删除日志
    @Scheduled(cron = "0 0 3 * * *")
    public void physicalCleanup() {
        try {
            worklogService.physicalCleanupDeleted();
        } catch (Exception e) {
            System.out.println("[工作日志清理] 异常：" + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 9 * * MON")
    public void weeklySummary() {
        try {
            worklogService.sendWeeklySummary();
        } catch (Exception e) {
            System.out.println("[工作日志周报] 异常：" + e.getMessage());
        }
    }
}
