package com.officeautomationsystemapi.scheduler;

import com.officeautomationsystemapi.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 每分钟检查一次，上班前 15 分钟提醒签到，下班后半小时和 23 点提醒签退
@Component
public class AttendanceRemindScheduler {

    @Autowired
    private AttendanceService attendanceService;

    @Scheduled(cron = "0 * * * * ?")
    public void run() {
        try {
            attendanceService.sendAttendanceReminders();
        } catch (Exception e) {
            System.out.println("[出勤提醒] 异常：" + e.getMessage());
        }
    }
}
