package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.service.MallAfterSaleService;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAgentAfterSaleRpcServiceImplTests {

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @InjectMocks
    private ClientAgentAfterSaleRpcServiceImpl service;

    @Test
    void getAfterSaleDetail_ShouldMapNestedProductAndTimeline() {
        when(mallAfterSaleService.getAfterSaleDetail("AS202511130001", 66L)).thenReturn(createDetailVo());

        AfterSaleDetailDto result = service.getAfterSaleDetail("AS202511130001", 66L);

        verify(mallAfterSaleService).getAfterSaleDetail("AS202511130001", 66L);
        assertNotNull(result);
        assertEquals("AS202511130001", result.getAfterSaleNo());
        assertEquals("待审核", result.getAfterSaleStatusName());
        assertEquals("999感冒灵颗粒", result.getProductInfo().getProductName());
        assertEquals(1, result.getTimeline().size());
        assertEquals("退款申请", result.getTimeline().getFirst().getEventTypeName());
    }

    @Test
    void checkAfterSaleEligibility_ShouldDelegateToService() {
        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        ClientAgentAfterSaleEligibilityDto dto = ClientAgentAfterSaleEligibilityDto.builder()
                .orderNo("O202511130001")
                .eligible(true)
                .build();
        when(mallAfterSaleService.checkAfterSaleEligibility(request, 66L)).thenReturn(dto);

        var result = service.checkAfterSaleEligibility(request, 66L);

        verify(mallAfterSaleService).checkAfterSaleEligibility(request, 66L);
        assertEquals("O202511130001", result.getOrderNo());
        assertEquals(true, result.getEligible());
    }

    @Test
    void checkAfterSaleEligibility_ShouldPreserveExpiredReason() {
        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        ClientAgentAfterSaleEligibilityDto dto = ClientAgentAfterSaleEligibilityDto.builder()
                .orderNo("O202511130001")
                .eligible(false)
                .reasonCode("AFTER_SALE_EXPIRED")
                .reasonMessage("订单确认收货已超过3个月，无法申请售后")
                .build();
        when(mallAfterSaleService.checkAfterSaleEligibility(request, 66L)).thenReturn(dto);

        var result = service.checkAfterSaleEligibility(request, 66L);

        assertEquals("AFTER_SALE_EXPIRED", result.getReasonCode());
        assertEquals("订单确认收货已超过3个月，无法申请售后", result.getReasonMessage());
    }

    private AfterSaleDetailVo createDetailVo() {
        AfterSaleTimelineVo timeline = AfterSaleTimelineVo.builder()
                .id(1L)
                .eventType("REFUND_APPLY")
                .eventTypeName("退款申请")
                .eventStatus("PENDING")
                .operatorType("USER")
                .operatorTypeName("用户")
                .description("用户申请退款")
                .createTime(new Date())
                .build();

        AfterSaleDetailVo.ProductInfo productInfo = AfterSaleDetailVo.ProductInfo.builder()
                .productId(9L)
                .productName("999感冒灵颗粒")
                .productImage("https://example.com/product.jpg")
                .productPrice(new BigDecimal("29.90"))
                .quantity(1)
                .totalPrice(new BigDecimal("29.90"))
                .build();

        return AfterSaleDetailVo.builder()
                .id(1L)
                .afterSaleNo("AS202511130001")
                .orderNo("O202511130001")
                .userId(66L)
                .afterSaleType("REFUND_ONLY")
                .afterSaleTypeName("仅退款")
                .afterSaleStatus("PENDING")
                .afterSaleStatusName("待审核")
                .refundAmount(new BigDecimal("29.90"))
                .productInfo(productInfo)
                .timeline(List.of(timeline))
                .build();
    }
}
