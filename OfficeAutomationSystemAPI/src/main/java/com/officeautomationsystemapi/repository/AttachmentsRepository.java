package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Attachments;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttachmentsRepository extends BaseMapper<Attachments> {
}
