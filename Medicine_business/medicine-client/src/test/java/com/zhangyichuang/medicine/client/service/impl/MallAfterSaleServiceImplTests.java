package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.client.mapper.MallAfterSaleMapper;
import com.zhangyichuang.medicine.client.model.request.AfterSaleApplyRequest;
import com.zhangyichuang.medicine.client.model.request.AfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.client.service.*;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallAfterSaleServiceImplTests {

    @Mock
    private MallAfterSaleMapper mallAfterSaleMapper;
    @Mock
    private MallAfterSaleTimelineService mallAfterSaleTimelineService;
    @Mock
    private MallOrderItemService mallOrderItemService;
    @Mock
    private MallOrderTimelineService mallOrderTimelineService;
    @Mock
    private MallOrderService mallOrderService;
    @Mock
    private UserService userService;

    @Spy
    @InjectMocks
    private MallAfterSaleServiceImpl service;

    /**
     * 初始化测试所需的 MyBatis-Plus 实体元数据。
     */
    @BeforeEach
    void setUp() {
        initializeTableInfo(MallAfterSale.class);
        initializeTableInfo(MallOrder.class);
        initializeTableInfo(MallOrderItem.class);
    }

    @Test
    void getAfterSaleDetail_WhenAfterSaleDoesNotBelongToUser_ShouldThrowNotFound() {
        TestLambdaQueryChainWrapper<MallAfterSale> query = createQueryWrapper(MallAfterSale.class);
        query.setOneResult(null);
        doReturn(query).when(service).lambdaQuery();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.getAfterSaleDetail("AS202511130001", 66L));

        assertEquals(ResponseCode.RESULT_IS_NULL.getCode(), exception.getCode());
        assertEquals("售后申请不存在", exception.getMessage());
    }

    @Test
    void getAfterSaleDetail_ShouldAssembleProductInfoAndTimeline() {
        TestLambdaQueryChainWrapper<MallAfterSale> query = createQueryWrapper(MallAfterSale.class);
        query.setOneResult(createAfterSale());
        doReturn(query).when(service).lambdaQuery();
        when(userService.getById(66L)).thenReturn(createUser());
        when(mallOrderItemService.getById(9L)).thenReturn(createOrderItem(9L, new BigDecimal("29.90"), BigDecimal.ZERO));
        when(mallAfterSaleTimelineService.getTimelineList(1L)).thenReturn(List.of(createTimeline()));

        var result = service.getAfterSaleDetail("AS202511130001", 66L);

        assertNotNull(result);
        assertEquals("AS202511130001", result.getAfterSaleNo());
        assertEquals("测试用户", result.getUserNickname());
        assertEquals("待审核", result.getAfterSaleStatusName());
        assertNotNull(result.getProductInfo());
        assertEquals("999感冒灵颗粒-9", result.getProductInfo().getProductName());
        assertEquals(1, result.getTimeline().size());
        verify(mallAfterSaleTimelineService).getTimelineList(1L);
    }

    @Test
    void checkAfterSaleEligibility_WhenOrderNotFound_ShouldReturnIneligibleResult() {
        TestLambdaQueryChainWrapper<MallOrder> orderQuery = createQueryWrapper(MallOrder.class);
        orderQuery.setOneResult(null);
        when(mallOrderService.lambdaQuery()).thenReturn(orderQuery);

        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");

        var result = service.checkAfterSaleEligibility(request, 66L);

        assertFalse(result.getEligible());
        assertEquals("ORDER", result.getScope());
        assertEquals("ORDER_NOT_FOUND", result.getReasonCode());
        assertEquals("订单不存在或无权访问", result.getReasonMessage());
    }

    @Test
    void checkAfterSaleEligibility_ForOrderItem_ShouldReturnRefundableAmountWhenEligible() {
        mockOrderQuery(createCompletedOrder("O202511130001", new BigDecimal("59.80"), BigDecimal.ZERO, daysAgo(10)));
        mockOrderItemsQuery(List.of(
                createOrderItem(9L, new BigDecimal("29.90"), new BigDecimal("10.00")),
                createOrderItem(10L, new BigDecimal("29.90"), BigDecimal.ZERO)
        ));
        TestLambdaQueryChainWrapper<MallAfterSale> activeQuery1 = createQueryWrapper(MallAfterSale.class);
        TestLambdaQueryChainWrapper<MallAfterSale> activeQuery2 = createQueryWrapper(MallAfterSale.class);
        activeQuery1.setCountResult(0L);
        activeQuery2.setCountResult(0L);
        mockAfterSaleQuerySequence(activeQuery1, activeQuery2);

        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        request.setOrderItemId(9L);

        var result = service.checkAfterSaleEligibility(request, 66L);

        assertTrue(result.getEligible());
        assertEquals("ITEM", result.getScope());
        assertEquals("ELIGIBLE", result.getReasonCode());
        assertEquals(new BigDecimal("19.90"), result.getRefundableAmount());
    }

    @Test
    void checkAfterSaleEligibility_WhenCompletedOrderExpired_ShouldReturnExpiredReason() {
        mockOrderQuery(createCompletedOrder("O202511130001", new BigDecimal("29.90"), BigDecimal.ZERO, daysAgo(95)));
        mockOrderItemsQuery(List.of(createOrderItem(9L, new BigDecimal("29.90"), BigDecimal.ZERO)));

        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");

        var result = service.checkAfterSaleEligibility(request, 66L);

        assertFalse(result.getEligible());
        assertEquals("AFTER_SALE_EXPIRED", result.getReasonCode());
        assertEquals("订单确认收货已超过3个月，无法申请售后", result.getReasonMessage());
    }

    @Test
    void getAfterSaleEligibility_ShouldReturnItemsAndAmounts() {
        doReturn(66L).when(service).getUserId();
        mockOrderQuery(createCompletedOrder("O202511130001", new BigDecimal("59.80"), BigDecimal.ZERO, daysAgo(7)));
        mockOrderItemsQuery(List.of(
                createOrderItem(9L, new BigDecimal("19.90"), BigDecimal.ZERO),
                createOrderItem(10L, new BigDecimal("39.90"), BigDecimal.ZERO)
        ));
        TestLambdaQueryChainWrapper<MallAfterSale> activeQuery1 = createQueryWrapper(MallAfterSale.class);
        TestLambdaQueryChainWrapper<MallAfterSale> activeQuery2 = createQueryWrapper(MallAfterSale.class);
        activeQuery1.setCountResult(0L);
        activeQuery2.setCountResult(0L);
        mockAfterSaleQuerySequence(activeQuery1, activeQuery2);

        AfterSaleEligibilityRequest request = new AfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        request.setScope(AfterSaleScopeEnum.ORDER);

        var result = service.getAfterSaleEligibility(request);

        assertTrue(result.getEligible());
        assertEquals("ORDER", result.getRequestedScope());
        assertEquals("ORDER", result.getResolvedScope());
        assertEquals(new BigDecimal("59.80"), result.getTotalRefundableAmount());
        assertEquals(new BigDecimal("59.80"), result.getSelectedRefundableAmount());
        assertEquals(2, result.getItems().size());
        assertEquals(new BigDecimal("19.90"), result.getItems().getFirst().getRefundableAmount());
    }

    @Test
    void applyAfterSale_ShouldAutoConvertSingleItemToOrderScopeAndIgnoreClientRefundAmount() {
        doReturn(66L).when(service).getUserId();
        doReturn("tester").when(service).getUsername();
        mockOrderQuery(createCompletedOrder("O202511130001", new BigDecimal("29.90"), BigDecimal.ZERO, daysAgo(5)));
        mockOrderItemsQuery(List.of(createOrderItem(9L, new BigDecimal("29.90"), new BigDecimal("10.00"))));
        TestLambdaQueryChainWrapper<MallAfterSale> activeQuery = createQueryWrapper(MallAfterSale.class);
        TestLambdaQueryChainWrapper<MallAfterSale> existingQuery = createQueryWrapper(MallAfterSale.class);
        activeQuery.setCountResult(0L);
        existingQuery.setOneResult(null);
        mockAfterSaleQuerySequence(activeQuery, existingQuery);
        doAnswer(invocation -> {
            MallAfterSale afterSale = invocation.getArgument(0);
            afterSale.setId(1L);
            return true;
        }).when(service).save(any(MallAfterSale.class));
        when(mallOrderItemService.updateById(any(MallOrderItem.class))).thenReturn(true);
        when(mallOrderService.updateById(any(MallOrder.class))).thenReturn(true);

        AfterSaleApplyRequest request = new AfterSaleApplyRequest();
        request.setOrderNo("O202511130001");
        request.setScope(AfterSaleScopeEnum.ITEM);
        request.setOrderItemId(9L);
        request.setAfterSaleType(AfterSaleTypeEnum.REFUND_ONLY);
        request.setRefundAmount(new BigDecimal("1.00"));
        request.setApplyReason(AfterSaleReasonEnum.DAMAGED);
        request.setApplyDescription("包装破损");

        var result = service.applyAfterSale(request);

        ArgumentCaptor<MallAfterSale> afterSaleCaptor = ArgumentCaptor.forClass(MallAfterSale.class);
        verify(service).save(afterSaleCaptor.capture());
        MallAfterSale savedAfterSale = afterSaleCaptor.getValue();

        assertEquals("ITEM", result.getRequestedScope());
        assertEquals("ORDER", result.getResolvedScope());
        assertEquals(1, result.getAfterSaleNos().size());
        assertEquals(List.of(9L), result.getOrderItemIds());
        assertEquals(savedAfterSale.getAfterSaleNo(), result.getAfterSaleNos().getFirst());
        assertEquals(new BigDecimal("19.90"), savedAfterSale.getRefundAmount());
        assertEquals(ReceiveStatusEnum.RECEIVED.getStatus(), savedAfterSale.getReceiveStatus());
    }

    @Test
    void applyAfterSale_ShouldRejectUnsupportedAfterSaleTypeForOrderScope() {
        AfterSaleApplyRequest request = new AfterSaleApplyRequest();
        request.setOrderNo("O202511130001");
        request.setScope(AfterSaleScopeEnum.ORDER);
        request.setAfterSaleType(AfterSaleTypeEnum.EXCHANGE);
        request.setApplyReason(AfterSaleReasonEnum.DAMAGED);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.applyAfterSale(request));

        assertEquals(ResponseCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertEquals("整单申请仅支持仅退款", exception.getMessage());
    }

    /**
     * 创建查询包装器测试替身。
     *
     * @param entityClass 实体类型
     * @param <T>         实体泛型
     * @return 查询包装器测试替身
     */
    private <T> TestLambdaQueryChainWrapper<T> createQueryWrapper(Class<T> entityClass) {
        return new TestLambdaQueryChainWrapper<>(entityClass);
    }

    /**
     * 初始化 MyBatis-Plus 的实体元数据缓存。
     *
     * @param entityClass 实体类型
     */
    private void initializeTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(),
                entityClass.getSimpleName() + "Mapper");
        builderAssistant.setCurrentNamespace(entityClass.getName() + "Mapper");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 模拟订单查询结果。
     *
     * @param order 订单查询结果
     */
    private void mockOrderQuery(MallOrder order) {
        TestLambdaQueryChainWrapper<MallOrder> orderQuery = createQueryWrapper(MallOrder.class);
        orderQuery.setOneResult(order);
        when(mallOrderService.lambdaQuery()).thenReturn(orderQuery);
    }

    /**
     * 模拟订单项查询结果。
     *
     * @param orderItems 订单项列表
     */
    private void mockOrderItemsQuery(List<MallOrderItem> orderItems) {
        TestLambdaQueryChainWrapper<MallOrderItem> orderItemQuery = createQueryWrapper(MallOrderItem.class);
        orderItemQuery.setListResult(orderItems);
        when(mallOrderItemService.lambdaQuery()).thenReturn(orderItemQuery);
    }

    /**
     * 模拟售后查询包装器的连续调用结果。
     *
     * @param first  第一次调用返回的包装器
     * @param second 第二次调用返回的包装器
     */
    private void mockAfterSaleQuerySequence(TestLambdaQueryChainWrapper<MallAfterSale> first,
                                            TestLambdaQueryChainWrapper<MallAfterSale> second) {
        doReturn(first, second).when(service).lambdaQuery();
    }

    private MallAfterSale createAfterSale() {
        MallAfterSale afterSale = new MallAfterSale();
        afterSale.setId(1L);
        afterSale.setAfterSaleNo("AS202511130001");
        afterSale.setOrderId(2L);
        afterSale.setOrderNo("O202511130001");
        afterSale.setOrderItemId(9L);
        afterSale.setUserId(66L);
        afterSale.setAfterSaleType(AfterSaleTypeEnum.REFUND_ONLY.getType());
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PENDING.getStatus());
        afterSale.setRefundAmount(new BigDecimal("29.90"));
        afterSale.setApplyReason(AfterSaleReasonEnum.DAMAGED.getReason());
        afterSale.setReceiveStatus(ReceiveStatusEnum.RECEIVED.getStatus());
        afterSale.setApplyTime(new Date());
        return afterSale;
    }

    private User createUser() {
        User user = new User();
        user.setId(66L);
        user.setNickname("测试用户");
        return user;
    }

    private MallOrder createCompletedOrder(String orderNo, BigDecimal payAmount, BigDecimal refundPrice, Date receiveTime) {
        MallOrder order = new MallOrder();
        order.setId(2L);
        order.setOrderNo(orderNo);
        order.setUserId(66L);
        order.setPaid(1);
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getType());
        order.setPayAmount(payAmount);
        order.setRefundPrice(refundPrice);
        order.setReceiveTime(receiveTime);
        order.setFinishTime(receiveTime);
        return order;
    }

    private MallOrderItem createOrderItem(Long itemId, BigDecimal totalPrice, BigDecimal refundedAmount) {
        MallOrderItem orderItem = new MallOrderItem();
        orderItem.setId(itemId);
        orderItem.setOrderId(2L);
        orderItem.setProductId(10L + itemId);
        orderItem.setProductName("999感冒灵颗粒-" + itemId);
        orderItem.setImageUrl("https://example.com/product.jpg");
        orderItem.setPrice(totalPrice);
        orderItem.setQuantity(1);
        orderItem.setTotalPrice(totalPrice);
        orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.NONE.getStatus());
        orderItem.setRefundedAmount(refundedAmount);
        return orderItem;
    }

    private Date daysAgo(int days) {
        LocalDateTime dateTime = LocalDateTime.now().minusDays(days);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private AfterSaleTimelineVo createTimeline() {
        return AfterSaleTimelineVo.builder()
                .id(1L)
                .eventType("REFUND_APPLY")
                .eventTypeName("退款申请")
                .eventStatus("PENDING")
                .operatorType("USER")
                .operatorTypeName("用户")
                .description("用户申请退款")
                .createTime(new Date())
                .build();
    }

    /**
     * 售后测试使用的查询包装器替身。
     *
     * @param <T> 实体泛型
     */
    private static final class TestLambdaQueryChainWrapper<T> extends LambdaQueryChainWrapper<T> {

        /**
         * 单条查询结果。
         */
        private T oneResult;

        /**
         * 列表查询结果。
         */
        private List<T> listResult = List.of();

        /**
         * 聚合计数结果。
         */
        private Long countResult = 0L;

        /**
         * 构造查询包装器替身。
         *
         * @param entityClass 实体类型
         */
        private TestLambdaQueryChainWrapper(Class<T> entityClass) {
            super(entityClass);
        }

        /**
         * 设置单条查询结果。
         *
         * @param oneResult 单条查询结果
         */
        private void setOneResult(T oneResult) {
            this.oneResult = oneResult;
        }

        /**
         * 设置列表查询结果。
         *
         * @param listResult 列表查询结果
         */
        private void setListResult(List<T> listResult) {
            this.listResult = listResult == null ? List.of() : listResult;
        }

        /**
         * 设置聚合计数结果。
         *
         * @param countResult 聚合计数结果
         */
        private void setCountResult(Long countResult) {
            this.countResult = countResult == null ? 0L : countResult;
        }

        @Override
        public T one() {
            return oneResult;
        }

        @Override
        public List<T> list() {
            return listResult;
        }

        @Override
        public Long count() {
            return countResult;
        }
    }
}
