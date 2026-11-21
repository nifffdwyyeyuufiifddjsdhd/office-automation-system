package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Menus;
import com.officeautomationsystemapi.entity.Roles;
import com.officeautomationsystemapi.repository.MenusRepository;
import com.officeautomationsystemapi.repository.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 权限管理：角色维护、权限勾选、成员分配
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private MenusRepository menusRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // 角色列表，带用户数和已授权限数
    @GetMapping
    @RequirePermission("role:manage")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.id, r.role_code, r.role_name, r.description, r.data_scope, r.role_type, ");
        sql.append("r.sort_order, r.status, r.is_system, r.created_at, ");
        sql.append("(SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id = r.id AND ur.deleted_at IS NULL) AS user_count, ");
        sql.append("(SELECT COUNT(*) FROM role_permissions rp WHERE rp.role_id = r.id AND rp.deleted_at IS NULL) AS perm_count ");
        sql.append("FROM roles r WHERE r.deleted_at IS NULL");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (r.role_name LIKE ? OR r.role_code LIKE ? OR r.description LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY r.sort_order, r.id");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            row.put("dataScopeText", dataScopeText(asInt(row.get("data_scope"), 4)));
        }
        return Result.success(rows);
    }

    // 下拉用的精简列表，管理员看全部，人事只看角色名
    @GetMapping("/options")
    @RequirePermission({"role:manage", "user:list", "user:onboard"})
    public Result<List<Map<String, Object>>> options() {
        return Result.success(jdbc.queryForList(
                "SELECT id, role_code, role_name FROM roles WHERE deleted_at IS NULL AND status = 1 ORDER BY sort_order, id"));
    }

    // 角色详情，带已勾选的菜单权限 ID
    @GetMapping("/{id}")
    @RequirePermission("role:manage")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Roles role = rolesRepository.selectById(id);
        if (role == null) {
            return Result.error(404, "角色不存在或已删除");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", role.getId());
        // 和列表接口一样用下划线字段，前端复用同一份类型
        data.put("role_code", role.getRole_code());
        data.put("role_name", role.getRole_name());
        data.put("description", role.getDescription());
        data.put("data_scope", role.getData_scope());
        data.put("dataScopeText", dataScopeText(asInt(role.getData_scope(), 4)));
        data.put("role_type", role.getRole_type());
        data.put("sort_order", role.getSort_order());
        data.put("status", role.getStatus());
        data.put("is_system", role.getIs_system());
        List<Long> menuIds = jdbc.queryForList(
                "SELECT menu_id FROM role_permissions WHERE role_id = ? AND deleted_at IS NULL", Long.class, id);
        data.put("menuIds", menuIds);
        return Result.success(data);
    }

    // 新建角色，编码唯一，内置标记固定为否
    @PostMapping
    @RequirePermission("role:manage")
    public Result<Map<String, Object>> create(@RequestBody Roles body) {
        String code = trim(body.getRole_code());
        String name = trim(body.getRole_name());
        if (code.isEmpty()) {
            return Result.error("角色编码不能为空");
        }
        if (name.isEmpty()) {
            return Result.error("角色名称不能为空");
        }
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE role_code = ? AND deleted_at IS NULL", Long.class, code);
        if (exists != null && exists > 0) {
            return Result.error("角色编码 " + code + " 已存在，请换一个");
        }
        Roles role = new Roles();
        role.setRole_code(code);
        role.setRole_name(name);
        role.setDescription(body.getDescription());
        role.setData_scope(body.getData_scope() == null ? 4 : body.getData_scope());
        role.setRole_type(body.getRole_type() == null ? 2 : body.getRole_type());
        role.setSort_order(body.getSort_order() == null ? nextSortOrder() : body.getSort_order());
        role.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        role.setIs_system(0);
        role.setCreated_at(LocalDateTime.now());
        role.setUpdated_at(LocalDateTime.now());
        rolesRepository.insert(role);
        Map<String, Object> data = new HashMap<>();
        data.put("id", role.getId());
        return Result.success(data);
    }

    // 编辑角色，编码不能改，代码里按编码做判断
    @PutMapping("/{id}")
    @RequirePermission("role:manage")
    public Result<?> update(@PathVariable Long id, @RequestBody Roles body) {
        Roles role = rolesRepository.selectById(id);
        if (role == null) {
            return Result.error(404, "角色不存在或已删除");
        }
        if (body.getRole_name() != null) {
            String name = trim(body.getRole_name());
            if (name.isEmpty()) {
                return Result.error("角色名称不能为空");
            }
            role.setRole_name(name);
        }
        if (body.getDescription() != null) {
            role.setDescription(body.getDescription());
        }
        if (body.getData_scope() != null) {
            role.setData_scope(body.getData_scope());
        }
        if (body.getSort_order() != null) {
            role.setSort_order(body.getSort_order());
        }
        if (body.getStatus() != null) {
            if (Integer.valueOf(0).equals(body.getStatus()) && Integer.valueOf(1).equals(role.getIs_system())) {
                return Result.error("系统内置角色不能停用");
            }
            role.setStatus(body.getStatus());
        }
        role.setUpdated_at(LocalDateTime.now());
        rolesRepository.updateById(role);
        return Result.success(null);
    }

    // 内置角色和有用户的角色不能删
    @DeleteMapping("/{id}")
    @RequirePermission("role:manage")
    public Result<?> delete(@PathVariable Long id) {
        Roles role = rolesRepository.selectById(id);
        if (role == null) {
            return Result.error(404, "角色不存在或已删除");
        }
        if (Integer.valueOf(1).equals(role.getIs_system())) {
            return Result.error("系统内置角色不能删除");
        }
        Long users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE role_id = ? AND deleted_at IS NULL", Long.class, id);
        if (users != null && users > 0) {
            return Result.error("该角色还有 " + users + " 个用户在用，请先在用户管理里调整他们的角色");
        }
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", id);
        rolesRepository.deleteById(id);
        return Result.success(null);
    }

    // 保存角色权限，全量覆盖，菜单记 1、按钮码记 2
    @PutMapping("/{id}/permissions")
    @RequirePermission("role:manage")
    public Result<Map<String, Object>> savePermissions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Roles role = rolesRepository.selectById(id);
        if (role == null) {
            return Result.error(404, "角色不存在或已删除");
        }
        List<Long> menuIds = toLongList(body.get("menuIds"));
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", id);
        int count = 0;
        for (Long menuId : menuIds) {
            Menus menu = menusRepository.selectById(menuId);
            if (menu == null) {
                continue;
            }
            int permissionType = menu.getMenu_type() != null && menu.getMenu_type() == 3 ? 2 : 1;
            jdbc.update("INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                    id, menuId, permissionType);
            count++;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success("已保存 " + count + " 项权限", data);
    }

    // 权限树：目录、菜单、按钮码三级，前端用 el-tree 渲染
    @GetMapping("/menus/tree")
    @RequirePermission("role:manage")
    public Result<List<Map<String, Object>>> menuTree() {
        List<Menus> menus = menusRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Menus>()
                        .eq("status", 1)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"));
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Menus m : menus) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParent_id() == null ? 0L : m.getParent_id());
            node.put("label", m.getMenu_name());
            node.put("menuCode", m.getMenu_code());
            node.put("menuType", m.getMenu_type());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(m.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodes.values()) {
            Long parentId = (Long) node.get("parentId");
            Map<String, Object> parent = parentId == null ? null : nodes.get(parentId);
            if (parent == null || parentId == 0L) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            }
        }
        return Result.success(roots);
    }

    // 角色下的成员列表
    @GetMapping("/{id}/users")
    @RequirePermission("role:manage")
    public Result<List<Map<String, Object>>> roleUsers(@PathVariable Long id) {
        return Result.success(jdbc.queryForList(
                "SELECT u.id, u.username, u.real_name, o.org_name, u.status FROM users u " +
                        "JOIN user_roles ur ON ur.user_id = u.id " +
                        "LEFT JOIN organizations o ON o.id = u.org_id " +
                        "WHERE ur.role_id = ? AND ur.deleted_at IS NULL AND u.deleted_at IS NULL ORDER BY u.id", id));
    }

    // 查用户当前角色，用户管理里分配角色时用
    @GetMapping("/user/{userId}")
    @RequirePermission("role:assign")
    public Result<Map<String, Object>> userRoles(@PathVariable Long userId) {
        List<Long> roleIds = jdbc.queryForList(
                "SELECT role_id FROM user_roles WHERE user_id = ? AND deleted_at IS NULL", Long.class, userId);
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT r.id, r.role_code, r.role_name FROM roles r " +
                        "JOIN user_roles ur ON ur.role_id = r.id " +
                        "WHERE ur.user_id = ? AND ur.deleted_at IS NULL AND r.deleted_at IS NULL ORDER BY r.id", userId);
        Map<String, Object> data = new HashMap<>();
        data.put("roleIds", roleIds);
        data.put("roles", roles);
        return Result.success(data);
    }

    // 覆盖保存用户角色，最后一个启用的管理员不能摘掉管理员角色
    @PutMapping("/user/{userId}")
    @RequirePermission("role:assign")
    public Result<Map<String, Object>> saveUserRoles(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", Long.class, userId);
        if (exists == null || exists == 0) {
            return Result.error(404, "用户不存在或已删除");
        }
        List<Long> roleIds = toLongList(body.get("roleIds"));
        if (roleIds.isEmpty()) {
            return Result.error("至少要给用户保留一个角色");
        }
        List<Long> current = jdbc.queryForList(
                "SELECT role_id FROM user_roles WHERE user_id = ? AND deleted_at IS NULL", Long.class, userId);
        if (current.contains(1L) && !roleIds.contains(1L)) {
            Long admins = jdbc.queryForObject(
                    "SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN users u ON u.id = ur.user_id " +
                            "WHERE ur.role_id = 1 AND ur.deleted_at IS NULL AND u.deleted_at IS NULL AND u.status = 1", Long.class);
            if (admins != null && admins <= 1) {
                return Result.error("至少要保留一个启用状态的管理员，不能摘掉最后一个管理员的管理员角色");
            }
        }
        for (Long roleId : roleIds) {
            Long valid = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE id = ? AND deleted_at IS NULL AND status = 1", Long.class, roleId);
            if (valid == null || valid == 0) {
                return Result.error("角色 " + roleId + " 不存在或已停用");
            }
        }
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            jdbc.update("INSERT INTO user_roles (user_id, role_id, grant_type, created_at, updated_at) VALUES (?, ?, 1, NOW(), NOW())",
                    userId, roleId);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", roleIds.size());
        return Result.success("已更新，用户下次刷新页面或重新登录后生效", data);
    }

    // 新建角色排在现有角色之后
    private int nextSortOrder() {
        Long max = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order), 0) FROM roles WHERE deleted_at IS NULL", Long.class);
        return max == null ? 10 : max.intValue() + 1;
    }

    // 数据范围文案
    private String dataScopeText(int scope) {
        return switch (scope) {
            case 1 -> "全部数据";
            case 2 -> "本部门";
            case 3 -> "本部门及下级";
            case 4 -> "仅本人";
            case 5 -> "项目数据";
            default -> "仅本人";
        };
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    // roleIds 可能是数组、逗号串或单个数字，统一转成列表
    private List<Long> toLongList(Object value) {
        List<Long> ids = new ArrayList<>();
        if (value == null) {
            return ids;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
            return ids;
        }
        for (String part : value.toString().split(",")) {
            Long id = toLong(part);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
