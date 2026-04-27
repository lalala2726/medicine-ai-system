package com.zhangyichuang.medicine.model.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自动选券计算器。
 *
 * <p>
 * 核心能力：
 * </p>
 * <ol>
 * <li>在候选券集合中搜索“总抵扣最大”的最优券组合（全局搜索，不使用贪心近似）。</li>
 * <li>每张券应用时，按“商品剩余应付金额从低到高”执行扣减（最低价商品优先）。</li>
 * <li>支持 continueUseEnabled / stackableEnabled 业务规则，并输出订单聚合结果与商品分摊结果。</li>
 * </ol>
 *
 * <p>
 * 重要约束：
 * </p>
 * <ol>
 * <li>每张券门槛校验按“原始商品金额”执行，不受前序券抵扣影响。</li>
 * <li>金额统一按 2 位小数（HALF_UP）计算，保证跨模块结算一致性。</li>
 * </ol>
 */
public final class CouponAutoSelectCalculator {

    /**
     * 商品允许使用优惠券的标记值。
     */
    private static final int COUPON_ENABLED_FLAG = 1;

    /**
     * 继续使用开关的允许值。
     */
    private static final int CONTINUE_USE_ENABLED_FLAG = 1;

    /**
     * 叠加开关的允许值。
     */
    private static final int STACKABLE_ENABLED_FLAG = 1;

    /**
     * 金额统一保留的小数位数。
     */
    private static final int MONEY_SCALE = 2;

    /**
     * 金额零值常量。
     */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    /**
     * 私有构造方法，禁止实例化工具类。
     */
    private CouponAutoSelectCalculator() {
    }

    /**
     * 自动搜索最优优惠券组合。
     *
     * <p>
     * 算法过程：
     * </p>
     * <ol>
     * <li>预处理：标准化商品与候选券，过滤掉无效券。</li>
     * <li>搜索：使用 DFS + 上界剪枝遍历可行应用序列，保证“总抵扣最大”全局最优。</li>
     * <li>并列决策：总抵扣相同则优先总浪费更低，再优先到期更早，再优先券ID字典序更小。</li>
     * <li>输出：返回选中券、券明细、商品分摊、订单聚合金额。</li>
     * </ol>
     *
     * @param candidateCoupons 候选优惠券快照集合（来源于可用券列表）。
     * @param items            商品结算项集合（包含 itemKey、productId、totalAmount、couponEnabled）。
     * @return CouponAutoSelectResultDto 自动选券结果；无可用券时返回全零结果。
     */
    public static CouponAutoSelectResultDto selectBestCoupons(List<OrderCouponSnapshotDto> candidateCoupons,
                                                              List<CouponSettlementItemDto> items) {
        List<ItemState> initialItems = buildInitialItemStates(items);
        if (initialItems.isEmpty()) {
            return buildEmptyResult(initialItems, true);
        }

        Date now = new Date();
        List<OrderCouponSnapshotDto> normalizedCandidates = normalizeAndFilterCandidates(candidateCoupons, initialItems, now);
        if (normalizedCandidates.isEmpty()) {
            return buildEmptyResult(initialItems, true);
        }

        List<ItemState> searchItems = copyItems(initialItems);
        boolean[] usedFlags = new boolean[normalizedCandidates.size()];
        SearchBestHolder bestHolder = new SearchBestHolder(buildEmptyResult(initialItems, true));
        dfsSearch(
                normalizedCandidates,
                initialItems,
                searchItems,
                usedFlags,
                new ArrayList<>(),
                new ArrayList<>(),
                new LinkedHashMap<>(),
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                bestHolder
        );
        CouponAutoSelectResultDto bestResult = bestHolder.bestResult;
        bestResult.setAutoSelected(Boolean.TRUE);
        return bestResult;
    }

