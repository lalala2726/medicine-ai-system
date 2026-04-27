package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.CouponLogMapper;
import com.zhangyichuang.medicine.client.mapper.UserCouponMapper;
import com.zhangyichuang.medicine.client.model.request.UserCouponListRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import com.zhangyichuang.medicine.client.model.vo.coupon.UserCouponVo;
import com.zhangyichuang.medicine.client.service.UserCouponService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.entity.CouponLog;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import com.zhangyichuang.medicine.model.enums.CouponChangeTypeEnum;
import com.zhangyichuang.medicine.model.enums.CouponSourceTypeEnum;
import com.zhangyichuang.medicine.model.enums.CouponTypeEnum;
import com.zhangyichuang.medicine.model.enums.UserCouponStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户优惠券服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon>
        implements UserCouponService, BaseService {

    /**
     * 商品允许使用优惠券的标记值。
     */
    private static final int COUPON_ENABLED_FLAG = 1;

    /**
     * 金额小数位数。
     */
    private static final int MONEY_SCALE = 2;

    /**
     * 逻辑删除标记值。
     */
    private static final int LOGIC_DELETED_FLAG = 1;

    /**
     * 零金额常量。
     */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    /**
     * 用户删除优惠券备注。
     */
    private static final String USER_DELETE_COUPON_REMARK = "用户删除优惠券";

    /**
     * 优惠券日志 Mapper。
     */
    private final CouponLogMapper couponLogMapper;

    /**
     * 查询当前用户优惠券列表。
     *
     * @param request 查询请求
     * @return 优惠券分页结果
     */
    @Override
    public Page<UserCouponVo> listCurrentUserCoupons(UserCouponListRequest request) {
        Long userId = getUserId();
        UserCouponStatusEnum couponStatusEnum = resolveClientCouponListStatus(request.getCouponStatus());
        Date now = new Date();
        Date oneYearAgo = buildOneYearAgoDate();
        Page<UserCoupon> couponPage;
        if (couponStatusEnum == UserCouponStatusEnum.AVAILABLE) {
            couponPage = lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                    .ge(UserCoupon::getExpireTime, now)
                    .orderByDesc(UserCoupon::getCreateTime)
                    .page(request.toPage());
        } else if (couponStatusEnum == UserCouponStatusEnum.USED) {
            couponPage = lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.USED.getType())
                    .ge(UserCoupon::getUpdateTime, oneYearAgo)
                    .orderByDesc(UserCoupon::getUpdateTime)
                    .page(request.toPage());
        } else {
            couponPage = lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.EXPIRED.getType())
                    .ge(UserCoupon::getExpireTime, oneYearAgo)
                    .orderByDesc(UserCoupon::getExpireTime)
                    .page(request.toPage());
        }
        List<UserCouponVo> records = couponPage.getRecords().stream()
                .map(this::toUserCouponVo)
                .toList();
        Page<UserCouponVo> result = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 删除当前用户优惠券。
     *
     * @param couponId 用户优惠券ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCurrentUserCoupon(Long couponId) {
        if (couponId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠券ID不能为空");
        }
        Long userId = getUserId();
        String operatorId = String.valueOf(userId);
        UserCoupon userCoupon = getOwnedCoupon(userId, couponId);
        validateCouponForDelete(userCoupon);

        Date now = new Date();
        BigDecimal beforeAvailableAmount = normalizeAmount(userCoupon.getAvailableAmount());
        boolean updated = lambdaUpdate()
                .eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getVersion, userCoupon.getVersion())
                .set(UserCoupon::getAvailableAmount, ZERO_AMOUNT)
                .set(UserCoupon::getLockedConsumeAmount, ZERO_AMOUNT)
                .set(UserCoupon::getIsDeleted, LOGIC_DELETED_FLAG)
                .set(UserCoupon::getUpdateTime, now)
                .set(UserCoupon::getUpdateBy, operatorId)
                .update();
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "删除优惠券失败，请稍后重试");
        }

        writeCouponLog(CouponLog.builder()
                .couponId(userCoupon.getId())
                .userId(userCoupon.getUserId())
                .changeType(CouponChangeTypeEnum.MANUAL_ADJUST.getType())
                .changeAmount(beforeAvailableAmount)
                .deductAmount(ZERO_AMOUNT)
                .wasteAmount(ZERO_AMOUNT)
                .beforeAvailableAmount(beforeAvailableAmount)
                .afterAvailableAmount(ZERO_AMOUNT)
                .sourceType(CouponSourceTypeEnum.MANUAL.getType())
                .sourceBizNo(String.valueOf(userCoupon.getId()))
                .remark(USER_DELETE_COUPON_REMARK)
                .operatorId(operatorId)
                .build());
        return true;
    }

    /**
     * 统计客户端应展示的优惠券数量。
     *
     * @param userId 用户ID
     * @return 客户端应展示的优惠券数量
     */
    @Override
    public long countDisplayableCoupons(Long userId) {
        Date now = new Date();
        return lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .ge(UserCoupon::getExpireTime, now)
                .count();
    }

    /**
     * 查询当前订单可选优惠券列表。
     *
     * @param userId 用户ID
     * @param items  商品项列表
     * @return 当前订单可选优惠券列表
     */
    @Override
    public List<OrderCouponOptionVo> listMatchedCoupons(Long userId, List<CouponSettlementItemDto> items) {
        List<UserCoupon> coupons = listDisplayablePreviewCoupons(userId);
        List<OrderCouponOptionVo> options = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            options.add(buildCouponOption(coupon, items));
        }
        options.sort((left, right) -> {
            int matchedCompare = Boolean.compare(Boolean.TRUE.equals(right.getMatched()), Boolean.TRUE.equals(left.getMatched()));
            if (matchedCompare != 0) {
                return matchedCompare;
            }
            int deductCompare = normalizeAmount(right.getCouponDeductAmount()).compareTo(normalizeAmount(left.getCouponDeductAmount()));
            if (deductCompare != 0) {
                return deductCompare;
            }
            Date leftExpireTime = left.getExpireTime();
            Date rightExpireTime = right.getExpireTime();
            if (leftExpireTime == null && rightExpireTime == null) {
                return 0;
            }
            if (leftExpireTime == null) {
                return 1;
            }
            if (rightExpireTime == null) {
                return -1;
            }
            return leftExpireTime.compareTo(rightExpireTime);
        });
        return options;
    }

    /**
     * 查询指定优惠券在当前订单下的可用信息。
     *
     * @param userId   用户ID
     * @param couponId 用户优惠券ID
     * @param items    商品项列表
     * @return 当前订单优惠券信息
     */
    @Override
    public OrderCouponOptionVo getSelectedCouponOption(Long userId, Long couponId, List<CouponSettlementItemDto> items) {
        if (couponId == null) {
            return null;
        }
        UserCoupon userCoupon = getOwnedCoupon(userId, couponId);
        OrderCouponOptionVo option = buildCouponOption(userCoupon, items);
        if (!Boolean.TRUE.equals(option.getMatched())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    StringUtils.hasText(option.getUnusableReason()) ? option.getUnusableReason() : "当前优惠券不可用");
        }
        return option;
    }

    /**
     * 自动选择优惠券组合。
     *
     * @param userId 用户ID
     * @param items  商品项列表
     * @return 自动选券结果
     */
    @Override
    public CouponAutoSelectResultDto autoSelectCoupons(Long userId, List<CouponSettlementItemDto> items) {
        List<UserCoupon> coupons = listAutoSelectablePreviewCoupons(userId);
        List<OrderCouponSnapshotDto> couponSnapshots = coupons.stream()
                .map(coupon -> buildSnapshot(coupon, items))
                .toList();
        return CouponAutoSelectCalculator.selectBestCoupons(couponSnapshots, items);
    }

    /**
     * 批量锁定优惠券并返回订单快照。
     *
     * @param userId    用户ID
     * @param couponIds 用户优惠券ID列表
     * @param items     商品项列表
     * @param orderNo   订单号
     * @return 订单优惠券快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCouponSelectionSnapshotDto lockCoupons(Long userId,
                                                       List<Long> couponIds,
                                                       List<CouponSettlementItemDto> items,
                                                       String orderNo) {
        if (couponIds == null || couponIds.isEmpty()) {
            return buildEmptySelectionSnapshot();
        }

        List<Long> normalizedCouponIds = couponIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedCouponIds.isEmpty()) {
            return buildEmptySelectionSnapshot();
        }

        Map<Long, UserCoupon> couponMap = new HashMap<>();
        List<OrderCouponSnapshotDto> orderedSnapshots = new ArrayList<>();
        for (Long couponId : normalizedCouponIds) {
            UserCoupon userCoupon = getOwnedCoupon(userId, couponId);
            OrderCouponOptionVo option = buildCouponOption(userCoupon, items);
            if (!Boolean.TRUE.equals(option.getMatched())) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        StringUtils.hasText(option.getUnusableReason()) ? option.getUnusableReason() : "当前优惠券不可用");
            }
            couponMap.put(couponId, userCoupon);
            orderedSnapshots.add(buildSnapshot(userCoupon, items));
        }

        CouponAutoSelectResultDto applyResult = CouponAutoSelectCalculator.applyCouponsInOrder(orderedSnapshots, items);
        List<CouponAppliedDetailDto> appliedCoupons = applyResult.getAppliedCoupons() == null
                ? List.of()
                : applyResult.getAppliedCoupons();
        if (appliedCoupons.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前优惠券不可用");
        }
        if (appliedCoupons.size() != normalizedCouponIds.size()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券组合不满足使用条件");
        }

        Map<Long, CouponAppliedDetailDto> appliedDetailMap = appliedCoupons.stream()
                .filter(detail -> detail != null && detail.getCouponId() != null)
                .collect(Collectors.toMap(CouponAppliedDetailDto::getCouponId, detail -> detail, (left, right) -> left));
        Date now = new Date();
        for (Long couponId : normalizedCouponIds) {
            UserCoupon userCoupon = couponMap.get(couponId);
            CouponAppliedDetailDto appliedDetail = appliedDetailMap.get(couponId);
            if (userCoupon == null || appliedDetail == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券组合不满足使用条件");
            }
            boolean updated = lambdaUpdate()
                    .eq(UserCoupon::getId, userCoupon.getId())
                    .eq(UserCoupon::getVersion, userCoupon.getVersion())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                    .set(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .set(UserCoupon::getLockOrderNo, orderNo)
                    .set(UserCoupon::getLockTime, now)
                    .set(UserCoupon::getLockedConsumeAmount, normalizeAmount(appliedDetail.getCouponConsumeAmount()))
                    .set(UserCoupon::getUpdateTime, now)
                    .update();
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券已被其他订单占用，请重新选择");
            }

            writeCouponLog(CouponLog.builder()
                    .couponId(userCoupon.getId())
                    .userId(userCoupon.getUserId())
                    .orderNo(orderNo)
                    .changeType(CouponChangeTypeEnum.LOCK.getType())
                    .changeAmount(normalizeAmount(appliedDetail.getCouponConsumeAmount()))
                    .deductAmount(normalizeAmount(appliedDetail.getCouponDeductAmount()))
                    .wasteAmount(normalizeAmount(appliedDetail.getCouponWasteAmount()))
                    .beforeAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .afterAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .sourceType(CouponSourceTypeEnum.ORDER.getType())
                    .sourceBizNo(orderNo)
                    .remark("订单锁券")
                    .operatorId(String.valueOf(userId))
                    .build());
        }

        return OrderCouponSelectionSnapshotDto.builder()
                .selectedCoupons(applyResult.getSelectedCoupons())
                .appliedCoupons(applyResult.getAppliedCoupons())
                .allocations(applyResult.getAllocations())
                .couponDeductAmount(normalizeAmount(applyResult.getCouponDeductAmount()))
                .couponConsumeAmount(normalizeAmount(applyResult.getCouponConsumeAmount()))
                .couponWasteAmount(normalizeAmount(applyResult.getCouponWasteAmount()))
                .autoSelected(Boolean.FALSE)
                .build();
    }

    /**
     * 批量消耗订单已锁定的优惠券。
     *
     * @param order 订单实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeCouponsForOrder(MallOrder order) {
        OrderCouponSelectionSnapshotDto snapshot = parseOrderCouponSelectionSnapshot(order);
        if (snapshot == null || snapshot.getAppliedCoupons() == null || snapshot.getAppliedCoupons().isEmpty()) {
            return;
        }
        for (CouponAppliedDetailDto appliedDetail : snapshot.getAppliedCoupons()) {
            if (appliedDetail == null || appliedDetail.getCouponId() == null) {
                continue;
            }
            UserCoupon userCoupon = lambdaQuery()
                    .eq(UserCoupon::getId, appliedDetail.getCouponId())
                    .eq(UserCoupon::getLockOrderNo, order.getOrderNo())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .one();
            if (userCoupon == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券锁定状态异常");
            }

            BigDecimal beforeAvailableAmount = normalizeAmount(userCoupon.getAvailableAmount());
            BigDecimal consumeAmount = normalizeAmount(appliedDetail.getCouponConsumeAmount());
            BigDecimal afterAvailableAmount = beforeAvailableAmount.subtract(consumeAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (afterAvailableAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券可用金额不足");
            }
            Date now = new Date();
            String nextStatus = resolveConsumedStatus(afterAvailableAmount, userCoupon.getExpireTime(), now);
            boolean updated = lambdaUpdate()
                    .eq(UserCoupon::getId, userCoupon.getId())
                    .eq(UserCoupon::getVersion, userCoupon.getVersion())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .set(UserCoupon::getAvailableAmount, afterAvailableAmount)
                    .set(UserCoupon::getLockedConsumeAmount, ZERO_AMOUNT)
                    .set(UserCoupon::getCouponStatus, nextStatus)
                    .set(UserCoupon::getLockOrderNo, null)
                    .set(UserCoupon::getLockTime, null)
                    .set(UserCoupon::getUpdateTime, now)
                    .update();
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券消耗失败，请重试");
            }

            writeCouponLog(CouponLog.builder()
                    .couponId(userCoupon.getId())
                    .userId(userCoupon.getUserId())
                    .orderNo(order.getOrderNo())
                    .changeType(CouponChangeTypeEnum.CONSUME.getType())
                    .changeAmount(consumeAmount)
                    .deductAmount(normalizeAmount(appliedDetail.getCouponDeductAmount()))
                    .wasteAmount(normalizeAmount(appliedDetail.getCouponWasteAmount()))
                    .beforeAvailableAmount(beforeAvailableAmount)
                    .afterAvailableAmount(afterAvailableAmount)
                    .sourceType(CouponSourceTypeEnum.ORDER.getType())
                    .sourceBizNo(order.getOrderNo())
                    .remark("订单支付完成消耗优惠券")
                    .operatorId(String.valueOf(order.getUserId()))
                    .build());
        }
    }

    /**
     * 批量释放订单已锁定的优惠券。
     *
     * @param order      订单实体
     * @param operatorId 操作人标识
     * @param reason     操作原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCouponsForOrder(MallOrder order, String operatorId, String reason) {
        OrderCouponSelectionSnapshotDto snapshot = parseOrderCouponSelectionSnapshot(order);
        if (snapshot == null || snapshot.getAppliedCoupons() == null || snapshot.getAppliedCoupons().isEmpty()) {
            return;
        }
        for (CouponAppliedDetailDto appliedDetail : snapshot.getAppliedCoupons()) {
            if (appliedDetail == null || appliedDetail.getCouponId() == null) {
                continue;
            }
            UserCoupon userCoupon = lambdaQuery()
                    .eq(UserCoupon::getId, appliedDetail.getCouponId())
                    .eq(UserCoupon::getLockOrderNo, order.getOrderNo())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .one();
            if (userCoupon == null) {
                continue;
            }
            Date now = new Date();
            String nextStatus = now.after(userCoupon.getExpireTime())
                    ? UserCouponStatusEnum.EXPIRED.getType()
                    : UserCouponStatusEnum.AVAILABLE.getType();
            BigDecimal availableAmount = normalizeAmount(userCoupon.getAvailableAmount());
            boolean updated = lambdaUpdate()
                    .eq(UserCoupon::getId, userCoupon.getId())
                    .eq(UserCoupon::getVersion, userCoupon.getVersion())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .set(UserCoupon::getCouponStatus, nextStatus)
                    .set(UserCoupon::getLockedConsumeAmount, ZERO_AMOUNT)
                    .set(UserCoupon::getLockOrderNo, null)
                    .set(UserCoupon::getLockTime, null)
                    .set(UserCoupon::getUpdateTime, now)
                    .update();
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "释放优惠券失败，请重试");
            }

            writeCouponLog(CouponLog.builder()
                    .couponId(userCoupon.getId())
                    .userId(userCoupon.getUserId())
                    .orderNo(order.getOrderNo())
                    .changeType(CouponChangeTypeEnum.RELEASE.getType())
                    .changeAmount(ZERO_AMOUNT)
                    .deductAmount(normalizeAmount(appliedDetail.getCouponDeductAmount()))
                    .wasteAmount(normalizeAmount(appliedDetail.getCouponWasteAmount()))
                    .beforeAvailableAmount(availableAmount)
                    .afterAvailableAmount(availableAmount)
                    .sourceType(CouponSourceTypeEnum.ORDER.getType())
                    .sourceBizNo(order.getOrderNo())
                    .remark(StringUtils.hasText(reason) ? reason : "订单释放优惠券")
                    .operatorId(operatorId)
                    .build());
        }
    }

    /**
     * 解析订单多券快照。
     *
     * @param order 订单实体
     * @return 订单多券快照
     */
    private OrderCouponSelectionSnapshotDto parseOrderCouponSelectionSnapshot(MallOrder order) {
        if (order == null || !StringUtils.hasText(order.getCouponSnapshotJson())) {
            return null;
        }
        OrderCouponSelectionSnapshotDto snapshot = JSONUtils.fromJson(order.getCouponSnapshotJson(), OrderCouponSelectionSnapshotDto.class);
        if (snapshot == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券快照解析失败");
        }
        return snapshot;
    }

    /**
     * 构建空的订单多券快照。
     *
     * @return 空快照
     */
    private OrderCouponSelectionSnapshotDto buildEmptySelectionSnapshot() {
        return OrderCouponSelectionSnapshotDto.builder()
                .selectedCoupons(List.of())
                .appliedCoupons(List.of())
                .allocations(List.of())
                .couponDeductAmount(ZERO_AMOUNT)
                .couponConsumeAmount(ZERO_AMOUNT)
                .couponWasteAmount(ZERO_AMOUNT)
                .autoSelected(Boolean.FALSE)
                .build();
    }

    /**
     * 查询用于结算页展示的优惠券列表。
     *
     * @param userId 用户ID
     * @return 可展示的优惠券列表
     */
    private List<UserCoupon> listDisplayablePreviewCoupons(Long userId) {
        Date now = new Date();
        return lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .ge(UserCoupon::getExpireTime, now)
                .orderByAsc(UserCoupon::getExpireTime)
                .orderByDesc(UserCoupon::getAvailableAmount)
                .list();
    }

    /**
     * 查询用于自动选券的优惠券列表。
     *
     * @param userId 用户ID
     * @return 当前可自动选中的优惠券列表
     */
    private List<UserCoupon> listAutoSelectablePreviewCoupons(Long userId) {
        Date now = new Date();
        return lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .le(UserCoupon::getEffectiveTime, now)
                .ge(UserCoupon::getExpireTime, now)
                .orderByAsc(UserCoupon::getExpireTime)
                .orderByDesc(UserCoupon::getAvailableAmount)
                .list();
    }

    /**
     * 构建订单优惠券选项。
     *
     * @param userCoupon 用户优惠券实体
     * @param items      商品项列表
     * @return 订单优惠券选项
     */
    private OrderCouponOptionVo buildCouponOption(UserCoupon userCoupon, List<CouponSettlementItemDto> items) {
        Date now = new Date();
        String unusableReason = validateCouponForOrder(userCoupon, items, now);
        CouponSettlementResultDto settlementResult = CouponSettlementCalculator.calculate(buildSnapshot(userCoupon, items), items);
        boolean matched = !StringUtils.hasText(unusableReason)
                && normalizeAmount(settlementResult.getCouponDeductAmount()).compareTo(BigDecimal.ZERO) > 0;
        if (!matched && !StringUtils.hasText(unusableReason)) {
            // 不满足优惠券使用条件文案
            unusableReason = "当前订单不满足此优惠券";
        }
        return OrderCouponOptionVo.builder()
                .couponId(userCoupon.getId())
                .couponName(userCoupon.getCouponNameSnapshot())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .availableAmount(userCoupon.getAvailableAmount())
                .continueUseEnabled(userCoupon.getContinueUseEnabled())
                .couponStatus(userCoupon.getCouponStatus())
                .effectiveTime(userCoupon.getEffectiveTime())
                .expireTime(userCoupon.getExpireTime())
                .matched(matched)
                .unusableReason(unusableReason)
                .couponDeductAmount(settlementResult.getCouponDeductAmount())
                .couponConsumeAmount(settlementResult.getCouponConsumeAmount())
                .couponWasteAmount(settlementResult.getCouponWasteAmount())
                .build();
    }

    /**
     * 校验优惠券在当前订单下是否可用。
     *
     * @param userCoupon 用户优惠券实体
     * @param items      商品项列表
     * @param now        当前时间
     * @return 不可用原因；可用时返回空
     */
    private String validateCouponForOrder(UserCoupon userCoupon, List<CouponSettlementItemDto> items, Date now) {
        if (userCoupon == null) {
            return "优惠券不存在";
        }
        if (!Objects.equals(userCoupon.getCouponStatus(), UserCouponStatusEnum.AVAILABLE.getType())) {
            return "优惠券当前不可用";
        }
        if (userCoupon.getEffectiveTime() != null && userCoupon.getEffectiveTime().after(now)) {
            return "优惠券尚未生效";
        }
        if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().before(now)) {
            return "优惠券已过期";
        }
        if (items == null || items.isEmpty()) {
            return "订单商品不能为空";
        }
        boolean hasEligibleItem = items.stream().anyMatch(item -> Objects.equals(item.getCouponEnabled(), COUPON_ENABLED_FLAG));
        if (!hasEligibleItem) {
            return "订单中没有可用券商品";
        }
        return null;
    }

    /**
     * 解析客户端优惠券列表状态。
     *
     * @param couponStatus 优惠券状态
     * @return 客户端支持的优惠券状态
     */
    private UserCouponStatusEnum resolveClientCouponListStatus(String couponStatus) {
        UserCouponStatusEnum couponStatusEnum = UserCouponStatusEnum.fromCode(couponStatus);
        if (couponStatusEnum == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠券状态不支持");
        }
        if (couponStatusEnum == UserCouponStatusEnum.LOCKED) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "已锁定优惠券不支持查询");
        }
        return couponStatusEnum;
    }

    /**
     * 构建近一年的起始时间。
     *
     * @return 近一年的起始时间
     */
    private Date buildOneYearAgoDate() {
        return Date.from(LocalDateTime.now()
                .minusYears(1)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * 根据用户ID与优惠券ID查询归属优惠券。
     *
     * @param userId   用户ID
     * @param couponId 优惠券ID
     * @return 用户优惠券实体
     */
    private UserCoupon getOwnedCoupon(Long userId, Long couponId) {
        UserCoupon userCoupon = lambdaQuery()
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .one();
        if (userCoupon == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "优惠券不存在");
        }
        return userCoupon;
    }

    /**
     * 校验当前优惠券是否允许删除。
     *
     * @param userCoupon 用户优惠券实体
     */
    private void validateCouponForDelete(UserCoupon userCoupon) {
        if (userCoupon == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "优惠券不存在");
        }
        if (Objects.equals(userCoupon.getCouponStatus(), UserCouponStatusEnum.LOCKED.getType())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "已锁定优惠券不允许删除");
        }
    }

    /**
     * 构建订单优惠券快照。
     *
     * @param userCoupon 用户优惠券实体
     * @param items      商品项列表
     * @return 订单优惠券快照
     */
    private OrderCouponSnapshotDto buildSnapshot(UserCoupon userCoupon, List<CouponSettlementItemDto> items) {
        List<Long> eligibleProductIds = items == null ? List.of() : items.stream()
                .filter(item -> Objects.equals(item.getCouponEnabled(), COUPON_ENABLED_FLAG))
                .map(CouponSettlementItemDto::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return OrderCouponSnapshotDto.builder()
                .couponId(userCoupon.getId())
                .templateId(userCoupon.getTemplateId())
                .couponName(userCoupon.getCouponNameSnapshot())
                .couponType(CouponTypeEnum.FULL_REDUCTION.getType())
                .thresholdAmount(normalizeAmount(userCoupon.getThresholdAmount()))
                .lockedAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                .continueUseEnabled(userCoupon.getContinueUseEnabled())
                .stackableEnabled(userCoupon.getStackableEnabled())
                .effectiveTime(userCoupon.getEffectiveTime())
                .expireTime(userCoupon.getExpireTime())
                .eligibleProductIds(eligibleProductIds)
                .build();
    }

    /**
     * 解析优惠券消耗后的目标状态。
     *
     * @param remainingAmount 剩余金额
     * @param expireTime      失效时间
     * @param now             当前时间
     * @return 优惠券目标状态
     */
    private String resolveConsumedStatus(BigDecimal remainingAmount, Date expireTime, Date now) {
        if (expireTime != null && expireTime.before(now)) {
            return UserCouponStatusEnum.EXPIRED.getType();
        }
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            return UserCouponStatusEnum.AVAILABLE.getType();
        }
        return UserCouponStatusEnum.USED.getType();
    }

    /**
     * 写入优惠券日志。
     *
     * @param couponLog 优惠券日志
     */
    private void writeCouponLog(CouponLog couponLog) {
        if (couponLog == null) {
            return;
        }
        couponLog.setCreateTime(new Date());
        couponLog.setIsDeleted(0);
        couponLogMapper.insert(couponLog);
    }

    /**
     * 转换为用户优惠券视图对象。
     *
     * @param userCoupon 用户优惠券实体
     * @return 用户优惠券视图对象
     */
    private UserCouponVo toUserCouponVo(UserCoupon userCoupon) {
        return UserCouponVo.builder()
                .couponId(userCoupon.getId())
                .templateId(userCoupon.getTemplateId())
                .couponName(userCoupon.getCouponNameSnapshot())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .totalAmount(userCoupon.getTotalAmount())
                .availableAmount(userCoupon.getAvailableAmount())
                .lockedConsumeAmount(userCoupon.getLockedConsumeAmount())
                .continueUseEnabled(userCoupon.getContinueUseEnabled())
                .couponStatus(userCoupon.getCouponStatus())
                .effectiveTime(userCoupon.getEffectiveTime())
                .expireTime(userCoupon.getExpireTime())
                .build();
    }

    /**
     * 规范化金额字段。
     *
     * @param amount 原始金额
     * @return 规范化后的金额
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
