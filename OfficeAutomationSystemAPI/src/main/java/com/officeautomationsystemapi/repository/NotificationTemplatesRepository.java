package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.NotificationTemplates;
import org.apache.ibatis.annotations.Mapper;

// 通知模板仓库
@Mapper
public interface NotificationTemplatesRepository extends BaseMapper<NotificationTemplates> {
}