    /**
     * 按指定顺序应用优惠券集合。
     *
     * <p>
     * 该方法用于“手选券下单”或“已确定券集合后的重算”场景，
     * 不做全局最优搜索，仅严格按照传入顺序应用并返回结果。
     * </p>
     *
     * @param orderedCoupons 需按顺序应用的优惠券快照集合。
     * @param items          商品结算项集合。
     * @return CouponAutoSelectResultDto 顺序应用结果；无可应用券时返回全零结果。
     */
    public static CouponAutoSelectResultDto applyCouponsInOrder(List<OrderCouponSnapshotDto> orderedCoupons,
                                                                List<CouponSettlementItemDto> items) {
        List<ItemState> initialItems = buildInitialItemStates(items);
        if (initialItems.isEmpty()) {
            return buildEmptyResult(initialItems, false);
        }

        Date now = new Date();
        List<OrderCouponSnapshotDto> normalizedCoupons = normalizeAndFilterCandidates(orderedCoupons, initialItems, now);
        if (normalizedCoupons.isEmpty()) {
            return buildEmptyResult(initialItems, false);
        }

        List<ItemState> runningItems = copyItems(initialItems);
        Map<String, BigDecimal> allocationMap = new LinkedHashMap<>();
        List<OrderCouponSnapshotDto> selectedCoupons = new ArrayList<>();
        List<CouponAppliedDetailDto> appliedCoupons = new ArrayList<>();
        BigDecimal totalDeductAmount = ZERO_AMOUNT;
        BigDecimal totalConsumeAmount = ZERO_AMOUNT;
        BigDecimal totalWasteAmount = ZERO_AMOUNT;

        for (OrderCouponSnapshotDto couponSnapshot : normalizedCoupons) {
            CouponApplicationResult applicationResult = applySingleCoupon(couponSnapshot, initialItems, runningItems);
            if (applicationResult == null) {
                continue;
            }
            selectedCoupons.add(couponSnapshot);
            appliedCoupons.add(applicationResult.appliedDetail());
            totalDeductAmount = normalizeAmount(totalDeductAmount.add(applicationResult.appliedDetail().getCouponDeductAmount()));
            totalConsumeAmount = normalizeAmount(totalConsumeAmount.add(applicationResult.appliedDetail().getCouponConsumeAmount()));
            totalWasteAmount = normalizeAmount(totalWasteAmount.add(applicationResult.appliedDetail().getCouponWasteAmount()));
            mergeAllocationMap(allocationMap, applicationResult.itemDeductMap());
            runningItems = applicationResult.nextItems();
            if (!applicationResult.canContinue()) {
                break;
            }
        }

        return buildResult(
                initialItems,
                selectedCoupons,
                appliedCoupons,
                allocationMap,
                totalDeductAmount,
                totalConsumeAmount,
                totalWasteAmount,
                false
        );
    }

    /**
     * 递归搜索最优券组合。
     *
     * <p>
     * 状态定义：
     * </p>
     * <ol>
     * <li>remainingItems：每个商品项剩余可扣金额。</li>
     * <li>usedFlags：已使用的候选券标记。</li>
     * <li>selectedCoupons/appliedCoupons：当前路径已选择券及其结果。</li>
     * <li>allocationMap：当前路径下商品累计分摊。</li>
     * <li>totalDeduct/totalConsume/totalWaste：当前路径聚合金额。</li>
     * </ol>
     *
     * <p>
     * 剪枝条件：
     * </p>
     * <ol>
     * <li>上界估计（当前抵扣 + 未使用券的理论最大附加抵扣）不超过当前最优时直接剪枝。</li>
     * </ol>
     *
     * @param candidateCoupons   候选券集合。
     * @param originalItems      原始商品项（门槛判断使用）。
     * @param remainingItems     当前剩余商品项状态。
     * @param usedFlags          已使用券标记。
     * @param selectedCoupons    当前路径选中的券快照列表。
     * @param appliedCoupons     当前路径券应用明细列表。
     * @param allocationMap      当前路径商品累计分摊。
     * @param totalDeductAmount  当前路径累计抵扣。
     * @param totalConsumeAmount 当前路径累计消耗。
     * @param totalWasteAmount   当前路径累计浪费。
     * @param bestHolder         当前全局最优结果容器。
     * @return 无返回值；最优结果通过 bestHolder 回写。
     */
    private static void dfsSearch(List<OrderCouponSnapshotDto> candidateCoupons,
                                  List<ItemState> originalItems,
                                  List<ItemState> remainingItems,
                                  boolean[] usedFlags,
                                  List<OrderCouponSnapshotDto> selectedCoupons,
                                  List<CouponAppliedDetailDto> appliedCoupons,
                                  Map<String, BigDecimal> allocationMap,
                                  BigDecimal totalDeductAmount,
                                  BigDecimal totalConsumeAmount,
                                  BigDecimal totalWasteAmount,
                                  SearchBestHolder bestHolder) {
        CouponAutoSelectResultDto currentResult = buildResult(
                originalItems,
                selectedCoupons,
                appliedCoupons,
                allocationMap,
                totalDeductAmount,
                totalConsumeAmount,
                totalWasteAmount,
                true
        );
        if (isBetterResult(currentResult, bestHolder.bestResult)) {
            bestHolder.bestResult = currentResult;
        }

        BigDecimal upperBound = estimateUpperBoundDeduct(candidateCoupons, remainingItems, usedFlags, totalDeductAmount, originalItems);
        if (upperBound.compareTo(bestHolder.bestResult.getCouponDeductAmount()) <= 0) {
            return;
        }

        for (int couponIndex = 0; couponIndex < candidateCoupons.size(); couponIndex++) {
            if (usedFlags[couponIndex]) {
                continue;
            }
            OrderCouponSnapshotDto candidateCoupon = candidateCoupons.get(couponIndex);
            CouponApplicationResult applicationResult = applySingleCoupon(candidateCoupon, originalItems, remainingItems);
            if (applicationResult == null) {
                continue;
            }

            usedFlags[couponIndex] = true;
            selectedCoupons.add(candidateCoupon);
            appliedCoupons.add(applicationResult.appliedDetail());
            Map<String, BigDecimal> nextAllocationMap = copyAllocationMap(allocationMap);
            mergeAllocationMap(nextAllocationMap, applicationResult.itemDeductMap());
            BigDecimal nextTotalDeductAmount = normalizeAmount(totalDeductAmount.add(applicationResult.appliedDetail().getCouponDeductAmount()));
            BigDecimal nextTotalConsumeAmount = normalizeAmount(totalConsumeAmount.add(applicationResult.appliedDetail().getCouponConsumeAmount()));
            BigDecimal nextTotalWasteAmount = normalizeAmount(totalWasteAmount.add(applicationResult.appliedDetail().getCouponWasteAmount()));

            if (applicationResult.canContinue()) {
                dfsSearch(
                        candidateCoupons,
                        originalItems,
                        applicationResult.nextItems(),
                        usedFlags,
                        selectedCoupons,
                        appliedCoupons,
                        nextAllocationMap,
                        nextTotalDeductAmount,
                        nextTotalConsumeAmount,
                        nextTotalWasteAmount,
                        bestHolder
                );
            } else {
                CouponAutoSelectResultDto nonStackableResult = buildResult(
                        originalItems,
                        selectedCoupons,
                        appliedCoupons,
                        nextAllocationMap,
                        nextTotalDeductAmount,
                        nextTotalConsumeAmount,
                        nextTotalWasteAmount,
                        true
                );
                if (isBetterResult(nonStackableResult, bestHolder.bestResult)) {
                    bestHolder.bestResult = nonStackableResult;
                }
            }

            appliedCoupons.remove(appliedCoupons.size() - 1);
            selectedCoupons.remove(selectedCoupons.size() - 1);
            usedFlags[couponIndex] = false;
        }
    }

