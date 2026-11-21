package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Positions;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PositionsRepository extends BaseMapper<Positions> {
}
