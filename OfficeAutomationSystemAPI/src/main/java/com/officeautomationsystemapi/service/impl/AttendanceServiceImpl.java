package com.officeautomationsystemapi.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.officeautomationsystemapi.entity.AttendanceMakeup;
import com.officeautomationsystemapi.entity.AttendanceRecords;
import com.officeautomationsystemapi.entity.NotificationRecords;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.AttendanceMakeupRepository;
import com.officeautomationsystemapi.repository.AttendanceRecordsRepository;
import com.officeautomationsystemapi.repository.NotificationRecordsRepository;
import com.officeautomationsystemapi.service.DataScopeService;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.AttendanceService;
import com.officeautomationsystemapi.util.LunarUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 考勤业务：打卡、补签两级审批、出勤统计、自动结算、地理校验、提醒
@Service
public class AttendanceServiceImpl implements AttendanceService {

    // 每人每月最多补签次数
    private static final int MAX_MAKEUP_PER_MONTH = 3;

    // 兼容 yyyy/MM/dd 的日期格式
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // 排班匹配用的字段，命中后删掉，只留上下班时间给调用方
    private static final String[] SCHEDULE_MATCH_FIELDS = {
        "schedule_type", "schedule_date", "week_day", "start_date",
        "end_date", "lunar_month", "lunar_day", "yearly_month", "yearly_day"
    };

    @Autowired
    private AttendanceRecordsRepository attendanceRepo;

    @Autowired
    private AttendanceMakeupRepository makeupRepo;

    @Autowired
    private UsersRepository usersRepo;

    @Autowired
    private NotificationRecordsRepository notificationRepo;

    // 出勤异常提醒的标题正文取通知模板
    @Autowired
    private com.officeautomationsystemapi.service.NotificationTemplateService notificationTemplateService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataScopeService dataScopeService;

    // 用户是否拥有某个角色 ID
    private boolean hasRoleId(Long userId, long roleId) {
        if (userId == null) return false;
        try {
            List<Long> roles = jdbc.queryForList(
                "SELECT role_id FROM user_roles WHERE user_id=? AND deleted_at IS NULL", Long.class, userId);
            return roles != null && roles.contains(roleId);
        } catch (Exception e) {
            return false;
        }
    }

    // 是否系统管理员，角色 ID 1
    private boolean isAdminUser(Long userId) {
        return hasRoleId(userId, 1L);
    }

    // 是否人事专员，角色 ID 4
    private boolean isHrUser(Long userId) {
        return hasRoleId(userId, 4L);
    }

    // 是否人事/行政主管，角色 ID 7，补签终审与申诉审核由他们负责
    private boolean isHrManagerUser(Long userId) {
        return hasRoleId(userId, 7L);
    }

    // 目标员工是否在操作者的数据范围内，看全部的人直接返回 true
    // 人事专员只看本部门，所以走 DataScopeService 而不是按角色写死
    private boolean inDataScope(Long operatorId, Long targetUserId) {
        if (operatorId == null || targetUserId == null) return false;
        List<Long> visible = dataScopeService.visibleUserIds(operatorId);
        return visible == null || visible.contains(targetUserId);
    }

