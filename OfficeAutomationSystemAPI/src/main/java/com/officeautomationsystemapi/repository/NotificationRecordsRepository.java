package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.NotificationRecords;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface NotificationRecordsRepository extends BaseMapper<NotificationRecords> {

    // 物理删除已读的历史通知：实体带 @TableLogic，用 delete 只会写 deleted_at，行永远留在表里
    @Delete("DELETE FROM notification_records WHERE is_read = 1 AND created_at < #{before}")
    int physicalDeleteReadBefore(@Param("before") LocalDateTime before);
}
