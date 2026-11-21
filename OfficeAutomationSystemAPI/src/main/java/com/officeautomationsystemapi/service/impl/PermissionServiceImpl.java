package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.RolePermissions;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.entity.UserRoles;
import com.officeautomationsystemapi.repository.RolePermissionsRepository;
import com.officeautomationsystemapi.repository.RolesRepository;
import com.officeautomationsystemapi.repository.UserRolesRepository;
import com.officeautomationsystemapi.service.PermissionService;
import com.officeautomationsystemapi.service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 权限查询：把用户、角色和菜单权限串起来
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private RolePermissionsRepository rolePermissionsRepository;

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Autowired
    private UserPermissionService userPermissionService;

    // 用户拥有的权限标识，来自启用角色关联的菜单
    @Override
    public Set<String> getUserPermissions(Long userId) {
        Set<String> permissions = new HashSet<>();
        // 角色之外的单独授权，如派了任务的员工才有任务中心权
        permissions.addAll(userPermissionService.codesOf(userId));
        List<Long> roleIds = enabledRoleIds(userId);
        if (roleIds.isEmpty()) {
            return permissions;
        }

        QueryWrapper<RolePermissions> wrapper = new QueryWrapper<>();
        wrapper.in("role_id", roleIds);
        List<RolePermissions> rolePermissions = rolePermissionsRepository.selectList(wrapper);
        if (rolePermissions.isEmpty()) {
            return permissions;
        }

        List<Long> menuIds = rolePermissions.stream()
                .map(RolePermissions::getMenu_id)
                .distinct()
                .collect(Collectors.toList());
        permissions.addAll(rolesRepository.findMenuCodesByMenuIds(menuIds));
        return permissions;
    }

    // 用户拥有的角色编码
    @Override
    public List<String> getUserRoleCodes(Long userId) {
        List<Long> roleIds = enabledRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<Roles> wrapper = new QueryWrapper<>();
        wrapper.in("id", roleIds);
        return rolesRepository.selectList(wrapper).stream()
                .map(Roles::getRole_code)
                .collect(Collectors.toList());
    }

    // 管理员判断固定看 admin 角色
    public boolean isAdmin(Long userId) {
        return getUserRoleCodes(userId).contains("admin");
    }

    // 用户启用中的角色 ID，为空返回空列表，避免拼出 IN ()
    private List<Long> enabledRoleIds(Long userId) {
        QueryWrapper<UserRoles> userRoleWrapper = new QueryWrapper<>();
        userRoleWrapper.eq("user_id", userId);
        List<UserRoles> userRoles = userRolesRepository.selectList(userRoleWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRoles::getRole_id)
                .distinct()
                .collect(Collectors.toList());
        QueryWrapper<Roles> roleWrapper = new QueryWrapper<>();
        roleWrapper.in("id", roleIds).eq("status", 1);
        return rolesRepository.selectList(roleWrapper).stream()
                .map(Roles::getId)
                .collect(Collectors.toList());
    }
}
