package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.UserGroupMembers;
import com.officeautomationsystemapi.entity.UserGroups;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.UserGroupMembersRepository;
import com.officeautomationsystemapi.repository.UserGroupsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 群组管理，群组增删改查和成员维护
@RestController
@RequestMapping("/api/groups")
public class UserGroupsController {

    @Autowired
    private UserGroupsRepository groupsRepository;
    @Autowired
    private UserGroupMembersRepository membersRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OrganizationsRepository organizationsRepository;

    // 群组列表，成员数批量统计，不逐个 count
    @GetMapping
    @RequirePermission("group:manage")
    public Result<List<Map<String, Object>>> list() {
        List<UserGroups> groups = groupsRepository.selectList(
                new QueryWrapper<UserGroups>().orderByDesc("created_at"));
        Map<Long, Long> memberCounts = countMembers(groups);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserGroups g : groups) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", g.getId());
            row.put("group_name", g.getGroup_name());
            row.put("group_code", g.getGroup_code());
            row.put("description", g.getDescription());
            row.put("status", g.getStatus());
            row.put("created_at", g.getCreated_at());
            row.put("memberCount", memberCounts.getOrDefault(g.getId(), 0L));
            result.add(row);
        }
        return Result.success(result);
    }

    // 一次查出成员关系，在内存里按群组计数
    private Map<Long, Long> countMembers(List<UserGroups> groups) {
        if (groups.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> ids = groups.stream()
                .map(UserGroups::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        List<UserGroupMembers> members = membersRepository.selectList(
                new QueryWrapper<UserGroupMembers>().select("group_id").in("group_id", ids));
        return members.stream()
                .filter(m -> m.getGroup_id() != null)
                .collect(Collectors.groupingBy(UserGroupMembers::getGroup_id, Collectors.counting()));
    }

    // 新增时 creator_id 取当前登录人
    @PostMapping
    @RequirePermission("group:manage")
    public Result<Long> create(@RequestBody UserGroups g, HttpServletRequest request) {
        String name = g.getGroup_name() == null ? "" : g.getGroup_name().trim();
        if (name.isEmpty()) {
            return Result.error("群组名称不能为空");
        }
        // 状态只允许 0 停用、1 启用
        if (g.getStatus() != null && g.getStatus() != 0 && g.getStatus() != 1) {
            return Result.error("状态只能是 0 或 1");
        }
        g.setId(null);
        g.setGroup_name(name);
        if (g.getStatus() == null) {
            g.setStatus(1);
        }
        g.setCreator_id(RequestUser.id(request));
        LocalDateTime now = LocalDateTime.now();
        g.setCreated_at(now);
        g.setUpdated_at(now);
        groupsRepository.insert(g);
        return Result.success(g.getId());
    }

    // 只更新传了的字段，没传的保留原值
    @PutMapping
    @RequirePermission("group:manage")
    public Result<Void> update(@RequestBody UserGroups g) {
        if (g.getId() == null) {
            return Result.error("缺少群组ID");
        }
        UserGroups db = groupsRepository.selectById(g.getId());
        if (db == null) {
            return Result.error("群组不存在");
        }
        if (g.getGroup_name() != null) {
            String name = g.getGroup_name().trim();
            if (name.isEmpty()) {
                return Result.error("群组名称不能为空");
            }
            db.setGroup_name(name);
        }
        if (g.getGroup_code() != null) {
            db.setGroup_code(g.getGroup_code().trim());
        }
        if (g.getDescription() != null) {
            db.setDescription(g.getDescription());
        }
        if (g.getStatus() != null) {
            // 状态只允许 0 停用、1 启用
            if (g.getStatus() != 0 && g.getStatus() != 1) {
                return Result.error("状态只能是 0 或 1");
            }
            db.setStatus(g.getStatus());
        }
        db.setUpdated_at(LocalDateTime.now());
        groupsRepository.updateById(db);
        return Result.success(null);
    }

    // 成员关系和群组一起删，失败回滚
    @Transactional
    @DeleteMapping("/{id}")
    @RequirePermission("group:manage")
    public Result<Void> delete(@PathVariable Long id) {
        QueryWrapper<UserGroupMembers> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", id);
        membersRepository.delete(wrapper);
        groupsRepository.deleteById(id);
        return Result.success(null);
    }

    // 群组成员，用户和部门都批量查
    @GetMapping("/{id}/members")
    @RequirePermission("group:manage")
    public Result<List<Map<String, Object>>> members(@PathVariable Long id) {
        List<UserGroupMembers> members = membersRepository.selectList(
                new QueryWrapper<UserGroupMembers>().eq("group_id", id).orderByDesc("join_time"));
        List<Long> userIds = members.stream()
                .map(UserGroupMembers::getUser_id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Users> userMap = new HashMap<>();
        Map<Long, String> orgNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Users u : usersRepository.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u);
            }
            List<Long> orgIds = userMap.values().stream()
                    .map(Users::getOrg_id)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!orgIds.isEmpty()) {
                for (Organizations o : organizationsRepository.selectBatchIds(orgIds)) {
                    orgNameMap.put(o.getId(), o.getOrg_name());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserGroupMembers m : members) {
            Users u = userMap.get(m.getUser_id());
            if (u == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("userId", u.getId());
            row.put("realName", u.getReal_name());
            row.put("username", u.getUsername());
            row.put("orgName", u.getOrg_id() == null ? "-" : orgNameMap.getOrDefault(u.getOrg_id(), "-"));
            row.put("joinTime", m.getJoin_time());
            result.add(row);
        }
        return Result.success(result);
    }

    // 批量加成员，已存在的用户跳过
    @Transactional
    @PostMapping("/{id}/members")
    @RequirePermission("group:manage")
    public Result<Void> addMembers(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> userIds = body == null ? null : body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return Result.error("请选择要添加的用户");
        }
        List<Long> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Result.error("请选择要添加的用户");
        }

        // 先确认群组存在
        if (groupsRepository.selectById(id) == null) {
            return Result.error("群组不存在");
        }
        // 批量查用户，数量对不上说明有不存在或已删除的
        if (usersRepository.selectBatchIds(distinctIds).size() != distinctIds.size()) {
            return Result.error("存在无效的用户ID");
        }

        QueryWrapper<UserGroupMembers> existQuery = new QueryWrapper<>();
        existQuery.select("user_id").eq("group_id", id).in("user_id", distinctIds);
        Set<Long> existing = membersRepository.selectList(existQuery).stream()
                .map(UserGroupMembers::getUser_id)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        for (Long userId : distinctIds) {
            if (existing.contains(userId)) {
                continue;
            }
            UserGroupMembers m = new UserGroupMembers();
            m.setGroup_id(id);
            m.setUser_id(userId);
            m.setJoin_time(now);
            m.setCreated_at(now);
            m.setUpdated_at(now);
            membersRepository.insert(m);
        }
        return Result.success(null);
    }

    // 移除成员
    @DeleteMapping("/{id}/members/{userId}")
    @RequirePermission("group:manage")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        QueryWrapper<UserGroupMembers> wrapper = new QueryWrapper<>();
        wrapper.eq("group_id", id).eq("user_id", userId);
        membersRepository.delete(wrapper);
        return Result.success(null);
    }
}
