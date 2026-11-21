package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.officeautomationsystemapi.entity.Organizations;
import com.officeautomationsystemapi.vo.OrgNodeVO;

import java.util.List;

public interface OrganizationService extends IService<Organizations> {

    // 管理端用的完整部门树，包含停用节点
    List<OrgNodeVO> orgTreeForAdmin();

    // 普通用户用的部门树，只保留启用节点
    List<OrgNodeVO> orgTreeEnabled();

    // 新增部门，返回新部门 id
    Long createOrg(Organizations org);

    // 修改部门
    void updateOrg(Organizations org);

    // 删除部门，有子部门或部门下有员工时拒绝
    void deleteOrg(Long id);
}