    /**
     * 估算当前搜索状态的抵扣上界。
     *
     * <p>
     * 上界用于剪枝：当前抵扣 + 每张未使用券在当前剩余商品上的理论最大可抵扣。
     * 该上界允许高估，但不能低估。
     * </p>
     *
     * @param candidateCoupons    候选券集合。
     * @param remainingItems      当前剩余商品项状态。
     * @param usedFlags           已使用券标记。
     * @param currentDeductAmount 当前累计抵扣。
     * @param originalItems       原始商品项（门槛判断使用）。
     * @return BigDecimal 抵扣上界估计值。
     */
    private static BigDecimal estimateUpperBoundDeduct(List<OrderCouponSnapshotDto> candidateCoupons,
                                                       List<ItemState> remainingItems,
                                                       boolean[] usedFlags,
                                                       BigDecimal currentDeductAmount,
                                                       List<ItemState> originalItems) {
        BigDecimal upperBoundAmount = normalizeAmount(currentDeductAmount);
        for (int couponIndex = 0; couponIndex < candidateCoupons.size(); couponIndex++) {
            if (usedFlags[couponIndex]) {
                continue;
            }
            OrderCouponSnapshotDto couponSnapshot = candidateCoupons.get(couponIndex);
            if (!isThresholdMatched(couponSnapshot, originalItems)) {
                continue;
            }
            BigDecimal remainingEligibleAmount = sumEligibleRemainingAmount(couponSnapshot, remainingItems);
            BigDecimal potentialDeductAmount = normalizeAmount(couponSnapshot.getLockedAvailableAmount()).min(remainingEligibleAmount);
            upperBoundAmount = normalizeAmount(upperBoundAmount.add(potentialDeductAmount));
        }
        return upperBoundAmount;
    }

