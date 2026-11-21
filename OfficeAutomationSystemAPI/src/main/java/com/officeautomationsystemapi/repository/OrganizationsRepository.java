package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Organizations;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationsRepository extends BaseMapper<Organizations> {
}
