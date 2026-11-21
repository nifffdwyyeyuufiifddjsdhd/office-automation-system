package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.UserGroupMembers;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserGroupMembersRepository extends BaseMapper<UserGroupMembers> {
}