    /**
     * 对单张券执行应用计算。
     *
     * <p>
     * 关键业务规则：
     * </p>
     * <ol>
     * <li>门槛按原始商品金额判断。</li>
     * <li>分配顺序按当前剩余金额升序（最低价优先）。</li>
     * <li>continueUseEnabled 决定消耗/浪费。</li>
     * <li>stackableEnabled 决定后续是否允许继续叠加。</li>
     * </ol>
     *
     * @param couponSnapshot 优惠券快照。
     * @param originalItems  原始商品项（门槛判断使用）。
     * @param remainingItems 当前剩余商品项。
     * @return CouponApplicationResult 单券应用结果；不命中时返回 null。
     */
    private static CouponApplicationResult applySingleCoupon(OrderCouponSnapshotDto couponSnapshot,
                                                             List<ItemState> originalItems,
                                                             List<ItemState> remainingItems) {
        if (couponSnapshot == null) {
            return null;
        }
        if (!isThresholdMatched(couponSnapshot, originalItems)) {
            return null;
        }

        BigDecimal lockedAvailableAmount = normalizeAmount(couponSnapshot.getLockedAvailableAmount());
        if (lockedAvailableAmount.compareTo(ZERO_AMOUNT) <= 0) {
            return null;
        }

        List<Integer> eligibleIndexes = findEligibleIndexesForDeduct(couponSnapshot, remainingItems);
        if (eligibleIndexes.isEmpty()) {
            return null;
        }

        BigDecimal remainingEligibleAmount = eligibleIndexes.stream()
                .map(index -> remainingItems.get(index).remainingAmount)
                .reduce(ZERO_AMOUNT, BigDecimal::add);
        BigDecimal couponDeductAmount = normalizeAmount(remainingEligibleAmount.min(lockedAvailableAmount));
        if (couponDeductAmount.compareTo(ZERO_AMOUNT) <= 0) {
            return null;
        }

        List<ItemState> nextItems = copyItems(remainingItems);
        Map<String, BigDecimal> itemDeductMap = new LinkedHashMap<>();
        BigDecimal deductLeftAmount = couponDeductAmount;
        for (Integer index : eligibleIndexes) {
            if (deductLeftAmount.compareTo(ZERO_AMOUNT) <= 0) {
                break;
            }
            ItemState currentItem = nextItems.get(index);
            BigDecimal itemDeductAmount = currentItem.remainingAmount.min(deductLeftAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (itemDeductAmount.compareTo(ZERO_AMOUNT) <= 0) {
                continue;
            }
            currentItem.remainingAmount = normalizeAmount(currentItem.remainingAmount.subtract(itemDeductAmount));
            deductLeftAmount = normalizeAmount(deductLeftAmount.subtract(itemDeductAmount));
            itemDeductMap.put(currentItem.itemKey, itemDeductAmount);
        }

        couponDeductAmount = normalizeAmount(couponDeductAmount.subtract(deductLeftAmount));
        if (couponDeductAmount.compareTo(ZERO_AMOUNT) <= 0) {
            return null;
        }

        BigDecimal couponConsumeAmount;
        BigDecimal couponWasteAmount;
        if (Objects.equals(couponSnapshot.getContinueUseEnabled(), CONTINUE_USE_ENABLED_FLAG)) {
            couponConsumeAmount = couponDeductAmount;
            couponWasteAmount = ZERO_AMOUNT;
        } else {
            couponConsumeAmount = lockedAvailableAmount;
            couponWasteAmount = normalizeAmount(lockedAvailableAmount.subtract(couponDeductAmount));
        }

        CouponAppliedDetailDto appliedDetail = CouponAppliedDetailDto.builder()
                .couponId(couponSnapshot.getCouponId())
                .couponName(couponSnapshot.getCouponName())
                .couponDeductAmount(couponDeductAmount)
                .couponConsumeAmount(couponConsumeAmount)
                .couponWasteAmount(couponWasteAmount)
                .continueUseEnabled(couponSnapshot.getContinueUseEnabled())
                .stackableEnabled(couponSnapshot.getStackableEnabled())
                .build();

        boolean canContinue = Objects.equals(couponSnapshot.getStackableEnabled(), STACKABLE_ENABLED_FLAG);
        return new CouponApplicationResult(nextItems, itemDeductMap, appliedDetail, canContinue);
    }

    /**
     * 查找单张券可参与扣减的商品索引列表。
     *
     * <p>
     * 返回顺序即实际扣减顺序：按剩余金额升序（最低价优先），金额相同按 itemKey 升序稳定排序。
     * </p>
     *
     * @param couponSnapshot 优惠券快照。
     * @param remainingItems 当前剩余商品项。
     * @return List<Integer> 可参与扣减的索引集合。
     */
    private static List<Integer> findEligibleIndexesForDeduct(OrderCouponSnapshotDto couponSnapshot,
                                                              List<ItemState> remainingItems) {
        Set<Long> eligibleProductIdSet = buildEligibleProductIdSet(couponSnapshot);
        List<Integer> eligibleIndexes = new ArrayList<>();
        for (int index = 0; index < remainingItems.size(); index++) {
            ItemState itemState = remainingItems.get(index);
            if (!Objects.equals(itemState.couponEnabled, COUPON_ENABLED_FLAG)) {
                continue;
            }
            if (itemState.remainingAmount.compareTo(ZERO_AMOUNT) <= 0) {
                continue;
            }
            if (!isProductMatched(eligibleProductIdSet, itemState.productId)) {
                continue;
            }
            eligibleIndexes.add(index);
        }
        eligibleIndexes.sort((leftIndex, rightIndex) -> {
            ItemState leftItem = remainingItems.get(leftIndex);
            ItemState rightItem = remainingItems.get(rightIndex);
            int remainingCompare = leftItem.remainingAmount.compareTo(rightItem.remainingAmount);
            if (remainingCompare != 0) {
                return remainingCompare;
            }
            return leftItem.itemKey.compareTo(rightItem.itemKey);
        });
        return eligibleIndexes;
    }

    /**
     * 计算单张券在当前剩余状态下的可抵扣剩余额度。
     *
     * @param couponSnapshot 优惠券快照。
     * @param remainingItems 当前剩余商品项。
     * @return BigDecimal 可抵扣剩余金额。
     */
    private static BigDecimal sumEligibleRemainingAmount(OrderCouponSnapshotDto couponSnapshot,
                                                         List<ItemState> remainingItems) {
        Set<Long> eligibleProductIdSet = buildEligibleProductIdSet(couponSnapshot);
        BigDecimal eligibleAmount = ZERO_AMOUNT;
        for (ItemState itemState : remainingItems) {
            if (!Objects.equals(itemState.couponEnabled, COUPON_ENABLED_FLAG)) {
                continue;
            }
            if (!isProductMatched(eligibleProductIdSet, itemState.productId)) {
                continue;
            }
            eligibleAmount = normalizeAmount(eligibleAmount.add(itemState.remainingAmount));
        }
        return eligibleAmount;
    }

    /**
     * 判断单张券是否满足门槛（按原始商品金额）。
     *
     * @param couponSnapshot 优惠券快照。
     * @param originalItems  原始商品项。
     * @return boolean 是否满足门槛。
     */
    private static boolean isThresholdMatched(OrderCouponSnapshotDto couponSnapshot,
                                              List<ItemState> originalItems) {
        BigDecimal thresholdAmount = normalizeAmount(couponSnapshot == null ? null : couponSnapshot.getThresholdAmount());
        BigDecimal originalEligibleAmount = sumEligibleOriginalAmount(couponSnapshot, originalItems);
        return originalEligibleAmount.compareTo(thresholdAmount) >= 0;
    }

    /**
     * 计算单张券可命中商品的原始金额总和。
     *
     * @param couponSnapshot 优惠券快照。
     * @param originalItems  原始商品项。
     * @return BigDecimal 原始可命中金额。
     */
    private static BigDecimal sumEligibleOriginalAmount(OrderCouponSnapshotDto couponSnapshot,
                                                        List<ItemState> originalItems) {
        Set<Long> eligibleProductIdSet = buildEligibleProductIdSet(couponSnapshot);
        BigDecimal amount = ZERO_AMOUNT;
        for (ItemState itemState : originalItems) {
            if (!Objects.equals(itemState.couponEnabled, COUPON_ENABLED_FLAG)) {
                continue;
            }
            if (!isProductMatched(eligibleProductIdSet, itemState.productId)) {
                continue;
            }
            amount = normalizeAmount(amount.add(itemState.totalAmount));
        }
        return amount;
    }

    /**
     * 构建候选券商品ID匹配集合。
     *
     * @param couponSnapshot 优惠券快照。
     * @return Set<Long> 商品ID集合；为空表示按商品券标记兜底匹配。
     */
    private static Set<Long> buildEligibleProductIdSet(OrderCouponSnapshotDto couponSnapshot) {
        if (couponSnapshot == null || couponSnapshot.getEligibleProductIds() == null) {
            return Set.of();
        }
        return couponSnapshot.getEligibleProductIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 判断商品是否命中券可用商品集合。
     *
     * @param eligibleProductIdSet 优惠券可用商品ID集合。
     * @param productId            商品ID。
     * @return boolean 是否命中。
     */
    private static boolean isProductMatched(Set<Long> eligibleProductIdSet, Long productId) {
        if (eligibleProductIdSet == null || eligibleProductIdSet.isEmpty()) {
            return true;
        }
        return productId != null && eligibleProductIdSet.contains(productId);
    }

    /**
     * 规范化并过滤候选券集合。
     *
     * @param candidateCoupons 候选券集合。
     * @param originalItems    原始商品项。
     * @param now              当前时间。
     * @return List<OrderCouponSnapshotDto> 可参与计算的券集合。
     */
    private static List<OrderCouponSnapshotDto> normalizeAndFilterCandidates(List<OrderCouponSnapshotDto> candidateCoupons,
                                                                             List<ItemState> originalItems,
                                                                             Date now) {
        if (candidateCoupons == null || candidateCoupons.isEmpty()) {
            return List.of();
        }
        List<OrderCouponSnapshotDto> result = new ArrayList<>();
        for (OrderCouponSnapshotDto candidateCoupon : candidateCoupons) {
            if (candidateCoupon == null) {
                continue;
            }
            BigDecimal availableAmount = normalizeAmount(candidateCoupon.getLockedAvailableAmount());
            if (availableAmount.compareTo(ZERO_AMOUNT) <= 0) {
                continue;
            }
            if (candidateCoupon.getEffectiveTime() != null && candidateCoupon.getEffectiveTime().after(now)) {
                continue;
            }
            if (candidateCoupon.getExpireTime() != null && candidateCoupon.getExpireTime().before(now)) {
                continue;
            }
            if (!isThresholdMatched(candidateCoupon, originalItems)) {
                continue;
            }
            if (sumEligibleOriginalAmount(candidateCoupon, originalItems).compareTo(ZERO_AMOUNT) <= 0) {
                continue;
            }
            result.add(candidateCoupon);
        }
        result.sort((leftCoupon, rightCoupon) -> {
            Date leftExpireTime = leftCoupon.getExpireTime();
            Date rightExpireTime = rightCoupon.getExpireTime();
            if (leftExpireTime != null && rightExpireTime != null) {
                int expireCompare = leftExpireTime.compareTo(rightExpireTime);
                if (expireCompare != 0) {
                    return expireCompare;
                }
            } else if (leftExpireTime != null) {
                return -1;
            } else if (rightExpireTime != null) {
                return 1;
            }
            Long leftCouponId = leftCoupon.getCouponId();
            Long rightCouponId = rightCoupon.getCouponId();
            if (leftCouponId == null && rightCouponId == null) {
                return 0;
            }
            if (leftCouponId == null) {
                return 1;
            }
            if (rightCouponId == null) {
                return -1;
            }
            return leftCouponId.compareTo(rightCouponId);
        });
        return result;
    }

    /**
     * 判断候选结果是否优于当前最优结果。
     *
     * @param candidateResult 候选结果。
     * @param bestResult      当前最优结果。
     * @return boolean 候选是否更优。
     */
    private static boolean isBetterResult(CouponAutoSelectResultDto candidateResult,
                                          CouponAutoSelectResultDto bestResult) {
        BigDecimal candidateDeductAmount = normalizeAmount(candidateResult == null ? null : candidateResult.getCouponDeductAmount());
        BigDecimal bestDeductAmount = normalizeAmount(bestResult == null ? null : bestResult.getCouponDeductAmount());
        int deductCompare = candidateDeductAmount.compareTo(bestDeductAmount);
        if (deductCompare != 0) {
            return deductCompare > 0;
        }

        BigDecimal candidateWasteAmount = normalizeAmount(candidateResult == null ? null : candidateResult.getCouponWasteAmount());
        BigDecimal bestWasteAmount = normalizeAmount(bestResult == null ? null : bestResult.getCouponWasteAmount());
        int wasteCompare = candidateWasteAmount.compareTo(bestWasteAmount);
        if (wasteCompare != 0) {
            return wasteCompare < 0;
        }

        long candidateExpireScore = buildExpireScore(candidateResult == null ? null : candidateResult.getSelectedCoupons());
        long bestExpireScore = buildExpireScore(bestResult == null ? null : bestResult.getSelectedCoupons());
        if (candidateExpireScore != bestExpireScore) {
            return candidateExpireScore < bestExpireScore;
        }

        String candidateIdKey = buildCouponIdKey(candidateResult == null ? null : candidateResult.getSelectedCoupons());
        String bestIdKey = buildCouponIdKey(bestResult == null ? null : bestResult.getSelectedCoupons());
        return candidateIdKey.compareTo(bestIdKey) < 0;
    }

    /**
     * 构建到期优先比较分值。
     *
     * @param selectedCoupons 选中券列表。
     * @return long 到期比较分值（越小表示整体越早到期）。
     */
    private static long buildExpireScore(List<OrderCouponSnapshotDto> selectedCoupons) {
        if (selectedCoupons == null || selectedCoupons.isEmpty()) {
            return Long.MAX_VALUE;
        }
        long score = 0L;
        for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
            if (selectedCoupon == null || selectedCoupon.getExpireTime() == null) {
                score += Long.MAX_VALUE / 8;
            } else {
                score += selectedCoupon.getExpireTime().getTime() / 1000;
            }
        }
        return score;
    }

    /**
     * 构建券ID稳定排序键。
     *
     * @param selectedCoupons 选中券列表。
     * @return String 稳定排序键。
     */
    private static String buildCouponIdKey(List<OrderCouponSnapshotDto> selectedCoupons) {
        if (selectedCoupons == null || selectedCoupons.isEmpty()) {
            return "";
        }
        return selectedCoupons.stream()
                .map(couponSnapshot -> String.format("%020d", couponSnapshot == null || couponSnapshot.getCouponId() == null ? Long.MAX_VALUE : couponSnapshot.getCouponId()))
                .collect(Collectors.joining(","));
    }

    /**
     * 构建最终结果对象。
     *
     * @param originalItems      原始商品项。
     * @param selectedCoupons    选中券列表。
     * @param appliedCoupons     券应用明细列表。
     * @param allocationMap      商品累计分摊映射。
     * @param totalDeductAmount  总抵扣。
     * @param totalConsumeAmount 总消耗。
     * @param totalWasteAmount   总浪费。
     * @param autoSelected       是否自动选券。
     * @return CouponAutoSelectResultDto 最终结果。
     */
    private static CouponAutoSelectResultDto buildResult(List<ItemState> originalItems,
                                                         List<OrderCouponSnapshotDto> selectedCoupons,
                                                         List<CouponAppliedDetailDto> appliedCoupons,
                                                         Map<String, BigDecimal> allocationMap,
                                                         BigDecimal totalDeductAmount,
                                                         BigDecimal totalConsumeAmount,
                                                         BigDecimal totalWasteAmount,
                                                         boolean autoSelected) {
        List<CouponSettlementAllocationDto> allocations = new ArrayList<>();
        Map<String, BigDecimal> normalizedAllocationMap = allocationMap == null ? Map.of() : allocationMap;
        for (ItemState itemState : originalItems) {
            BigDecimal itemDeductAmount = normalizeAmount(normalizedAllocationMap.get(itemState.itemKey));
            BigDecimal payableAmount = normalizeAmount(itemState.totalAmount.subtract(itemDeductAmount));
            allocations.add(CouponSettlementAllocationDto.builder()
                    .itemKey(itemState.itemKey)
                    .couponDeductAmount(itemDeductAmount)
                    .payableAmount(payableAmount)
                    .build());
        }
        return CouponAutoSelectResultDto.builder()
                .selectedCoupons(selectedCoupons == null ? List.of() : new ArrayList<>(selectedCoupons))
                .appliedCoupons(appliedCoupons == null ? List.of() : new ArrayList<>(appliedCoupons))
                .allocations(allocations)
                .couponDeductAmount(normalizeAmount(totalDeductAmount))
                .couponConsumeAmount(normalizeAmount(totalConsumeAmount))
                .couponWasteAmount(normalizeAmount(totalWasteAmount))
                .autoSelected(autoSelected)
                .build();
    }

    /**
     * 构建全零结果对象。
     *
     * @param originalItems 原始商品项。
     * @param autoSelected  是否自动选券。
     * @return CouponAutoSelectResultDto 全零结果。
     */
    private static CouponAutoSelectResultDto buildEmptyResult(List<ItemState> originalItems,
                                                              boolean autoSelected) {
        return buildResult(
                originalItems == null ? List.of() : originalItems,
                List.of(),
                List.of(),
                new LinkedHashMap<>(),
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                ZERO_AMOUNT,
                autoSelected
        );
    }

    /**
     * 合并商品分摊映射。
     *
     * @param targetMap 目标映射。
     * @param sourceMap 来源映射。
     */
    private static void mergeAllocationMap(Map<String, BigDecimal> targetMap,
                                           Map<String, BigDecimal> sourceMap) {
        if (targetMap == null || sourceMap == null || sourceMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, BigDecimal> entry : sourceMap.entrySet()) {
            String itemKey = entry.getKey();
            BigDecimal sourceAmount = normalizeAmount(entry.getValue());
            BigDecimal targetAmount = normalizeAmount(targetMap.get(itemKey));
            targetMap.put(itemKey, normalizeAmount(targetAmount.add(sourceAmount)));
        }
    }

    /**
     * 复制商品项状态集合。
     *
     * @param sourceItems 源商品项状态集合。
     * @return List<ItemState> 深拷贝后的商品项状态集合。
     */
    private static List<ItemState> copyItems(List<ItemState> sourceItems) {
        if (sourceItems == null || sourceItems.isEmpty()) {
            return List.of();
        }
        List<ItemState> copiedItems = new ArrayList<>(sourceItems.size());
        for (ItemState sourceItem : sourceItems) {
            copiedItems.add(sourceItem.copy());
        }
        return copiedItems;
    }

    /**
     * 复制商品分摊映射。
     *
     * @param sourceMap 源映射。
     * @return Map<String, BigDecimal> 深拷贝后的映射。
     */
    private static Map<String, BigDecimal> copyAllocationMap(Map<String, BigDecimal> sourceMap) {
        Map<String, BigDecimal> copiedMap = new LinkedHashMap<>();
        if (sourceMap == null || sourceMap.isEmpty()) {
            return copiedMap;
        }
        for (Map.Entry<String, BigDecimal> entry : sourceMap.entrySet()) {
            copiedMap.put(entry.getKey(), normalizeAmount(entry.getValue()));
        }
        return copiedMap;
    }

    /**
     * 构建初始商品项状态集合。
     *
     * @param items 原始商品结算项集合。
     * @return List<ItemState> 初始化后的商品项状态集合。
     */
    private static List<ItemState> buildInitialItemStates(List<CouponSettlementItemDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ItemState> itemStates = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            CouponSettlementItemDto item = items.get(index);
            if (item == null) {
                continue;
            }
            BigDecimal totalAmount = normalizeAmount(item.getTotalAmount());
            if (totalAmount.compareTo(ZERO_AMOUNT) <= 0) {
                continue;
            }
            String normalizedItemKey = buildNormalizedItemKey(item.getItemKey(), index);
            ItemState itemState = new ItemState();
            itemState.itemKey = normalizedItemKey;
            itemState.productId = item.getProductId();
            itemState.couponEnabled = Objects.equals(item.getCouponEnabled(), COUPON_ENABLED_FLAG) ? COUPON_ENABLED_FLAG : 0;
            itemState.totalAmount = totalAmount;
            itemState.remainingAmount = totalAmount;
            itemStates.add(itemState);
        }
        return itemStates;
    }

