package com.zhangyichuang.medicine.agent.controller.client;

import com.zhangyichuang.medicine.agent.advice.AgentResponseDescriptionAdvice;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentOrderService;
import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.core.exception.GlobalExceptionHandel;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentClientOrderControllerTests {

    private final StubClientAgentOrderService orderService = new StubClientAgentOrderService();
    private final AgentClientOrderController controller = new AgentClientOrderController(orderService);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AgentResponseDescriptionAdvice descriptionAdvice =
                new AgentResponseDescriptionAdvice(new AgentVoDescriptionResolver(), objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandel(), descriptionAdvice)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrderDetail_ShouldUseCurrentUserId() {
        setupAuthentication(88L);
        orderService.orderDetail = createOrderDetail();

        var result = controller.getOrderDetail("O202511130001");

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", orderService.capturedOrderNo);
        assertEquals(88L, orderService.capturedUserId);
        assertNotNull(result.getData());
        assertEquals("待支付", result.getData().getOrderStatusName());
        assertEquals("无售后", result.getData().getAfterSaleFlagName());
    }

    @Test
    void getOrderDetail_ShouldAppendMeta() throws Exception {
        setupAuthentication(88L);
        orderService.orderDetail = createOrderDetail();

        mockMvc.perform(get("/agent/client/order/O202511130001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderStatus.value").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.orderStatus.description").value("待支付"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("客户端智能体订单详情"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.orderNo").value("订单编号"));
    }

    @Test
    void getOrderDetail_ShouldRejectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/agent/client/order/O202511130001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户未登录"));
    }

    /**
     * 测试目的：验证订单卡摘要按订单编号和当前登录用户查询。
     * 预期结果：响应成功，service 收到订单编号和当前用户 ID。
     */
    @Test
    void getOrderCardSummary_ShouldUseCurrentUserId() {
        setupAuthentication(88L);
        orderService.orderCardSummary = createOrderCardSummary();

        var result = controller.getOrderCardSummary("O202511130001");

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", orderService.capturedSummaryOrderNo);
        assertEquals(88L, orderService.capturedSummaryUserId);
        assertNotNull(result.getData());
        assertEquals("O202511130001", result.getData().getOrderNo());
    }

    @Test
    void getOrderShipping_ShouldUseCurrentUserId() {
        setupAuthentication(88L);
        orderService.orderShipping = createOrderShipping();

        var result = controller.getOrderShipping("O202511130001");

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", orderService.capturedShippingOrderNo);
        assertEquals(88L, orderService.capturedShippingUserId);
        assertNotNull(result.getData());
        assertEquals("顺丰", result.getData().getLogisticsCompany());
        assertEquals(1, result.getData().getNodes().size());
    }

    @Test
    void getOrderShipping_ShouldAppendMeta() throws Exception {
        setupAuthentication(88L);
        orderService.orderShipping = createOrderShipping();

        mockMvc.perform(get("/agent/client/order/shipping/O202511130001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.shippingStatus.value").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.shippingStatus.description").value("运输中"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("客户端智能体订单物流"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.trackingNumber").value("运单号"));
    }

    @Test
    void getOrderTimeline_ShouldUseCurrentUserId() {
        setupAuthentication(88L);
        orderService.orderTimeline = createOrderTimeline();

        var result = controller.getOrderTimeline("O202511130001");

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", orderService.capturedTimelineOrderNo);
        assertEquals(88L, orderService.capturedTimelineUserId);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTimeline().size());
        assertEquals("订单创建", result.getData().getTimeline().getFirst().getEventTypeName());
    }

    @Test
    void checkOrderCancelable_ShouldUseCurrentUserId() {
        setupAuthentication(88L);
        orderService.orderCancelCheck = ClientAgentOrderCancelCheckDto.builder()
                .orderNo("O202511130001")
                .orderStatus("PENDING_PAYMENT")
                .orderStatusName("待支付")
                .cancelable(true)
                .reasonCode("CAN_CANCEL")
                .reasonMessage("当前订单允许取消")
                .build();

        var result = controller.checkOrderCancelable("O202511130001");

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", orderService.capturedCancelCheckOrderNo);
        assertEquals(88L, orderService.capturedCancelCheckUserId);
        assertNotNull(result.getData());
        assertEquals("CAN_CANCEL", result.getData().getReasonCode());
        assertEquals(true, result.getData().getCancelable());
    }

    private void setupAuthentication(Long userId) {
        AuthUser authUser = AuthUser.builder().id(userId).username("client_user").build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private ClientAgentOrderDetailDto createOrderDetail() {
        ClientAgentOrderDetailDto.OrderItemDetail item = ClientAgentOrderDetailDto.OrderItemDetail.builder()
                .id(1L)
                .productId(9L)
                .productName("999感冒灵颗粒")
                .quantity(2)
                .price(new BigDecimal("29.90"))
                .totalPrice(new BigDecimal("59.80"))
                .afterSaleStatus("NONE")
                .afterSaleStatusName("无售后")
                .build();

        return ClientAgentOrderDetailDto.builder()
                .id(1L)
                .orderNo("O202511130001")
                .orderStatus("PENDING_PAYMENT")
                .orderStatusName("待支付")
                .totalAmount(new BigDecimal("59.80"))
                .afterSaleFlag("NONE")
                .afterSaleFlagName("无售后")
                .createTime(new Date())
                .items(java.util.List.of(item))
                .build();
    }

    /**
     * 功能描述：构造客户端订单卡摘要模拟数据，供摘要接口测试复用。
     *
     * @return 返回订单卡摘要 DTO
     */
    private ClientAgentOrderCardSummaryDto createOrderCardSummary() {
        return ClientAgentOrderCardSummaryDto.builder()
                .orderNo("O202511130001")
                .orderStatus("PENDING_PAYMENT")
                .orderStatusText("待支付")
                .productCount(2)
                .payAmount(new BigDecimal("59.80"))
                .totalAmount(new BigDecimal("59.80"))
                .createTime(new Date())
                .previewProduct(ClientAgentOrderCardSummaryDto.PreviewProduct.builder()
                        .productId(9L)
                        .productName("999感冒灵颗粒")
                        .imageUrl("https://example.com/product.jpg")
                        .build())
                .build();
    }

    private ClientAgentOrderShippingDto createOrderShipping() {
        ClientAgentOrderShippingDto.ReceiverInfo receiverInfo = ClientAgentOrderShippingDto.ReceiverInfo.builder()
                .receiverName("张三")
                .receiverPhone("13800000000")
                .receiverDetail("上海市浦东新区")
                .deliveryType("EXPRESS")
                .deliveryTypeName("快递配送")
                .build();
        ClientAgentOrderShippingDto.ShippingNode node = ClientAgentOrderShippingDto.ShippingNode.builder()
                .time("2025-11-13 12:00:00")
                .content("包裹已到达上海转运中心")
                .location("上海")
                .build();
        return ClientAgentOrderShippingDto.builder()
                .orderId(1L)
                .orderNo("O202511130001")
                .orderStatus("PENDING_RECEIPT")
                .orderStatusName("待收货")
                .shippingStatus("IN_TRANSIT")
                .shippingStatusName("运输中")
                .logisticsCompany("顺丰")
                .trackingNumber("SF1234567890")
                .receiverInfo(receiverInfo)
                .nodes(List.of(node))
                .build();
    }

    private ClientAgentOrderTimelineDto createOrderTimeline() {
        ClientAgentOrderTimelineDto.TimelineNode node = ClientAgentOrderTimelineDto.TimelineNode.builder()
                .id(1L)
                .eventType("ORDER_CREATED")
                .eventTypeName("订单创建")
                .eventStatus("PENDING_PAYMENT")
                .eventStatusName("待支付")
                .operatorType("USER")
                .operatorTypeName("用户")
                .description("用户创建订单")
                .createdTime(new Date())
                .build();
        return ClientAgentOrderTimelineDto.builder()
                .orderId(1L)
                .orderNo("O202511130001")
                .orderStatus("PENDING_RECEIPT")
                .orderStatusName("待收货")
                .timeline(List.of(node))
                .build();
    }

    private static class StubClientAgentOrderService implements ClientAgentOrderService {

        private ClientAgentOrderCardSummaryDto orderCardSummary;
        private ClientAgentOrderDetailDto orderDetail;
        private ClientAgentOrderShippingDto orderShipping;
        private ClientAgentOrderTimelineDto orderTimeline;
        private ClientAgentOrderCancelCheckDto orderCancelCheck;
        private String capturedSummaryOrderNo;
        private Long capturedSummaryUserId;
        private String capturedOrderNo;
        private Long capturedUserId;
        private String capturedShippingOrderNo;
        private Long capturedShippingUserId;
        private String capturedTimelineOrderNo;
        private Long capturedTimelineUserId;
        private String capturedCancelCheckOrderNo;
        private Long capturedCancelCheckUserId;

        @Override
        public ClientAgentOrderCardSummaryDto getOrderCardSummary(String orderNo, Long userId) {
            this.capturedSummaryOrderNo = orderNo;
            this.capturedSummaryUserId = userId;
            return orderCardSummary;
        }

        @Override
        public ClientAgentOrderDetailDto getOrderDetail(String orderNo, Long userId) {
            this.capturedOrderNo = orderNo;
            this.capturedUserId = userId;
            return orderDetail;
        }

        @Override
        public ClientAgentOrderShippingDto getOrderShipping(String orderNo, Long userId) {
            this.capturedShippingOrderNo = orderNo;
            this.capturedShippingUserId = userId;
            return orderShipping;
        }

        @Override
        public ClientAgentOrderTimelineDto getOrderTimeline(String orderNo, Long userId) {
            this.capturedTimelineOrderNo = orderNo;
            this.capturedTimelineUserId = userId;
            return orderTimeline;
        }

        @Override
        public ClientAgentOrderCancelCheckDto checkOrderCancelable(String orderNo, Long userId) {
            this.capturedCancelCheckOrderNo = orderNo;
            this.capturedCancelCheckUserId = userId;
            return orderCancelCheck;
        }
    }
}
