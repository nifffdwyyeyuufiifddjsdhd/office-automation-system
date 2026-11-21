package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.SystemParams;
import org.apache.ibatis.annotations.Mapper;

// 系统参数仓库
@Mapper
public interface SystemParamsRepository extends BaseMapper<SystemParams> {
}
