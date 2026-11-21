package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.TaskMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMemberRepository extends BaseMapper<TaskMember> {
}
