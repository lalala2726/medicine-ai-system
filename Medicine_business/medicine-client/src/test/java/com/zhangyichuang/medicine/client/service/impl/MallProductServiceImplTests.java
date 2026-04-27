package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.client.mapper.MallProductMapper;
import com.zhangyichuang.medicine.client.model.dto.AssistantProductPurchaseCardDto;
import com.zhangyichuang.medicine.client.service.MallOrderItemService;
import com.zhangyichuang.medicine.client.service.MallProductImageService;
import com.zhangyichuang.medicine.client.service.MallProductViewHistoryService;
import com.zhangyichuang.medicine.model.enums.DrugCategoryEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MallProductServiceImplTests {

    @Mock
    private MallProductMapper mallProductMapper;

    @Mock
    private MallProductImageService mallProductImageService;

    @Mock
    private MallProductViewHistoryService mallProductViewHistoryService;

    @Mock
    private MallOrderItemService mallOrderItemService;

    @Mock
    private MallProductSearchService mallProductSearchService;

    @InjectMocks
    private MallProductServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", mallProductMapper);
    }

    @Test
    void getAssistantProductPurchaseCards_ShouldPreserveInputOrderAndCalculateTotalPrice() {
        List<Long> productIds = List.of(102L, 205L, 101L);
        when(mallProductMapper.listAssistantProductPurchaseCardsByIds(productIds)).thenReturn(List.of(
                createCardDto(101L, "布洛芬缓释胶囊", "https://xxx/101.png", "16.8", "24粒/盒", "缓解发热、头痛", false, 56),
                createCardDto(102L, "维生素C咀嚼片", "https://xxx/102.png", "19.9", "60片/瓶", "补充维生素C", false, 98)
        ));

        var result = service.getAssistantProductPurchaseCards(productIds);

        assertEquals("36.70", result.getTotalPrice());
        assertEquals(2, result.getItems().size());
        assertEquals("102", result.getItems().get(0).getId());
        assertEquals("19.90", result.getItems().get(0).getPrice());
        assertEquals("维生素C咀嚼片", result.getItems().get(0).getName());
        assertEquals("101", result.getItems().get(1).getId());
        assertEquals("16.80", result.getItems().get(1).getPrice());
        verify(mallProductMapper).listAssistantProductPurchaseCardsByIds(productIds);
    }

    @Test
    void getAssistantProductPurchaseCards_WhenNoValidProducts_ShouldReturnEmptyCards() {
        List<Long> productIds = List.of(999L);
        when(mallProductMapper.listAssistantProductPurchaseCardsByIds(productIds)).thenReturn(List.of());

        var result = service.getAssistantProductPurchaseCards(productIds);

        assertEquals("0.00", result.getTotalPrice());
        assertTrue(result.getItems().isEmpty());
        verify(mallProductMapper).listAssistantProductPurchaseCardsByIds(productIds);
    }

    private AssistantProductPurchaseCardDto createCardDto(Long id, String name, String image, String price,
                                                          String spec, String efficacy, Boolean prescription,
                                                          Integer stock) {
        AssistantProductPurchaseCardDto dto = new AssistantProductPurchaseCardDto();
        dto.setId(id);
        dto.setName(name);
        dto.setImage(image);
        dto.setPrice(new BigDecimal(price));
        dto.setSpec(spec);
        dto.setEfficacy(efficacy);
        dto.setDrugCategory(Boolean.TRUE.equals(prescription)
                ? DrugCategoryEnum.RX.getCode()
                : DrugCategoryEnum.OTC_GREEN.getCode());
        dto.setStock(stock);
        return dto;
    }
}
