package com.officeautomationsystemapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 数据范围：让 roles.data_scope 真正生效，不再只是数据库里躺着的一个字段
// 1=全公司 2=本部门 3=本部门及下级 4=仅本人
// 一个人挂多个角色时取最宽的范围，也就是数字最小的那个
@Service
public class DataScopeService {

    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT = 2;
    public static final int SCOPE_DEPT_TREE = 3;
    public static final int SCOPE_SELF = 4;

    @Autowired
    private JdbcTemplate jdbc;

    // 取当前用户的生效范围码，取不到就按仅本人
    public int scopeOf(Long userId) {
        if (userId == null) {
            return SCOPE_SELF;
        }
        List<Integer> scopes = jdbc.queryForList(
                "SELECT r.data_scope FROM user_roles ur JOIN roles r ON r.id = ur.role_id " +
                        "WHERE ur.user_id = ? AND r.status = 1 AND ur.deleted_at IS NULL",
                Integer.class, userId);
        int result = SCOPE_SELF;
        for (Integer scope : scopes) {
            if (scope != null && scope < result) {
                result = scope;
            }
        }
        return result;
    }

    public boolean canSeeAll(Long userId) {
        return scopeOf(userId) == SCOPE_ALL;
    }

    // 可见部门：null 表示不限部门；空集合表示一个部门都看不到（仅本人）
    public List<Long> visibleOrgIds(Long userId) {
        int scope = scopeOf(userId);
        if (scope == SCOPE_ALL) {
            return null;
        }
        Long ownOrgId = ownOrgId(userId);
        if (ownOrgId == null) {
            return new ArrayList<>();
        }
        if (scope == SCOPE_DEPT) {
            return new ArrayList<>(List.of(ownOrgId));
        }
        if (scope == SCOPE_DEPT_TREE) {
            return deptTree(ownOrgId);
        }
        return new ArrayList<>();
    }

    // 可见用户：null 表示不限用户；空集合表示谁都看不到
    public List<Long> visibleUserIds(Long userId) {
        int scope = scopeOf(userId);
        if (scope == SCOPE_ALL) {
            return null;
        }
        if (scope == SCOPE_SELF) {
            List<Long> self = new ArrayList<>();
            if (userId != null) {
                self.add(userId);
            }
            return self;
        }
        List<Long> orgIds = visibleOrgIds(userId);
        if (orgIds == null || orgIds.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(",", Collections.nCopies(orgIds.size(), "?"));
        return jdbc.queryForList(
                "SELECT id FROM users WHERE deleted_at IS NULL AND org_id IN (" + placeholders + ")",
                Long.class, orgIds.toArray());
    }

    // 能否处理该用户：范围不限放行，否则要在可见用户里
    public boolean canOperateUser(Long actorId, Long targetUserId) {
        if (actorId == null || targetUserId == null) {
            return false;
        }
        if (actorId.equals(targetUserId)) {
            return true;
        }
        List<Long> visible = visibleUserIds(actorId);
        return visible == null || visible.contains(targetUserId);
    }

    // 能否处理该部门：范围不限放行，否则要在可见部门里
    public boolean canOperateOrg(Long actorId, Long orgId) {
        if (actorId == null || orgId == null) {
            return false;
        }
        List<Long> visible = visibleOrgIds(actorId);
        return visible == null || visible.contains(orgId);
    }

    private Long ownOrgId(Long userId) {
        List<Long> rows = jdbc.queryForList(
                "SELECT org_id FROM users WHERE id = ? AND deleted_at IS NULL AND org_id IS NOT NULL",
                Long.class, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // 本部门及所有下级，整表读出来在内存里做广度优先
    private List<Long> deptTree(Long rootId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, parent_id FROM organizations WHERE deleted_at IS NULL");
        Map<Long, List<Long>> children = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Object parent = row.get("parent_id");
            Long parentId = parent == null ? 0L : ((Number) parent).longValue();
            children.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (result.contains(current)) {
                continue;
            }
            result.add(current);
            List<Long> next = children.get(current);
            if (next != null) {
                queue.addAll(next);
            }
        }
        return result;
    }
}
