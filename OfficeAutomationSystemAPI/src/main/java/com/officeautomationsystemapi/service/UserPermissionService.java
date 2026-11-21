package com.officeautomationsystemapi.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

// 用户级额外权限：角色以外单独授予，和角色权限一起生效
public interface UserPermissionService {

    // 用户在角色之外额外拥有的权限标识
    Set<String> codesOf(Long userId);

    // 用户是否额外拥有某个权限标识
    boolean has(Long userId, String menuCode);

    // 授予权限，已有返回 false；source 1 管理员授 2 派任务开通
    boolean grantIfAbsent(Long userId, String menuCode, Long grantedBy, int source, String reason);

    // 回收权限，返回是否真的删掉了
    boolean revoke(Long userId, Long menuId);

    // 用户的额外权限明细，含权限名、来源、授权人与授权时间
    List<Map<String, Object>> listOf(Long userId);
}
