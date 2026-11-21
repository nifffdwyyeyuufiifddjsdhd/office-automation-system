package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.UserPermissions;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.UserPermissionsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 用户级额外权限：授予、回收、查询都在这里，菜单按权限码查
@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private UserPermissionsRepository userPermissionsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public Set<String> codesOf(Long userId) {
        Set<String> codes = new HashSet<>();
        if (userId == null) return codes;
        for (Map<String, Object> row : rows(userId)) {
            Object code = row.get("menu_code");
            if (code != null) codes.add(code.toString());
        }
        return codes;
    }

    @Override
    public boolean has(Long userId, String menuCode) {
        if (userId == null || menuCode == null) return false;
        return codesOf(userId).contains(menuCode);
    }

    @Override
    public boolean grantIfAbsent(Long userId, String menuCode, Long grantedBy, int source, String reason) {
        if (userId == null || menuCode == null) return false;
        Map<String, Object> menu = menuOf(menuCode);
        if (menu == null) return false;
        Long menuId = Long.valueOf(menu.get("id").toString());
        // 已有时不再重复插入，避免同一用户同一权限出现多条
        UserPermissions exist = userPermissionsRepository.selectOne(new QueryWrapper<UserPermissions>()
            .eq("user_id", userId).eq("menu_id", menuId));
        if (exist != null) return false;
        UserPermissions up = new UserPermissions();
        up.setUser_id(userId);
        up.setMenu_id(menuId);
        up.setGranted_by(grantedBy);
        up.setSource(source);
        up.setReason(reason);
        userPermissionsRepository.insert(up);
        return true;
    }

    @Override
    public boolean revoke(Long userId, Long menuId) {
        if (userId == null || menuId == null) return false;
        UserPermissions exist = userPermissionsRepository.selectOne(new QueryWrapper<UserPermissions>()
            .eq("user_id", userId).eq("menu_id", menuId));
        if (exist == null) return false;
        userPermissionsRepository.deleteById(exist.getId());
        return true;
    }

    @Override
    public List<Map<String, Object>> listOf(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows(userId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("menuId", row.get("menu_id"));
            item.put("menuName", row.get("menu_name"));
            item.put("menuCode", row.get("menu_code"));
            item.put("source", row.get("source"));
            item.put("sourceText", toInt(row.get("source")) == 2 ? "派发任务自动开通" : "管理员授予");
            item.put("reason", row.get("reason"));
            Object granter = row.get("granted_by");
            item.put("grantedBy", granter);
            item.put("grantedByName", nameOf(granter));
            item.put("createdAt", row.get("created_at"));
            out.add(item);
        }
        return out;
    }

    // 用户的额外权限明细，连表拿权限名与权限码
    private List<Map<String, Object>> rows(Long userId) {
        String sql = "SELECT up.menu_id, up.source, up.reason, up.granted_by, up.created_at, m.menu_name, m.menu_code "
            + "FROM user_permissions up JOIN menus m ON m.id = up.menu_id "
            + "WHERE up.user_id = ? AND up.deleted_at IS NULL AND m.deleted_at IS NULL AND m.status = 1 "
            + "ORDER BY up.id";
        return jdbc.queryForList(sql, userId);
    }

    // 按权限码找菜单，找不到说明这个权限码还没建
    private Map<String, Object> menuOf(String menuCode) {
        List<Map<String, Object>> list = jdbc.queryForList(
            "SELECT id, menu_name FROM menus WHERE menu_code = ? AND deleted_at IS NULL AND status = 1 LIMIT 1", menuCode);
        return list.isEmpty() ? null : list.get(0);
    }

    private String nameOf(Object userId) {
        if (userId == null) return null;
        Users u = usersRepository.selectById(Long.valueOf(userId.toString()));
        if (u == null) return null;
        return u.getReal_name() != null ? u.getReal_name() : u.getUsername();
    }

    private int toInt(Object v) {
        if (v == null) return 1;
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 1;
        }
    }
}
