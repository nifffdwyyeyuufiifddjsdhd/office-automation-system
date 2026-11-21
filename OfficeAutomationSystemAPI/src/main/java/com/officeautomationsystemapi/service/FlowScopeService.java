package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.officeautomationsystemapi.entity.FlowDefinitions;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.UserRoles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.UserRolesRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 流程使用范围：发起拦截和模板列表共用这套规则
// 0 全员、1 指定部门（含下级部门）、2 指定角色；管理员不受限制
@Service
public class FlowScopeService {

    private static final ObjectMapper OM = new ObjectMapper();

    @Autowired
    private OrganizationsRepository organizationsRepository;
    @Autowired
    private UserRolesRepository userRolesRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private PermissionService permissionService;

    // 这个模板当前用户能不能用；管理员或范围为空一律放行
    public boolean allowed(FlowDefinitions def, Long userId) {
        if (def == null) return false;
        int scope = def.getUse_scope() == null ? 0 : def.getUse_scope();
        if (scope == 0) return true;
        List<Long> ids = parseIds(def.getUse_scope_ids());
        if (ids.isEmpty()) return true;
        if (permissionService.isAdmin(userId)) return true;
        if (scope == 2) {
            for (UserRoles ur : userRolesRepository.selectList(new QueryWrapper<UserRoles>().eq("user_id", userId))) {
                if (ur.getRole_id() != null && ids.contains(ur.getRole_id())) return true;
            }
            return false;
        }
        Users me = usersRepository.selectById(userId);
        Long myOrg = me == null ? null : me.getOrg_id();
        if (myOrg == null) return false;
        for (Long orgId : ids) {
            if (orgSubtreeIds(orgId).contains(myOrg)) return true;
        }
        return false;
    }

    // 使用范围对象 id 存成 JSON 数组，解析失败按空处理
    public List<Long> parseIds(String json) {
        List<Long> ids = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return ids;
        try {
            List<?> raw = OM.readValue(json, List.class);
            for (Object o : raw) {
                if (o != null) ids.add(Long.valueOf(o.toString()));
            }
        } catch (Exception ignore) {
            // 配置坏了就按全员放行，别把整个流程堵死
        }
        return ids;
    }

    // 部门及其全部下级部门
    public List<Long> orgSubtreeIds(Long rootId) {
        List<Long> result = new ArrayList<>();
        if (rootId == null) return result;
        List<Organizations> all = organizationsRepository.selectList(new QueryWrapper<Organizations>());
        Map<Long, List<Long>> children = new HashMap<>();
        for (Organizations o : all) {
            if (o.getParent_id() == null) continue;
            children.computeIfAbsent(o.getParent_id(), k -> new ArrayList<>()).add(o.getId());
        }
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        result.add(rootId);
        // 广度优先展开，出现过的跳过，防止脏数据成环
        for (int i = 0; i < queue.size(); i++) {
            List<Long> kids = children.get(queue.get(i));
            if (kids == null) continue;
            for (Long kid : kids) {
                if (kid == null || result.contains(kid)) continue;
                result.add(kid);
                queue.add(kid);
            }
        }
        return result;
    }
}
