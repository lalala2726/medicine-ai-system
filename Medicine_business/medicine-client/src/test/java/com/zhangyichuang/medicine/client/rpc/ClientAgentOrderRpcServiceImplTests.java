package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.model.vo.OrderDetailVo;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.model.dto.ClientAgentOrderCancelCheckDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentOrderTimelineDto;
import com.zhangyichuang.medicine.model.enums.OrderItemAfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
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
class ClientAgentOrderRpcServiceImplTests {

    @Mock
    private MallOrderService mallOrderService;

    @InjectMocks
    private ClientAgentOrderRpcServiceImpl service;

    @Test
    void getOrderDetail_ShouldMapScopedOrderDetail() {
        when(mallOrderService.getOrderDetail("O202511130001", 88L)).thenReturn(createOrderDetail());

        var result = service.getOrderDetail("O202511130001", 88L);

        verify(mallOrderService).getOrderDetail("O202511130001", 88L);
        assertNotNull(result);
        assertEquals("O202511130001", result.getOrderNo());
        assertEquals("IN_PROGRESS", result.getAfterSaleFlag());
        assertEquals("售后中", result.getAfterSaleFlagName());
        assertEquals("张三", result.getReceiverInfo().getReceiverName());
        assertEquals(1, result.getItems().size());
        assertEquals("运输中", result.getShippingInfo().getShippingStatusName());
    }

    @Test
    void getOrderShipping_ShouldMapShippingNodes() {
        when(mallOrderService.getOrderShipping("O202511130001", 88L)).thenReturn(createOrderShipping());

        var result = service.getOrderShipping("O202511130001", 88L);

        verify(mallOrderService).getOrderShipping("O202511130001", 88L);
        assertNotNull(result);
        assertEquals("顺丰", result.getLogisticsCompany());
        assertEquals("IN_TRANSIT", result.getShippingStatus());
        assertEquals(1, result.getNodes().size());
        assertEquals("上海", result.getNodes().getFirst().getLocation());
    }

    @Test
    void getOrderTimeline_ShouldDelegateToService() {
        ClientAgentOrderTimelineDto timelineDto = ClientAgentOrderTimelineDto.builder()
                .orderNo("O202511130001")
                .build();
        when(mallOrderService.getOrderTimeline("O202511130001", 88L)).thenReturn(timelineDto);

        var result = service.getOrderTimeline("O202511130001", 88L);

        verify(mallOrderService).getOrderTimeline("O202511130001", 88L);
        assertEquals("O202511130001", result.getOrderNo());
    }

    @Test
    void checkOrderCancelable_ShouldDelegateToService() {
        ClientAgentOrderCancelCheckDto checkDto = ClientAgentOrderCancelCheckDto.builder()
                .orderNo("O202511130001")
                .reasonCode("CAN_CANCEL")
                .build();
        when(mallOrderService.checkOrderCancelable("O202511130001", 88L)).thenReturn(checkDto);

        var result = service.checkOrderCancelable("O202511130001", 88L);

        verify(mallOrderService).checkOrderCancelable("O202511130001", 88L);
        assertEquals("CAN_CANCEL", result.getReasonCode());
    }

    private OrderDetailVo createOrderDetail() {
        OrderDetailVo.OrderItemDetailVo item = OrderDetailVo.OrderItemDetailVo.builder()
                .id(1L)
                .productId(9L)
                .productName("999感冒灵颗粒")
                .quantity(2)
                .price(new BigDecimal("29.90"))
                .totalPrice(new BigDecimal("59.80"))
                .afterSaleStatus("NONE")
                .afterSaleStatusName("无售后")
                .build();

        OrderDetailVo.ReceiverInfo receiverInfo = OrderDetailVo.ReceiverInfo.builder()
                .receiverName("张三")
                .receiverPhone("13800000000")
                .receiverDetail("上海市浦东新区")
                .build();

        OrderDetailVo.ShippingInfo shippingInfo = OrderDetailVo.ShippingInfo.builder()
                .logisticsCompany("顺丰")
                .trackingNumber("SF1234567890")
                .shippingStatus("IN_TRANSIT")
                .shippingStatusName("运输中")
                .shipTime(new Date())
                .build();

        return OrderDetailVo.builder()
                .id(1L)
                .orderNo("O202511130001")
                .orderStatus("PENDING_PAYMENT")
                .orderStatusName("待支付")
                .payType("WALLET")
                .payTypeName("WALLET")
                .deliveryType("EXPRESS")
                .deliveryTypeName("快递配送")
                .totalAmount(new BigDecimal("59.80"))
                .afterSaleFlag(OrderItemAfterSaleStatusEnum.IN_PROGRESS)
                .receiverInfo(receiverInfo)
                .items(List.of(item))
                .shippingInfo(shippingInfo)
                .build();
    }

    private OrderShippingVo createOrderShipping() {
        OrderShippingVo.ReceiverInfo receiverInfo = OrderShippingVo.ReceiverInfo.builder()
                .receiverName("张三")
                .receiverPhone("13800000000")
                .receiverDetail("上海市浦东新区")
                .deliveryType("EXPRESS")
                .deliveryTypeName("快递配送")
                .build();
        OrderShippingVo.ShippingNode node = OrderShippingVo.ShippingNode.builder()
                .time("2025-11-13 12:00:00")
                .content("快件已到达上海转运中心")
                .location("上海")
                .build();
        return OrderShippingVo.builder()
                .orderId(1L)
                .orderNo("O202511130001")
                .orderStatus("PENDING_RECEIPT")
                .orderStatusName("待收货")
                .logisticsCompany("顺丰")
                .trackingNumber("SF1234567890")
                .status("IN_TRANSIT")
                .statusName("运输中")
                .receiverInfo(receiverInfo)
                .nodes(List.of(node))
                .build();
    }
}
