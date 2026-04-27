package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.enums.CouponTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动选券算法单元测试。
 */
class CouponAutoSelectCalculatorTests {

    /**
     * 商品允许使用优惠券的标记值。
     */
    private static final Integer COUPON_ENABLED = 1;

    /**
     * 商品不允许使用优惠券的标记值。
     */
    private static final Integer COUPON_DISABLED = 0;

    /**
     * 继续使用标记值。
     */
    private static final Integer CONTINUE_ENABLED = 1;

    /**
     * 不允许继续使用标记值。
     */
    private static final Integer CONTINUE_DISABLED = 0;

    /**
     * 允许叠加标记值。
     */
    private static final Integer STACKABLE_ENABLED = 1;

    /**
     * 不允许叠加标记值。
     */
    private static final Integer STACKABLE_DISABLED = 0;

    /**
     * 验证单券命中门槛后可正确抵扣。
     */
    @Test
    void selectBestCoupons_WhenSingleCouponMatches_ShouldApplySingleCoupon() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_A", 101L, "100.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "单券", "50.00", "20.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.selectBestCoupons(coupons, items);

        assertEquals(1, result.getSelectedCoupons().size());
        assertEquals(new BigDecimal("20.00"), result.getCouponDeductAmount());
        assertEquals(new BigDecimal("20.00"), result.getCouponConsumeAmount());
        assertEquals(new BigDecimal("0.00"), result.getCouponWasteAmount());
        assertEquals(new BigDecimal("20.00"), findAllocationAmount(result.getAllocations(), "ITEM_A"));
    }

    /**
     * 验证两张可叠加券可以连续生效。
     */
    @Test
    void selectBestCoupons_WhenTwoCouponsStackable_ShouldApplyBothCoupons() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_A", 101L, "100.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "券1", "0.00", "30.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L)),
                buildCoupon(2L, "券2", "0.00", "40.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.selectBestCoupons(coupons, items);

        assertEquals(2, result.getSelectedCoupons().size());
        assertEquals(new BigDecimal("70.00"), result.getCouponDeductAmount());
        assertEquals(new BigDecimal("70.00"), result.getCouponConsumeAmount());
        assertEquals(new BigDecimal("0.00"), result.getCouponWasteAmount());
    }

    /**
     * 验证非叠加券被应用后会终止后续券。
     */
    @Test
    void applyCouponsInOrder_WhenCouponIsNonStackable_ShouldStopApplyingAfterCurrentCoupon() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_A", 101L, "100.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "非叠加券", "0.00", "50.00", CONTINUE_ENABLED, STACKABLE_DISABLED, List.of(101L)),
                buildCoupon(2L, "后续券", "0.00", "30.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.applyCouponsInOrder(coupons, items);

        assertEquals(1, result.getSelectedCoupons().size());
        assertEquals(1L, result.getSelectedCoupons().getFirst().getCouponId());
        assertEquals(new BigDecimal("50.00"), result.getCouponDeductAmount());
    }

    /**
     * 验证 continueUseEnabled=0 时，消耗金额与浪费金额按锁定额度计算。
     */
    @Test
    void applyCouponsInOrder_WhenContinueUseDisabled_ShouldCalculateConsumeAndWaste() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_A", 101L, "30.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "不可续用券", "0.00", "50.00", CONTINUE_DISABLED, STACKABLE_ENABLED, List.of(101L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.applyCouponsInOrder(coupons, items);

        assertEquals(new BigDecimal("30.00"), result.getCouponDeductAmount());
        assertEquals(new BigDecimal("50.00"), result.getCouponConsumeAmount());
        assertEquals(new BigDecimal("20.00"), result.getCouponWasteAmount());
    }

    /**
     * 验证分摊顺序为最低价商品优先。
     */
    @Test
    void applyCouponsInOrder_WhenAllocating_ShouldDeductLowerPricedItemFirst() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_LOW", 101L, "30.00", COUPON_ENABLED),
                buildItem("ITEM_HIGH", 102L, "70.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "顺序券", "0.00", "40.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L, 102L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.applyCouponsInOrder(coupons, items);

        assertEquals(new BigDecimal("30.00"), findAllocationAmount(result.getAllocations(), "ITEM_LOW"));
        assertEquals(new BigDecimal("10.00"), findAllocationAmount(result.getAllocations(), "ITEM_HIGH"));
    }

    /**
     * 验证算法使用全局搜索而非贪心。
     */
    @Test
    void selectBestCoupons_WhenGreedyWouldFail_ShouldReturnGlobalBestCombination() {
        List<CouponSettlementItemDto> items = List.of(
                buildItem("ITEM_A", 101L, "40.00", COUPON_ENABLED),
                buildItem("ITEM_B", 102L, "40.00", COUPON_ENABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "大额非叠加", "0.00", "60.00", CONTINUE_ENABLED, STACKABLE_DISABLED, List.of(101L, 102L)),
                buildCoupon(2L, "A券", "0.00", "40.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(101L)),
                buildCoupon(3L, "B券", "0.00", "40.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(102L))
        );

        CouponAutoSelectResultDto result = CouponAutoSelectCalculator.selectBestCoupons(coupons, items);
        List<Long> selectedCouponIds = result.getSelectedCoupons().stream()
                .map(OrderCouponSnapshotDto::getCouponId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        assertEquals(2, selectedCouponIds.size());
        assertEquals(new BigDecimal("80.00"), result.getCouponDeductAmount());
    }

    /**
     * 验证边界场景：无可用券、无可券商品、商品金额为0。
     */
    @Test
    void selectBestCoupons_WhenBoundaryCases_ShouldReturnZeroResult() {
        List<CouponSettlementItemDto> normalItems = List.of(
                buildItem("ITEM_A", 101L, "50.00", COUPON_ENABLED)
        );
        CouponAutoSelectResultDto emptyCouponResult = CouponAutoSelectCalculator.selectBestCoupons(List.of(), normalItems);
        assertEquals(new BigDecimal("0.00"), emptyCouponResult.getCouponDeductAmount());

        List<CouponSettlementItemDto> disabledItems = List.of(
                buildItem("ITEM_B", 102L, "50.00", COUPON_DISABLED)
        );
        List<OrderCouponSnapshotDto> coupons = List.of(
                buildCoupon(1L, "不可命中券", "0.00", "20.00", CONTINUE_ENABLED, STACKABLE_ENABLED, List.of(102L))
        );
        CouponAutoSelectResultDto disabledItemResult = CouponAutoSelectCalculator.selectBestCoupons(coupons, disabledItems);
        assertEquals(new BigDecimal("0.00"), disabledItemResult.getCouponDeductAmount());

        List<CouponSettlementItemDto> zeroAmountItems = List.of(
                buildItem("ITEM_ZERO", 103L, "0.00", COUPON_ENABLED)
        );
        CouponAutoSelectResultDto zeroAmountResult = CouponAutoSelectCalculator.selectBestCoupons(coupons, zeroAmountItems);
        assertEquals(new BigDecimal("0.00"), zeroAmountResult.getCouponDeductAmount());
        assertTrue(zeroAmountResult.getAllocations().isEmpty());
    }

    /**
     * 构建测试商品结算项。
     *
     * @param itemKey       商品项业务键
     * @param productId     商品ID
     * @param totalAmount   商品总金额
     * @param couponEnabled 是否允许使用优惠券
     * @return 商品结算项
     */
    private CouponSettlementItemDto buildItem(String itemKey,
                                              Long productId,
                                              String totalAmount,
                                              Integer couponEnabled) {
        return CouponSettlementItemDto.builder()
                .itemKey(itemKey)
                .productId(productId)
                .totalAmount(new BigDecimal(totalAmount))
                .couponEnabled(couponEnabled)
                .build();
    }

    /**
     * 构建测试优惠券快照。
     *
     * @param couponId           优惠券ID
     * @param couponName         优惠券名称
     * @param thresholdAmount    使用门槛
     * @param lockedAmount       锁定可用金额
     * @param continueUseEnabled 是否允许继续使用
     * @param stackableEnabled   是否允许叠加
     * @param eligibleProductIds 可用商品ID集合
     * @return 优惠券快照
     */
    private OrderCouponSnapshotDto buildCoupon(Long couponId,
                                               String couponName,
                                               String thresholdAmount,
                                               String lockedAmount,
                                               Integer continueUseEnabled,
                                               Integer stackableEnabled,
                                               List<Long> eligibleProductIds) {
        long now = System.currentTimeMillis();
        return OrderCouponSnapshotDto.builder()
                .couponId(couponId)
                .templateId(couponId)
                .couponName(couponName)
                .couponType(CouponTypeEnum.FULL_REDUCTION.getType())
                .thresholdAmount(new BigDecimal(thresholdAmount))
                .lockedAvailableAmount(new BigDecimal(lockedAmount))
                .continueUseEnabled(continueUseEnabled)
                .stackableEnabled(stackableEnabled)
                .effectiveTime(new Date(now - 60_000L))
                .expireTime(new Date(now + 86_400_000L))
                .eligibleProductIds(eligibleProductIds)
                .build();
    }

    /**
     * 查找商品项分摊金额。
     *
     * @param allocations 分摊集合
     * @param itemKey     商品项业务键
     * @return 商品分摊金额
     */
    private BigDecimal findAllocationAmount(List<CouponSettlementAllocationDto> allocations,
                                            String itemKey) {
        Map<String, BigDecimal> allocationMap = allocations.stream()
                .collect(Collectors.toMap(CouponSettlementAllocationDto::getItemKey,
                        CouponSettlementAllocationDto::getCouponDeductAmount,
                        (left, right) -> left));
        return allocationMap.getOrDefault(itemKey, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
