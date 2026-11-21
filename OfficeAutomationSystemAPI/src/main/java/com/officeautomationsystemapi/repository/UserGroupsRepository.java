package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.UserGroups;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserGroupsRepository extends BaseMapper<UserGroups> {
}
