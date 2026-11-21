package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.ExceptionLogs;
import org.apache.ibatis.annotations.Mapper;

// 异常日志仓库
@Mapper
public interface ExceptionLogsRepository extends BaseMapper<ExceptionLogs> {
}
