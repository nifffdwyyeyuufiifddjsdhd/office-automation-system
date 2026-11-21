package com.officeautomationsystemapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.officeautomationsystemapi.entity.ExpenseCategories;
import com.officeautomationsystemapi.repository.ExpenseCategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 费用类别匹配：付款给默认类别，统计优先用单据上的类别
@Component
public class ExpenseCategoryMatcher {

    @Autowired
    private ExpenseCategoriesRepository categoriesRepository;

    private static final String UNCATEGORIZED = "未分类";

    // 只取启用的类别，按排序号排，关键词为空的那条作默认
    public List<ExpenseCategories> enabled() {
        QueryWrapper<ExpenseCategories> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        List<ExpenseCategories> list = categoriesRepository.selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    // 按关键词把描述归到某个类别，匹配不到用默认类别
    public ExpenseCategories guess(List<ExpenseCategories> categories, String text) {
        if (categories == null || categories.isEmpty()) return null;
        if (text == null || text.isBlank()) return fallback(categories);
        for (ExpenseCategories category : categories) {
            String keywords = category.getKeywords();
            if (keywords == null || keywords.isBlank()) continue;
            for (String keyword : keywords.split(",")) {
                String word = keyword.trim();
                if (!word.isEmpty() && text.contains(word)) return category;
            }
        }
        return fallback(categories);
    }

    // 类别名，关联不到返回空串
    public String nameOf(ExpenseCategories category) {
        return category == null || category.getCategory_name() == null ? "" : category.getCategory_name();
    }

    public ExpenseCategories byId(Long id) {
        return id == null ? null : categoriesRepository.selectById(id);
    }

    // 没有关键词的类别（默认「其他」）当默认类别
    private ExpenseCategories fallback(List<ExpenseCategories> categories) {
        for (ExpenseCategories category : categories) {
            if (category.getKeywords() == null || category.getKeywords().isBlank()) return category;
        }
        return null;
    }

    public String uncategorizedName() {
        return UNCATEGORIZED;
    }
}
