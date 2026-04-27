package com.zhangyichuang.medicine.agent.support;

import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.OrderDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.UserDetailVo;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentVoDescriptionResolverTests {

    private final AgentVoDescriptionResolver resolver = new AgentVoDescriptionResolver();

    @Test
    void resolve_ShouldReturnMeta_WhenVoIsAnnotated() {
        Optional<AgentVoDescriptionResolver.Meta> metaOptional = resolver.resolve(UserDetailVo.class);

        assertTrue(metaOptional.isPresent());
        AgentVoDescriptionResolver.Meta meta = metaOptional.get();
        assertEquals("管理端用户详情", meta.entityDescription());

        Map<String, String> fieldDescriptions = meta.fieldDescriptions();
        assertEquals("头像", fieldDescriptions.get("avatar"));
        assertEquals("基础信息", fieldDescriptions.get("basicInfo"));
        assertEquals("用户ID", fieldDescriptions.get("basicInfo.userId"));
        assertEquals("用户状态", fieldDescriptions.get("securityInfo.status"));
    }

    @Test
    void resolve_ShouldBuildCollectionFieldPath_WhenElementTypeIsAnnotatedVo() {
        Optional<AgentVoDescriptionResolver.Meta> orderMetaOptional = resolver.resolve(OrderDetailVo.class);
        Optional<AgentVoDescriptionResolver.Meta> afterSaleMetaOptional = resolver.resolve(AgentAfterSaleDetailVo.class);

        assertTrue(orderMetaOptional.isPresent());
        assertTrue(afterSaleMetaOptional.isPresent());

        Map<String, String> orderDescriptions = orderMetaOptional.get().fieldDescriptions();
        Map<String, String> afterSaleDescriptions = afterSaleMetaOptional.get().fieldDescriptions();

        assertEquals("商品信息", orderDescriptions.get("productInfo"));
        assertEquals("商品ID", orderDescriptions.get("productInfo[].productId"));
        assertEquals("时间线列表", afterSaleDescriptions.get("timeline"));
        assertEquals("事件类型编码", afterSaleDescriptions.get("timeline[].eventType"));
    }

    @Test
    void resolve_ShouldReturnEmpty_WhenVoIsNotAnnotated() {
        Optional<AgentVoDescriptionResolver.Meta> metaOptional = resolver.resolve(PlainPojo.class);
        assertFalse(metaOptional.isPresent());
    }

    private static class PlainPojo {
        private String name;
    }
}
