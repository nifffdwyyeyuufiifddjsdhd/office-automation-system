package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.OnlineUsers;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OnlineUsersRepository extends BaseMapper<OnlineUsers> {

    @Select("SELECT * FROM online_users WHERE user_id = #{userId} AND deleted_at IS NULL LIMIT 1")
    OnlineUsers findByUserId(@Param("userId") Long userId);

    // 物理删除该用户的在线记录，含已标记删除的
    @Delete("DELETE FROM online_users WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM online_users WHERE last_active_time < #{threshold}")
    int deleteInactive(@Param("threshold") LocalDateTime threshold);

    @Select("SELECT * FROM online_users WHERE last_active_time >= #{threshold} AND deleted_at IS NULL ORDER BY last_active_time DESC")
    List<OnlineUsers> findOnlineSince(@Param("threshold") LocalDateTime threshold);

    @Select("SELECT COUNT(*) FROM online_users WHERE last_active_time >= #{threshold} AND deleted_at IS NULL")
    long countOnlineSince(@Param("threshold") LocalDateTime threshold);
}
