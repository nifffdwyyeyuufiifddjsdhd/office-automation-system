package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Roles;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RolesRepository extends BaseMapper<Roles> {

    // 按菜单ID批量取权限标识
    @Select("<script>"
            + "SELECT DISTINCT menu_code FROM menus "
            + "WHERE deleted_at IS NULL AND status = 1 "
            + "AND menu_code IS NOT NULL AND menu_code != '' "
            + "AND id IN "
            + "<foreach collection='menuIds' item='mid' open='(' separator=',' close=')'>#{mid}</foreach>"
            + "</script>")
    List<String> findMenuCodesByMenuIds(@Param("menuIds") List<Long> menuIds);
}
