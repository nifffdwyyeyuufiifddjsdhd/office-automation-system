package com.officeautomationsystemapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.OrganizationsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
import com.officeautomationsystemapi.service.OrganizationService;
import com.officeautomationsystemapi.vo.OrgNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 部门管理：把平铺的部门表拼成树，新增修改删除前先校验
@Service
@Transactional
public class OrganizationServiceImpl extends ServiceImpl<OrganizationsRepository, Organizations>
        implements OrganizationService {

    @Autowired
    private UsersRepository usersRepository;

    // 管理端部门树，停用部门也保留，便于在树上重新启用
    @Override
    public List<OrgNodeVO> orgTreeForAdmin() {
        QueryWrapper<Organizations> wrapper = new QueryWrapper<>();
        // 排序号相同的部门按 id 排，保证节点顺序稳定
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        return buildTree(list(wrapper));
    }

    // 只读部门树，只给启用部门，供下拉选择和筛选使用
    @Override
    public List<OrgNodeVO> orgTreeEnabled() {
        QueryWrapper<Organizations> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order").orderByAsc("id");
        return buildTree(list(wrapper));
    }

    // 新增部门：名称必填，其余字段补默认值；顶层部门的 parent_id 统一存 0
    @Override
    public Long createOrg(Organizations org) {
        String name = checkName(org);
        if (org.getParent_id() == null) {
            org.setParent_id(0L);
        }
        if (org.getOrg_type() == null) {
            org.setOrg_type(2);
        }
        if (org.getSort_order() == null) {
            org.setSort_order(0);
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        // 上级部门必须存在且未删除，否则新部门会挂到空节点下
        checkParentExists(org.getParent_id());
        org.setId(null);
        org.setDeleted_at(null);
        org.setOrg_name(name);
        org.setCreated_at(LocalDateTime.now());
        org.setUpdated_at(LocalDateTime.now());
        save(org);
        return org.getId();
    }

    // 修改部门：不传的字段保持原值，改上级要先查存在和成环
    @Override
    public void updateOrg(Organizations org) {
        if (org.getId() == null) {
            throw new RuntimeException("缺少部门ID");
        }
        String name = checkName(org);
        Organizations db = getById(org.getId());
        if (db == null) {
            throw new RuntimeException("部门不存在");
        }
        if (org.getParent_id() != null) {
            // 先确认上级部门真实存在，再判断成环
            checkParentExists(org.getParent_id());
            checkParent(org.getId(), org.getParent_id());
            db.setParent_id(org.getParent_id());
        }

        db.setOrg_name(name);
        if (org.getOrg_code() != null) {
            db.setOrg_code(org.getOrg_code());
        }
        if (org.getOrg_type() != null) {
            db.setOrg_type(org.getOrg_type());
        }
        if (org.getSort_order() != null) {
            db.setSort_order(org.getSort_order());
        }
        if (org.getStatus() != null) {
            db.setStatus(org.getStatus());
        }
        db.setUpdated_at(LocalDateTime.now());
        updateById(db);
    }

    // 删除部门：不存在报错，有子部门或还有员工时拒绝
    @Override
    public void deleteOrg(Long id) {
        // 逻辑删除后 getById 查不到，先拦住重复删除，避免对不存在的 id 返回成功
        if (getById(id) == null) {
            throw new RuntimeException("记录不存在或已删除");
        }
        QueryWrapper<Organizations> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", id);
        if (count(childWrapper) > 0) {
            throw new RuntimeException("该部门下还有子部门，请先删除或迁移子部门");
        }

        QueryWrapper<Users> userWrapper = new QueryWrapper<>();
        userWrapper.eq("org_id", id);
        if (usersRepository.selectCount(userWrapper) > 0) {
            throw new RuntimeException("该部门下还有员工，请先迁移员工到其他部门");
        }
        removeById(id);
    }

    // 部门名称非空校验，返回去掉首尾空格后的名称
    private String checkName(Organizations org) {
        if (org.getOrg_name() == null || org.getOrg_name().trim().isEmpty()) {
            throw new RuntimeException("部门名称不能为空");
        }
        return org.getOrg_name().trim();
    }

    // 上级部门必须是存在且未删除的部门，parent_id 为 0 或空表示挂到根节点
    private void checkParentExists(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (getById(parentId) == null) {
            throw new RuntimeException("上级部门不存在或已删除");
        }
    }

    // 上级不能是自己或自己的下级，沿 parent 链向上找
    private void checkParent(Long orgId, Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(orgId)) {
            throw new RuntimeException("上级部门不能是自己");
        }
        Set<Long> visited = new HashSet<>();
        Long current = parentId;
        while (current != null && current != 0 && visited.add(current)) {
            if (current.equals(orgId)) {
                throw new RuntimeException("上级部门不能选择自己的下级部门");
            }
            Organizations parent = getById(current);
            if (parent == null) {
                return;
            }
            current = parent.getParent_id();
        }
    }

    // 把平铺列表拼成树，没有上级或上级不存在的挂到根节点
    private List<OrgNodeVO> buildTree(List<Organizations> all) {
        Map<Long, OrgNodeVO> nodeMap = new HashMap<>();
        for (Organizations org : all) {
            OrgNodeVO node = new OrgNodeVO();
            node.setId(org.getId());
            node.setLabel(org.getOrg_name());
            nodeMap.put(org.getId(), node);
        }

        List<OrgNodeVO> roots = new ArrayList<>();
        for (Organizations org : all) {
            OrgNodeVO node = nodeMap.get(org.getId());
            Long parentId = org.getParent_id();
            if (parentId == null || parentId == 0) {
                roots.add(node);
                continue;
            }
            OrgNodeVO parent = nodeMap.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
