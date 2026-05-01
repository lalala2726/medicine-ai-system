package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.admin.service.MallOrderShippingService;
import com.zhangyichuang.medicine.admin.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderShipping;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import com.zhangyichuang.medicine.model.enums.ShippingStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentOrderRpcServiceImplTests {

    @Mock
    private MallOrderService mallOrderService;

    @Mock
    private MallOrderTimelineService mallOrderTimelineService;

    @Mock
    private MallOrderShippingService mallOrderShippingService;

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @InjectMocks
    private AdminAgentOrderRpcServiceImpl rpcService;

    /**
     * 测试目的：验证订单 context 聚合包含完整收货地址、物流节点和订单时间线。
     * 预期结果：返回 map key 为订单号，并保留查询到的全部时间线和物流节点。
     */
    @SuppressWarnings("unchecked")
    @Test
    void getOrderContextsByOrderNos_ShouldBuildFullOrderContext() {
        LambdaQueryChainWrapper<MallOrder> orderQuery = mock(LambdaQueryChainWrapper.class);
        when(mallOrderService.lambdaQuery()).thenReturn(orderQuery);
        when(orderQuery.in(any(), anyCollection())).thenReturn(orderQuery);
        when(orderQuery.list()).thenReturn(List.of(createOrder()));

        LambdaQueryChainWrapper<MallOrderTimeline> timelineQuery = mock(LambdaQueryChainWrapper.class);
        when(mallOrderTimelineService.lambdaQuery()).thenReturn(timelineQuery);
        when(timelineQuery.in(any(), anyCollection())).thenReturn(timelineQuery);
        when(timelineQuery.orderByDesc((SFunction<MallOrderTimeline, ?>) any())).thenReturn(timelineQuery);
        when(timelineQuery.list()).thenReturn(createTimeline());

        LambdaQueryChainWrapper<MallOrderShipping> shippingQuery = mock(LambdaQueryChainWrapper.class);
        when(mallOrderShippingService.lambdaQuery()).thenReturn(shippingQuery);
        when(shippingQuery.in(any(), anyCollection())).thenReturn(shippingQuery);
        when(shippingQuery.list()).thenReturn(List.of(createShipping()));

        LambdaQueryChainWrapper<MallAfterSale> afterSaleQuery = mock(LambdaQueryChainWrapper.class);
        when(mallAfterSaleService.lambdaQuery()).thenReturn(afterSaleQuery);
        when(afterSaleQuery.in(any(), anyCollection())).thenReturn(afterSaleQuery);
        when(afterSaleQuery.list()).thenReturn(List.of(MallAfterSale.builder()
                .orderNo("O20251108001")
                .build()));

        when(mallOrderService.getOrderByOrderNo(List.of("O20251108001"))).thenReturn(List.of(createOrderDetail()));

        Map<String, OrderContextDto> result = rpcService.getOrderContextsByOrderNos(List.of("O20251108001"));

        assertNotNull(result);
        assertTrue(result.containsKey("O20251108001"));
        OrderContextDto context = result.get("O20251108001");
        assertEquals("O20251108001", context.getOrderNo());
        assertEquals("待收货", context.getStatusText());
        assertEquals("上海市浦东新区张江路1号", context.getReceiverSummary().getReceiverAddress());
        assertEquals(3, context.getProductSummary().getProductCount());
        assertEquals(2, context.getProductSummary().getProductLineCount());
        assertEquals(2, context.getTimeline().size());
        assertEquals(2, context.getShippingSummary().getNodes().size());
        assertTrue(context.getAiHints().getHasAfterSale());
        verify(mallOrderService).getOrderByOrderNo(List.of("O20251108001"));
        verify(mallOrderService, never()).orderDetail(anyLong());
        verify(mallOrderTimelineService, never()).getTimelineByOrderId(anyLong());
        verify(mallOrderService, never()).getOrderShipping(anyLong());
    }

    /**
     * 测试目的：验证订单 context 批量上限按参数异常处理。
     * 预期结果：超过 20 个订单号时直接抛出 ParamException。
     */
    @Test
    void getOrderContextsByOrderNos_WhenOverLimit_ShouldThrowParamException() {
        List<String> orderNos = IntStream.range(0, 21)
                .mapToObj(index -> "O20251108" + index)
                .toList();

        assertThrows(ParamException.class, () -> rpcService.getOrderContextsByOrderNos(orderNos));
        verifyNoInteractions(mallOrderService, mallOrderTimelineService, mallOrderShippingService, mallAfterSaleService);
    }

    /**
     * 功能描述：构造订单实体模拟数据，供 context 聚合测试复用。
     *
     * @return 返回订单实体
     */
    private MallOrder createOrder() {
        return MallOrder.builder()
                .id(1L)
                .orderNo("O20251108001")
                .orderStatus(OrderStatusEnum.PENDING_RECEIPT.getType())
                .payType("WALLET")
                .totalAmount(new BigDecimal("120.00"))
                .payAmount(new BigDecimal("110.00"))
                .freightAmount(new BigDecimal("10.00"))
                .receiverName("张三")
                .receiverPhone("13800000000")
                .receiverDetail("上海市浦东新区张江路1号")
                .deliveryType("EXPRESS")
                .build();
    }

    /**
     * 功能描述：构造订单详情模拟数据，供商品摘要测试复用。
     *
     * @return 返回订单详情 DTO
     */
    private OrderDetailDto createOrderDetail() {
        return OrderDetailDto.builder()
                .orderInfo(OrderDetailDto.OrderInfo.builder()
                        .orderNo("O20251108001")
                        .build())
                .deliveryInfo(OrderDetailDto.DeliveryInfo.builder()
                        .deliveryMethod("EXPRESS")
                        .build())
                .productInfo(List.of(
                        OrderDetailDto.ProductInfo.builder()
                                .productName("感冒药")
                                .productQuantity(1)
                                .build(),
                        OrderDetailDto.ProductInfo.builder()
                                .productName("维生素C")
                                .productQuantity(2)
                                .build()
                ))
                .build();
    }

    /**
     * 功能描述：构造完整订单时间线模拟数据。
     *
     * @return 返回订单时间线列表
     */
    private List<MallOrderTimeline> createTimeline() {
        MallOrderTimeline created = createTimelineItem(1L, "ORDER_CREATED", "SUCCESS", "用户创建订单");
        MallOrderTimeline shipped = createTimelineItem(2L, "SHIP", "SUCCESS", "订单已发货");
        return List.of(created, shipped);
    }

    /**
     * 功能描述：构造单条订单时间线模拟数据。
     *
     * @param id          时间线 ID
     * @param eventType   事件类型
     * @param eventStatus 事件状态
     * @param description 事件描述
     * @return 返回订单时间线实体
     */
    private MallOrderTimeline createTimelineItem(Long id, String eventType, String eventStatus, String description) {
        MallOrderTimeline timeline = new MallOrderTimeline();
        timeline.setId(id);
        timeline.setOrderId(1L);
        timeline.setEventType(eventType);
        timeline.setEventStatus(eventStatus);
        timeline.setOperatorType("SYSTEM");
        timeline.setDescription(description);
        timeline.setCreatedTime(new Date());
        return timeline;
    }

    /**
     * 功能描述：构造订单物流模拟数据，供完整物流节点测试复用。
     *
     * @return 返回订单物流实体
     */
    private MallOrderShipping createShipping() {
        return MallOrderShipping.builder()
                .orderId(1L)
                .shippingCompany("顺丰速运")
                .shippingNo("SF1234567890")
                .status(ShippingStatusEnum.IN_TRANSIT.getType())
                .deliverTime(new Date())
                .shippingInfo("""
                        [
                          {"time":"2026-01-01 10:00:00","content":"快件已揽收","location":"上海"},
                          {"time":"2026-01-01 12:00:00","content":"快件已发出","location":"上海"}
                        ]
                        """)
                .build();
    }
}
