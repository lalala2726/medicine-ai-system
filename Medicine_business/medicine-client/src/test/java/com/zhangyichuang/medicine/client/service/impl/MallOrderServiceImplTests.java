package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.client.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.client.model.request.CartSettleRequest;
import com.zhangyichuang.medicine.client.model.request.OrderCheckoutRequest;
import com.zhangyichuang.medicine.client.model.request.OrderPayRequest;
import com.zhangyichuang.medicine.client.model.request.OrderPreviewRequest;
import com.zhangyichuang.medicine.client.model.vo.OrderPreviewVo;
import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import com.zhangyichuang.medicine.client.service.*;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.publisher.OrderTimeoutMessagePublisher;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.dto.MallProductWithImageDto;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.zhangyichuang.medicine.common.core.constants.Constants.ORDER_TIMEOUT_MINUTES;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallOrderServiceImplTests {

    @Mock
    private MallProductService mallProductService;

    @Mock
    private MallOrderItemService mallOrderItemService;

    @Mock
    private OrderTimeoutMessagePublisher orderTimeoutMessagePublisher;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private MallOrderTimelineService mallOrderTimelineService;

    @Mock
    private MallOrderShippingService mallOrderShippingService;

    @Mock
    private MallCartService mallCartService;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private MallProductSearchService mallProductSearchService;

    @Mock
    private MallOrderMapper mallOrderMapper;

    @Mock
    private UserCouponService userCouponService;

    @Mock
    private DistributedLockExecutor distributedLockExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    private MallOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        initializeTableInfo(MallOrder.class);
        initializeTableInfo(MallOrderItem.class);
        initializeTableInfo(MallCart.class);
        service = new MallOrderServiceImpl(
                mallProductService,
                mallOrderItemService,
                orderTimeoutMessagePublisher,
                userWalletService,
                mallOrderTimelineService,
                mallOrderShippingService,
                mallCartService,
                userAddressService,
                null,
                mallProductSearchService,
                userCouponService,
                distributedLockExecutor,
                transactionTemplate
        );
        service = org.mockito.Mockito.spy(service);
        ReflectionTestUtils.setField(service, "baseMapper", mallOrderMapper);
        lenient().when(userCouponService.autoSelectCoupons(anyLong(), anyList())).thenReturn(CouponAutoSelectResultDto.builder()
                .selectedCoupons(List.of())
                .appliedCoupons(List.of())
                .allocations(List.of())
                .couponDeductAmount(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                .couponConsumeAmount(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                .couponWasteAmount(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP))
                .autoSelected(Boolean.TRUE)
                .build());
        lenient().when(userCouponService.lockCoupons(anyLong(), anyList(), anyList(), anyString()))
                .thenReturn(buildCouponSelectionSnapshot(0L, "0.00", "0.00", "0.00", "0.00"));
        lenient().when(distributedLockExecutor.tryExecuteOrElse(anyString(), anyLong(), anyLong(), any(), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(3);
                    return supplier.get();
                });
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 测试目的：验证单商品下单时会写入待支付订单、保存订单项并立即发布 30 分钟超时消息。
     * 测试结果：订单金额、过期时间、时间线与 RabbitMQ 延迟消息均符合预期。
     */
    @Test
    void checkoutOrder_WhenInputIsValid_ShouldCreateOrderAndPublishTimeoutMessage() {
        mockAuthenticatedUser(88L, "test-user");
        MallProductWithImageDto product = createProduct(10L, "连花清瘟胶囊", new BigDecimal("19.90"), 50);
        product.setProductImages(List.of(MallProductImage.builder()
                .productId(10L)
                .imageUrl("https://example.com/product.png")
                .build()));
        when(mallProductService.getProductWithImagesById(10L)).thenReturn(product);
        when(userAddressService.getById(1001L)).thenReturn(createAddress(1001L, 88L));
        doAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(200L);
            return true;
        }).when(service).save(any(MallOrder.class));
        when(mallOrderItemService.save(any(MallOrderItem.class))).thenReturn(true);

        OrderCheckoutRequest request = OrderCheckoutRequest.builder()
                .productId(10L)
                .quantity(2)
                .addressId(1001L)
                .deliveryType(DeliveryTypeEnum.EXPRESS)
                .remark("请当天发货")
                .build();

        OrderCheckoutRequest actualRequest = request;
        var result = service.checkoutOrder(actualRequest);

        // 测试结果：创建出的订单返回值完整，且处于待支付状态。
        assertNotNull(result.getOrderNo());
        assertEquals(new BigDecimal("39.80"), result.getTotalAmount());
        assertEquals(OrderStatusEnum.PENDING_PAYMENT.getType(), result.getOrderStatus());
        assertNotNull(result.getPayExpireTime());
        assertEquals("连花清瘟胶囊 2盒", result.getProductSummary());

        ArgumentCaptor<MallOrder> orderCaptor = ArgumentCaptor.forClass(MallOrder.class);
        verify(service).save(orderCaptor.capture());
        MallOrder savedOrder = orderCaptor.getValue();
        assertEquals(88L, savedOrder.getUserId());
        assertEquals(result.getOrderNo(), savedOrder.getOrderNo());
        assertEquals(result.getPayExpireTime(), savedOrder.getPayExpireTime());
        assertEquals(new BigDecimal("39.80"), savedOrder.getTotalAmount());
        assertEquals("上海市浦东新区 张江药谷 88 号", savedOrder.getReceiverDetail());

        ArgumentCaptor<MallOrderItem> itemCaptor = ArgumentCaptor.forClass(MallOrderItem.class);
        verify(mallOrderItemService).save(itemCaptor.capture());
        MallOrderItem savedItem = itemCaptor.getValue();
        assertEquals(200L, savedItem.getOrderId());
        assertEquals(10L, savedItem.getProductId());
        assertEquals(2, savedItem.getQuantity());
        assertEquals(new BigDecimal("39.80"), savedItem.getTotalPrice());

        verify(mallProductService).deductStock(10L, 2);
        verify(orderTimeoutMessagePublisher).publishOrderTimeout(result.getOrderNo(), ORDER_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        verify(mallOrderTimelineService).addTimelineIfNotExists(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证购物车结算下单时会批量生成订单项、清理购物车并发布 30 分钟超时消息。
     * 测试结果：订单总金额、订单项数量与购物车清理动作均符合预期。
     */
    @SuppressWarnings("unchecked")
    @Test
    void createOrderFromCart_WhenInputIsValid_ShouldCreateOrderAndPublishTimeoutMessage() {
        mockAuthenticatedUser(88L, "cart-user");
        TestLambdaQueryChainWrapper<MallCart> cartQuery = createQueryWrapper(MallCart.class);
        when(mallCartService.lambdaQuery()).thenReturn(cartQuery);
        MallCart cartItem = MallCart.builder()
                .id(1L)
                .userId(88L)
                .productId(10L)
                .productName("阿莫西林")
                .cartNum(3)
                .build();
        cartQuery.setListResult(List.of(cartItem));
        when(mallProductService.getProductWithImagesById(10L))
                .thenReturn(createProduct(10L, "阿莫西林", new BigDecimal("12.50"), 100));
        when(userAddressService.getById(1001L)).thenReturn(createAddress(1001L, 88L));
        doAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(300L);
            return true;
        }).when(service).save(any(MallOrder.class));
        when(mallOrderItemService.saveBatch(anyList())).thenReturn(true);

        CartSettleRequest request = CartSettleRequest.builder()
                .cartIds(List.of(1L))
                .addressId(1001L)
                .deliveryType(DeliveryTypeEnum.EXPRESS)
                .remark("购物车结算")
                .build();

        var result = service.createOrderFromCart(request);

        // 测试结果：购物车下单成功后会返回订单摘要并发送超时消息。
        assertNotNull(result.getOrderNo());
        assertEquals(new BigDecimal("37.50"), result.getTotalAmount());
        assertEquals(1, result.getItemCount());
        assertEquals(OrderStatusEnum.PENDING_PAYMENT.getType(), result.getOrderStatus());
        assertNotNull(result.getPayExpireTime());

        verify(mallProductService).deductStock(10L, 3);
        verify(mallOrderItemService).saveBatch(anyList());
        verify(orderTimeoutMessagePublisher).publishOrderTimeout(result.getOrderNo(), ORDER_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        verify(mallCartService).removeCartItems(List.of(1L));
        verify(userCouponService).lockCoupons(eq(88L), anyList(), anyList(), eq(result.getOrderNo()));
        verify(mallOrderTimelineService).addTimeline(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证预览订单在未手选优惠券时会触发自动选券逻辑。
     * 测试结果：返回结果标记为自动选券，并回填选中券集合。
     */
    @Test
    void previewOrder_WhenCouponIdIsNull_ShouldTriggerAutoSelect() {
        mockAuthenticatedUser(88L, "preview-user");
        MallProductWithImageDto product = createProduct(10L, "布洛芬", new BigDecimal("19.90"), 20);
        product.setCouponEnabled(1);
        when(mallProductService.getProductWithImagesById(10L)).thenReturn(product);
        when(userAddressService.getById(1001L)).thenReturn(createAddress(1001L, 88L));
        when(userCouponService.listMatchedCoupons(eq(88L), anyList()))
                .thenReturn(List.of(buildCouponOption(501L, "自动券候选", "6.00")));
        when(userCouponService.autoSelectCoupons(eq(88L), anyList()))
                .thenReturn(buildAutoSelectResult(501L, "自动券候选", "6.00", "6.00", "0.00", "PREVIEW_PRODUCT_10"));

        OrderPreviewRequest request = OrderPreviewRequest.builder()
                .type(OrderPreviewRequest.PreviewType.PRODUCT)
                .productId(10L)
                .quantity(2)
                .addressId(1001L)
                .build();

        OrderPreviewVo result = service.previewOrder(request);

        assertTrue(Boolean.TRUE.equals(result.getAutoCouponSelected()));
        assertEquals(1, result.getSelectedCoupons().size());
        assertEquals(501L, result.getSelectedCoupons().getFirst().getCouponId());
        assertEquals(new BigDecimal("6.00"), result.getCouponDeductAmount());
        verify(userCouponService).autoSelectCoupons(eq(88L), anyList());
    }

    /**
     * 测试目的：验证单商品下单在未手选优惠券时会自动选券并批量锁券。
     * 测试结果：锁券入参为自动结果中的券ID，订单聚合金额使用锁券快照结果。
     */
    @Test
    void checkoutOrder_WhenCouponIdIsNull_ShouldAutoSelectAndLockCoupons() {
        mockAuthenticatedUser(88L, "auto-coupon-user");
        MallProductWithImageDto product = createProduct(10L, "连花清瘟胶囊", new BigDecimal("19.90"), 50);
        product.setCouponEnabled(1);
        when(mallProductService.getProductWithImagesById(10L)).thenReturn(product);
        when(userAddressService.getById(1001L)).thenReturn(createAddress(1001L, 88L));
        when(userCouponService.autoSelectCoupons(eq(88L), anyList()))
                .thenReturn(buildAutoSelectResult(701L, "自动券", "8.00", "8.00", "0.00", "PRODUCT_10"));
        when(userCouponService.lockCoupons(eq(88L), anyList(), anyList(), anyString()))
                .thenReturn(buildCouponSelectionSnapshot(701L, "8.00", "8.00", "0.00", "PRODUCT_10"));
        doAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(220L);
            return true;
        }).when(service).save(any(MallOrder.class));
        when(mallOrderItemService.save(any(MallOrderItem.class))).thenReturn(true);

        OrderCheckoutRequest request = OrderCheckoutRequest.builder()
                .productId(10L)
                .quantity(2)
                .addressId(1001L)
                .deliveryType(DeliveryTypeEnum.EXPRESS)
                .build();

        var result = service.checkoutOrder(request);

        ArgumentCaptor<List<Long>> couponIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userCouponService).lockCoupons(eq(88L), couponIdsCaptor.capture(), anyList(), eq(result.getOrderNo()));
        assertEquals(List.of(701L), couponIdsCaptor.getValue());
        verify(userCouponService).autoSelectCoupons(eq(88L), anyList());

        ArgumentCaptor<MallOrder> orderCaptor = ArgumentCaptor.forClass(MallOrder.class);
        verify(service).save(orderCaptor.capture());
        MallOrder savedOrder = orderCaptor.getValue();
        assertEquals(701L, savedOrder.getCouponId());
        assertEquals(new BigDecimal("8.00"), savedOrder.getCouponDeductAmount());
        assertEquals(new BigDecimal("31.80"), savedOrder.getTotalAmount());
    }

    /**
     * 测试目的：验证手选优惠券时不会触发自动选券覆盖。
     * 测试结果：自动选券方法不被调用，锁券参数仅包含手选券ID。
     */
    @Test
    void checkoutOrder_WhenCouponIdProvided_ShouldKeepManualSelection() {
        mockAuthenticatedUser(88L, "manual-coupon-user");
        MallProductWithImageDto product = createProduct(10L, "连花清瘟胶囊", new BigDecimal("19.90"), 50);
        product.setCouponEnabled(1);
        when(mallProductService.getProductWithImagesById(10L)).thenReturn(product);
        when(userAddressService.getById(1001L)).thenReturn(createAddress(1001L, 88L));
        when(userCouponService.lockCoupons(eq(88L), anyList(), anyList(), anyString()))
                .thenReturn(buildCouponSelectionSnapshot(900L, "5.00", "5.00", "0.00", "PRODUCT_10"));
        doAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(221L);
            return true;
        }).when(service).save(any(MallOrder.class));
        when(mallOrderItemService.save(any(MallOrderItem.class))).thenReturn(true);

        OrderCheckoutRequest request = OrderCheckoutRequest.builder()
                .productId(10L)
                .quantity(2)
                .addressId(1001L)
                .deliveryType(DeliveryTypeEnum.EXPRESS)
                .couponId(900L)
                .build();

        var result = service.checkoutOrder(request);

        ArgumentCaptor<List<Long>> couponIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(userCouponService).lockCoupons(eq(88L), couponIdsCaptor.capture(), anyList(), eq(result.getOrderNo()));
        assertEquals(List.of(900L), couponIdsCaptor.getValue());
        verify(userCouponService, never()).autoSelectCoupons(anyLong(), anyList());
    }

    /**
     * 测试目的：验证钱包支付会扣减余额、标记订单已支付并返回成功结果。
     * 测试结果：支付结果状态为 SUCCESS，订单状态推进到待发货，并写入支付时间线。
     */
    @SuppressWarnings("unchecked")
    @Test
    void payOrder_WhenWalletPaymentSucceeds_ShouldMarkOrderPaid() {
        MallOrder order = createOrder(OrderStatusEnum.PENDING_PAYMENT.getType());
        order.setTotalAmount(new BigDecimal("59.80"));
        order.setId(1L);

        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(order);
        TestLambdaUpdateChainWrapper<MallOrder> update = createUpdateWrapper(MallOrder.class);
        doReturn(update).when(service).lambdaUpdate();
        update.setUpdateResult(true);
        doReturn(88L).when(service).getUserId();
        when(userWalletService.deductBalance(88L, new BigDecimal("59.80"), "订单支付-O202511130001")).thenReturn(true);

        OrderPayRequest request = OrderPayRequest.builder()
                .orderNo("O202511130001")
                .payMethod(PayTypeEnum.WALLET)
                .build();

        var result = service.payOrder(request);

        // 测试结果：钱包支付走成功分支，并把订单推进到待发货。
        assertEquals("SUCCESS", result.getPaymentStatus());
        assertEquals(PayTypeEnum.WALLET.getType(), result.getPaymentMethod());
        assertEquals(OrderStatusEnum.PENDING_SHIPMENT.getType(), result.getOrderStatus());
        assertEquals(new BigDecimal("59.80"), result.getPayAmount());
        assertTrue(update.isUpdateCalled());

        verify(userWalletService).deductBalance(88L, new BigDecimal("59.80"), "订单支付-O202511130001");
        verify(userCouponService).consumeCouponsForOrder(order);
        verify(mallOrderTimelineService).addTimelineIfNotExists(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证超时消息提前到达时，不会误关单，而是按剩余过期时间重新投递。
     * 测试结果：订单保持待支付状态，并重新向 RabbitMQ 发布剩余毫秒数的延迟消息。
     */
    @SuppressWarnings("unchecked")
    @Test
    void closeOrderIfUnpaid_WhenPayExpireTimeNotReached_ShouldRepublishRemainingDelay() {
        MallOrder order = MallOrder.builder()
                .id(1L)
                .orderNo("O202511130001")
                .orderStatus(OrderStatusEnum.PENDING_PAYMENT.getType())
                .payExpireTime(new Date(System.currentTimeMillis() + 60_000L))
                .version(2)
                .build();

        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(order);

        service.closeOrderIfUnpaid("O202511130001");

        // 测试结果：订单未被关闭，而是重新投递剩余延迟时间。
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(orderTimeoutMessagePublisher).publishOrderTimeout(eq("O202511130001"), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertTrue(delayCaptor.getValue() > 0);
        verifyNoInteractions(mallOrderTimelineService);
    }

    /**
     * 测试目的：验证真正过期的待支付订单会被关闭、恢复库存并记录时间线。
     * 测试结果：订单状态更新成功后，会恢复库存并新增“系统自动关闭”时间线。
     */
    @SuppressWarnings("unchecked")
    @Test
    void closeOrderIfUnpaid_WhenOrderExpired_ShouldCloseOrderAndRestoreStock() {
        MallOrder order = MallOrder.builder()
                .id(1L)
                .orderNo("O202511130001")
                .orderStatus(OrderStatusEnum.PENDING_PAYMENT.getType())
                .payExpireTime(new Date(System.currentTimeMillis() - 60_000L))
                .version(3)
                .build();

        TestLambdaQueryChainWrapper<MallOrder> orderQuery = createQueryWrapper(MallOrder.class);
        doReturn(orderQuery).when(service).lambdaQuery();
        orderQuery.setOneResult(order);

        TestLambdaQueryChainWrapper<MallOrderItem> itemQuery = createQueryWrapper(MallOrderItem.class);
        when(mallOrderItemService.lambdaQuery()).thenReturn(itemQuery);
        itemQuery.setListResult(List.of(MallOrderItem.builder()
                .orderId(1L)
                .productId(10L)
                .quantity(2)
                .build()));

        TestLambdaUpdateChainWrapper<MallOrder> update = createUpdateWrapper(MallOrder.class);
        doReturn(update).when(service).lambdaUpdate();
        update.setUpdateResult(true);

        service.closeOrderIfUnpaid("O202511130001");

        // 测试结果：过期订单被关闭，同时恢复库存并补写时间线。
        assertTrue(update.isUpdateCalled());
        verify(mallProductService).restoreStock(10L, 2);
        verify(userCouponService).releaseCouponsForOrder(eq(order), eq("系统自动关闭"), eq("订单支付超时，系统自动关闭"));
        verify(mallOrderTimelineService).addTimelineIfNotExists(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证查询不属于当前用户的订单详情时，会返回“订单不存在”异常。
     * 测试结果：服务直接抛出业务异常，并且不会继续查询订单项和物流信息。
     */
    @Test
    void getOrderDetail_WhenOrderDoesNotBelongToUser_ShouldThrowNotFound() {
        when(mallOrderMapper.getOrderDetailByOrderNo("O202511130001", 88L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.getOrderDetail("O202511130001", 88L));

        // 测试结果：返回准确的业务错误码和错误信息。
        assertEquals(ResponseCode.RESULT_IS_NULL.getCode(), exception.getCode());
        assertEquals("订单不存在", exception.getMessage());
        verify(mallOrderMapper).getOrderDetailByOrderNo("O202511130001", 88L);
        verifyNoInteractions(mallOrderItemService, mallOrderShippingService);
    }

    /**
     * 测试目的：验证查询订单物流时，会按照用户维度过滤订单，并正确解析物流节点。
     * 测试结果：返回的物流节点信息完整且顺序可用于前端直接展示。
     */
    @SuppressWarnings("unchecked")
    @Test
    void getOrderShipping_ShouldScopeByUserAndParseNodes() {
        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(createOrder(OrderStatusEnum.PENDING_RECEIPT.getType()));
        when(mallOrderShippingService.getByOrderId(1L)).thenReturn(MallOrderShipping.builder()
                .orderId(1L)
                .shippingCompany("顺丰")
                .shippingNo("SF1234567890")
                .status(ShippingStatusEnum.IN_TRANSIT.getType())
                .shippingInfo("[{\"time\":\"2025-11-13 12:00:00\",\"content\":\"快件已揽收\",\"location\":\"上海\"}]")
                .build());

        var result = service.getOrderShipping("O202511130001", 88L);

        // 测试结果：物流公司、单号和节点信息均能正确返回。
        assertNotNull(result);
        assertEquals("O202511130001", result.getOrderNo());
        assertEquals("顺丰", result.getLogisticsCompany());
        assertEquals(1, result.getNodes().size());
        assertEquals("上海", result.getNodes().getFirst().getLocation());
    }

    /**
     * 测试目的：验证订单时间线查询会将事件类型、订单状态和操作人信息映射为展示对象。
     * 测试结果：返回的时间线条目具备可读的事件名称和订单状态名称。
     */
    @SuppressWarnings("unchecked")
    @Test
    void getOrderTimeline_ShouldMapTimelineForOwnedOrder() {
        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(createOrder(OrderStatusEnum.PENDING_RECEIPT.getType()));

        MallOrderTimeline timeline = new MallOrderTimeline();
        timeline.setId(1L);
        timeline.setOrderId(1L);
        timeline.setEventType(OrderEventTypeEnum.ORDER_CREATED.getType());
        timeline.setEventStatus(OrderStatusEnum.PENDING_PAYMENT.getType());
        timeline.setOperatorType(OperatorTypeEnum.USER.getType());
        timeline.setDescription("用户创建订单");
        timeline.setCreatedTime(new Date());
        when(mallOrderTimelineService.getTimelineByOrderId(1L)).thenReturn(List.of(timeline));

        var result = service.getOrderTimeline("O202511130001", 88L);

        // 测试结果：时间线事件名称和订单状态名称都能正确映射。
        assertEquals("O202511130001", result.getOrderNo());
        assertEquals("待收货", result.getOrderStatusName());
        assertEquals(1, result.getTimeline().size());
        assertEquals("订单创建", result.getTimeline().getFirst().getEventTypeName());
    }

    /**
     * 测试目的：验证当订单不属于当前用户时，取消校验接口会返回不可取消及明确原因。
     * 测试结果：返回的原因码为 ORDER_NOT_FOUND，提示文案为“订单不存在或无权访问”。
     */
    @SuppressWarnings("unchecked")
    @Test
    void checkOrderCancelable_WhenOrderDoesNotBelongToUser_ShouldReturnNotFoundResult() {
        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(null);

        var result = service.checkOrderCancelable("O202511130001", 88L);

        // 测试结果：取消校验返回不可取消，并附带明确原因。
        assertFalse(result.getCancelable());
        assertEquals("ORDER_NOT_FOUND", result.getReasonCode());
        assertEquals("订单不存在或无权访问", result.getReasonMessage());
    }

    /**
     * 测试目的：验证待支付订单会被识别为可取消状态。
     * 测试结果：返回的 cancelable 为 true，状态名称映射为“待支付”。
     */
    @SuppressWarnings("unchecked")
    @Test
    void checkOrderCancelable_WhenPendingPayment_ShouldReturnCancelable() {
        TestLambdaQueryChainWrapper<MallOrder> query = createQueryWrapper(MallOrder.class);
        doReturn(query).when(service).lambdaQuery();
        query.setOneResult(createOrder(OrderStatusEnum.PENDING_PAYMENT.getType()));

        var result = service.checkOrderCancelable("O202511130001", 88L);

        // 测试结果：待支付订单允许取消。
        assertTrue(result.getCancelable());
        assertEquals("CAN_CANCEL", result.getReasonCode());
        assertEquals("待支付", result.getOrderStatusName());
    }

    /**
     * 构建锁券快照测试数据。
     *
     * @param couponId      优惠券ID
     * @param deductAmount  抵扣金额
     * @param consumeAmount 消耗金额
     * @param wasteAmount   浪费金额
     * @param itemKey       分摊商品项键
     * @return 锁券快照
     */
    private OrderCouponSelectionSnapshotDto buildCouponSelectionSnapshot(Long couponId,
                                                                         String deductAmount,
                                                                         String consumeAmount,
                                                                         String wasteAmount,
                                                                         String itemKey) {
        if (couponId == null || couponId <= 0) {
            return OrderCouponSelectionSnapshotDto.builder()
                    .selectedCoupons(List.of())
                    .appliedCoupons(List.of())
                    .allocations(List.of())
                    .couponDeductAmount(new BigDecimal("0.00"))
                    .couponConsumeAmount(new BigDecimal("0.00"))
                    .couponWasteAmount(new BigDecimal("0.00"))
                    .autoSelected(Boolean.FALSE)
                    .build();
        }
        return OrderCouponSelectionSnapshotDto.builder()
                .selectedCoupons(List.of(OrderCouponSnapshotDto.builder()
                        .couponId(couponId)
                        .couponName("测试券-" + couponId)
                        .build()))
                .appliedCoupons(List.of(CouponAppliedDetailDto.builder()
                        .couponId(couponId)
                        .couponName("测试券-" + couponId)
                        .couponDeductAmount(new BigDecimal(deductAmount))
                        .couponConsumeAmount(new BigDecimal(consumeAmount))
                        .couponWasteAmount(new BigDecimal(wasteAmount))
                        .build()))
                .allocations(List.of(CouponSettlementAllocationDto.builder()
                        .itemKey(itemKey)
                        .couponDeductAmount(new BigDecimal(deductAmount))
                        .payableAmount(new BigDecimal("0.00"))
                        .build()))
                .couponDeductAmount(new BigDecimal(deductAmount))
                .couponConsumeAmount(new BigDecimal(consumeAmount))
                .couponWasteAmount(new BigDecimal(wasteAmount))
                .autoSelected(Boolean.FALSE)
                .build();
    }

    /**
     * 构建自动选券结果测试数据。
     *
     * @param couponId      优惠券ID
     * @param couponName    优惠券名称
     * @param deductAmount  抵扣金额
     * @param consumeAmount 消耗金额
     * @param wasteAmount   浪费金额
     * @param itemKey       分摊商品项键
     * @return 自动选券结果
     */
    private CouponAutoSelectResultDto buildAutoSelectResult(Long couponId,
                                                            String couponName,
                                                            String deductAmount,
                                                            String consumeAmount,
                                                            String wasteAmount,
                                                            String itemKey) {
        return CouponAutoSelectResultDto.builder()
                .selectedCoupons(List.of(OrderCouponSnapshotDto.builder()
                        .couponId(couponId)
                        .couponName(couponName)
                        .build()))
                .appliedCoupons(List.of(CouponAppliedDetailDto.builder()
                        .couponId(couponId)
                        .couponName(couponName)
                        .couponDeductAmount(new BigDecimal(deductAmount))
                        .couponConsumeAmount(new BigDecimal(consumeAmount))
                        .couponWasteAmount(new BigDecimal(wasteAmount))
                        .build()))
                .allocations(List.of(CouponSettlementAllocationDto.builder()
                        .itemKey(itemKey)
                        .couponDeductAmount(new BigDecimal(deductAmount))
                        .payableAmount(new BigDecimal("0.00"))
                        .build()))
                .couponDeductAmount(new BigDecimal(deductAmount))
                .couponConsumeAmount(new BigDecimal(consumeAmount))
                .couponWasteAmount(new BigDecimal(wasteAmount))
                .autoSelected(Boolean.TRUE)
                .build();
    }

    /**
     * 构建订单优惠券候选对象。
     *
     * @param couponId     优惠券ID
     * @param couponName   优惠券名称
     * @param deductAmount 订单抵扣金额
     * @return 优惠券候选对象
     */
    private OrderCouponOptionVo buildCouponOption(Long couponId,
                                                  String couponName,
                                                  String deductAmount) {
        return OrderCouponOptionVo.builder()
                .couponId(couponId)
                .couponName(couponName)
                .matched(Boolean.TRUE)
                .couponDeductAmount(new BigDecimal(deductAmount))
                .couponConsumeAmount(new BigDecimal(deductAmount))
                .couponWasteAmount(new BigDecimal("0.00"))
                .build();
    }

    /**
     * 构造认证上下文，供直接调用 SecurityUtils 的下单逻辑使用。
     *
     * @param userId   登录用户ID
     * @param username 登录用户名
     */
    private void mockAuthenticatedUser(Long userId, String username) {
        AuthUser authUser = AuthUser.builder()
                .id(userId)
                .username(username)
                .roles(Set.of("ROLE_USER"))
                .build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 构造测试商品对象。
     *
     * @param productId 商品ID
     * @param name      商品名称
     * @param price     商品单价
     * @param stock     商品库存
     * @return 测试商品
     */
    private MallProductWithImageDto createProduct(Long productId, String name, BigDecimal price, Integer stock) {
        MallProductWithImageDto product = new MallProductWithImageDto();
        product.setId(productId);
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(1);
        product.setUnit("盒");
        product.setDeliveryType(DeliveryTypeEnum.EXPRESS.ordinal());
        return product;
    }

    /**
     * 构造测试收货地址。
     *
     * @param addressId 地址ID
     * @param userId    用户ID
     * @return 测试收货地址
     */
    private UserAddress createAddress(Long addressId, Long userId) {
        UserAddress address = new UserAddress();
        address.setId(addressId);
        address.setUserId(userId);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800000000");
        address.setAddress("上海市浦东新区");
        address.setDetailAddress("张江药谷 88 号");
        return address;
    }

    /**
     * 构造测试订单对象。
     *
     * @param orderStatus 订单状态
     * @return 测试订单
     */
    private MallOrder createOrder(String orderStatus) {
        return MallOrder.builder()
                .id(1L)
                .orderNo("O202511130001")
                .userId(88L)
                .orderStatus(orderStatus)
                .deliveryType(DeliveryTypeEnum.EXPRESS.getType())
                .receiverName("张三")
                .receiverPhone("13800000000")
                .receiverDetail("上海市浦东新区")
                .totalAmount(new BigDecimal("59.80"))
                .payExpireTime(new Date(System.currentTimeMillis() + 30_000L))
                .build();
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
     * 创建更新包装器测试替身。
     *
     * @param entityClass 实体类型
     * @param <T>         实体泛型
     * @return 更新包装器测试替身
     */
    private <T> TestLambdaUpdateChainWrapper<T> createUpdateWrapper(Class<T> entityClass) {
        return new TestLambdaUpdateChainWrapper<>(entityClass);
    }

    /**
     * 初始化 MyBatis-Plus 的实体元数据缓存，避免测试替身解析 Lambda 字段时报错。
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
     * 订单查询包装器测试替身。
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
         * 构造查询包装器测试替身。
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

        @Override
        public T one() {
            return oneResult;
        }

        @Override
        public List<T> list() {
            return listResult;
        }
    }

    /**
     * 订单更新包装器测试替身。
     *
     * @param <T> 实体泛型
     */
    private static final class TestLambdaUpdateChainWrapper<T> extends LambdaUpdateChainWrapper<T> {

        /**
         * 更新执行结果。
         */
        private boolean updateResult;

        /**
         * 是否执行过更新。
         */
        private boolean updateCalled;

        /**
         * 构造更新包装器测试替身。
         *
         * @param entityClass 实体类型
         */
        private TestLambdaUpdateChainWrapper(Class<T> entityClass) {
            super(entityClass);
        }

        /**
         * 设置更新执行结果。
         *
         * @param updateResult 更新执行结果
         */
        private void setUpdateResult(boolean updateResult) {
            this.updateResult = updateResult;
        }

        /**
         * 判断是否执行过更新。
         *
         * @return 是否执行过更新
         */
        private boolean isUpdateCalled() {
            return updateCalled;
        }

        @Override
        public boolean update() {
            updateCalled = true;
            return updateResult;
        }
    }
}
