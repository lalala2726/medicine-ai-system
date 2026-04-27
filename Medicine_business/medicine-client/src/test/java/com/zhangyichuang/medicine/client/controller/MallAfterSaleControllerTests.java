package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.AfterSaleApplyRequest;
import com.zhangyichuang.medicine.client.model.request.AfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleApplyResultVo;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleEligibilityVo;
import com.zhangyichuang.medicine.client.service.MallAfterSaleService;
import com.zhangyichuang.medicine.model.enums.AfterSaleReasonEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleScopeEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MallAfterSaleControllerTests {

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @InjectMocks
    private MallAfterSaleController controller;

    @Test
    void applyAfterSale_ShouldReturnUnifiedResult() {
        AfterSaleApplyRequest request = new AfterSaleApplyRequest();
        request.setOrderNo("O202511130001");
        request.setScope(AfterSaleScopeEnum.ITEM);
        request.setOrderItemId(9L);
        request.setAfterSaleType(AfterSaleTypeEnum.REFUND_ONLY);
        request.setRefundAmount(new BigDecimal("19.90"));
        request.setApplyReason(AfterSaleReasonEnum.DAMAGED);

        AfterSaleApplyResultVo resultVo = AfterSaleApplyResultVo.builder()
                .orderNo("O202511130001")
                .requestedScope("ITEM")
                .resolvedScope("ORDER")
                .afterSaleNos(List.of("AS202511130001"))
                .orderItemIds(List.of(9L))
                .build();
        when(mallAfterSaleService.applyAfterSale(request)).thenReturn(resultVo);

        var result = controller.applyAfterSale(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("ORDER", result.getData().getResolvedScope());
        assertEquals(List.of("AS202511130001"), result.getData().getAfterSaleNos());
        verify(mallAfterSaleService).applyAfterSale(request);
    }

    @Test
    void getAfterSaleEligibility_ShouldReturnEligibilityVo() {
        AfterSaleEligibilityRequest request = new AfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        request.setScope(AfterSaleScopeEnum.ORDER);

        AfterSaleEligibilityVo resultVo = AfterSaleEligibilityVo.builder()
                .orderNo("O202511130001")
                .requestedScope("ORDER")
                .resolvedScope("ORDER")
                .eligible(true)
                .selectedRefundableAmount(new BigDecimal("59.80"))
                .totalRefundableAmount(new BigDecimal("59.80"))
                .items(List.of(
                        AfterSaleEligibilityVo.ItemEligibility.builder()
                                .orderItemId(9L)
                                .refundableAmount(new BigDecimal("19.90"))
                                .eligible(true)
                                .reasonCode("ELIGIBLE")
                                .reasonMessage("该商品满足售后条件")
                                .build()
                ))
                .build();
        when(mallAfterSaleService.getAfterSaleEligibility(request)).thenReturn(resultVo);

        var result = controller.getAfterSaleEligibility(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(new BigDecimal("59.80"), result.getData().getTotalRefundableAmount());
        assertEquals(1, result.getData().getItems().size());
        verify(mallAfterSaleService).getAfterSaleEligibility(request);
    }

    @Test
    void refundOrderEndpoint_ShouldNotExist() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/mall/order/after_sale/refund/order"))
                .andExpect(status().isNotFound());
    }
}