    /**
     * 构建规范化商品项键。
     *
     * @param itemKey 原始商品项键。
     * @param index   商品项序号。
     * @return String 规范化后的商品项键。
     */
    private static String buildNormalizedItemKey(String itemKey, int index) {
        if (itemKey != null && !itemKey.trim().isEmpty()) {
            return itemKey.trim();
        }
        return "ITEM_" + index;
    }

    /**
     * 规范化金额。
     *
     * @param amount 原始金额。
     * @return BigDecimal 规范化金额（2位小数，HALF_UP）。
     */
    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? ZERO_AMOUNT : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 搜索最优结果容器。
     */
    private static final class SearchBestHolder {

        /**
         * 当前最优结果。
         */
        private CouponAutoSelectResultDto bestResult;

        /**
         * 构造最优结果容器。
         *
         * @param bestResult 初始最优结果。
         */
        private SearchBestHolder(CouponAutoSelectResultDto bestResult) {
            this.bestResult = bestResult;
        }
    }

    /**
     * 商品项运行时状态。
     */
    private static final class ItemState {

        /**
         * 商品项业务键。
         */
        private String itemKey;

        /**
         * 商品ID。
         */
        private Long productId;

        /**
         * 是否允许使用优惠券。
         */
        private Integer couponEnabled;

        /**
         * 商品项原始金额。
         */
        private BigDecimal totalAmount;

        /**
         * 商品项剩余可抵扣金额。
         */
        private BigDecimal remainingAmount;

        /**
         * 深拷贝当前对象。
         *
         * @return ItemState 深拷贝对象。
         */
        private ItemState copy() {
            ItemState copiedState = new ItemState();
            copiedState.itemKey = this.itemKey;
            copiedState.productId = this.productId;
            copiedState.couponEnabled = this.couponEnabled;
            copiedState.totalAmount = this.totalAmount;
            copiedState.remainingAmount = this.remainingAmount;
            return copiedState;
        }
    }

    /**
     * 单张券应用中间结果。
     *
     * @param nextItems     应用该券后的商品剩余状态。
     * @param itemDeductMap 该券对商品项的分摊映射。
     * @param appliedDetail 该券应用明细。
     * @param canContinue   应用后是否允许继续叠加后续券。
     */
    private record CouponApplicationResult(
            List<ItemState> nextItems,
            Map<String, BigDecimal> itemDeductMap,
            CouponAppliedDetailDto appliedDetail,
            boolean canContinue
    ) {
    }
}
