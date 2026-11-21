package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.TaskOperationLogs;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskOperationLogsRepository extends BaseMapper<TaskOperationLogs> {
}
