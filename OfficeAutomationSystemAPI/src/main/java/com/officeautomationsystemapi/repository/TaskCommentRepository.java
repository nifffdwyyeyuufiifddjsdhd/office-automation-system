package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskCommentRepository extends BaseMapper<TaskComment> {
}
