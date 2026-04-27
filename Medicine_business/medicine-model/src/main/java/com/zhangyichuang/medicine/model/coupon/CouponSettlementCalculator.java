package com.zhangyichuang.medicine.model.coupon;

import com.zhangyichuang.medicine.model.enums.CouponTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 优惠券结算计算器。
 */
public final class CouponSettlementCalculator {

    /**
     * 商品允许使用优惠券的标记值。
     */
    private static final int COUPON_ENABLED = 1;

    /**
     * 金额统一保留的小数位数。
     */
    private static final int MONEY_SCALE = 2;

    /**
     * 私有构造方法，禁止实例化。
     */
    private CouponSettlementCalculator() {
    }

    /**
     * 执行优惠券结算。
     *
     * @param couponSnapshot 优惠券快照
     * @param items          商品项列表
     * @return 优惠券结算结果
     */
    public static CouponSettlementResultDto calculate(OrderCouponSnapshotDto couponSnapshot,
                                                      List<CouponSettlementItemDto> items) {
        BigDecimal itemsAmount = normalizeAmount(sumItemsAmount(items));
        BigDecimal eligibleItemsAmount = normalizeAmount(sumEligibleItemsAmount(items));
        if (couponSnapshot == null) {
            return CouponSettlementResultDto.builder()
                    .itemsAmount(itemsAmount)
                    .eligibleItemsAmount(eligibleItemsAmount)
                    .couponDeductAmount(BigDecimal.ZERO)
                    .couponConsumeAmount(BigDecimal.ZERO)
                    .couponWasteAmount(BigDecimal.ZERO)
                    .allocations(buildZeroAllocations(items))
                    .build();
        }

        BigDecimal thresholdAmount = normalizeAmount(couponSnapshot.getThresholdAmount());
        BigDecimal lockedAvailableAmount = normalizeAmount(couponSnapshot.getLockedAvailableAmount());
        boolean thresholdMatched = eligibleItemsAmount.compareTo(thresholdAmount) >= 0;
        boolean fullReductionType = CouponTypeEnum.FULL_REDUCTION.getType().equals(couponSnapshot.getCouponType());

        if (!thresholdMatched || !fullReductionType || lockedAvailableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponSettlementResultDto.builder()
                    .itemsAmount(itemsAmount)
                    .eligibleItemsAmount(eligibleItemsAmount)
                    .couponDeductAmount(BigDecimal.ZERO)
                    .couponConsumeAmount(BigDecimal.ZERO)
                    .couponWasteAmount(BigDecimal.ZERO)
                    .allocations(buildZeroAllocations(items))
                    .build();
        }

        BigDecimal couponDeductAmount = eligibleItemsAmount.min(lockedAvailableAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal couponConsumeAmount;
        BigDecimal couponWasteAmount;
        if (Objects.equals(couponSnapshot.getContinueUseEnabled(), COUPON_ENABLED)) {
            couponConsumeAmount = couponDeductAmount;
            couponWasteAmount = BigDecimal.ZERO;
        } else {
            couponConsumeAmount = lockedAvailableAmount;
            couponWasteAmount = lockedAvailableAmount.subtract(couponDeductAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return CouponSettlementResultDto.builder()
                .itemsAmount(itemsAmount)
                .eligibleItemsAmount(eligibleItemsAmount)
                .couponDeductAmount(couponDeductAmount)
                .couponConsumeAmount(couponConsumeAmount)
                .couponWasteAmount(couponWasteAmount)
                .allocations(allocate(items, couponDeductAmount))
                .build();
    }

    /**
     * 汇总商品项总金额。
     *
     * @param items 商品项列表
     * @return 商品原始总金额
     */
    private static BigDecimal sumItemsAmount(List<CouponSettlementItemDto> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CouponSettlementItemDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总可用券商品金额。
     *
     * @param items 商品项列表
     * @return 可使用优惠券的商品总金额
     */
    private static BigDecimal sumEligibleItemsAmount(List<CouponSettlementItemDto> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(item -> Objects.equals(item.getCouponEnabled(), COUPON_ENABLED))
                .map(CouponSettlementItemDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 为商品项按比例分摊优惠金额。
     *
     * @param items              商品项列表
     * @param couponDeductAmount 订单抵扣金额
     * @return 商品项分摊列表
     */
    private static List<CouponSettlementAllocationDto> allocate(List<CouponSettlementItemDto> items,
                                                                BigDecimal couponDeductAmount) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        BigDecimal eligibleItemsAmount = sumEligibleItemsAmount(items);
        if (couponDeductAmount == null || couponDeductAmount.compareTo(BigDecimal.ZERO) <= 0
                || eligibleItemsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return buildZeroAllocations(items);
        }

        List<CouponSettlementItemDto> eligibleItems = items.stream()
                .filter(item -> Objects.equals(item.getCouponEnabled(), COUPON_ENABLED))
                .toList();
        BigDecimal remainingDeductAmount = couponDeductAmount;
        List<CouponSettlementAllocationDto> allocations = new ArrayList<>();
        for (CouponSettlementItemDto item : items) {
            if (!Objects.equals(item.getCouponEnabled(), COUPON_ENABLED)) {
                allocations.add(buildAllocation(item, BigDecimal.ZERO));
                continue;
            }
            BigDecimal itemDeductAmount;
            if (item.equals(eligibleItems.getLast())) {
                itemDeductAmount = remainingDeductAmount;
            } else {
                itemDeductAmount = normalizeAmount(item.getTotalAmount())
                        .multiply(couponDeductAmount)
                        .divide(eligibleItemsAmount, MONEY_SCALE, RoundingMode.HALF_UP);
                remainingDeductAmount = remainingDeductAmount.subtract(itemDeductAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            allocations.add(buildAllocation(item, itemDeductAmount));
        }
        return allocations;
    }

    /**
     * 构建零优惠分摊列表。
     *
     * @param items 商品项列表
     * @return 零优惠分摊列表
     */
    private static List<CouponSettlementAllocationDto> buildZeroAllocations(List<CouponSettlementItemDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<CouponSettlementAllocationDto> allocations = new ArrayList<>();
        for (CouponSettlementItemDto item : items) {
            allocations.add(buildAllocation(item, BigDecimal.ZERO));
        }
        return allocations;
    }

    /**
     * 构建单个商品项分摊结果。
     *
     * @param item               商品项
     * @param couponDeductAmount 分摊优惠金额
     * @return 商品项分摊结果
     */
    private static CouponSettlementAllocationDto buildAllocation(CouponSettlementItemDto item,
                                                                 BigDecimal couponDeductAmount) {
        BigDecimal normalizedItemAmount = normalizeAmount(item == null ? null : item.getTotalAmount());
        BigDecimal normalizedDeductAmount = normalizeAmount(couponDeductAmount);
        return CouponSettlementAllocationDto.builder()
                .itemKey(item == null ? null : item.getItemKey())
                .couponDeductAmount(normalizedDeductAmount)
                .payableAmount(normalizedItemAmount.subtract(normalizedDeductAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 规范化金额，统一处理空值与小数位。
     *
     * @param amount 原始金额
     * @return 规范化后的金额
     */
    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
