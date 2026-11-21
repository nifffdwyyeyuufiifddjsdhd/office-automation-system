package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.AttendanceMakeup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AttendanceMakeupRepository extends BaseMapper<AttendanceMakeup> {

    // 某用户某月占用补签次数（待审批+已通过，驳回不占次数）
    @Select("SELECT COUNT(*) FROM attendance_makeup WHERE user_id=#{userId} AND status IN (0,1) " +
            "AND deleted_at IS NULL AND DATE_FORMAT(attendance_date,'%Y-%m')=#{month}")
    int countApprovedByUserMonth(@Param("userId") Long userId, @Param("month") String month);

    // 查待审批列表（联表用户名）
    @Select({"<script>",
            "SELECT m.*, u.username, u.real_name FROM attendance_makeup m",
            "LEFT JOIN users u ON m.user_id = u.id",
            "WHERE m.deleted_at IS NULL",
            "<if test='status != null'> AND m.status = #{status}</if>",
            "<if test='deptId != null'> AND u.org_id = #{deptId}</if>",
            "ORDER BY m.created_at DESC",
            "</script>"})
    List<AttendanceMakeup> listPending(@Param("status") Integer status, @Param("deptId") Long deptId);

    // 同一天已发起的补签次数，含待审和已结束的
    @Select("SELECT COUNT(*) FROM attendance_makeup WHERE user_id=#{userId} AND attendance_date=#{date} AND deleted_at IS NULL")
    int countByUserAndDate(@Param("userId") Long userId, @Param("date") java.time.LocalDate date);

    // 某用户的补签记录
    @Select("SELECT m.*, u.username, u.real_name FROM attendance_makeup m " +
            "LEFT JOIN users u ON m.user_id = u.id " +
            "WHERE m.deleted_at IS NULL AND m.user_id=#{userId} " +
            "ORDER BY m.created_at DESC")
    List<AttendanceMakeup> listByUser(@Param("userId") Long userId);
}
