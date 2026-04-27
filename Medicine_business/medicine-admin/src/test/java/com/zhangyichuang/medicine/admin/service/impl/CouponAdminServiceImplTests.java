package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zhangyichuang.medicine.admin.mapper.CouponLogMapper;
import com.zhangyichuang.medicine.admin.mapper.CouponTemplateMapper;
import com.zhangyichuang.medicine.admin.mapper.UserCouponMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.publisher.CouponBatchIssueMessagePublisher;
import com.zhangyichuang.medicine.admin.publisher.CouponIssueMessagePublisher;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.model.coupon.CouponAppliedDetailDto;
import com.zhangyichuang.medicine.model.coupon.CouponSettlementResultDto;
import com.zhangyichuang.medicine.model.coupon.OrderCouponSelectionSnapshotDto;
import com.zhangyichuang.medicine.model.coupon.OrderCouponSnapshotDto;
import com.zhangyichuang.medicine.model.entity.CouponLog;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import com.zhangyichuang.medicine.model.enums.CouponTypeEnum;
import com.zhangyichuang.medicine.model.enums.UserCouponStatusEnum;
import com.zhangyichuang.medicine.shared.service.CouponGrantCoreService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 管理端优惠券服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponAdminServiceImplTests {

    /**
     * 优惠券模板 Mapper。
     */
    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    /**
     * 用户优惠券 Mapper。
     */
    @Mock
    private UserCouponMapper userCouponMapper;

    /**
     * 优惠券日志 Mapper。
     */
    @Mock
    private CouponLogMapper couponLogMapper;

    /**
     * 用户 Mapper。
     */
    @Mock
    private UserMapper userMapper;

    /**
     * 批量发券发布器。
     */
    @Mock
    private CouponBatchIssueMessagePublisher couponBatchIssueMessagePublisher;

    /**
     * 单发券发布器。
     */
    @Mock
    private CouponIssueMessagePublisher couponIssueMessagePublisher;

    /**
     * 分布式锁执行器。
     */
    @Mock
    private DistributedLockExecutor distributedLockExecutor;

    /**
     * 事务模板。
     */
    @Mock
    private TransactionTemplate transactionTemplate;

    /**
     * 优惠券共享发放核心服务。
     */
    @Mock
    private CouponGrantCoreService couponGrantCoreService;

    /**
     * 滑块验证码服务。
     */
    @Mock
    private CaptchaService captchaService;

    /**
     * 被测服务。
     */
    private CouponAdminServiceImpl service;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        initializeTableInfo(UserCoupon.class);
        service = new CouponAdminServiceImpl(
                couponTemplateMapper,
                userCouponMapper,
                couponLogMapper,
                userMapper,
                couponBatchIssueMessagePublisher,
                couponIssueMessagePublisher,
                distributedLockExecutor,
                transactionTemplate,
                couponGrantCoreService,
                captchaService
        );
    }

    /**
     * 验证新多券快照结构可被解析并完成重算。
     */
    @Test
    void recalculateLockedCoupon_WhenSnapshotIsValid_ShouldReturnAggregatedResult() {
        MallOrder order = MallOrder.builder()
                .orderNo("O20260408001")
                .couponId(1001L)
                .freightAmount(new BigDecimal("0.00"))
                .couponSnapshotJson(buildSelectionSnapshotJson(
                        List.of(
                                buildCouponSnapshot(1001L, "20.00", List.of(101L), "0.00", 1, 1),
                                buildCouponSnapshot(1002L, "10.00", List.of(102L), "0.00", 1, 1)
                        ),
                        List.of()
                ))
                .build();
        List<MallOrderItem> orderItems = List.of(
                MallOrderItem.builder().id(1L).productId(101L).totalPrice(new BigDecimal("40.00")).build(),
                MallOrderItem.builder().id(2L).productId(102L).totalPrice(new BigDecimal("30.00")).build()
        );

        when(userCouponMapper.selectOne(any()))
                .thenReturn(buildLockedCoupon(1001L, "O20260408001"), buildLockedCoupon(1002L, "O20260408001"));
        when(userCouponMapper.update(isNull(), any())).thenReturn(1);

        CouponSettlementResultDto result = service.recalculateLockedCoupon(order, orderItems);

        assertEquals(new BigDecimal("70.00"), result.getItemsAmount());
        assertEquals(new BigDecimal("30.00"), result.getCouponDeductAmount());
        assertEquals(new BigDecimal("30.00"), result.getCouponConsumeAmount());
        assertEquals(new BigDecimal("0.00"), result.getCouponWasteAmount());
        verify(userCouponMapper, times(2)).update(isNull(), any());
    }

    /**
     * 验证改价后不满足门槛时会阻断继续下单。
     */
    @Test
    void recalculateLockedCoupon_WhenThresholdNotMatched_ShouldThrowException() {
        MallOrder order = MallOrder.builder()
                .orderNo("O20260408002")
                .couponId(2001L)
                .freightAmount(new BigDecimal("0.00"))
                .couponSnapshotJson(buildSelectionSnapshotJson(
                        List.of(buildCouponSnapshot(2001L, "20.00", List.of(101L), "100.00", 1, 1)),
                        List.of()
                ))
                .build();
        List<MallOrderItem> orderItems = List.of(
                MallOrderItem.builder().id(1L).productId(101L).totalPrice(new BigDecimal("50.00")).build()
        );

        when(userCouponMapper.selectOne(any())).thenReturn(buildLockedCoupon(2001L, "O20260408002"));

        assertThrows(ServiceException.class, () -> service.recalculateLockedCoupon(order, orderItems));
    }

    /**
     * 验证后台释放锁券会按多券快照逐张释放并记录日志。
     */
    @Test
    void releaseLockedCouponForOrder_WhenMultipleCouponsApplied_ShouldReleaseAllCoupons() {
        MallOrder order = MallOrder.builder()
                .orderNo("O20260408003")
                .couponId(3001L)
                .couponSnapshotJson(buildSelectionSnapshotJson(
                        List.of(
                                buildCouponSnapshot(3001L, "12.00", List.of(101L), "0.00", 1, 1),
                                buildCouponSnapshot(3002L, "8.00", List.of(102L), "0.00", 1, 1)
                        ),
                        List.of(
                                CouponAppliedDetailDto.builder()
                                        .couponId(3001L)
                                        .couponDeductAmount(new BigDecimal("12.00"))
                                        .couponWasteAmount(new BigDecimal("0.00"))
                                        .build(),
                                CouponAppliedDetailDto.builder()
                                        .couponId(3002L)
                                        .couponDeductAmount(new BigDecimal("8.00"))
                                        .couponWasteAmount(new BigDecimal("0.00"))
                                        .build()
                        )
                ))
                .build();

        when(userCouponMapper.selectOne(any()))
                .thenReturn(buildLockedCoupon(3001L, "O20260408003"), buildLockedCoupon(3002L, "O20260408003"));
        when(userCouponMapper.updateById(any(UserCoupon.class))).thenReturn(1);

        service.releaseLockedCouponForOrder(order, "admin", "后台取消订单");

        verify(userCouponMapper, times(2)).updateById(any(UserCoupon.class));
        verify(couponLogMapper, times(2)).insert(any(CouponLog.class));
    }

    /**
     * 构建多券快照 JSON。
     *
     * @param selectedCoupons 选中的优惠券集合
     * @param appliedCoupons  应用明细集合
     * @return 快照 JSON
     */
    private String buildSelectionSnapshotJson(List<OrderCouponSnapshotDto> selectedCoupons,
                                              List<CouponAppliedDetailDto> appliedCoupons) {
        return JSONUtils.toJson(OrderCouponSelectionSnapshotDto.builder()
                .selectedCoupons(selectedCoupons)
                .appliedCoupons(appliedCoupons)
                .allocations(List.of())
                .couponDeductAmount(BigDecimal.ZERO)
                .couponConsumeAmount(BigDecimal.ZERO)
                .couponWasteAmount(BigDecimal.ZERO)
                .autoSelected(Boolean.FALSE)
                .build());
    }

    /**
     * 构建订单优惠券快照。
     *
     * @param couponId           优惠券ID
     * @param availableAmount    可用金额
     * @param eligibleProductIds 可用商品ID列表
     * @param thresholdAmount    使用门槛
     * @param continueEnabled    是否可续用
     * @param stackableEnabled   是否可叠加
     * @return 优惠券快照
     */
    private OrderCouponSnapshotDto buildCouponSnapshot(Long couponId,
                                                       String availableAmount,
                                                       List<Long> eligibleProductIds,
                                                       String thresholdAmount,
                                                       Integer continueEnabled,
                                                       Integer stackableEnabled) {
        Date now = new Date();
        return OrderCouponSnapshotDto.builder()
                .couponId(couponId)
                .templateId(couponId)
                .couponName("测试券-" + couponId)
                .couponType(CouponTypeEnum.FULL_REDUCTION.getType())
                .thresholdAmount(new BigDecimal(thresholdAmount))
                .lockedAvailableAmount(new BigDecimal(availableAmount))
                .continueUseEnabled(continueEnabled)
                .stackableEnabled(stackableEnabled)
                .effectiveTime(new Date(now.getTime() - 60_000L))
                .expireTime(new Date(now.getTime() + 86_400_000L))
                .eligibleProductIds(eligibleProductIds)
                .build();
    }

    /**
     * 构建锁定状态用户优惠券。
     *
     * @param couponId 优惠券ID
     * @param orderNo  锁定订单号
     * @return 锁定中的用户优惠券
     */
    private UserCoupon buildLockedCoupon(Long couponId, String orderNo) {
        Date now = new Date();
        return UserCoupon.builder()
                .id(couponId)
                .userId(88L)
                .availableAmount(new BigDecimal("100.00"))
                .couponStatus(UserCouponStatusEnum.LOCKED.getType())
                .lockOrderNo(orderNo)
                .effectiveTime(new Date(now.getTime() - 60_000L))
                .expireTime(new Date(now.getTime() + 86_400_000L))
                .build();
    }

    /**
     * 初始化 MyBatis-Plus 元数据，避免 Lambda 字段解析异常。
     *
     * @param entityClass 实体类型
     * @return 无返回值
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
}
