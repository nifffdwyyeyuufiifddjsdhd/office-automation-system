package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Positions;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.PositionsRepository;
import com.officeautomationsystemapi.repository.UsersRepository;
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

import java.time.LocalDateTime;
import java.util.List;

// 职位维护接口，管理端职位设置页使用
@RestController
@RequestMapping("/api/positions")
public class PositionsController {

    @Autowired
    private PositionsRepository positionsRepository;

    @Autowired
    private UsersRepository usersRepository;

    // 按排序号、等级、id 升序，停用的也返回，编辑要回显
    // 只读列表也给 user:onboard，办理入职要选职位
    @GetMapping
    @RequirePermission({"position:manage", "user:onboard"})
    public Result<List<Positions>> list() {
        QueryWrapper<Positions> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order").orderByAsc("level").orderByAsc("id");
        return Result.success(positionsRepository.selectList(wrapper));
    }

    // 新增职位，名称必填，其它字段没传用默认值，id 自增
    @PostMapping
    @RequirePermission("position:manage")
    public Result<Long> create(@RequestBody Positions position) {
        if (position.getPosition_name() == null || position.getPosition_name().isBlank()) {
            return Result.error("职位名称不能为空");
        }
        if (position.getLevel() == null) {
            position.setLevel(1);
        }
        if (position.getSort_order() == null) {
            position.setSort_order(0);
        }
        if (position.getStatus() == null) {
            position.setStatus(1);
        }
        position.setId(null);
        position.setDeleted_at(null);
        position.setPosition_name(position.getPosition_name().trim());
        position.setCreated_at(LocalDateTime.now());
        position.setUpdated_at(LocalDateTime.now());
        // 职位编码唯一，重复会抛 DuplicateKeyException，转成中文提示
        try {
            positionsRepository.insert(position);
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
        return Result.success(position.getId());
    }

    // 修改职位，只覆盖传了的字段
    @PutMapping
    @RequirePermission("position:manage")
    public Result<Void> update(@RequestBody Positions position) {
        if (position.getId() == null) {
            return Result.error("缺少职位ID");
        }
        if (position.getPosition_name() == null || position.getPosition_name().isBlank()) {
            return Result.error("职位名称不能为空");
        }
        Positions db = positionsRepository.selectById(position.getId());
        if (db == null) {
            return Result.error("职位不存在");
        }
        db.setPosition_name(position.getPosition_name().trim());
        if (position.getPosition_code() != null) {
            db.setPosition_code(position.getPosition_code());
        }
        if (position.getLevel() != null) {
            db.setLevel(position.getLevel());
        }
        if (position.getSort_order() != null) {
            db.setSort_order(position.getSort_order());
        }
        if (position.getStatus() != null) {
            db.setStatus(position.getStatus());
        }
        if (position.getRemark() != null) {
            db.setRemark(position.getRemark());
        }
        db.setUpdated_at(LocalDateTime.now());
        // 修改也可能撞编码唯一索引，按业务错误返回
        try {
            positionsRepository.updateById(db);
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
        return Result.success(null);
    }

    // 删除前先确认记录在，再看有没有员工在用
    @DeleteMapping("/{id}")
    @RequirePermission("position:manage")
    public Result<Void> delete(@PathVariable Long id) {
        if (positionsRepository.selectById(id) == null) {
            return Result.error("记录不存在或已删除");
        }
        QueryWrapper<Users> userWrapper = new QueryWrapper<>();
        userWrapper.eq("position_id", id);
        if (usersRepository.selectCount(userWrapper) > 0) {
            return Result.error("该职位下还有员工，请先调整员工职位后再删除");
        }
        positionsRepository.deleteById(id);
        return Result.success(null);
    }
}
