package com.officeautomationsystemapi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.officeautomationsystemapi.entity.Users;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UsersRepository extends BaseMapper<Users> {
    // 按 id 查一条
    @Select("select * from users where id=#{id}")
    Users findUserById(@Param("id") Integer id);

    // 按用户名查账号，软删除的不返回，limit 1 防重名
    @Select("select * from users where username=#{username} and deleted_at is null limit 1")
    Users findUserByUserName(@Param("username") String username);

    // 查全部用户
    @Select("select * from users")
    List<Users> findUserAll();

}
