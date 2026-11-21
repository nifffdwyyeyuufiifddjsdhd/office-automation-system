package com.officeautomationsystemapi.scheduler;

import com.officeautomationsystemapi.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 每天凌晨两点按前一天的签到签退结算出勤状态
@Component
public class AttendanceSettlementScheduler {

    @Autowired
    private AttendanceService attendanceService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        try {
            attendanceService.settleAttendance();
        } catch (Exception ignored) {
            // 服务内部已记日志，这里只防止调度线程被打断
        }
    }
}
