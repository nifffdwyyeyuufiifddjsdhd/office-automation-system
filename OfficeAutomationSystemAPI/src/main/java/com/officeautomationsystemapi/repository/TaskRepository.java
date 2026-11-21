package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskRepository extends BaseMapper<Task> {
}
