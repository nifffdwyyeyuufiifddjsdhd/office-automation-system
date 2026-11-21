package com.officeautomationsystemapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 项目数据范围：项目经理只看自己负责或参与的项目
@Service
public class ProjectScopeService {

    @Autowired
    private JdbcTemplate jdbc;

    // 我负责（或创建）的项目：项目经理的管理范围
    public List<Long> managedProjectIds(Long userId) {
        List<Long> ids = new ArrayList<>();
        if (userId == null) return ids;
        ids.addAll(jdbc.queryForList(
            "SELECT id FROM projects WHERE deleted_at IS NULL AND (manager_id = ? OR creator_id = ?)",
            Long.class, userId, userId));
        return distinct(ids);
    }

    // 我以成员身份加入的项目
    public List<Long> joinedProjectIds(Long userId) {
        List<Long> ids = new ArrayList<>();
        if (userId == null) return ids;
        ids.addAll(jdbc.queryForList(
            "SELECT pm.project_id FROM project_members pm JOIN projects p ON p.id = pm.project_id"
                + " WHERE pm.user_id = ? AND pm.deleted_at IS NULL AND p.deleted_at IS NULL",
            Long.class, userId));
        return distinct(ids);
    }

    // 所有能看到的项目：负责的 + 参与的
    public List<Long> visibleProjectIds(Long userId) {
        Set<Long> all = new LinkedHashSet<>(managedProjectIds(userId));
        all.addAll(joinedProjectIds(userId));
        return new ArrayList<>(all);
    }

    // 是否是该项目的项目经理（负责人或创建人）
    public boolean isProjectManager(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) return false;
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE id = ? AND deleted_at IS NULL AND (manager_id = ? OR creator_id = ?)",
            Integer.class, projectId, userId, userId);
        return c != null && c > 0;
    }

    // 是否是项目成员（含项目经理）
    public boolean isProjectMember(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) return false;
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ? AND deleted_at IS NULL",
            Integer.class, projectId, userId);
        return c != null && c > 0;
    }

    // 能否看到该项目下的任务与统计
    public boolean canSeeProject(Long projectId, Long userId) {
        return isProjectManager(userId, projectId) || isProjectMember(userId, projectId);
    }

    // 项目成员用户 ID 列表
    public List<Long> projectMemberIds(Long projectId) {
        if (projectId == null || projectId <= 0) return new ArrayList<>();
        return distinct(jdbc.queryForList(
            "SELECT user_id FROM project_members WHERE project_id = ? AND deleted_at IS NULL",
            Long.class, projectId));
    }

    // 项目是否还在（未删除）
    public boolean projectExists(Long projectId) {
        if (projectId == null || projectId <= 0) return false;
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE id = ? AND deleted_at IS NULL", Integer.class, projectId);
        return c != null && c > 0;
    }

    private List<Long> distinct(List<Long> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
