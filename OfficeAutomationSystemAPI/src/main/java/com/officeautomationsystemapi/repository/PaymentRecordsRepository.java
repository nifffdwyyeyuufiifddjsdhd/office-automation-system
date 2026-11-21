package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.PaymentRecords;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordsRepository extends BaseMapper<PaymentRecords> {
}
