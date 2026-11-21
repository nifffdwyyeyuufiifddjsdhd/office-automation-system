package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.UserNotificationPreferences;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationPreferencesRepository extends BaseMapper<UserNotificationPreferences> {
}
