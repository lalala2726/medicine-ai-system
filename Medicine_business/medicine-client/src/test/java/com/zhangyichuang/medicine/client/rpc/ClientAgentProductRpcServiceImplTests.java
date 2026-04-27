package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.AssistantProductPurchaseCardsVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchVo;
import com.zhangyichuang.medicine.client.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.enums.DrugCategoryEnum;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientAgentProductRpcServiceImplTests {

    @Mock
    private MallProductService mallProductService;

    @InjectMocks
    private ClientAgentProductRpcServiceImpl service;

    @Test
    void searchProducts_ShouldClampPageSizeAndMapRows() {
        ClientAgentProductSearchRequest request = new ClientAgentProductSearchRequest();
        request.setKeyword("感冒灵");
        request.setCategoryName("感冒药");
        request.setUsage("缓解感冒");
        request.setPageNum(0);
        request.setPageSize(99);

        PageResult<MallProductSearchVo> searchResult = new PageResult<>(1L, 20L, 1L, List.of(
                MallProductSearchVo.builder()
                        .productId(1L)
                        .productName("999感冒灵颗粒")
                        .cover("https://example.com/product.jpg")
                        .price(new BigDecimal("29.90"))
                        .build()
        ));
        when(mallProductService.search(any(MallProductSearchRequest.class))).thenReturn(searchResult);

        PageResult<?> result = service.searchProducts(request);

        ArgumentCaptor<MallProductSearchRequest> captor = ArgumentCaptor.forClass(MallProductSearchRequest.class);
        verify(mallProductService).search(captor.capture());
        assertEquals("感冒灵", captor.getValue().getKeyword());
        assertEquals("感冒药", captor.getValue().getCategoryName());
        assertEquals("缓解感冒", captor.getValue().getEfficacy());
        assertEquals(1, captor.getValue().getPageNum());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    @Test
    void searchProducts_WhenSearchServiceReturnsNull_ShouldReturnEmptyPage() {
        when(mallProductService.search(any(MallProductSearchRequest.class))).thenReturn(null);

        PageResult<?> result = service.searchProducts(null);

        assertEquals(1L, result.getPageNum());
        assertEquals(10L, result.getPageSize());
        assertEquals(0L, result.getTotal());
        assertTrue(result.getRows().isEmpty());
    }

    @Test
    void getProductDetails_ShouldUseReadOnlyDrugQuery() {
        MallProductDetailDto detail = new MallProductDetailDto();
        detail.setId(1L);
        detail.setStatus(1);
        when(mallProductService.getProductAndDrugInfoById(1L)).thenReturn(detail);

        List<ClientAgentProductDetailDto> result = service.getProductDetails(List.of(1L));

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getProductId());
        verify(mallProductService).getProductAndDrugInfoById(1L);
        verify(mallProductService, never()).getMallProductDetail(1L);
    }

    @Test
    void getProductDetails_ShouldExtractDrugDetailFields() {
        DrugDetailDto drugDetail = DrugDetailDto.builder()
                .commonName("复方感冒灵颗粒")
                .composition("三叉苦")
                .packaging("10g*9袋")
                .usageMethod("开水冲服")
                .precautions("详见说明书")
                .build();
        MallProductDetailDto detail = new MallProductDetailDto();
        detail.setId(1L);
        detail.setName("999感冒灵颗粒");
        detail.setCategoryNames(List.of("感冒药"));
        detail.setUnit("盒");
        detail.setStatus(1);
        detail.setDrugDetail(drugDetail);
        when(mallProductService.getProductAndDrugInfoById(1L)).thenReturn(detail);

        ClientAgentProductDetailDto result = service.getProductDetails(List.of(1L)).getFirst();

        assertEquals(1L, result.getProductId());
        assertEquals("999感冒灵颗粒", result.getProductName());
        assertEquals("10g*9袋", result.getPackaging());
        assertEquals("三叉苦", result.getComposition());
        assertEquals("开水冲服", result.getUsageMethod());
    }

    @Test
    void getProductDetails_WhenProductOffShelf_ShouldThrowNotFound() {
        MallProductDetailDto detail = new MallProductDetailDto();
        detail.setId(1L);
        detail.setStatus(0);
        when(mallProductService.getProductAndDrugInfoById(1L)).thenReturn(detail);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> service.getProductDetails(List.of(1L))
        );

        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    void getProductCards_ShouldMapResultAndPreserveOrder() {
        List<Long> productIds = List.of(102L, 101L);
        AssistantProductPurchaseCardsVo cards = AssistantProductPurchaseCardsVo.builder()
                .totalPrice("36.70")
                .items(List.of(
                        AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo.builder()
                                .id("102")
                                .name("维生素C咀嚼片")
                                .image("https://example.com/102.png")
                                .price("19.90")
                                .spec("60片/瓶")
                                .efficacy("补充维生素C")
                                .drugCategory(DrugCategoryEnum.OTC_GREEN.getCode())
                                .stock(98)
                                .build(),
                        AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo.builder()
                                .id("101")
                                .name("布洛芬缓释胶囊")
                                .image("https://example.com/101.png")
                                .price("16.80")
                                .spec("24粒/盒")
                                .efficacy("缓解发热、头痛")
                                .drugCategory(DrugCategoryEnum.OTC_GREEN.getCode())
                                .stock(56)
                                .build()
                ))
                .build();
        when(mallProductService.getAssistantProductPurchaseCards(productIds)).thenReturn(cards);

        ClientAgentProductCardsDto result = service.getProductCards(productIds);

        assertEquals("36.70", result.getTotalPrice());
        assertEquals(2, result.getItems().size());
        assertEquals("102", result.getItems().get(0).getId());
        assertEquals("101", result.getItems().get(1).getId());
        verify(mallProductService).getAssistantProductPurchaseCards(productIds);
    }

    @Test
    void getProductCards_WhenServiceReturnsNull_ShouldReturnEmptyCards() {
        when(mallProductService.getAssistantProductPurchaseCards(List.of(999L))).thenReturn(null);

        ClientAgentProductCardsDto result = service.getProductCards(List.of(999L));

        assertEquals("0.00", result.getTotalPrice());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void getProductPurchaseCards_ShouldMapResultAndCalculateTotalPrice() {
        List<ClientAgentProductPurchaseQueryDto> items = List.of(
                ClientAgentProductPurchaseQueryDto.builder().productId(101L).quantity(2).build(),
                ClientAgentProductPurchaseQueryDto.builder().productId(205L).quantity(1).build()
        );
        AssistantProductPurchaseCardsVo cards = AssistantProductPurchaseCardsVo.builder()
                .totalPrice("36.70")
                .items(List.of(
                        AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo.builder()
                                .id("101")
                                .name("布洛芬缓释胶囊")
                                .image("https://example.com/101.png")
                                .price("16.80")
                                .spec("24粒/盒")
                                .efficacy("缓解发热、头痛")
                                .drugCategory(DrugCategoryEnum.OTC_GREEN.getCode())
                                .stock(56)
                                .build(),
                        AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo.builder()
                                .id("205")
                                .name("维生素C咀嚼片")
                                .image("https://example.com/205.png")
                                .price("19.90")
                                .spec("60片/瓶")
                                .efficacy("补充维生素C")
                                .drugCategory(DrugCategoryEnum.OTC_GREEN.getCode())
                                .stock(98)
                                .build()
                ))
                .build();
        when(mallProductService.getAssistantProductPurchaseCards(List.of(101L, 205L))).thenReturn(cards);

        ClientAgentProductPurchaseCardsDto result = service.getProductPurchaseCards(items);

        assertEquals(new BigDecimal("53.50"), result.getTotalPrice());
        assertEquals(2, result.getItems().size());
        assertEquals("101", result.getItems().get(0).getId());
        assertEquals(new BigDecimal("16.80"), result.getItems().get(0).getPrice());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertEquals("205", result.getItems().get(1).getId());
        assertEquals(1, result.getItems().get(1).getQuantity());
        verify(mallProductService).getAssistantProductPurchaseCards(List.of(101L, 205L));
    }

    @Test
    void getProductPurchaseCards_WhenServiceReturnsNull_ShouldReturnEmptyCards() {
        List<ClientAgentProductPurchaseQueryDto> items = List.of(
                ClientAgentProductPurchaseQueryDto.builder().productId(999L).quantity(1).build()
        );
        when(mallProductService.getAssistantProductPurchaseCards(List.of(999L))).thenReturn(null);

        ClientAgentProductPurchaseCardsDto result = service.getProductPurchaseCards(items);

        assertEquals(new BigDecimal("0.00"), result.getTotalPrice());
        assertTrue(result.getItems().isEmpty());
    }
}
