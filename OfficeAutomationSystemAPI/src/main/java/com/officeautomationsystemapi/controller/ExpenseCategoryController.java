package com.officeautomationsystemapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.common.RequirePermission;
import com.officeautomationsystemapi.common.Result;
import com.officeautomationsystemapi.entity.ExpenseCategories;
import com.officeautomationsystemapi.repository.ExpenseCategoriesRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 费用类别维护，财务统计按这里的 keywords 给报销单归类
@RestController
@RequestMapping("/api/finance/categories")
public class ExpenseCategoryController {

    @Autowired
    private ExpenseCategoriesRepository categoriesRepository;

    // 类别列表只要求登录，增删改各自校验权限码
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        QueryWrapper<ExpenseCategories> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        List<ExpenseCategories> list = categoriesRepository.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExpenseCategories c : list) {
            rows.add(toRow(c));
        }
        return Result.success(rows);
    }

    // 新增类别，名称必填，编码不填自动生成，关键词可为空
    @PostMapping
    @RequirePermission("finance:category:add")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        String name = text(body, "categoryName", "category_name");
        if (name == null) {
            return Result.error("类别名称不能为空");
        }
        String code = text(body, "categoryCode", "category_code");
        ExpenseCategories category = new ExpenseCategories();
        category.setCategory_name(name);
        category.setCategory_code(code == null
                ? "CAT" + System.currentTimeMillis() % 1000000
                : code.toUpperCase());
        category.setKeywords(trimKeywords(text(body, "keywords", "keywords")));
        Integer sortOrder = toInt(body.get("sortOrder"));
        if (sortOrder == null) {
            sortOrder = toInt(body.get("sort_order"));
        }
        category.setSort_order(sortOrder == null ? 0 : sortOrder);
        Integer status = toInt(body.get("status"));
        category.setStatus(status == null ? 1 : status);
        category.setRemark(text(body, "remark", "remark"));
        category.setCreated_at(LocalDateTime.now());
        category.setUpdated_at(LocalDateTime.now());
        // 编码是唯一索引，撞了就按业务提示返回
        try {
            categoriesRepository.insert(category);
        } catch (DuplicateKeyException e) {
            return Result.error("类别编码已存在，请更换");
        }
        return Result.success(category.getId());
    }

    // 修改类别，只覆盖传了的字段
    @PutMapping
    @RequirePermission("finance:category:edit")
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        Long id = toLong(body.get("id"));
        if (id == null) {
            return Result.error("缺少类别ID");
        }
        String name = text(body, "categoryName", "category_name");
        if (name == null) {
            return Result.error("类别名称不能为空");
        }
        ExpenseCategories db = categoriesRepository.selectById(id);
        if (db == null) {
            return Result.error("类别不存在或已删除");
        }
        db.setCategory_name(name);
        String code = text(body, "categoryCode", "category_code");
        if (code != null) {
            db.setCategory_code(code.toUpperCase());
        }
        String keywords = text(body, "keywords", "keywords");
        if (keywords != null) {
            db.setKeywords(trimKeywords(keywords));
        }
        Integer sortOrder = toInt(body.get("sortOrder"));
        if (sortOrder == null) {
            sortOrder = toInt(body.get("sort_order"));
        }
        if (sortOrder != null) {
            db.setSort_order(sortOrder);
        }
        Integer status = toInt(body.get("status"));
        if (status != null) {
            db.setStatus(status);
        }
        String remark = text(body, "remark", "remark");
        if (remark != null) {
            db.setRemark(remark);
        }
        db.setUpdated_at(LocalDateTime.now());
        try {
            categoriesRepository.updateById(db);
        } catch (DuplicateKeyException e) {
            return Result.error("类别编码已存在，请更换");
        }
        return Result.success(null);
    }

    // 删除类别，没有外键引用，只标记删除
    @DeleteMapping("/{id}")
    @RequirePermission("finance:category:delete")
    public Result<Void> delete(@PathVariable Long id) {
        if (categoriesRepository.selectById(id) == null) {
            return Result.error("类别不存在或已删除");
        }
        categoriesRepository.deleteById(id);
        return Result.success(null);
    }

    // 取字符串字段，小驼峰和下划线两种写法都认
    private String text(Map<String, Object> body, String camelKey, String snakeKey) {
        if (body == null) {
            return null;
        }
        Object value = body.get(camelKey);
        if (value == null) {
            value = body.get(snakeKey);
        }
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    // 取整数字段，空串按没传处理
    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 取主键
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 关键词统一成英文逗号分隔，去掉空白项
    private String trimKeywords(String keywords) {
        if (keywords == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : keywords.split("[,，、\\s]+")) {
            String word = part.trim();
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(word);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // 实体转前端字段
    private Map<String, Object> toRow(ExpenseCategories c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", c.getId());
        row.put("categoryName", c.getCategory_name());
        row.put("categoryCode", c.getCategory_code());
        row.put("keywords", c.getKeywords() == null ? "" : c.getKeywords());
        row.put("sortOrder", c.getSort_order());
        row.put("status", c.getStatus());
        row.put("remark", c.getRemark() == null ? "" : c.getRemark());
        row.put("createdAt", c.getCreated_at());
        return row;
    }
}
