package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.Titles;
import com.officeautomationsystemapi.entity.Users;
import com.officeautomationsystemapi.repository.TitlesRepository;
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

// 职称维护接口，管理端职称设置页使用
@RestController
@RequestMapping("/api/titles")
public class TitlesController {

    @Autowired
    private TitlesRepository titlesRepository;

    @Autowired
    private UsersRepository usersRepository;

    // 按排序号、等级、id 升序，停用的也返回，编辑要回显
    @GetMapping
    @RequirePermission("title:manage")
    public Result<List<Titles>> list() {
        QueryWrapper<Titles> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order").orderByAsc("title_level").orderByAsc("id");
        return Result.success(titlesRepository.selectList(wrapper));
    }

    // 新增职称，名称必填，其它字段没传用默认值，id 自增
    @PostMapping
    @RequirePermission("title:manage")
    public Result<Long> create(@RequestBody Titles title) {
        if (title.getTitle_name() == null || title.getTitle_name().isBlank()) {
            return Result.error("职称名称不能为空");
        }
        if (title.getTitle_level() == null) {
            title.setTitle_level(1);
        }
        if (title.getSort_order() == null) {
            title.setSort_order(0);
        }
        if (title.getStatus() == null) {
            title.setStatus(1);
        }
        title.setId(null);
        title.setDeleted_at(null);
        title.setTitle_name(title.getTitle_name().trim());
        title.setCreated_at(LocalDateTime.now());
        title.setUpdated_at(LocalDateTime.now());
        // 职称编号唯一，重复会抛 DuplicateKeyException，转成中文提示
        try {
            titlesRepository.insert(title);
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
        return Result.success(title.getId());
    }

    // 修改职称，只覆盖传了的字段
    @PutMapping
    @RequirePermission("title:manage")
    public Result<Void> update(@RequestBody Titles title) {
        if (title.getId() == null) {
            return Result.error("缺少职称ID");
        }
        if (title.getTitle_name() == null || title.getTitle_name().isBlank()) {
            return Result.error("职称名称不能为空");
        }
        Titles db = titlesRepository.selectById(title.getId());
        if (db == null) {
            return Result.error("职称不存在");
        }
        db.setTitle_name(title.getTitle_name().trim());
        if (title.getCode() != null) {
            db.setCode(title.getCode());
        }
        if (title.getTitle_level() != null) {
            db.setTitle_level(title.getTitle_level());
        }
        if (title.getSort_order() != null) {
            db.setSort_order(title.getSort_order());
        }
        if (title.getStatus() != null) {
            db.setStatus(title.getStatus());
        }
        if (title.getRemark() != null) {
            db.setRemark(title.getRemark());
        }
        db.setUpdated_at(LocalDateTime.now());
        // 修改也可能撞编号唯一索引，按业务错误返回
        try {
            titlesRepository.updateById(db);
        } catch (DuplicateKeyException e) {
            return Result.error("编码已存在，请更换");
        }
        return Result.success(null);
    }

    // 删除前先确认记录在，再看有没有员工在用
    @DeleteMapping("/{id}")
    @RequirePermission("title:manage")
    public Result<Void> delete(@PathVariable Long id) {
        if (titlesRepository.selectById(id) == null) {
            return Result.error("记录不存在或已删除");
        }
        QueryWrapper<Users> userWrapper = new QueryWrapper<>();
        userWrapper.eq("title_id", id);
        if (usersRepository.selectCount(userWrapper) > 0) {
            return Result.error("该职称下还有员工，请先调整员工职称后再删除");
        }
        titlesRepository.deleteById(id);
        return Result.success(null);
    }
}
