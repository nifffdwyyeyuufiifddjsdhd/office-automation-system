package com.officeautomationsystemapi.service;

import java.util.List;
import java.util.Set;

public interface PermissionService {
    // 根据用户ID查询其所有权限标识
    Set<String> getUserPermissions(Long userId);

    // 根据用户Id查询其角色列表
    List<String> getUserRoleCodes(Long userId);

    // 判断用户是否为Admin
    boolean isAdmin(Long userId);
}