    // 是否部门主管，负责至少一个部门
    private boolean isDeptManagerUser(Long userId) {
        if (userId == null) return false;
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizations WHERE leader_id=? AND deleted_at IS NULL",
                Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 是否可看全公司数据：管理员、总经理、人事主管
    private boolean isGlobalViewer(Long userId) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON ur.role_id=r.id " +
                "WHERE ur.user_id=? AND ur.deleted_at IS NULL AND r.status=1 AND r.deleted_at IS NULL AND r.role_code IN ('admin','gm','hr_manager')",
                Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // 找员工的直属主管当补签初审人：沿部门 parent_id 往上找负责人
    private Long findDirectManager(Long userId) {
        try {
            Users user = usersRepo.selectById(userId);
            if (user == null || user.getOrg_id() == null) return null;
            Long orgId = user.getOrg_id();
            Set<Long> visited = new HashSet<>();
            while (orgId != null && visited.add(orgId)) {
                Map<String, Object> org = jdbc.queryForMap(
                    "SELECT leader_id,parent_id FROM organizations WHERE id=? AND deleted_at IS NULL", orgId);
                Object leader = org.get("leader_id");
                if (leader != null) {
                    Long leaderId = ((Number) leader).longValue();
                    if (!leaderId.equals(userId)) return leaderId;
                }
                Object parent = org.get("parent_id");
                orgId = parent == null ? null : ((Number) parent).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // 主管负责的部门 ID（含下级部门），不是主管返回空集合
    private Set<Long> managedDeptIds(Long managerId) {
        try {
            List<Long> leadOrgIds = jdbc.queryForList(
                "SELECT id FROM organizations WHERE leader_id=? AND deleted_at IS NULL", Long.class, managerId);
            if (leadOrgIds.isEmpty()) return new HashSet<>();
            List<Map<String, Object>> allOrgs = jdbc.queryForList(
                "SELECT id,parent_id FROM organizations WHERE deleted_at IS NULL");
            Set<Long> scope = new HashSet<>();
            Deque<Long> stack = new ArrayDeque<>(leadOrgIds);
            while (!stack.isEmpty()) {
                Long orgId = stack.pop();
                if (!scope.add(orgId)) continue;
                for (Map<String, Object> org : allOrgs) {
                    Object parent = org.get("parent_id");
                    if (parent != null && orgId.equals(((Number) parent).longValue())) {
                        stack.add(((Number) org.get("id")).longValue());
                    }
                }
            }
            return scope;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    // 主管负责的部门（含下级）里的用户 ID，不是主管返回空列表
    private List<Long> managedDeptUserIds(Long managerId) {
        try {
            List<Long> leadOrgIds = jdbc.queryForList(
                "SELECT id FROM organizations WHERE leader_id=? AND deleted_at IS NULL", Long.class, managerId);
            if (leadOrgIds.isEmpty()) return new ArrayList<>();
            List<Map<String, Object>> allOrgs = jdbc.queryForList(
                "SELECT id,parent_id FROM organizations WHERE deleted_at IS NULL");
            Set<Long> scope = new HashSet<>();
            Deque<Long> stack = new ArrayDeque<>(leadOrgIds);
            while (!stack.isEmpty()) {
                Long orgId = stack.pop();
                if (!scope.add(orgId)) continue;
                for (Map<String, Object> org : allOrgs) {
                    Object parent = org.get("parent_id");
                    if (parent != null && orgId.equals(((Number) parent).longValue())) {
                        stack.add(((Number) org.get("id")).longValue());
                    }
                }
            }
            String placeholders = scope.stream().map(x -> "?").collect(Collectors.joining(","));
            return jdbc.queryForList(
                "SELECT id FROM users WHERE org_id IN (" + placeholders + ") AND deleted_at IS NULL",
                Long.class, scope.toArray());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 某个角色下所有启用状态的用户 ID，用于人事终审通知
    private List<Long> userIdsByRole(Long roleId) {
        try {
            return jdbc.queryForList(
                "SELECT DISTINCT ur.user_id FROM user_roles ur JOIN users u ON u.id=ur.user_id " +
                "WHERE ur.role_id=? AND u.status=1 AND u.deleted_at IS NULL", Long.class, roleId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // 分钟换算成小时保留两位，单班次最多 24 小时
    private BigDecimal hoursOf(long minutes) {
        if (minutes < 0) minutes = 0;
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (hours.compareTo(MAX_WORK_HOURS) > 0) return MAX_WORK_HOURS;
        return hours;
    }

    // 单个班次工时上限：一天 24 小时，超过按 24 记，避免脏数据写不进 decimal(4,2)
    private static final BigDecimal MAX_WORK_HOURS = new BigDecimal("24.00");

    // 夜班（下班不晚于上班）的下班时刻算第二天，日班算当天
    private LocalDateTime workEndReference(LocalDate date, LocalTime workStart, LocalTime workEnd) {
        LocalDateTime end = LocalDateTime.of(date, workEnd);
        return workEnd.isAfter(workStart) ? end : end.plusDays(1);
    }

    // 按签到和签退时间重算工时与加班，加班不满 1 小时不记
    private void recalcWorkHours(AttendanceRecords rec) {
        if (rec == null || rec.getCheck_in_time() == null || rec.getCheck_out_time() == null) return;
        long minutes = Duration.between(rec.getCheck_in_time(), rec.getCheck_out_time()).toMinutes();
        rec.setWork_hours(hoursOf(minutes));
        LocalDateTime endRef = workEndReference(rec.getAttendance_date(), getWorkStart(), getWorkEnd());
        long overtimeMinutes = Duration.between(endRef, rec.getCheck_out_time()).toMinutes();
        rec.setOvertime_hours(overtimeMinutes >= 60 ? hoursOf(overtimeMinutes) : null);
    }

    // 缺勤提醒：标题正文优先取 attendance_abnormal 模板，运营改文案不用改代码
    private void notifyAbsence(Long userId, LocalDate date) {
        try {
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("date", date == null ? "" : date.toString());
            vars.put("status", "缺勤");
            com.officeautomationsystemapi.service.NotificationTemplateService.Rendered message =
                    notificationTemplateService.render("attendance_abnormal", vars,
                            "出勤异常提醒：" + date,
                            "您 " + date + " 的考勤状态为「缺勤」，如有异议请在出勤管理里提交申诉。");
            notify(userId, message.title(), message.content(), "attendance", null, "/home/attendance");
        } catch (Exception ignore) {
            // 提醒失败不影响结算
        }
    }

    // 站内信通知，通知失败不影响主流程
    private void notify(Long userId, String title, String content, String bizType, Long bizId, String jumpUrl) {
        if (userId == null) return;
        try {
            NotificationRecords n = new NotificationRecords();
            n.setUser_id(userId);
            n.setTitle(title);
            n.setContent(content);
            n.setType(2);
            n.setBiz_type(bizType);
            n.setBiz_id(bizId);
            n.setJump_url(jumpUrl);
            n.setIs_read(0);
            n.setPush_status(0);
            n.setCreated_at(LocalDateTime.now());
            n.setUpdated_at(LocalDateTime.now());
            notificationRepo.insert(n);
        } catch (Exception ignored) {
        }
    }

    // 记录重新签到、签退的操作日志，方便管理员追溯
    private void logRecheck(Long userId, String ip, Long recordId, String action, String url, String change) {
        try {
            String username = "";
            try {
                username = jdbc.queryForObject("SELECT username FROM users WHERE id=? AND deleted_at IS NULL", String.class, userId);
            } catch (Exception ignored) {
            }
            jdbc.update("INSERT INTO operation_logs (user_id, username, module, action, method, url, ip_address, target_type, target_id, request_params, response_result, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                userId, username, "attendance", action, "POST", url, ip, "attendance", recordId, change, "ok", 1);
        } catch (Exception ignored) {
        }
    }

    // 当天的班次时间
    private Map<String, Object> activeShift() {
        return activeShift(LocalDate.now());
    }

    // 指定日期的班次：先匹配排班规则，没有就用启用的模板
    private Map<String, Object> activeShift(LocalDate date) {
        try {
            int weekday = date.getDayOfWeek().getValue() % 7;
            List<Map<String, Object>> schedules = jdbc.queryForList(
                "SELECT work_start, work_end, late_threshold, early_leave_threshold, schedule_type, schedule_date, " +
                "week_day, start_date, end_date, lunar_month, lunar_day, yearly_month, yearly_day " +
                "FROM shift_schedules WHERE is_active=1");
            for (Map<String, Object> row : schedules) {
                if (!matchesSchedule(row, date, weekday)) continue;
                for (String field : SCHEDULE_MATCH_FIELDS) row.remove(field);
                return row;
            }
        } catch (Exception ignored) {
            // 排班规则读不到就退回班次模板
        }
        try {
            return jdbc.queryForMap("SELECT work_start, work_end, late_threshold, early_leave_threshold, is_all_day FROM shift_templates WHERE is_active=1 LIMIT 1");
        } catch (Exception e) {
            return null;
        }
    }

    // 判断某天的排班规则是否命中：单次、每周、区间、每年、农历
    private boolean matchesSchedule(Map<String, Object> row, LocalDate date, int weekday) {
        String type = (String) row.get("schedule_type");
        if ("once".equals(type)) {
            return date.toString().equals(String.valueOf(row.get("schedule_date")));
        }
        if ("weekly".equals(type)) {
            Object day = row.get("week_day");
            return day != null && weekday == ((Number) day).intValue();
        }
        if ("range".equals(type)) {
            Object start = row.get("start_date");
            Object end = row.get("end_date");
            if (start == null || end == null) return false;
            String today = date.toString();
            return today.compareTo(String.valueOf(start)) >= 0 && today.compareTo(String.valueOf(end)) <= 0;
        }
        if ("yearly".equals(type)) {
            Object month = row.get("yearly_month");
            Object day = row.get("yearly_day");
            return month != null && day != null
                && date.getMonthValue() == ((Number) month).intValue()
                && date.getDayOfMonth() == ((Number) day).intValue();
        }
        if ("lunar".equals(type)) {
            // 农历要按日期换算，只能逐条比对
            int[] lunar = LunarUtil.toLunar(date);
            Object month = row.get("lunar_month");
            Object day = row.get("lunar_day");
            return lunar != null && month != null && day != null
                && lunar[0] == ((Number) month).intValue()
                && lunar[1] == ((Number) day).intValue();
        }
        return false;
    }

    // 从班次配置里取时间，没有就用默认值
    private LocalTime shiftTime(Map<String, Object> shift, String key, LocalTime defaultValue) {
        Object value = shift == null ? null : shift.get(key);
        return value == null ? defaultValue : LocalTime.parse(value.toString());
    }

    private LocalTime getWorkStart() {
        return shiftTime(activeShift(), "work_start", LocalTime.of(9, 0));
    }

    private LocalTime getWorkEnd() {
        return getWorkEnd(LocalDate.now());
    }

    private LocalTime getWorkEnd(LocalDate date) {
        return shiftTime(activeShift(date), "work_end", LocalTime.of(18, 0));
    }

    private LocalTime getLateThreshold() {
        return shiftTime(activeShift(), "late_threshold", LocalTime.of(9, 30));
    }

    private LocalTime getEarlyLeaveThreshold() {
        return shiftTime(activeShift(), "early_leave_threshold", LocalTime.of(17, 30));
    }

    // 是否全天班次，全天班次不限制打卡时间
    private boolean isAllDay() {
        Map<String, Object> shift = activeShift();
        if (shift == null || shift.get("is_all_day") == null) return false;
        String value = String.valueOf(shift.get("is_all_day"));
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    // 节假日类型：1 法定假日、2 调休上班日，没有配置返回 null
    private String holidayType(LocalDate date) {
        try {
            return jdbc.queryForObject("SELECT holiday_type FROM holidays WHERE holiday_date=?", String.class, date.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // 是否法定假日
    private boolean isHoliday(LocalDate date) {
        return "1".equals(holidayType(date));
    }

    // 应出勤天数：工作日加调休上班，周末按配置算
    private int expectedWorkDays(String month) {
        try {
            LocalDate first = LocalDate.parse(month + "-01");
            boolean weekendCount = "1".equals(getParam("attendance_weekend_count", "0"));
            int count = 0;
            for (int day = 1; day <= first.lengthOfMonth(); day++) {
                LocalDate date = first.withDayOfMonth(day);
                String type = holidayType(date);
                if ("2".equals(type)) {
                    count++;
                    continue;
                }
                boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                if (weekend && !weekendCount) continue;
                if ("1".equals(type)) continue;
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Map<String, Object> getTodayStatus(Long userId) {
        LocalDate today = LocalDate.now();
        AttendanceRecords rec = attendanceRepo.findByUserAndDate(userId, today);
        LocalTime workStart = getWorkStart();
        LocalTime workEnd = getWorkEnd();
        boolean holiday = isHoliday(today);

        LocalTime nowTime = LocalTime.now();
        LocalTime checkInStart = workStart.minusHours(1);
        LocalTime checkInEnd = workStart.plusHours(2);
        LocalTime checkOutStart = workEnd.minusHours(1);
        boolean allDay = isAllDay();

        Map<String, Object> map = new HashMap<>();
        map.put("canCheckIn", !holiday && (allDay || (nowTime.isAfter(checkInStart) && nowTime.isBefore(checkInEnd))));
        map.put("canCheckOut", !holiday && (allDay || nowTime.isAfter(checkOutStart)));
        map.put("isHoliday", holiday);
        map.put("workStart", workStart.toString());
        map.put("workEnd", workEnd.toString());
        map.put("checkInStart", checkInStart.toString());
        map.put("checkInEnd", checkInEnd.toString());
        map.put("checkOutStart", checkOutStart.toString());

        // 没有记录又过了签到窗口，前端按缺勤显示
        boolean overWindow = !holiday && nowTime.isAfter(checkInEnd);
        map.put("checkInTime", rec != null ? rec.getCheck_in_time() : null);
        map.put("checkOutTime", rec != null ? rec.getCheck_out_time() : null);
        map.put("status", rec != null ? rec.getStatus() : (overWindow ? 3 : null));
        map.put("workHours", rec != null ? rec.getWork_hours() : null);
        return map;
    }

    @Override
    public AttendanceRecords checkIn(Long userId, String ip) {
        return checkIn(userId, ip, null, null, "normal", null);
    }

    public AttendanceRecords checkIn(Long userId, String ip, BigDecimal lat, BigDecimal lng, String checkType) {
        return checkIn(userId, ip, lat, lng, checkType, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecords checkIn(Long userId, String ip, BigDecimal lat, BigDecimal lng, String checkType, Boolean force) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        LocalTime workStart = getWorkStart();
        LocalTime lateThreshold = getLateThreshold();
        AttendanceRecords rec = attendanceRepo.findByUserAndDate(userId, today);

        // 出差期间不用打卡，出差记录由审批流程生成
        if (!"出差".equals(checkType) && rec != null && rec.getStatus() != null && rec.getStatus() == 5) {
            throw new RuntimeException("您正处于出差期间，无需打卡（出差：" + (rec.getRemark() == null ? "" : rec.getRemark()) + "）");
        }

        // 节假日不用打卡，出差和外出类型除外
        if (!"出差".equals(checkType) && !"外出".equals(checkType) && isHoliday(today)) {
            throw new RuntimeException("今天是节假日，无需打卡");
        }

        // 签到窗口：上班前 1 小时到上班后 2 小时，全天班次不限制
        LocalTime checkInStart = workStart.minusHours(1);
        LocalTime checkInEnd = workStart.plusHours(2);
        if (!isAllDay()) {
            if (nowTime.isBefore(checkInStart)) {
                throw new RuntimeException("还未到签到时间，请在" + checkInStart + "后签到");
            }
            if (nowTime.isAfter(checkInEnd)) {
                throw new RuntimeException("已超过签到时间（" + checkInEnd + "前可签到），请联系管理员或发起补签申请");
            }
        }

        // 地理校验，返回 1 表示异地签到，等审核
        int geo = geoMark(lat, lng, force != null && force);

        // 重新签到：2 小时内、最多 3 次，并记一条操作日志
        if (rec != null && rec.getCheck_in_time() != null) {
            int count = rec.getCheck_in_count() != null ? rec.getCheck_in_count() : 1;
            if (count >= 3) throw new RuntimeException("今天已重新签到3次，不能再改了");
            if (rec.getCheck_in_time().plusHours(2).isBefore(now)) {
                throw new RuntimeException("签到已超过2小时，不能重新签到");
            }
            rec.setCheck_in_count(count + 1);
            logRecheck(userId, ip, rec.getId(), "recheck_in", "/api/attendance/check-in",
                "original=" + rec.getCheck_in_time() + " -> " + now);
        }

        // 迟到判定：超过迟到线算严重迟到，只超过上班时间算迟到
        int status = 0;
        String remark = null;
        if (!isAllDay() && nowTime.isAfter(workStart)) {
            if (nowTime.isAfter(lateThreshold)) {
                status = 7;
                remark = "严重迟到";
            } else {
                status = 1;
                remark = "迟到";
            }
        }

        if (rec == null) {
            Users user = usersRepo.selectById(userId);
            rec = new AttendanceRecords();
            rec.setUser_id(userId);
            rec.setDept_id(user != null ? user.getOrg_id() : null);
            rec.setAttendance_date(today);
            rec.setCheck_in_time(now);
            rec.setCheck_in_ip(ip);
            rec.setCheck_in_count(1);
            rec.setStatus(status);
            rec.setIs_makeup(0);
            rec.setRemark(remark);
            rec.setCheck_type(checkType);
            rec.setCreated_at(now);
            rec.setUpdated_at(now);
            if (geo == 1) rec.setGeo_abnormal(1);
            if (lat != null) rec.setLatitude(lat);
            if (lng != null) rec.setLongitude(lng);
            attendanceRepo.insert(rec);
        } else {
            rec.setCheck_in_time(now);
            rec.setCheck_in_ip(ip);
            rec.setStatus(status);
            rec.setRemark(remark);
            rec.setCheck_type(checkType);
            rec.setUpdated_at(now);
            if (geo == 1) rec.setGeo_abnormal(1);
            if (lat != null) rec.setLatitude(lat);
            if (lng != null) rec.setLongitude(lng);
            // 覆盖签到时间后要重算工时
            recalcWorkHours(rec);
            attendanceRepo.updateById(rec);
        }
        return rec;
    }

    @Override
    public AttendanceRecords checkOut(Long userId, String ip) {
        return checkOut(userId, ip, null, null, null);
    }

    @Override
    public AttendanceRecords checkOut(Long userId, String ip, BigDecimal lat, BigDecimal lng) {
        return checkOut(userId, ip, lat, lng, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecords checkOut(Long userId, String ip, BigDecimal lat, BigDecimal lng, Boolean force) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime nowTime = now.toLocalTime();
        LocalTime workStart = getWorkStart();
        LocalTime workEnd = getWorkEnd();
        LocalTime earlyLeaveThreshold = getEarlyLeaveThreshold();

        // 签退窗口：下班前 1 小时之后，全天班次不限制
        LocalTime checkOutStart = workEnd.minusHours(1);
        if (!isAllDay() && nowTime.isBefore(checkOutStart)) {
            throw new RuntimeException("还未到签退时间，请在" + checkOutStart + "后签退");
        }

        int geo = geoMark(lat, lng, force != null && force);

        AttendanceRecords rec = attendanceRepo.findByUserAndDate(userId, today);
        if (rec == null) {
            // 跨天夜班：签到在前一天甚至更早、还没签退，取最近一条
            rec = attendanceRepo.selectOne(new QueryWrapper<AttendanceRecords>()
                .eq("user_id", userId)
                .le("attendance_date", today)
                .isNotNull("check_in_time")
                // 只接 24 小时内的跨天夜班，更早的漏签退记录不关
                .ge("check_in_time", now.minusHours(24))
                .isNull("check_out_time")
                .orderByDesc("attendance_date")
                .last("LIMIT 1"));
        }
        if (rec == null) {
            throw new RuntimeException("今天还没有签到记录，请先签到");
        }

        // 重新签退：2 小时内、最多 3 次，并记一条操作日志
        if (rec.getCheck_out_time() != null) {
            int count = rec.getCheck_out_count() != null ? rec.getCheck_out_count() : 1;
            if (count >= 3) throw new RuntimeException("今天已重新签退3次");
            if (rec.getCheck_out_time().plusHours(2).isBefore(now)) {
                throw new RuntimeException("签退已超过2小时，不能重新签退");
            }
            rec.setCheck_out_count(count + 1);
            logRecheck(userId, ip, rec.getId(), "recheck_out", "/api/attendance/check-out",
                "original=" + rec.getCheck_out_time() + " -> " + now);
        } else {
            rec.setCheck_out_count(1);
        }

        // 早退判定：只改正常和迟到的记录，全天班次不判定
        int currentStatus = rec.getStatus() == null ? 0 : rec.getStatus();
        if (!isAllDay() && (currentStatus == 0 || currentStatus == 1)) {
            if (nowTime.isBefore(earlyLeaveThreshold)) {
                rec.setStatus(9);
                rec.setRemark("严重早退");
            } else if (nowTime.isBefore(workEnd)) {
                rec.setStatus(2);
                rec.setRemark("早退");
            }
        }

        // 工时按签到到签退算，超过下班时间的部分算加班
        if (rec.getCheck_in_time() != null) {
            rec.setWork_hours(hoursOf(Duration.between(rec.getCheck_in_time(), now).toMinutes()));
            LocalDateTime endRef = workEndReference(rec.getAttendance_date(), workStart, workEnd);
            long overtimeMinutes = Duration.between(endRef, now).toMinutes();
            if (overtimeMinutes >= 60) {
                rec.setOvertime_hours(hoursOf(overtimeMinutes));
            }
        }

        rec.setCheck_out_time(now);
        rec.setCheck_out_ip(ip);
        if (geo == 1) rec.setGeo_abnormal(1);
        if (lat != null) rec.setLatitude(lat);
        if (lng != null) rec.setLongitude(lng);
        rec.setUpdated_at(now);
        attendanceRepo.updateById(rec);
        return rec;
    }

    @Override
    public List<AttendanceRecords> myRecords(Long userId, String month) {
        String targetMonth = (month == null || month.isEmpty()) ? LocalDate.now().toString().substring(0, 7) : month;
        return attendanceRepo.listByUserMonth(userId, targetMonth);
    }

    @Override
    public List<Map<String, Object>> deptStats(Long userId, Long deptId, String month) {
        if (deptId == null) {
            Users user = usersRepo.selectById(userId);
            deptId = user != null ? user.getOrg_id() : null;
        }
        // 部门统计只有管理员、总经理和该部门主管能看
        if (!isAdminUser(userId) && !isGlobalViewer(userId) && !managedDeptIds(userId).contains(deptId)) {
            throw new RuntimeException("无权查看该部门出勤统计");
        }
        String targetMonth = (month == null || month.isEmpty()) ? LocalDate.now().toString().substring(0, 7) : month;
        return attendanceRepo.statsByDeptMonth(deptId, targetMonth);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceMakeup applyMakeup(Long userId, AttendanceMakeup apply) {
        if (apply == null || apply.getAttendance_date() == null) {
            throw new RuntimeException("请选择要补签的日期");
        }
        LocalDate today = LocalDate.now();
        LocalDate applyDate = apply.getAttendance_date();

        if (applyDate.isAfter(today)) {
            throw new RuntimeException("不能补签未来的日期");
        }
        // 只能补最近 30 天，管理员不受限
        if (!isAdminUser(userId) && applyDate.isBefore(today.minusDays(30))) {
            throw new RuntimeException("只能补签最近30天内的日期");
        }

        // 同一天最多申请 3 次，被驳回也算
        if (makeupRepo.countByUserAndDate(userId, applyDate) >= 3) {
            throw new RuntimeException("该日期的补签申请已达3次上限，如需处理请联系管理员");
        }

        // 按补签日期所在月份算次数
        String month = applyDate.toString().substring(0, 7);
        if (makeupRepo.countApprovedByUserMonth(userId, month) >= MAX_MAKEUP_PER_MONTH) {
            throw new RuntimeException(month + "月补签次数已用完（最多" + MAX_MAKEUP_PER_MONTH + "次）");
        }

        apply.setUser_id(userId);
        apply.setStatus(0);
        // 有直属主管走初审，没有就直接人事终审
        Long directManager = findDirectManager(userId);
        apply.setCurrent_stage(directManager != null ? 1 : 2);
        apply.setCreated_at(LocalDateTime.now());
        apply.setUpdated_at(LocalDateTime.now());
        makeupRepo.insert(apply);

        // 通知对应环节的审批人，admin 始终能处理
        try {
            Users user = usersRepo.selectById(userId);
            String userName = user != null && user.getReal_name() != null ? user.getReal_name() : String.valueOf(userId);
            String typeText = "补签";
            if (apply.getType() != null) {
                if (apply.getType() == 1) typeText = "补签到";
                else if (apply.getType() == 2) typeText = "补签退";
                else if (apply.getType() == 3) typeText = "签+退";
            }
            String content = userName + " 提交了 " + apply.getAttendance_date() + " 的" + typeText + "申请，请审批";
            Set<Long> receivers = new HashSet<>();
            receivers.add(1L);
            if (directManager != null) {
                receivers.add(directManager);
            } else {
                receivers.addAll(userIdsByRole(4L));
        receivers.addAll(userIdsByRole(7L));
            }
            for (Long receiverId : receivers) {
                if (receiverId != null && !receiverId.equals(userId)) {
                    notify(receiverId, "新的补签申请待审批", content, "attendance_makeup", apply.getId(), "/home/attendance");
                }
            }
        } catch (Exception ignored) {
        }
        return apply;
    }

    @Override
    public List<AttendanceMakeup> myMakeups(Long userId) {
        List<AttendanceMakeup> list = makeupRepo.listByUser(userId);
        enrichApproverNames(list);
        return list;
    }

    // 补上两级审批人的姓名
    private void enrichApproverNames(List<AttendanceMakeup> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> ids = new HashSet<>();
        for (AttendanceMakeup makeup : list) {
            if (makeup.getFirst_approver_id() != null) ids.add(makeup.getFirst_approver_id());
            if (makeup.getApprover_id() != null) ids.add(makeup.getApprover_id());
        }
        Map<Long, String> names = new HashMap<>();
        for (Long id : ids) {
            Users user = usersRepo.selectById(id);
            if (user != null) names.put(id, user.getReal_name());
        }
        for (AttendanceMakeup makeup : list) {
            if (makeup.getFirst_approver_id() != null) makeup.setFirst_approver_name(names.get(makeup.getFirst_approver_id()));
            if (makeup.getApprover_id() != null) makeup.setApprover_name(names.get(makeup.getApprover_id()));
        }
    }

    @Override
    public List<AttendanceMakeup> pendingMakeups(Long userId, Integer status, Long deptId) {
        // 只看待审批的：管理员看全部，人事看终审，主管看本部门初审
        List<AttendanceMakeup> all = makeupRepo.listPending(0, null);
        boolean admin = isAdminUser(userId);
        // 终审只有人事主管能看，人事专员不参与终审
        boolean hrManager = isHrManagerUser(userId);
        boolean hrSpec = isHrUser(userId);
        List<Long> deptUserIds = isDeptManagerUser(userId) ? managedDeptUserIds(userId) : new ArrayList<>();
        List<AttendanceMakeup> result = new ArrayList<>();
        for (AttendanceMakeup makeup : all) {
            if (makeup.getStatus() == null || makeup.getStatus() != 0) continue;
            int stage = makeup.getCurrent_stage() == null ? 1 : makeup.getCurrent_stage();
            if (admin) {
                result.add(makeup);
            } else if (hrManager && stage == 2) {
                result.add(makeup);
            } else if (stage == 1 && deptUserIds.contains(makeup.getUser_id())) {
                result.add(makeup);
            } else if (stage == 1 && hrSpec && inDataScope(userId, makeup.getUser_id())) {
                // 人事专员可以协助初审，但初审通过后仍要人事主管终审
                result.add(makeup);
            }
        }
        enrichApproverNames(result);
        return result;
    }

    // 是否缺卡或缺勤记录，只有这种才需要写回补签时间
    private boolean isMissingPunchRecord(Integer status) {
        return status == null || status == 3 || status == 8;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceMakeup approveMakeup(Long approverId, Long makeupId, Integer result, String remark) {
        AttendanceMakeup makeup = makeupRepo.selectById(makeupId);
        if (makeup == null) {
            throw new RuntimeException("补签申请不存在");
        }
        if (makeup.getStatus() == null || makeup.getStatus() != 0) {
            throw new RuntimeException("该申请已处理过");
        }
        // 不能给自己审批
        if (approverId.equals(makeup.getUser_id())) {
            throw new RuntimeException("不能给自己审批补签申请，请联系人事或上级审批");
        }

        // 初审是部门主管，终审是人事主管，管理员哪个环节都能处理
        int stage = makeup.getCurrent_stage() == null ? 1 : makeup.getCurrent_stage();
        boolean admin = isAdminUser(approverId);
        boolean firstByManager = isDeptManagerUser(approverId)
            && managedDeptUserIds(approverId).contains(makeup.getUser_id());
        boolean firstByHrSpec = isHrUser(approverId) && inDataScope(approverId, makeup.getUser_id());
        boolean canFirst = admin || (stage == 1 && (firstByManager || firstByHrSpec));
        // 人事专员没有最终审批权，终审环节只认人事主管和管理员
        boolean canFinal = admin || (stage == 2 && isHrManagerUser(approverId));
        if (!canFirst && !canFinal) {
            throw new RuntimeException("您无权审批该申请（可能不在您的审批环节）");
        }

        boolean pass = result != null && result == 1;
        LocalDateTime now = LocalDateTime.now();

        // 终审通过要写出勤记录，当天已有正常出勤就提示且不动数据
        boolean writeBack = pass && !(stage == 1 && !admin);
        if (writeBack) {
            AttendanceRecords existing = attendanceRepo.findByUserAndDate(makeup.getUser_id(), makeup.getAttendance_date());
            if (existing != null && !isMissingPunchRecord(existing.getStatus())) {
                throw new RuntimeException("当天已有出勤记录，无需补签");
            }
        }

        // 主管初审通过就转人事终审，这一环不动出勤记录
        if (pass && stage == 1 && !admin) {
            makeup.setFirst_approver_id(approverId);
            makeup.setFirst_approve_time(now);
            makeup.setFirst_approve_remark(remark);
            makeup.setCurrent_stage(2);
            makeup.setUpdated_at(now);
            makeupRepo.updateById(makeup);
            try {
                Users user = usersRepo.selectById(makeup.getUser_id());
                String userName = user != null && user.getReal_name() != null ? user.getReal_name() : String.valueOf(makeup.getUser_id());
                String content = userName + " 的 " + makeup.getAttendance_date() + " 补签申请已通过部门主管初审，请人事终审";
                // 初审通过后只等终审，通知人事主管和管理员
                Set<Long> receivers = new HashSet<>(userIdsByRole(7L));
                receivers.add(1L);
                for (Long receiverId : receivers) {
                    if (receiverId != null && !receiverId.equals(makeup.getUser_id())) {
                        notify(receiverId, "补签申请待人事终审", content, "attendance_makeup", makeupId, "/home/attendance");
                    }
                }
                notify(makeup.getUser_id(), "补签主管初审通过",
                    "您 " + makeup.getAttendance_date() + " 的补签申请已通过部门主管初审，等待人事终审",
                    "attendance_makeup", makeupId, "/home/attendance");
            } catch (Exception ignored) {
            }
            return makeup;
        }

        // 驳回记在哪个环节：初审驳回记 first_，其余记 approver_
        boolean rejectAtFirst = !pass && stage == 1 && !admin;
        if (rejectAtFirst) {
            makeup.setFirst_approver_id(approverId);
            makeup.setFirst_approve_time(now);
            makeup.setFirst_approve_remark(remark);
        } else {
            makeup.setApprover_id(approverId);
            makeup.setApprove_time(now);
            makeup.setApprove_remark(remark);
        }
        makeup.setStatus(pass ? 1 : 2);
        makeup.setUpdated_at(now);
        makeupRepo.updateById(makeup);

        // 通过后把补签时间写回出勤记录
        if (pass) {
            AttendanceRecords rec = attendanceRepo.findByUserAndDate(makeup.getUser_id(), makeup.getAttendance_date());
            if (rec == null) {
                // 当天完全没有记录，新建一条
                Users user = usersRepo.selectById(makeup.getUser_id());
                rec = new AttendanceRecords();
                rec.setUser_id(makeup.getUser_id());
                rec.setDept_id(user != null ? user.getOrg_id() : null);
                rec.setAttendance_date(makeup.getAttendance_date());
                rec.setStatus(0);
                rec.setIs_makeup(1);
                rec.setMakeup_apply_id(makeupId);
                rec.setCreated_at(now);
                rec.setUpdated_at(now);
                if (makeup.getCheck_in_time() != null) rec.setCheck_in_time(makeup.getCheck_in_time());
                if (makeup.getCheck_out_time() != null) rec.setCheck_out_time(makeup.getCheck_out_time());
                recalcWorkHours(rec);
                attendanceRepo.insert(rec);
            } else {
                // 只补空缺的字段，已有的打卡时间不覆盖，避免把正常出勤改坏
                if (rec.getCheck_in_time() == null && makeup.getCheck_in_time() != null) {
                    rec.setCheck_in_time(makeup.getCheck_in_time());
                }
                if (rec.getCheck_out_time() == null && makeup.getCheck_out_time() != null) {
                    rec.setCheck_out_time(makeup.getCheck_out_time());
                }
                rec.setIs_makeup(1);
                rec.setMakeup_apply_id(makeupId);
                // 签到签退都补齐后状态才算正常
                if (rec.getCheck_in_time() != null && rec.getCheck_out_time() != null) {
                    rec.setStatus(0);
                }
                rec.setRemark("补签通过");
                rec.setUpdated_at(now);
                recalcWorkHours(rec);
                attendanceRepo.updateById(rec);
            }
        }

        // 通知申请人审批结果
        try {
            String dateText = String.valueOf(makeup.getAttendance_date());
            String title = pass ? "补签申请已通过" : "补签申请被驳回";
            String content = pass
                    ? "您 " + dateText + " 的补签申请已审批通过"
                    : "您 " + dateText + " 的补签申请被驳回" + (remark != null && !remark.isEmpty() ? "，意见：" + remark : "");
            notify(makeup.getUser_id(), title, content, "attendance_makeup", makeupId, "/home/attendance");
        } catch (Exception ignored) {
        }
        return makeup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawMakeup(Long userId, Long makeupId) {
        AttendanceMakeup makeup = makeupRepo.selectById(makeupId);
        if (makeup == null) throw new RuntimeException("补签申请不存在");
        if (makeup.getUser_id() == null || !makeup.getUser_id().equals(userId)) {
            throw new RuntimeException("只能撤回自己的申请");
        }
        if (makeup.getStatus() == null || makeup.getStatus() != 0) {
            throw new RuntimeException("只能撤回待审批的申请");
        }
        // 逻辑删除，deleted_at 由 @TableLogic 维护
        makeupRepo.deleteById(makeupId);
        // 撤回后把审批人的待审批通知标记为已读，避免消息数对不上
        try {
            NotificationRecords done = new NotificationRecords();
            done.setIs_read(1);
            done.setRead_time(LocalDateTime.now());
            notificationRepo.update(done, new QueryWrapper<NotificationRecords>()
                .eq("biz_type", "attendance_makeup")
                .eq("biz_id", makeupId)
                .eq("is_read", 0));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void deleteMakeup(Long makeupId) {
        AttendanceMakeup makeup = makeupRepo.selectById(makeupId);
        if (makeup == null) throw new RuntimeException("补签申请不存在");
        makeupRepo.deleteById(makeupId);
    }

    @Override
    public Map<String, Object> allRecords(Long userId, String startDate, String endDate, Long deptId, Integer status, int pageNum, int pageSize) {
        QueryWrapper<AttendanceRecords> qw = new QueryWrapper<>();
        // 管理员、总经理、人事主管看全公司，部门主管看本部门和下级
        if (isGlobalViewer(userId)) {
            if (deptId != null) qw.eq("dept_id", deptId);
        } else if (isDeptManagerUser(userId)) {
            List<Long> visibleUserIds = managedDeptUserIds(userId);
            // 指定了不在管理范围内的部门时返回空，避免越权探测
            if (deptId != null && !managedDeptIds(userId).contains(deptId)) {
                visibleUserIds = new ArrayList<>();
            }
            // in 空集合会拼出非法的 IN ()，塞一个不存在的 ID
            if (visibleUserIds.isEmpty()) {
                qw.eq("user_id", -1L);
            } else {
                qw.in("user_id", visibleUserIds);
            }
        } else {
            qw.eq("user_id", userId);
        }
        if (startDate != null && !startDate.isEmpty()) qw.ge("attendance_date", startDate);
        if (endDate != null && !endDate.isEmpty()) qw.le("attendance_date", endDate);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("attendance_date");

        Page<AttendanceRecords> result = attendanceRepo.selectPage(new Page<>(pageNum, pageSize), qw);
        // 补上姓名，管理员查看全员时要区分人
        for (AttendanceRecords rec : result.getRecords()) {
            if (rec.getUser_id() == null) continue;
            Users user = usersRepo.selectById(rec.getUser_id());
            if (user != null && user.getReal_name() != null) rec.setReal_name(user.getReal_name());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pageNum", pageNum);
        map.put("pageSize", pageSize);
        return map;
    }

    @Override
    public List<Map<String, Object>> statistics(Long userId, Long deptId, String month) {
        String targetMonth = (month == null || month.isEmpty()) ? LocalDate.now().toString().substring(0, 7) : month;
        if (deptId == null) {
            // 管理员和总经理不选部门时看全公司，其他人只看自己部门
            if (isGlobalViewer(userId)) {
                deptId = 0L;
            } else {
                Users user = usersRepo.selectById(userId);
                deptId = user != null ? user.getOrg_id() : null;
            }
        }
        // deptId 为 0 表示全公司
        List<Map<String, Object>> rows = (deptId != null && deptId == 0L)
            ? attendanceRepo.statsAllMonth(targetMonth)
            : attendanceRepo.statsByDeptMonth(deptId, targetMonth);

        int expected = expectedWorkDays(targetMonth);
        for (Map<String, Object> row : rows) {
            Users user = usersRepo.selectById(((Number) row.get("user_id")).longValue());
            if (user != null) row.put("real_name", user.getReal_name());
            Number total = (Number) row.get("total_days");
            int totalDays = total == null ? 0 : total.intValue();
            row.put("expected_days", expected);
            row.put("attendance_rate", expected > 0 ? Math.round(totalDays * 100.0 / expected) : 0);
        }
        return rows;
    }

    // 提醒未签退的人
    private void remindNoCheckout(LocalDate date, String title, String content) {
        List<Map<String, Object>> noCheckout = jdbc.queryForList(
            "SELECT a.user_id, u.real_name FROM attendance_records a JOIN users u ON u.id=a.user_id " +
            "WHERE a.attendance_date=? AND a.check_in_time IS NOT NULL AND a.check_out_time IS NULL AND a.deleted_at IS NULL",
            date);
        for (Map<String, Object> row : noCheckout) {
            notify(Long.valueOf(String.valueOf(row.get("user_id"))), title, row.get("real_name") + "，" + content,
                "attendance_remind", null, "/home/attendance");
        }
    }

    @Override
    public Map<String, Object> myStatistics(Long userId, String month) {
        String targetMonth = (month == null || month.isEmpty()) ? LocalDate.now().toString().substring(0, 7) : month;
        Users me = usersRepo.selectById(userId);
        Map<String, Object> row = null;
        // 复用部门月度统计，只取本人那一行，避免再写一套聚合 SQL
        if (me != null && me.getOrg_id() != null) {
            for (Map<String, Object> item : attendanceRepo.statsByDeptMonth(me.getOrg_id(), targetMonth)) {
                Object uid = item.get("user_id");
                if (uid instanceof Number && ((Number) uid).longValue() == userId) { row = item; break; }
            }
        }
        int expected = expectedWorkDays(targetMonth);
        int totalDays = intOf(row, "total_days");
        int lateDays = intOf(row, "late_days");
        int earlyDays = intOf(row, "early_days");
        int absentDays = intOf(row, "absent_days");
        int normalDays = intOf(row, "normal_days");
        // 出差请假外出另算一类，保证各类明细加起来等于总天数
        int otherDays = Math.max(0, totalDays - normalDays - lateDays - earlyDays - absentDays);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("realName", me != null ? me.getReal_name() : null);
        result.put("month", targetMonth);
        result.put("expectedDays", expected);
        result.put("totalDays", totalDays);
        result.put("normalDays", normalDays);
        result.put("otherDays", otherDays);
        result.put("lateDays", lateDays);
        result.put("earlyDays", earlyDays);
        result.put("absentDays", absentDays);
        // 异常天数按迟到加早退加缺勤算，是次数口径
        result.put("abnormalDays", lateDays + earlyDays + absentDays);
        result.put("attendanceRate", expected > 0 ? Math.round(totalDays * 100.0 / expected) : 0);
        return result;
    }

    // 统计行取整数，null 当 0
    private int intOf(Map<String, Object> row, String key) {
        if (row == null) return 0;
        Object v = row.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    @Override
    public void sendAttendanceReminders() {
        // 开关在系统参数里，attendance_remind_enable=0 时关闭
        if ("0".equals(getParam("attendance_remind_enable", "1"))) return;

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (isHoliday(today)) return;

        LocalTime workStart = getWorkStart();
        LocalTime workEnd = getWorkEnd();

        // 上班前 15 分钟提醒还没签到的人
        LocalTime remindIn = workStart.minusMinutes(15);
        if (nowTime.getHour() == remindIn.getHour() && nowTime.getMinute() == remindIn.getMinute()) {
            List<Map<String, Object>> noCheckin = jdbc.queryForList(
                "SELECT u.id, u.real_name FROM users u WHERE u.status=1 AND u.deleted_at IS NULL " +
                "AND NOT EXISTS (SELECT 1 FROM attendance_records a WHERE a.user_id=u.id AND a.attendance_date=? AND a.deleted_at IS NULL)",
                today);
            for (Map<String, Object> user : noCheckin) {
                notify(Long.valueOf(String.valueOf(user.get("id"))), "上班签到提醒",
                    user.get("real_name") + "，上班前15分钟了，请及时签到",
                    "attendance_remind", null, "/home/attendance");
            }
        }

        // 下班后 30 分钟提醒还没签退的人
        LocalTime remindOut = workEnd.plusMinutes(30);
        if (nowTime.getHour() == remindOut.getHour() && nowTime.getMinute() == remindOut.getMinute()) {
            remindNoCheckout(today, "下班签退提醒", "现在是下班后30分钟，请记得签退");
        }

        // 23 点提醒补签退，并通知主管关注连续 3 天未签退的人
        if (nowTime.getHour() == 23 && nowTime.getMinute() == 0) {
            remindNoCheckout(today, "忘记签退提醒", "您今天尚未签退，请在24:00前补签退（补签退不占用补签次数）");

            List<Map<String, Object>> repeated = jdbc.queryForList(
                "SELECT a.user_id, u.real_name, u.org_id, COUNT(*) cnt FROM attendance_records a " +
                "JOIN users u ON u.id=a.user_id " +
                "WHERE a.attendance_date >= ? AND a.check_in_time IS NOT NULL AND a.check_out_time IS NULL AND a.deleted_at IS NULL " +
                "GROUP BY a.user_id, u.real_name, u.org_id HAVING cnt >= 3", today.minusDays(2));
            for (Map<String, Object> row : repeated) {
                Object deptId = row.get("org_id");
                if (deptId == null) continue;
                List<Map<String, Object>> leaders = jdbc.queryForList(
                    "SELECT DISTINCT u.id, u.real_name FROM users u JOIN user_roles ur ON ur.user_id=u.id " +
                    "WHERE ur.role_id=2 AND ur.deleted_at IS NULL AND u.org_id=? AND u.status=1 AND u.deleted_at IS NULL", deptId);
                for (Map<String, Object> leader : leaders) {
                    notify(Long.valueOf(String.valueOf(leader.get("id"))), "连续未签退提醒",
                        row.get("real_name") + " 已连续3天未签退，请及时关注",
                        "attendance_remind", null, "/home/attendance");
                }
            }
        }
    }

    @Override
    public void settleAttendance() {
        settleAttendance(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleAttendance(String targetDate) {
        LocalDate date = (targetDate == null || targetDate.trim().isEmpty())
            ? LocalDate.now().minusDays(1)
            : LocalDate.parse(targetDate.trim());
        try {
            if (isHoliday(date)) return;
            // 周末只结算调休上班日
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (weekend && !"2".equals(holidayType(date))) return;

            List<Map<String, Object>> allUsers = jdbc.queryForList("SELECT id, org_id FROM users WHERE status=1 AND deleted_at IS NULL");

            // 已通过的请假、出差流程覆盖当天，记 6 请假、5 出差，不记缺勤
            Map<Long, Integer> exempt = new HashMap<>();
            List<Map<String, Object>> trips = jdbc.queryForList(
                "SELECT fi.creator_id, fd.code, fi.business_data FROM flow_instances fi " +
                "JOIN flow_definitions fd ON fd.id=fi.definition_id " +
                "WHERE fi.status=1 AND fi.deleted_at IS NULL AND (fd.code LIKE 'leave%' OR fd.code='business_trip')");
            for (Map<String, Object> trip : trips) {
                String code = String.valueOf(trip.get("code"));
                String data = trip.get("business_data") == null ? "" : String.valueOf(trip.get("business_data"));
                LocalDate start = null;
                LocalDate end = null;
                try {
                    JSONObject obj = JSON.parseObject(data);
                    for (String key : obj.keySet()) {
                        String value = obj.getString(key);
                        if (value == null || value.isEmpty()) continue;
                        String lowerKey = key.toLowerCase();
                        if (lowerKey.contains("start") && start == null) start = tryParseDate(value);
                        else if (lowerKey.contains("end") && end == null) end = tryParseDate(value);
                    }
                } catch (Exception ignored) {
                }
                // 没有日期字段的表单判断不出覆盖范围，跳过
                if (start == null || end == null) continue;
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    exempt.put(((Number) trip.get("creator_id")).longValue(), "leave".equals(code) ? 6 : 5);
                }
            }

            for (Map<String, Object> user : allUsers) {
                Long uid = ((Number) user.get("id")).longValue();
                if (attendanceRepo.findByUserAndDate(uid, date) != null) continue;
                Integer exemptStatus = exempt.get(uid);
                AttendanceRecords rec = new AttendanceRecords();
                rec.setUser_id(uid);
                rec.setDept_id(user.get("org_id") == null ? 0L : ((Number) user.get("org_id")).longValue());
                rec.setAttendance_date(date);
                rec.setCheck_type("normal");
                rec.setIs_makeup(0);
                rec.setCreated_at(LocalDateTime.now());
                rec.setUpdated_at(LocalDateTime.now());
                if (exemptStatus != null) {
                    rec.setStatus(exemptStatus);
                    rec.setRemark(exemptStatus == 6 ? "请假" : "出差");
                } else {
                    rec.setStatus(3);
                    rec.setRemark("自动结算缺勤");
                }
                attendanceRepo.insert(rec);
                // 缺勤要主动提醒本人，否则只有自己去翻出勤记录才发现
                if (rec.getStatus() != null && rec.getStatus() == 3) {
                    notifyAbsence(uid, date);
                }
            }

            // 签到了没签退的补时间并标异常，外出出差请假不处理
            List<AttendanceRecords> noCheckoutList = attendanceRepo.selectList(new QueryWrapper<AttendanceRecords>()
                .eq("attendance_date", date)
                .isNotNull("check_in_time")
                .isNull("check_out_time")
                .notIn("status", 4, 5, 6));
            for (AttendanceRecords rec : noCheckoutList) {
                rec.setCheck_out_time(LocalDateTime.of(date, getWorkEnd(date)));
                rec.setStatus(8);
                rec.setRemark("未签退（自动标记）");
                recalcWorkHours(rec);
                rec.setUpdated_at(LocalDateTime.now());
                attendanceRepo.updateById(rec);
            }
        } catch (Exception e) {
            // 结算失败不抛出，记日志让管理员排查
            try {
                jdbc.update("INSERT INTO operation_logs (user_id, action, detail, created_at) VALUES (1, '出勤自动结算', ?, NOW())",
                    "结算 " + date + " 失败: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }

    // 解析流程表单里的日期，兼容 yyyy-MM-dd 和 yyyy/MM/dd
    private LocalDate tryParseDate(String value) {
        String text = value.trim();
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(text, SLASH_DATE);
        } catch (Exception ignored) {
        }
        return null;
    }

    // 读系统参数，读不到用默认值
    private String getParam(String key, String defaultValue) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT param_value FROM system_params WHERE param_key=? AND deleted_at IS NULL", key);
            if (!rows.isEmpty() && rows.get(0).get("param_value") != null) {
                return String.valueOf(rows.get(0).get("param_value"));
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public Map<String, Object> geoConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("enable", getParam("geo_check_enable", "0"));
        cfg.put("lat", getParam("geo_lat", ""));
        cfg.put("lng", getParam("geo_lng", ""));
        cfg.put("radius", getParam("geo_radius", "500"));
        cfg.put("weekendCount", getParam("attendance_weekend_count", "0"));
        return cfg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGeoConfig(Map<String, Object> cfg) {
        String[][] pairs = {
            {"geo_check_enable", "enable"}, {"geo_lat", "lat"}, {"geo_lng", "lng"},
            {"geo_radius", "radius"}, {"attendance_weekend_count", "weekendCount"}
        };
        for (String[] pair : pairs) {
            Object value = cfg.get(pair[1]);
            if (value == null) continue;
            try {
                Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM system_params WHERE param_key=? AND deleted_at IS NULL", Integer.class, pair[0]);
                if (count != null && count > 0) {
                    jdbc.update("UPDATE system_params SET param_value=? WHERE param_key=? AND deleted_at IS NULL", String.valueOf(value), pair[0]);
                } else {
                    jdbc.update("INSERT INTO system_params (param_key,param_value,param_name,param_group,param_type,sort_order,status) VALUES (?,?,?,?,1,0,1)",
                        pair[0], String.valueOf(value), pair[0], "出勤管理");
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public List<Map<String, Object>> geoAbnormalList(Long reviewerId) {
        List<Map<String, Object>> list = jdbc.queryForList(
            "SELECT ar.id, ar.user_id, u.real_name, ar.attendance_date, ar.check_in_time, ar.check_out_time, " +
            "ar.latitude, ar.longitude, ar.remark, ar.status " +
            "FROM attendance_records ar LEFT JOIN users u ON u.id=ar.user_id " +
            "WHERE ar.geo_abnormal=1 AND ar.deleted_at IS NULL " +
            "ORDER BY ar.attendance_date DESC, ar.check_in_time DESC");
        if (isAdminUser(reviewerId)) return list;
        // 部门主管只能看本部门（含下级）成员的异地签到
        List<Long> deptUserIds = managedDeptUserIds(reviewerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Object uid = row.get("user_id");
            if (uid != null && deptUserIds.contains(((Number) uid).longValue())) result.add(row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewGeoAbnormal(Long reviewerId, Long recordId, int result, String remark) {
        AttendanceRecords rec = attendanceRepo.selectById(recordId);
        if (rec == null) throw new RuntimeException("记录不存在");
        if (rec.getGeo_abnormal() == null || rec.getGeo_abnormal() != 1) {
            throw new RuntimeException("该记录不是待审核的异地签到");
        }
        // 管理员能处理全部，部门主管只能处理本部门成员
        if (!isAdminUser(reviewerId)) {
            if (!isDeptManagerUser(reviewerId) || !managedDeptUserIds(reviewerId).contains(rec.getUser_id())) {
                throw new RuntimeException("您无权审核该异地签到（只能处理本部门成员）");
            }
        }
        if (result == 1) {
            rec.setGeo_abnormal(2);
            rec.setRemark("异地签到已确认正常");
        } else {
            rec.setGeo_abnormal(3);
            rec.setStatus(3);
            rec.setRemark("异地签到驳回，记缺勤");
        }
        rec.setUpdated_at(LocalDateTime.now());
        attendanceRepo.updateById(rec);
        try {
            notify(rec.getUser_id(), result == 1 ? "异地签到已确认" : "异地签到被驳回",
                result == 1
                    ? "您 " + rec.getAttendance_date() + " 的异地签到已确认正常"
                    : "您 " + rec.getAttendance_date() + " 的异地签到被驳回，记缺勤",
                "attendance_geo", recordId, "/home/attendance");
        } catch (Exception ignored) {
        }
    }

    // 地理校验：0 在范围内，1 异地待审核，没开校验也返回 0
    private int geoMark(BigDecimal lat, BigDecimal lng, boolean force) {
        try {
            if (!"1".equals(getParam("geo_check_enable", "0"))) return 0;
            String configLat = getParam("geo_lat", "");
            String configLng = getParam("geo_lng", "");
            if (configLat.isEmpty() || configLng.isEmpty()) return 0;
            double radius;
            try {
                radius = Double.parseDouble(getParam("geo_radius", "500"));
            } catch (Exception e) {
                radius = 500;
            }
            // 拿不到定位时，强制签到记待审核，否则让前端弹窗
            if (lat == null || lng == null) {
                if (force) return 1;
                throw new RuntimeException("NEED_FORCE:无法获取您的位置信息，请检查定位设置");
            }
            double distance = haversine(lat.doubleValue(), lng.doubleValue(),
                Double.parseDouble(configLat), Double.parseDouble(configLng));
            if (distance > radius) {
                if (force) return 1;
                throw new RuntimeException("NEED_FORCE:您当前不在签到范围内（距公司约" + Math.round(distance) + "米），是否强制签到？");
            }
            return 0;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return 0;
        }
    }

    // 两点球面距离，单位米
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.asin(Math.sqrt(a));
    }
}
