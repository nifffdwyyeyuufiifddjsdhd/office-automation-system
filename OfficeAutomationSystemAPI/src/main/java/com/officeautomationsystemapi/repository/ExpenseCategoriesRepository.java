package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.ExpenseCategories;
import org.apache.ibatis.annotations.Mapper;

// 费用类别仓储
@Mapper
public interface ExpenseCategoriesRepository extends BaseMapper<ExpenseCategories> {
}
