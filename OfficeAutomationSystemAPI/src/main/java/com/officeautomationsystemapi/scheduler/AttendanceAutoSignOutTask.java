package com.officeautomationsystemapi.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.AttendanceRecords;
import com.officeautomationsystemapi.repository.AttendanceRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// 每天 00:05 把昨天签到未签退的记录补上签退时间
@Component
public class AttendanceAutoSignOutTask {

    @Autowired
    private AttendanceRecordsRepository attendanceRepo;

    @Autowired
    private JdbcTemplate jdbc;

    // 下班时间先看排班规则，再看班次模板，都没有按 18:00
    private LocalTime resolveWorkEnd() {
        try {
            List<Map<String, Object>> sched = jdbc.queryForList("SELECT work_end FROM shift_schedules WHERE is_active=1 LIMIT 1");
            if (!sched.isEmpty() && sched.get(0).get("work_end") != null) {
                return LocalTime.parse(sched.get(0).get("work_end").toString());
            }
        } catch (Exception ignored) {
            // 表或字段不存在就往下试
        }
        try {
            Map<String, Object> tpl = jdbc.queryForMap("SELECT work_end FROM shift_templates WHERE is_active=1 LIMIT 1");
            if (tpl.get("work_end") != null) {
                return LocalTime.parse(tpl.get("work_end").toString());
            }
        } catch (Exception ignored) {
            // 再看班次模板
        }
        return LocalTime.of(18, 0);
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void autoSignOut() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        QueryWrapper<AttendanceRecords> qw = new QueryWrapper<>();
        qw.eq("attendance_date", yesterday)
                .isNotNull("check_in_time")
                .isNull("check_out_time")
                .isNull("deleted_at");
        List<AttendanceRecords> list = attendanceRepo.selectList(qw);
        if (list.isEmpty()) {
            return;
        }

        LocalTime workEnd = resolveWorkEnd();
        for (AttendanceRecords rec : list) {
            LocalDateTime autoOut = LocalDateTime.of(yesterday, workEnd);
            rec.setCheck_out_time(autoOut);
            rec.setStatus(8);
            rec.setRemark("未签退，系统自动标记");
            rec.setUpdated_at(LocalDateTime.now());
            if (rec.getCheck_in_time() != null) {
                long minutes = Duration.between(rec.getCheck_in_time(), autoOut).toMinutes();
                rec.setWork_hours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
            }
            attendanceRepo.updateById(rec);
        }
        System.out.println("[自动签退] 处理了 " + list.size() + " 条未签退记录，日期：" + yesterday);
    }
}
