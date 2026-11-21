package com.officeautomationsystemapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 取表单金额：先看配置字段，再看语义键，最后从文字里抠
@Component
public class FlowFormAmountReader {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final Pattern AMOUNT_IN_TEXT =
            Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:元|块钱|块)");
    private static final Pattern NUMBER_ONLY =
            Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*$");

    public Map<String, Object> parse(String businessData) {
        if (businessData == null || businessData.trim().isEmpty()) return new LinkedHashMap<>();
        try {
            Map<String, Object> map = OM.readValue(businessData, new TypeReference<Map<String, Object>>() {});
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    // 配置里 label 含「金额」的字段，动态 key 也能对上
    public String amountField(String formConfig) {
        if (formConfig == null || formConfig.trim().isEmpty()) return null;
        List<Map<String, Object>> fields;
        try {
            fields = OM.readValue(formConfig, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return null;
        }
        if (fields == null || fields.isEmpty()) return null;
        String byType = null;
        for (Map<String, Object> field : fields) {
            Object key = field.get("field");
            if (key == null) continue;
            String label = field.get("label") == null ? "" : field.get("label").toString();
            String type = field.get("type") == null ? "" : field.get("type").toString();
            if (label.contains("金额") && "number".equalsIgnoreCase(type)) return key.toString();
            if (byType == null && label.contains("金额")) byType = key.toString();
        }
        return byType;
    }

    // 取金额：金额字段 → 语义键 → 文字正则
    public BigDecimal extract(String businessData, String formConfig) {
        Map<String, Object> data = parse(businessData);
        if (data.isEmpty()) return null;

        String field = amountField(formConfig);
        if (field != null) {
            BigDecimal value = toDecimal(data.get(field));
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) return value;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            boolean amountKey = key.equals("amount") || key.equals("money") || key.equals("total")
                    || key.equals("total_amount") || key.contains("amount")
                    || entry.getKey().contains("金额");
            if (!amountKey) continue;
            BigDecimal value = toDecimal(entry.getValue());
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) return value;
        }

        for (Object value : data.values()) {
            if (!(value instanceof String text)) continue;
            Matcher matcher = AMOUNT_IN_TEXT.matcher(text);
            if (matcher.find()) {
                try {
                    return new BigDecimal(matcher.group(1));
                } catch (Exception ignore) {
                    // 数字格式不对就继续找下一个字段
                }
            }
        }
        return null;
    }

    // 表单里的金额可能是数字，也可能是「1500」「1500.00」这样的字符串
    public BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        try {
            return new BigDecimal(text).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            Matcher matcher = NUMBER_ONLY.matcher(text);
            if (matcher.find()) {
                try {
                    return new BigDecimal(matcher.group(1));
                } catch (Exception ignore) {
                    return null;
                }
            }
            return null;
        }
    }

    // 给下拉框用：列出能识别的类别名（当前未使用）
    public List<String> labels(String formConfig) {
        List<String> labels = new ArrayList<>();
        if (formConfig == null) return labels;
        try {
            List<Map<String, Object>> fields =
                    OM.readValue(formConfig, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> field : fields) {
                Object label = field.get("label");
                if (label != null) labels.add(label.toString());
            }
        } catch (Exception ignore) {
            // 配置不是数组就当没有标签
        }
        return labels;
    }
}
