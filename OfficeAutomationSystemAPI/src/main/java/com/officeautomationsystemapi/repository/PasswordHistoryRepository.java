package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.PasswordHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface PasswordHistoryRepository extends BaseMapper<PasswordHistory> {

    // 最近 3 条历史密码
    @Select("select * from password_history where user_id=#{userId} order by created_at desc limit 3")
    List<PasswordHistory> findRecent3(@Param("userId") Long userId);
}
