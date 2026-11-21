package com.officeautomationsystemapi.utilt;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 通知模板渲染：占位符替换、缺变量保留、空模板原样返回
class TemplateRendererTest {

    @Test
    void renderReplacesAllPlaceholders() {
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "请假申请");
        vars.put("stepName", "部门主管审批");
        String out = TemplateRenderer.render("新的待办流程：{title}，当前节点「{stepName}」", vars);
        assertEquals("新的待办流程：请假申请，当前节点「部门主管审批」", out);
    }

    @Test
    void renderKeepsUnknownPlaceholder() {
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "报销");
        String out = TemplateRenderer.render("流程{result}：{title}", vars);
        assertEquals("流程{result}：报销", out);
        assertTrue(TemplateRenderer.hasPlaceholder(out));
    }

    @Test
    void renderNullValueBecomesEmpty() {
        Map<String, String> vars = new HashMap<>();
        vars.put("approver", null);
        assertEquals("处理人：", TemplateRenderer.render("处理人：{approver}", vars));
    }

    @Test
    void renderEmptyTemplateOrVars() {
        assertEquals("{title}", TemplateRenderer.render("{title}", null));
        assertEquals("固定文案", TemplateRenderer.render("固定文案", new HashMap<>()));
        assertEquals(null, TemplateRenderer.render(null, Map.of("a", "b")));
    }

    @Test
    void hasPlaceholderDetectsOnlyCompleteBraces() {
        assertFalse(TemplateRenderer.hasPlaceholder("没有占位符"));
        assertFalse(TemplateRenderer.hasPlaceholder("{未闭合"));
        assertTrue(TemplateRenderer.hasPlaceholder("{closed}"));
        assertFalse(TemplateRenderer.hasPlaceholder(null));
    }
}
