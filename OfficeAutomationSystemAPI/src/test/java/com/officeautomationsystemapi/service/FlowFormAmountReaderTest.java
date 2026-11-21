package com.officeautomationsystemapi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// 金额识别：表单配置的金额字段优先，其次语义键，最后才从文字里抠
class FlowFormAmountReaderTest {

    private final FlowFormAmountReader reader = new FlowFormAmountReader();

    private static final String FORM_WITH_AMOUNT =
            "[{\"field\":\"reason\",\"label\":\"事由\",\"type\":\"text\"},"
                    + "{\"field\":\"field_mumk22px\",\"label\":\"报销金额\",\"type\":\"number\"}]";

    @Test
    @DisplayName("动态 key 的表单按配置里的金额字段取值")
    void readsAmountFieldFromFormConfig() {
        String data = "{\"reason\":\"出差报销\",\"field_mumk22px\":1500}";
        assertEquals(new BigDecimal("1500.00"), reader.extract(data, FORM_WITH_AMOUNT));
    }

    @Test
    @DisplayName("没有表单配置时退回语义键 amount")
    void fallsBackToSemanticKey() {
        assertEquals(new BigDecimal("2600.00"), reader.extract("{\"amount\":2600}", null));
    }

    @Test
    @DisplayName("金额字段为空时继续找文字里的数字")
    void fallsBackToTextWhenKeyEmpty() {
        String data = "{\"reason\":\"周五请甲方吃饭 2000 元\",\"amount\":null}";
        assertEquals(new BigDecimal("2000"), reader.extract(data, null));
    }

    @Test
    @DisplayName("完全没金额信息时返回 null，由上层显示待补")
    void returnsNullWhenNothingFound() {
        assertNull(reader.extract("{\"field_mumk22px\":\"周五请甲方吃饭\"}", null));
        assertNull(reader.extract(null, FORM_WITH_AMOUNT));
    }

    @Test
    @DisplayName("金额字段标签不叫金额时也能通过 type=number 之外的方式兜底")
    void amountFieldPrefersNumberType() {
        String config = "[{\"field\":\"a\",\"label\":\"金额说明\",\"type\":\"text\"},"
                + "{\"field\":\"b\",\"label\":\"金额\",\"type\":\"number\"}]";
        assertEquals("b", reader.amountField(config));
        assertEquals("a", reader.amountField("[{\"field\":\"a\",\"label\":\"金额说明\",\"type\":\"text\"}]"));
    }
}
