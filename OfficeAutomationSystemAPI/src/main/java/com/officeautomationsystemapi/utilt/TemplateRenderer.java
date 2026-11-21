package com.officeautomationsystemapi.utilt;

import java.util.Map;

// 把模板里的 {变量} 换成实际值，缺的值保留占位符
public final class TemplateRenderer {

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, String> vars) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (vars == null || vars.isEmpty()) {
            return template;
        }
        String out = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            out = out.replace("{" + entry.getKey() + "}", value);
        }
        return out;
    }

    // 模板里是否还有没替换的占位符
    public static boolean hasPlaceholder(String text) {
        if (text == null) {
            return false;
        }
        int open = text.indexOf('{');
        int close = text.indexOf('}', open + 1);
        return open >= 0 && close > open;
    }
}
