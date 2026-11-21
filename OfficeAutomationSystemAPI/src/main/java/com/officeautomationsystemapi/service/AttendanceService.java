package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.entity.AttendanceMakeup;
import com.officeautomationsystemapi.entity.AttendanceRecords;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    // 当天状态，是否已签到或签退
    Map<String, Object> getTodayStatus(Long userId);

    // 上班签到，后两个重载依次加定位和强制签到
    AttendanceRecords checkIn(Long userId, String ip);
    AttendanceRecords checkIn(Long userId, String ip, BigDecimal lat, BigDecimal lng, String checkType);
    AttendanceRecords checkIn(Long userId, String ip, BigDecimal lat, BigDecimal lng, String checkType, Boolean force);

    // 下班签退，重载规则同上
    AttendanceRecords checkOut(Long userId, String ip);
    AttendanceRecords checkOut(Long userId, String ip, BigDecimal lat, BigDecimal lng);
    AttendanceRecords checkOut(Long userId, String ip, BigDecimal lat, BigDecimal lng, Boolean force);

    // 我的出勤记录，按月
    List<AttendanceRecords> myRecords(Long userId, String month);

    // 部门出勤统计，主管或人事
    List<Map<String, Object>> deptStats(Long userId, Long deptId, String month);

    // 补签申请
    AttendanceMakeup applyMakeup(Long userId, AttendanceMakeup apply);       // 发起申请
    List<AttendanceMakeup> myMakeups(Long userId);                           // 我的补签记录
    List<AttendanceMakeup> pendingMakeups(Long userId, Integer status, Long deptId); // 待审批或已审批列表
    AttendanceMakeup approveMakeup(Long approverId, Long makeupId, Integer result, String remark); // 审批
    void withdrawMakeup(Long userId, Long makeupId);
    void deleteMakeup(Long makeupId);

    // 管理员查看
    Map<String, Object> allRecords(Long userId, String startDate, String endDate, Long deptId, Integer status, int pageNum, int pageSize);
    List<Map<String, Object>> statistics(Long userId, Long deptId, String month);   // 按月按部门统计

    // 我的月度出勤：应出勤、实出勤、出勤率和异常天数
    Map<String, Object> myStatistics(Long userId, String month);

    // 定时任务调用
    void sendAttendanceReminders();                     // 出勤提醒，每分钟一次
    void settleAttendance();                            // 自动结算，每天两点补缺勤等状态
    void settleAttendance(String targetDate);           // 管理员指定日期补跑结算

    // 地理位置校验
    Map<String, Object> geoConfig();
    void saveGeoConfig(Map<String, Object> cfg);
    List<Map<String, Object>> geoAbnormalList(Long reviewerId);
    void reviewGeoAbnormal(Long reviewerId, Long recordId, int result, String remark);
}
