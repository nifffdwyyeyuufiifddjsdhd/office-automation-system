package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.AttendanceRecords;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceRecordsRepository extends BaseMapper<AttendanceRecords> {

    // 查某用户某天的出勤记录
    @Select("SELECT * FROM attendance_records WHERE user_id=#{userId} AND attendance_date=#{date} AND deleted_at IS NULL")
    AttendanceRecords findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // 查某用户某月的出勤记录列表
    @Select("SELECT * FROM attendance_records WHERE user_id=#{userId} AND deleted_at IS NULL " +
            "AND DATE_FORMAT(attendance_date,'%Y-%m')=#{month} ORDER BY attendance_date DESC")
    List<AttendanceRecords> listByUserMonth(@Param("userId") Long userId, @Param("month") String month);

    // 部门月度考勤：在职人员左连考勤，没打卡的人也列出
    @Select("SELECT u.id AS user_id, u.org_id, u.real_name, o.org_name, " +
            "COUNT(ar.id) AS total_days, " +
            "SUM(CASE WHEN ar.status=0 THEN 1 ELSE 0 END) AS normal_days, " +
            "SUM(CASE WHEN ar.status=1 OR ar.status=7 THEN 1 ELSE 0 END) AS late_days, " +
            "SUM(CASE WHEN ar.status=2 OR ar.status=9 THEN 1 ELSE 0 END) AS early_days, " +
            "SUM(CASE WHEN ar.status=3 THEN 1 ELSE 0 END) AS absent_days " +
            "FROM users u LEFT JOIN organizations o ON o.id=u.org_id " +
            "LEFT JOIN attendance_records ar ON ar.user_id=u.id AND ar.deleted_at IS NULL " +
            "AND DATE_FORMAT(ar.attendance_date,'%Y-%m')=#{month} " +
            "WHERE u.deleted_at IS NULL AND u.status=1 AND u.org_id=#{deptId} " +
            "GROUP BY u.id, u.org_id, u.real_name, o.org_name ORDER BY u.id")
    List<Map<String, Object>> statsByDeptMonth(@Param("deptId") Long deptId, @Param("month") String month);

    // 全公司某月统计（不过滤部门），同样以在职人员为基准
    @Select("SELECT u.id AS user_id, u.org_id, u.real_name, o.org_name, " +
            "COUNT(ar.id) AS total_days, " +
            "SUM(CASE WHEN ar.status=0 THEN 1 ELSE 0 END) AS normal_days, " +
            "SUM(CASE WHEN ar.status=1 OR ar.status=7 THEN 1 ELSE 0 END) AS late_days, " +
            "SUM(CASE WHEN ar.status=2 OR ar.status=9 THEN 1 ELSE 0 END) AS early_days, " +
            "SUM(CASE WHEN ar.status=3 THEN 1 ELSE 0 END) AS absent_days " +
            "FROM users u LEFT JOIN organizations o ON o.id=u.org_id " +
            "LEFT JOIN attendance_records ar ON ar.user_id=u.id AND ar.deleted_at IS NULL " +
            "AND DATE_FORMAT(ar.attendance_date,'%Y-%m')=#{month} " +
            "WHERE u.deleted_at IS NULL AND u.status=1 " +
            "GROUP BY u.id, u.org_id, u.real_name, o.org_name ORDER BY u.id")
    List<Map<String, Object>> statsAllMonth(@Param("month") String month);
}
