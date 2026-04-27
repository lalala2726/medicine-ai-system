package com.zhangyichuang.medicine.agent.json;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import lombok.Data;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCodeLabelSerializerTests {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldUseProductStatusDictMappingWhenMatched() {
        ProductStatusSample sample = new ProductStatusSample();
        sample.setStatus(1);

        JsonNode node = serializeToNode(sample);

        assertEquals("上架", node.get("status").get("description").asText());
        assertEquals(1, node.get("status").get("value").asInt());
    }

    @Test
    void shouldUseDeliveryTypeDictMapping() {
        DictSample sample = new DictSample();
        sample.setDeliveryType(2);

        JsonNode node = serializeToNode(sample);

        assertEquals("快递配送", node.get("deliveryType").get("description").asText());
        assertEquals(2, node.get("deliveryType").get("value").asInt());
    }

    @Test
    void shouldFallbackToSourceWhenMappingMissing() {
        UnknownCodeSample sample = new UnknownCodeSample();
        sample.setPayType("MIX_PAY");

        JsonNode node = serializeToNode(sample);

        assertEquals("MIX_PAY", node.get("payType").get("description").asText());
        assertEquals("MIX_PAY", node.get("payType").get("value").asText());
    }

    @Test
    void shouldReturnNullWhenSourceFieldIsNull() {
        UnknownCodeSample sample = new UnknownCodeSample();
        sample.setPayType(null);

        JsonNode node = serializeToNode(sample);

        assertTrue(node.get("payType").isNull());
    }

    @Test
    void shouldSupportIntegerCodeInProductStatusDict() {
        ProductStatusSample sample = new ProductStatusSample();
        sample.setStatus(0);

        JsonNode node = serializeToNode(sample);

        assertEquals("下架", node.get("status").get("description").asText());
        assertTrue(node.get("status").get("value").isInt());
        assertEquals(0, node.get("status").get("value").asInt());
    }

    @Test
    void shouldResolveSourceFieldWhenConfigured() {
        SourceFieldSample sample = new SourceFieldSample();
        sample.setChangeType("订单支付");
        sample.setAmountDirection(2);

        JsonNode node = serializeToNode(sample);

        assertEquals(2, node.get("changeType").get("value").asInt());
        assertEquals("支出", node.get("changeType").get("description").asText());
        assertEquals(2, node.get("amountDirection").get("value").asInt());
        assertEquals("支出", node.get("amountDirection").get("description").asText());
    }

    @Test
    void shouldUseAfterSaleStatusDictMapping() {
        AfterSaleStatusSample sample = new AfterSaleStatusSample();
        sample.setAfterSaleStatus("PENDING");

        JsonNode node = serializeToNode(sample);

        assertEquals("待审核", node.get("afterSaleStatus").get("description").asText());
        assertEquals("PENDING", node.get("afterSaleStatus").get("value").asText());
    }

    @Test
    void shouldUseAfterSaleReasonDictMapping() {
        AfterSaleReasonSample sample = new AfterSaleReasonSample();
        sample.setApplyReason("DAMAGED");

        JsonNode node = serializeToNode(sample);

        assertEquals("收到商品损坏了", node.get("applyReason").get("description").asText());
        assertEquals("DAMAGED", node.get("applyReason").get("value").asText());
    }

    private JsonNode serializeToNode(Object value) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("serialize value failed", ex);
        }
    }

    @Data
    private static class ProductStatusSample {

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_PRODUCT_STATUS)
        private Integer status;
    }

    @Data
    private static class DictSample {

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_PRODUCT_DELIVERY_TYPE)
        private Integer deliveryType;
    }

    @Data
    private static class UnknownCodeSample {

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_PAY_TYPE)
        private String payType;
    }

    @Data
    private static class SourceFieldSample {

        @AgentCodeLabel(source = "amountDirection", dictKey = AgentCodeLabelRegistry.AGENT_USER_WALLET_CHANGE_TYPE)
        private String changeType;

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_USER_WALLET_CHANGE_TYPE)
        private Integer amountDirection;
    }

    @Data
    private static class AfterSaleStatusSample {

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_STATUS)
        private String afterSaleStatus;
    }

    @Data
    private static class AfterSaleReasonSample {

        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_REASON)
        private String applyReason;
    }
}
