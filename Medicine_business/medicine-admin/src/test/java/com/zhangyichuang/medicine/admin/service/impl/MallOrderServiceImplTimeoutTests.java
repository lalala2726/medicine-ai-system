package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.zhangyichuang.medicine.admin.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.request.OrderUpdatePriceRequest;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.common.rabbitmq.publisher.OrderTimeoutMessagePublisher;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zhangyichuang.medicine.common.core.constants.Constants.ORDER_TIMEOUT_MINUTES;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallOrderServiceImplTimeoutTests {

    @Mock
    private MallOrderMapper mallOrderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MallOrderItemService mallOrderItemService;

    @Mock
    private MallProductImageService mallProductImageService;

    @Mock
    private MallOrderTimelineService mallOrderTimelineService;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private MallOrderShippingService mallOrderShippingService;

    @Mock
    private MallInventoryService mallInventoryService;

    @Mock
    private OrderTimeoutMessagePublisher orderTimeoutMessagePublisher;

    @Mock
    private DistributedLockExecutor distributedLockExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Spy
    @InjectMocks
    private MallOrderServiceImpl service;

    /**
     * 初始化测试依赖与 MyBatis 元数据。
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        initializeTableInfo(MallOrder.class);
        initializeTableInfo(MallOrderItem.class);
        lenient().when(distributedLockExecutor.tryExecuteOrElse(anyString(), anyLong(), anyLong(), any(), any()))
                .thenAnswer(invocation -> ((java.util.function.Supplier<Boolean>) invocation.getArgument(3)).get());
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<Boolean>) invocation.getArgument(0)).doInTransaction(null));
    }

    /**
     * 测试目的：验证管理员改价后会同步重置支付时效，并重新发布 30 分钟订单超时消息。
     * 测试结果：订单主表金额、支付金额、支付过期时间和订单项价格分摊都被正确更新。
     */
    @SuppressWarnings("unchecked")
    @Test
    void updateOrderPrice_WhenPendingPayment_ShouldResetExpireTimeAndRepublishTimeoutMessage() {
        MallOrder order = MallOrder.builder()
                .id(1L)
                .orderNo("ORDER001")
                .orderStatus(OrderStatusEnum.PENDING_PAYMENT.getType())
                .totalAmount(new BigDecimal("100.00"))
                .payAmount(new BigDecimal("100.00"))
                .build();
        doReturn(order).when(service).getOrderById(1L);
        doReturn(true).when(service).updateById(any(MallOrder.class));

        TestLambdaQueryChainWrapper<MallOrderItem> itemQuery = createQueryWrapper(MallOrderItem.class);
        when(mallOrderItemService.lambdaQuery()).thenReturn(itemQuery);
        MallOrderItem item = MallOrderItem.builder()
                .id(11L)
                .orderId(1L)
                .quantity(2)
                .price(new BigDecimal("50.00"))
                .totalPrice(new BigDecimal("100.00"))
                .build();
        itemQuery.setListResult(List.of(item));
        when(mallOrderItemService.updateBatchById(anyList())).thenReturn(true);

        OrderUpdatePriceRequest request = new OrderUpdatePriceRequest();
        request.setOrderId(1L);
        request.setPrice("120.00");

        boolean result = service.updateOrderPrice(request);

        // 测试结果：改价成功后，支付金额和超时消息都会被同步刷新。
        assertTrue(result);
        assertEquals(new BigDecimal("120.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), order.getPayAmount());
        assertNotNull(order.getPayExpireTime());
        assertEquals(new BigDecimal("120.00"), item.getTotalPrice());
        assertEquals(new BigDecimal("60.00"), item.getPrice());
        verify(orderTimeoutMessagePublisher).publishOrderTimeout("ORDER001", ORDER_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        verify(mallOrderTimelineService).addTimelineIfNotExists(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证补偿任务命中的真正过期订单会被关闭、恢复库存并写入时间线。
     * 测试结果：补偿关闭返回 true，同时调用库存恢复与时间线记录。
     */
    @SuppressWarnings("unchecked")
    @Test
    void closeExpiredOrderForCompensation_WhenOrderExpired_ShouldCloseOrderAndRestoreStock() {
        MallOrder order = MallOrder.builder()
                .id(1L)
                .orderNo("ORDER001")
                .orderStatus(OrderStatusEnum.PENDING_PAYMENT.getType())
                .version(2)
                .payExpireTime(new Date(System.currentTimeMillis() - 60_000L))
                .build();

        TestLambdaQueryChainWrapper<MallOrder> orderQuery = createQueryWrapper(MallOrder.class);
        doReturn(orderQuery).when(service).lambdaQuery();
        orderQuery.setOneResult(order);

        TestLambdaQueryChainWrapper<MallOrderItem> itemQuery = createQueryWrapper(MallOrderItem.class);
        when(mallOrderItemService.lambdaQuery()).thenReturn(itemQuery);
        itemQuery.setListResult(List.of(MallOrderItem.builder()
                .orderId(1L)
                .productId(10L)
                .quantity(3)
                .build()));

        TestLambdaUpdateChainWrapper<MallOrder> update = createUpdateWrapper(MallOrder.class);
        doReturn(update).when(service).lambdaUpdate();
        update.setUpdateResult(true);

        boolean result = service.closeExpiredOrderForCompensation("ORDER001");

        // 测试结果：补偿逻辑会真正关闭订单，并触发库存恢复和时间线记录。
        assertTrue(result);
        assertTrue(update.isUpdateCalled());
        verify(mallInventoryService).restoreStock(10L, 3);
        verify(mallOrderTimelineService).addTimelineIfNotExists(any(OrderTimelineDto.class));
    }

    /**
     * 测试目的：验证补偿任务遇到尚未真正过期的订单时不会误关闭订单。
     * 测试结果：补偿关闭返回 false，且不会执行库存恢复和状态更新。
     */
    @SuppressWarnings("unchecked")
    @Test
    void closeExpiredOrderForCompensation_WhenOrderNotExpired_ShouldSkipClosing() {
        MallOrder order = MallOrder.builder()
                .id(1L)
                .orderNo("ORDER001")
                .orderStatus(OrderStatusEnum.PENDING_PAYMENT.getType())
                .version(2)
                .payExpireTime(new Date(System.currentTimeMillis() + 60_000L))
                .build();

        TestLambdaQueryChainWrapper<MallOrder> orderQuery = createQueryWrapper(MallOrder.class);
        doReturn(orderQuery).when(service).lambdaQuery();
        orderQuery.setOneResult(order);

        boolean result = service.closeExpiredOrderForCompensation("ORDER001");

        // 测试结果：未到期订单会直接跳过，不触发任何关闭副作用。
        assertFalse(result);
        verifyNoInteractions(mallInventoryService, mallOrderTimelineService);
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
     * 管理端查询包装器测试替身。
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
     * 管理端更新包装器测试替身。
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
