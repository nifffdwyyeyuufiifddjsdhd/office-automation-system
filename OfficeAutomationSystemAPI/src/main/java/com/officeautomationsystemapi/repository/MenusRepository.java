package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Menus;
import org.apache.ibatis.annotations.Mapper;

// 菜单与权限码仓库
@Mapper
public interface MenusRepository extends BaseMapper<Menus> {
}
