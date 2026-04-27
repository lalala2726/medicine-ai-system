package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.mapper.CouponLogMapper;
import com.zhangyichuang.medicine.client.mapper.UserCouponMapper;
import com.zhangyichuang.medicine.client.model.request.UserCouponListRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import com.zhangyichuang.medicine.client.model.vo.coupon.UserCouponVo;
import com.zhangyichuang.medicine.model.coupon.CouponAutoSelectResultDto;
import com.zhangyichuang.medicine.model.coupon.CouponSettlementItemDto;
import com.zhangyichuang.medicine.model.coupon.OrderCouponSelectionSnapshotDto;
import com.zhangyichuang.medicine.model.entity.CouponLog;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import com.zhangyichuang.medicine.model.enums.UserCouponStatusEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户优惠券服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserCouponServiceImplTests {

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
     * 被测服务。
     */
    private UserCouponServiceImpl service;

    /**
     * 初始化测试上下文。
     */
    @BeforeEach
    void setUp() {
        initializeTableInfo(UserCoupon.class);
        service = spy(new UserCouponServiceImpl(couponLogMapper));
        ReflectionTestUtils.setField(service, "baseMapper", userCouponMapper);
    }

    /**
     * 验证自动选券服务会基于可用券列表返回最优组合。
     */
    @Test
    void autoSelectCoupons_ShouldReturnBestMatchedCoupon() {
        TestLambdaQueryChainWrapper queryWrapper = new TestLambdaQueryChainWrapper();
        doReturn(queryWrapper).when(service).lambdaQuery();
        queryWrapper.setListResult(List.of(buildAvailableCoupon(1001L, "20.00")));

        List<CouponSettlementItemDto> items = List.of(
                CouponSettlementItemDto.builder()
                        .itemKey("ITEM_A")
                        .productId(10L)
                        .totalAmount(new BigDecimal("100.00"))
                        .couponEnabled(1)
                        .build()
        );

        CouponAutoSelectResultDto result = service.autoSelectCoupons(88L, items);

        assertEquals(1, result.getSelectedCoupons().size());
        assertEquals(1001L, result.getSelectedCoupons().getFirst().getCouponId());
        assertEquals(new BigDecimal("20.00"), result.getCouponDeductAmount());
        assertTrue(Boolean.TRUE.equals(result.getAutoSelected()));
        assertTrue(queryWrapper.hasLeColumn("effectiveTime"));
        assertTrue(queryWrapper.hasGeColumn("expireTime"));
    }

    /**
     * 验证结算页候选券查询会保留未过期但尚未生效的优惠券。
     */
    @Test
    void listMatchedCoupons_ShouldNotRequireEffectiveTimeInQuery() {
        TestLambdaQueryChainWrapper queryWrapper = new TestLambdaQueryChainWrapper();
        doReturn(queryWrapper).when(service).lambdaQuery();
        queryWrapper.setListResult(List.of(buildAvailableCoupon(1003L, "18.00")));

        List<CouponSettlementItemDto> items = List.of(
                CouponSettlementItemDto.builder()
                        .itemKey("ITEM_A")
                        .productId(10L)
                        .totalAmount(new BigDecimal("100.00"))
                        .couponEnabled(1)
                        .build()
        );

        List<OrderCouponOptionVo> result = service.listMatchedCoupons(88L, items);

        assertEquals(1, result.size());
        assertTrue(queryWrapper.hasGeColumn("expireTime"));
        assertFalse(queryWrapper.hasLeColumn("effectiveTime"));
    }

    /**
     * 验证客户端优惠券列表查询会过滤已过期优惠券，但不会额外要求已生效。
     */
    @Test
    void listCurrentUserCoupons_WhenQueryAvailable_ShouldOnlyFilterExpireTime() {
        TestLambdaQueryChainWrapper queryWrapper = new TestLambdaQueryChainWrapper();
        doReturn(88L).when(service).getUserId();
        doReturn(queryWrapper).when(service).lambdaQuery();
        Page<UserCoupon> pageResult = new Page<>(1, 20, 1);
        pageResult.setRecords(List.of(buildAvailableCoupon(1004L, "28.00")));
        queryWrapper.setPageResult(pageResult);

        UserCouponListRequest request = new UserCouponListRequest();
        request.setCouponStatus(UserCouponStatusEnum.AVAILABLE.getType());
        request.setPageNum(1);
        request.setPageSize(20);

        Page<UserCouponVo> result = service.listCurrentUserCoupons(request);

        assertEquals(1, result.getRecords().size());
        assertTrue(queryWrapper.hasGeColumn("expireTime"));
        assertFalse(queryWrapper.hasLeColumn("effectiveTime"));
    }

    /**
     * 验证个人中心优惠券数量统计只过滤已过期优惠券。
     */
    @Test
    void countDisplayableCoupons_ShouldOnlyFilterExpireTime() {
        TestLambdaQueryChainWrapper queryWrapper = new TestLambdaQueryChainWrapper();
        doReturn(queryWrapper).when(service).lambdaQuery();
        queryWrapper.setCountResult(5L);

        long result = service.countDisplayableCoupons(88L);

        assertEquals(5L, result);
        assertTrue(queryWrapper.hasGeColumn("expireTime"));
        assertFalse(queryWrapper.hasLeColumn("effectiveTime"));
    }

    /**
     * 验证锁券服务会批量锁定优惠券并返回聚合快照。
     */
    @Test
    void lockCoupons_ShouldLockCouponAndReturnSelectionSnapshot() {
        TestLambdaQueryChainWrapper queryWrapper = new TestLambdaQueryChainWrapper();
        doReturn(queryWrapper).when(service).lambdaQuery();
        queryWrapper.setOneResult(buildAvailableCoupon(1002L, "30.00"));

        TestLambdaUpdateChainWrapper updateWrapper = new TestLambdaUpdateChainWrapper();
        doReturn(updateWrapper).when(service).lambdaUpdate();
        updateWrapper.setUpdateResult(true);

        List<CouponSettlementItemDto> items = List.of(
                CouponSettlementItemDto.builder()
                        .itemKey("ITEM_A")
                        .productId(10L)
                        .totalAmount(new BigDecimal("100.00"))
                        .couponEnabled(1)
                        .build()
        );

        OrderCouponSelectionSnapshotDto snapshot = service.lockCoupons(88L, List.of(1002L), items, "O20260408001");

        assertEquals(1, snapshot.getSelectedCoupons().size());
        assertEquals(1, snapshot.getAppliedCoupons().size());
        assertEquals(new BigDecimal("30.00"), snapshot.getCouponDeductAmount());
        assertEquals(new BigDecimal("30.00"), snapshot.getCouponConsumeAmount());
        assertEquals(new BigDecimal("0.00"), snapshot.getCouponWasteAmount());
        assertFalse(Boolean.TRUE.equals(snapshot.getAutoSelected()));
        assertTrue(updateWrapper.isUpdateCalled());
        verify(couponLogMapper).insert(any(CouponLog.class));
    }

    /**
     * 构建可用优惠券测试数据。
     *
     * @param couponId        优惠券ID
     * @param availableAmount 可用金额
     * @return 用户优惠券实体
     */
    private UserCoupon buildAvailableCoupon(Long couponId, String availableAmount) {
        Date now = new Date();
        return UserCoupon.builder()
                .id(couponId)
                .userId(88L)
                .templateId(900L)
                .couponNameSnapshot("测试券")
                .thresholdAmount(new BigDecimal("0.00"))
                .availableAmount(new BigDecimal(availableAmount))
                .continueUseEnabled(1)
                .stackableEnabled(1)
                .couponStatus(UserCouponStatusEnum.AVAILABLE.getType())
                .effectiveTime(new Date(now.getTime() - 60_000L))
                .expireTime(new Date(now.getTime() + 86_400_000L))
                .version(1)
                .build();
    }

    /**
     * 初始化 MyBatis-Plus 元数据，避免 Lambda 字段解析异常。
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
     * 用户优惠券查询包装器测试替身。
     */
    private static final class TestLambdaQueryChainWrapper extends LambdaQueryChainWrapper<UserCoupon> {

        /**
         * 单条查询结果。
         */
        private UserCoupon oneResult;

        /**
         * 列表查询结果。
         */
        private List<UserCoupon> listResult = List.of();

        /**
         * 大于等于条件字段集合。
         */
        private final Set<String> geColumns = new HashSet<>();
        /**
         * 小于等于条件字段集合。
         */
        private final Set<String> leColumns = new HashSet<>();
        /**
         * 分页查询结果。
         */
        private Page<UserCoupon> pageResult;
        /**
         * 计数查询结果。
         */
        private long countResult;

        /**
         * 构造查询包装器测试替身。
         */
        private TestLambdaQueryChainWrapper() {
            super(UserCoupon.class);
        }

        /**
         * 设置单条查询结果。
         *
         * @param oneResult 单条查询结果
         */
        private void setOneResult(UserCoupon oneResult) {
            this.oneResult = oneResult;
        }

        /**
         * 设置列表查询结果。
         *
         * @param listResult 列表查询结果
         */
        private void setListResult(List<UserCoupon> listResult) {
            this.listResult = listResult == null ? List.of() : listResult;
        }

        /**
         * 设置分页查询结果。
         *
         * @param pageResult 分页查询结果
         */
        private void setPageResult(Page<UserCoupon> pageResult) {
            this.pageResult = pageResult;
        }

        /**
         * 设置计数查询结果。
         *
         * @param countResult 计数查询结果
         */
        private void setCountResult(long countResult) {
            this.countResult = countResult;
        }

        /**
         * 判断是否包含指定的大于等于条件字段。
         *
         * @param columnName 字段名
         * @return 是否包含指定字段
         */
        private boolean hasGeColumn(String columnName) {
            return geColumns.contains(columnName);
        }

        /**
         * 判断是否包含指定的小于等于条件字段。
         *
         * @param columnName 字段名
         * @return 是否包含指定字段
         */
        private boolean hasLeColumn(String columnName) {
            return leColumns.contains(columnName);
        }

        @Override
        public LambdaQueryChainWrapper<UserCoupon> eq(SFunction<UserCoupon, ?> column, Object val) {
            return this;
        }

        @Override
        public LambdaQueryChainWrapper<UserCoupon> ge(SFunction<UserCoupon, ?> column, Object val) {
            geColumns.add(resolvePropertyName(column));
            return this;
        }

        @Override
        public LambdaQueryChainWrapper<UserCoupon> le(SFunction<UserCoupon, ?> column, Object val) {
            leColumns.add(resolvePropertyName(column));
            return this;
        }

        @Override
        public UserCoupon one() {
            return oneResult;
        }

        @Override
        public List<UserCoupon> list() {
            return listResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E extends IPage<UserCoupon>> E page(E page) {
            return (E) (pageResult == null ? page : pageResult);
        }

        @Override
        public Long count() {
            return countResult;
        }

        /**
         * 解析 Lambda 表达式对应的字段名。
         *
         * @param column 字段 Lambda
         * @return 字段名
         */
        private String resolvePropertyName(SFunction<UserCoupon, ?> column) {
            try {
                Method writeReplaceMethod = column.getClass().getDeclaredMethod("writeReplace");
                writeReplaceMethod.setAccessible(true);
                SerializedLambda serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(column);
                String implMethodName = serializedLambda.getImplMethodName();
                if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
                    return Character.toLowerCase(implMethodName.charAt(3)) + implMethodName.substring(4);
                }
                if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
                    return Character.toLowerCase(implMethodName.charAt(2)) + implMethodName.substring(3);
                }
                return implMethodName;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("解析 Lambda 字段失败", exception);
            }
        }
    }

    /**
     * 用户优惠券更新包装器测试替身。
     */
    private static final class TestLambdaUpdateChainWrapper extends LambdaUpdateChainWrapper<UserCoupon> {

        /**
         * 更新执行结果。
         */
        private boolean updateResult;

        /**
         * 是否已触发更新。
         */
        private boolean updateCalled;

        /**
         * 构造更新包装器测试替身。
         */
        private TestLambdaUpdateChainWrapper() {
            super(UserCoupon.class);
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
         * 获取是否触发更新。
         *
         * @return 是否触发更新
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
