package com.officeautomationsystemapi.controller;

import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.common.RequestUser;
import com.officeautomationsystemapi.service.DataScopeService;
import com.officeautomationsystemapi.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import com.officeautomationsystemapi.vo.OrgNodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 部门接口，管理端部门树和部门选择共用
@RestController
@RequestMapping("/api/orgs")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private DataScopeService dataScopeService;

    // 管理端部门树，停用节点也返回，便于恢复
    @GetMapping("/tree")
    @RequirePermission("org:manage")
    public Result<List<OrgNodeVO>> tree() {
        return Result.success(organizationService.orgTreeForAdmin());
    }

    // 只读部门树，只给启用节点，登录即可访问
    @GetMapping("/tree-enabled")
    public Result<List<OrgNodeVO>> treeEnabled() {
        return Result.success(organizationService.orgTreeEnabled());
    }

    // 新增部门，返回新部门 id
    @PostMapping
    @RequirePermission("org:manage")
    public Result<Long> create(@RequestBody Organizations org, HttpServletRequest request) {
        // 只能建在自己能管的部门下，顶层部门留给全公司数据范围的角色
        Long parentId = org.getParent_id();
        if (parentId == null || parentId == 0L) {
            if (!dataScopeService.canSeeAll(RequestUser.id(request))) {
                return Result.error(403, "无权在根节点下新建部门");
            }
        } else if (!dataScopeService.canOperateOrg(RequestUser.id(request), parentId)) {
            return Result.error(403, "无权在该部门下新建部门：不在您的数据范围内");
        }
        // 组织编码是唯一索引，重复会抛 DuplicateKeyException，转成中文提示
        try {
            return Result.success(organizationService.createOrg(org));
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
    }

    // 修改部门，校验和错误提示都在服务层
    @PutMapping
    @RequirePermission("org:manage")
    public Result<Void> update(@RequestBody Organizations org, HttpServletRequest request) {
        if (org.getId() == null) {
            return Result.error("缺少部门ID");
        }
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), org.getId())) {
            return Result.error(403, "无权修改该部门：不在您的数据范围内");
        }
        // 修改也可能撞组织编码唯一索引，按业务错误返回
        try {
            organizationService.updateOrg(org);
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
        return Result.success(null);
    }

    // 删除部门，有子部门或员工时由服务层抛出业务异常
    @DeleteMapping("/{id}")
    @RequirePermission("org:manage")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        if (!dataScopeService.canOperateOrg(RequestUser.id(request), id)) {
            return Result.error(403, "无权删除该部门：不在您的数据范围内");
        }
        organizationService.deleteOrg(id);
        return Result.success(null);
    }
}
