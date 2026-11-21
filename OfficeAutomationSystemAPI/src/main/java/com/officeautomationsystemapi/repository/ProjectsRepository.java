package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Projects;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectsRepository extends BaseMapper<Projects> {
}
