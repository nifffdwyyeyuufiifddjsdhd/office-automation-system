package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMemberRepository extends BaseMapper<ProjectMember> {
}
