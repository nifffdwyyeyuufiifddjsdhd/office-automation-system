package com.officeautomationsystemapi.service;

import com.officeautomationsystemapi.entity.ExpenseCategories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// 费用类别匹配：关键词命中用命中项，否则用没有关键词的那条
class ExpenseCategoryMatcherTest {

    private final ExpenseCategoryMatcher matcher = new ExpenseCategoryMatcher();

    private ExpenseCategories category(String name, String keywords) {
        ExpenseCategories c = new ExpenseCategories();
        c.setCategory_name(name);
        c.setKeywords(keywords);
        return c;
    }

    private List<ExpenseCategories> categories() {
        List<ExpenseCategories> list = new ArrayList<>();
        list.add(category("差旅费", "差旅,机票,酒店,出差"));
        list.add(category("业务招待费", "招待,请客,餐费"));
        list.add(category("其他", null));
        return list;
    }

    @Test
    @DisplayName("描述里有关键词就归到对应类别")
    void matchesByKeyword() {
        assertEquals("差旅费", matcher.nameOf(matcher.guess(categories(), "10 月广州出差差旅费报销")));
        assertEquals("业务招待费", matcher.nameOf(matcher.guess(categories(), "周五请客招待甲方，订了个包间")));
    }

    @Test
    @DisplayName("关键词不命中时落到没有关键词的兜底类别")
    void fallsBackToUnkeyworded() {
        assertEquals("其他", matcher.nameOf(matcher.guess(categories(), "买了两箱打印纸")));
        assertEquals("其他", matcher.nameOf(matcher.guess(categories(), "")));
    }

    @Test
    @DisplayName("类别列表为空时返回 null，空类别名返回空串而不是 null")
    void handlesEmptyInput() {
        assertNull(matcher.guess(new ArrayList<>(), "任何描述"));
        assertNull(matcher.guess(null, "任何描述"));
        assertEquals("", matcher.nameOf(null));
    }

    @Test
    @DisplayName("未分类名称固定，前后端文案要一致")
    void uncategorizedName() {
        assertEquals("未分类", matcher.uncategorizedName());
    }
}
