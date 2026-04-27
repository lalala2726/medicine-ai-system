package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.admin.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.dto.OrderDetailRow;
import com.zhangyichuang.medicine.admin.model.dto.UserOrderStatistics;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.OrderAddressVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderPriceVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderRemarkVo;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.rabbitmq.publisher.OrderTimeoutMessagePublisher;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zhangyichuang.medicine.common.core.constants.Constants.ORDER_TIMEOUT_MINUTES;

/**
 * @author Chuang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl extends ServiceImpl<MallOrderMapper, MallOrder>
        implements MallOrderService {

    /**
     * 订单支付成功后在库中持久化的标记位。
     */
    private static final int PAID_FLAG = 1;
    /**
     * 完整退款的订单状态标记。
     */
    private static final String REFUND_STATUS_SUCCESS = "SUCCESS";
    /**
     * 部分退款的订单状态标记，表示仍有金额未退回。
     */
    private static final String REFUND_STATUS_PARTIAL = "PARTIAL";
    /**
     * 未传递退款原因时使用的默认描述，方便排查后台手工操作。
     */
    private static final String DEFAULT_REFUND_REASON = "管理员发起退款";

    /**
     * 订单自动关闭的关闭原因文案。
     */
    private static final String ORDER_TIMEOUT_CLOSE_REASON = "订单支付超时，系统自动关闭";

    /**
     * 订单自动关闭时写入的操作人标识。
     */
    private static final String SYSTEM_AUTO_CLOSE_OPERATOR = "系统自动关闭";
    /**
     * 优惠券改价缩放系数搜索上限。
     */
    private static final BigDecimal COUPON_SCALE_FACTOR_MAX = new BigDecimal("256");
    /**
     * 优惠券改价缩放系数的小数精度。
     */
    private static final int COUPON_SCALE_FACTOR_DECIMAL_SCALE = 8;
    /**
     * 单轮缩放系数采样点数量。
     */
    private static final int COUPON_SCALE_FACTOR_SCAN_POINTS = 160;
    /**
     * 缩放系数局部细化轮数。
     */
    private static final int COUPON_SCALE_FACTOR_REFINE_ROUNDS = 4;

    private final MallOrderMapper mallOrderMapper;
    private final UserMapper userMapper;
    private final MallOrderItemService mallOrderItemService;
    private final MallProductImageService mallProductImageService;
    private final MallOrderTimelineService mallOrderTimelineService;
    private final UserWalletService userWalletService;
    private final MallOrderShippingService mallOrderShippingService;
    private final MallInventoryService mallInventoryService;
    private final OrderTimeoutMessagePublisher orderTimeoutMessagePublisher;
    private final CouponAdminService couponAdminService;
    /**
     * 分布式锁执行器。
     */
    private final DistributedLockExecutor distributedLockExecutor;
    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;


    @Override
    public MallOrder getOrderByOrderNo(String orderNo) {
        Assert.isTrue(orderNo != null, "订单号不能为空");
        MallOrder mallOrder = lambdaQuery().eq(MallOrder::getOrderNo, orderNo).one();
        if (mallOrder == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }
        return mallOrder;
    }

    @Override
    public List<OrderDetailDto> getOrderByOrderNo(List<String> orderNos) {
        if (CollectionUtils.isEmpty(orderNos)) {
            return List.of();
        }
        List<String> normalizedOrderNos = orderNos.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedOrderNos.isEmpty()) {
            return List.of();
        }

        List<OrderDetailRow> rows = mallOrderMapper.selectOrderDetailRowsByOrderNos(normalizedOrderNos);
        if (CollectionUtils.isEmpty(rows)) {
            return List.of();
        }

        Map<String, OrderDetailDto> detailMap = new LinkedHashMap<>();
        for (OrderDetailRow row : rows) {
            if (row == null || !StringUtils.hasText(row.getOrderNo())) {
                continue;
            }
            OrderDetailDto detail = detailMap.computeIfAbsent(row.getOrderNo(), orderNo -> buildOrderDetailDto(row));
            if (row.getOrderItemId() == null) {
                continue;
            }
            detail.getProductInfo().add(OrderDetailDto.ProductInfo.builder()
                    .productId(row.getProductId())
                    .productName(row.getProductName())
                    .productImage(row.getProductImage())
                    .productPrice(row.getProductPrice())
                    .productQuantity(row.getProductQuantity())
                    .productTotalAmount(row.getProductTotalAmount())
                    .couponDeductAmount(row.getProductCouponDeductAmount())
                    .payableAmount(row.getProductPayableAmount())
                    .build());
        }

        List<OrderDetailDto> result = new ArrayList<>();
        for (String orderNo : normalizedOrderNos) {
            OrderDetailDto detail = detailMap.get(orderNo);
            if (detail != null) {
                result.add(detail);
            }
        }
        return result;
    }

    @Override
    public MallOrder getOrderById(Long id) {
        Assert.isPositive(id, "订单ID不能小于0");
        MallOrder mallOrder = getById(id);
        if (mallOrder == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }
        return mallOrder;
    }

    @Override
    public OrderDetailDto orderDetail(Long orderId) {

        // 获取订单信息
        MallOrder mallOrder = getOrderById(orderId);
        // 获取用户信息
        User userInfo = userMapper.selectById(mallOrder.getUserId());
        // 获取商品信息
        List<MallOrderItem> mallOrderItems = mallOrderItemService.getOrderItemByOrderId(mallOrder.getId());

        // 构建用户信息
        OrderDetailDto.UserInfo userInfoVo = OrderDetailDto.UserInfo.builder()
                .userId(userInfo.getId().toString())
                .nickname(userInfo.getNickname())
                .phoneNumber(userInfo.getPhoneNumber())
                .build();

        // 构建配送信息
        OrderDetailDto.DeliveryInfo deliveryInfo = OrderDetailDto.DeliveryInfo.builder()
                .receiverName(mallOrder.getReceiverName())
                .receiverAddress(mallOrder.getReceiverDetail())
                .receiverPhone(mallOrder.getReceiverPhone())
                .deliveryMethod(getDeliveryTypeDesc(mallOrder.getDeliveryType()))
                .build();

        // 构建订单信息
        OrderDetailDto.OrderInfo orderInfo = OrderDetailDto.OrderInfo.builder()
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .payType(mallOrder.getPayType())
                .itemsAmount(mallOrder.getItemsAmount())
                .totalAmount(mallOrder.getTotalAmount())
                .payAmount(mallOrder.getPayAmount())
                .freightAmount(mallOrder.getFreightAmount())
                .couponId(mallOrder.getCouponId())
                .couponName(mallOrder.getCouponName())
                .couponDeductAmount(mallOrder.getCouponDeductAmount())
                .couponConsumeAmount(mallOrder.getCouponConsumeAmount())
                .build();

        // 构建商品信息
        List<OrderDetailDto.ProductInfo> productInfoLists = new ArrayList<>();
        mallOrderItems.forEach(mallOrderItem -> {
            OrderDetailDto.ProductInfo productInfo = OrderDetailDto.ProductInfo.builder()
                    .productId(mallOrderItem.getProductId())
                    .productName(mallOrderItem.getProductName())
                    .productImage(mallOrderItem.getImageUrl())
                    .productPrice(mallOrderItem.getPrice())
                    .productQuantity(mallOrderItem.getQuantity())
                    .productTotalAmount(mallOrderItem.getTotalPrice())
                    .couponDeductAmount(mallOrderItem.getCouponDeductAmount())
                    .payableAmount(mallOrderItem.getPayableAmount())
                    .build();
            productInfoLists.add(productInfo);
        });

        // 构建完整的订单详情
        return OrderDetailDto.builder()
                .userInfo(userInfoVo)
                .deliveryInfo(deliveryInfo)
                .orderInfo(orderInfo)
                .productInfo(productInfoLists)
                .build();
    }

    /**
     * 获取订单地址信息
     *
     * @param orderId 订单ID
     * @return 订单地址信息
     */
    @Override
    public OrderAddressVo getOrderAddress(Long orderId) {
        MallOrder mallOrder = getOrderById(orderId);
        return OrderAddressVo.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .receiverName(mallOrder.getReceiverName())
                .receiverPhone(mallOrder.getReceiverPhone())
                .receiverDetail(mallOrder.getReceiverDetail())
                .deliveryType(mallOrder.getDeliveryType())
                .build();
    }

    /**
     * 更新订单配送信息
     *
     * @param request 更新参数
     * @return 是否更新成功
     */
    @Override
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean updateOrderAddress(AddressUpdateRequest request) {
        // 根据订单号查询订单
        MallOrder mallOrder = getOrderById(request.getOrderId());

        // 检查订单状态是否允许修改地址（只有待支付和待发货状态可以修改地址）
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum != null &&
                orderStatusEnum.ordinal() > OrderStatusEnum.PENDING_SHIPMENT.ordinal()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "当前订单状态不允许修改收货地址");
        }

        // 更新配送信息
        mallOrder.setReceiverName(request.getReceiverName());
        mallOrder.setReceiverPhone(request.getReceiverPhone());
        mallOrder.setReceiverDetail(request.getReceiverAddress());
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(request.getDeliveryType());
        Assert.isTrue(deliveryTypeEnum != null, "配送方式不存在");
        mallOrder.setDeliveryType(deliveryTypeEnum.getType());

        boolean updated = updateById(mallOrder);

        // 添加订单时间线记录
        if (updated) {
            OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                    .orderId(mallOrder.getId())
                    .eventType(OrderEventTypeEnum.OTHER.getType())
                    .eventStatus(mallOrder.getOrderStatus())
                    .operatorType(OperatorTypeEnum.ADMIN.getType())
                    .description("管理员修改了收货地址")
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
        }

        return updated;
    }

    /**
     * 获取订单备注信息
     *
     * @param orderId 订单ID
     * @return 订单备注信息
     */
    @Override
    public OrderRemarkVo getOrderRemark(Long orderId) {
        MallOrder mallOrder = getOrderById(orderId);
        return OrderRemarkVo.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .remark(mallOrder.getRemark())
                .note(mallOrder.getNote())
                .build();
    }

    /**
     * 更新订单备注
     *
     * @param request 更新参数
     * @return 是否更新成功
     */
    @Override
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean updateOrderRemark(RemarkUpdateRequest request) {
        // 根据订单号查询订单
        MallOrder mallOrder = getOrderById(request.getOrderId());

        // 更新订单备注
        mallOrder.setRemark(request.getRemark());

        boolean updated = updateById(mallOrder);

        // 添加订单时间线记录
        if (updated) {
            OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                    .orderId(mallOrder.getId())
                    .eventType(OrderEventTypeEnum.OTHER.getType())
                    .eventStatus(mallOrder.getOrderStatus())
                    .operatorType(OperatorTypeEnum.ADMIN.getType())
                    .description("管理员添加了订单备注")
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
        }

        return updated;
    }

    /**
     * 获取订单价格信息
     *
     * @param orderId 订单ID
     * @return 订单价格信息
     */
    @Override
    public OrderPriceVo getOrderPrice(Long orderId) {
        MallOrder mallOrder = getOrderById(orderId);
        return OrderPriceVo.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .itemsAmount(mallOrder.getItemsAmount())
                .totalAmount(mallOrder.getTotalAmount())
                .couponId(mallOrder.getCouponId())
                .couponName(mallOrder.getCouponName())
                .couponDeductAmount(mallOrder.getCouponDeductAmount())
                .build();
    }

    /**
     * 更新订单价格
     *
     * @param request 订单价格更新参数
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean updateOrderPrice(OrderUpdatePriceRequest request) {
        // 根据订单号查询订单
        MallOrder mallOrder = getOrderById(request.getOrderId());

        // 检查订单状态是否允许修改价格（只有待支付状态可以修改价格）
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum != null &&
                orderStatusEnum.ordinal() > OrderStatusEnum.PENDING_PAYMENT.ordinal()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "当前订单状态不允许修改价格");
        }

        try {
            // 将字符串价格转换为BigDecimal
            BigDecimal newPrice = new BigDecimal(request.getPrice()).setScale(2, RoundingMode.HALF_UP);
            Date now = new Date();
            Date newPayExpireTime = DateUtils.addMinutes(now, ORDER_TIMEOUT_MINUTES);

            // 验证价格是否合法
            if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "价格必须大于0");
            }

            // 1. 获取原订单项并执行改价。
            List<MallOrderItem> items = mallOrderItemService.lambdaQuery()
                    .eq(MallOrderItem::getOrderId, mallOrder.getId())
                    .list();

            if (!CollectionUtils.isEmpty(items)) {
                CouponSettlementResultDto couponSettlementResult = null;
                if (mallOrder.getCouponId() != null) {
                    couponSettlementResult = repriceOrderItemsWithCoupon(items, mallOrder, newPrice);
                    mallOrder.setItemsAmount(couponSettlementResult.getItemsAmount());
                    mallOrder.setCouponDeductAmount(couponSettlementResult.getCouponDeductAmount());
                    mallOrder.setCouponConsumeAmount(couponSettlementResult.getCouponConsumeAmount());
                    mallOrder.setCouponWasteAmount(couponSettlementResult.getCouponWasteAmount());
                    mallOrder.setTotalAmount(calculateOrderTotalAmount(
                            couponSettlementResult.getItemsAmount(),
                            mallOrder.getFreightAmount(),
                            couponSettlementResult.getCouponDeductAmount()
                    ));
                } else {
                    BigDecimal targetItemsAmount = newPrice.subtract(safeAmount(mallOrder.getFreightAmount())).setScale(2, RoundingMode.HALF_UP);
                    if (targetItemsAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ServiceException(ResponseCode.PARAM_ERROR, "扣除运费后商品金额必须大于0");
                    }
                    scaleOrderItems(items, targetItemsAmount, now);
                    mallOrder.setItemsAmount(targetItemsAmount);
                    mallOrder.setTotalAmount(targetItemsAmount.add(safeAmount(mallOrder.getFreightAmount())).setScale(2, RoundingMode.HALF_UP));
                }
                mallOrderItemService.updateBatchById(items);
            }

            // 2. 更新订单主表价格
            mallOrder.setPayAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            mallOrder.setPayExpireTime(newPayExpireTime);
            mallOrder.setUpdateTime(now);

            boolean updated = updateById(mallOrder);

            // 3. 添加订单时间线记录
            if (updated) {
                publishOrderTimeoutAfterCommit(mallOrder.getOrderNo(), ORDER_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                        .orderId(mallOrder.getId())
                        .eventType(OrderEventTypeEnum.OTHER.getType())
                        .eventStatus(mallOrder.getOrderStatus())
                        .operatorType(OperatorTypeEnum.ADMIN.getType())
                        .description(String.format("管理员修改了订单价格为: %s，并重置支付超时为 %d 分钟", newPrice, ORDER_TIMEOUT_MINUTES))
                        .build();
                mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
            }

            return updated;
        } catch (NumberFormatException e) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "价格格式不正确");
        }
    }

    /**
     * 重算带优惠券订单的商品金额与优惠分摊。
     *
     * @param items              订单项列表
     * @param mallOrder          订单实体
     * @param targetPayablePrice 目标应付金额
     * @return 重算后的优惠券结算结果
     */
    private CouponSettlementResultDto repriceOrderItemsWithCoupon(List<MallOrderItem> items,
                                                                  MallOrder mallOrder,
                                                                  BigDecimal targetPayablePrice) {
        OrderCouponSelectionSnapshotDto couponSnapshot = parseOrderCouponSnapshot(mallOrder.getCouponSnapshotJson());
        BigDecimal scaleFactor = findCouponScaleFactor(items, mallOrder, couponSnapshot, targetPayablePrice);
        CouponSettlementResultDto previewSettlementResult = calculateScaledCouponSettlement(items, couponSnapshot, scaleFactor);
        scaleOrderItems(items, previewSettlementResult.getItemsAmount(), new Date());
        CouponSettlementResultDto finalSettlementResult = couponAdminService.recalculateLockedCoupon(mallOrder, items);
        applyCouponAllocations(items, finalSettlementResult);
        return finalSettlementResult;
    }

    /**
     * 解析订单多券快照。
     *
     * @param couponSnapshotJson 优惠券快照JSON
     * @return 订单多券快照
     */
    private OrderCouponSelectionSnapshotDto parseOrderCouponSnapshot(String couponSnapshotJson) {
        if (!StringUtils.hasText(couponSnapshotJson)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券快照缺失");
        }
        OrderCouponSelectionSnapshotDto couponSnapshot = JSONUtils.fromJson(couponSnapshotJson, OrderCouponSelectionSnapshotDto.class);
        if (couponSnapshot == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券快照解析失败");
        }
        return couponSnapshot;
    }

    /**
     * 搜索带优惠券订单的商品金额缩放系数。
     *
     * @param items              订单项列表
     * @param mallOrder          订单实体
     * @param couponSnapshot     多券快照
     * @param targetPayablePrice 目标应付金额
     * @return 商品金额缩放系数
     */
    private BigDecimal findCouponScaleFactor(List<MallOrderItem> items,
                                             MallOrder mallOrder,
                                             OrderCouponSelectionSnapshotDto couponSnapshot,
                                             BigDecimal targetPayablePrice) {
        BigDecimal freightAmount = mallOrder.getFreightAmount();
        BigDecimal upperBound = resolveCouponScaleFactorUpperBound(items, couponSnapshot, freightAmount, targetPayablePrice);
        BigDecimal searchLeft = BigDecimal.ZERO;
        BigDecimal searchRight = upperBound;
        BigDecimal bestScaleFactor = BigDecimal.ZERO;
        BigDecimal bestDifference = null;
        BigDecimal bestPayableAmount = null;

        for (int refineRound = 0; refineRound <= COUPON_SCALE_FACTOR_REFINE_ROUNDS; refineRound++) {
            BigDecimal interval = searchRight.subtract(searchLeft);
            BigDecimal step = interval.divide(
                    new BigDecimal(COUPON_SCALE_FACTOR_SCAN_POINTS),
                    COUPON_SCALE_FACTOR_DECIMAL_SCALE,
                    RoundingMode.HALF_UP
            );
            for (int i = 0; i <= COUPON_SCALE_FACTOR_SCAN_POINTS; i++) {
                BigDecimal candidateScaleFactor = searchLeft.add(step.multiply(new BigDecimal(i)))
                        .setScale(COUPON_SCALE_FACTOR_DECIMAL_SCALE, RoundingMode.HALF_UP);
                if (candidateScaleFactor.compareTo(searchRight) > 0) {
                    candidateScaleFactor = searchRight;
                }
                BigDecimal candidatePayableAmount = calculateScaledPayableAmount(
                        items,
                        couponSnapshot,
                        freightAmount,
                        candidateScaleFactor
                );
                BigDecimal candidateDifference = candidatePayableAmount.subtract(targetPayablePrice).abs();
                if (isBetterScaleCandidate(candidateDifference, candidatePayableAmount, candidateScaleFactor,
                        bestDifference, bestPayableAmount, bestScaleFactor, targetPayablePrice)) {
                    bestDifference = candidateDifference;
                    bestPayableAmount = candidatePayableAmount;
                    bestScaleFactor = candidateScaleFactor;
                }
                if (candidateDifference.compareTo(BigDecimal.ZERO) == 0) {
                    return candidateScaleFactor;
                }
            }
            if (refineRound == COUPON_SCALE_FACTOR_REFINE_ROUNDS || step.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            searchLeft = bestScaleFactor.subtract(step).max(BigDecimal.ZERO);
            searchRight = bestScaleFactor.add(step).min(upperBound);
            if (searchRight.compareTo(searchLeft) <= 0) {
                break;
            }
        }
        return bestScaleFactor;
    }

    /**
     * 计算缩放系数搜索上限。
     *
     * @param items              订单项列表
     * @param couponSnapshot     多券快照
     * @param freightAmount      运费金额
     * @param targetPayablePrice 目标应付金额
     * @return 缩放系数搜索上限
     */
    private BigDecimal resolveCouponScaleFactorUpperBound(List<MallOrderItem> items,
                                                          OrderCouponSelectionSnapshotDto couponSnapshot,
                                                          BigDecimal freightAmount,
                                                          BigDecimal targetPayablePrice) {
        BigDecimal high = BigDecimal.ONE;
        while (high.compareTo(COUPON_SCALE_FACTOR_MAX) < 0
                && calculateScaledPayableAmount(items, couponSnapshot, freightAmount, high).compareTo(targetPayablePrice) < 0) {
            high = high.multiply(new BigDecimal("2"));
            if (high.compareTo(COUPON_SCALE_FACTOR_MAX) > 0) {
                high = COUPON_SCALE_FACTOR_MAX;
            }
        }
        return high;
    }

    /**
     * 判断候选缩放系数是否优于当前最优候选。
     *
     * @param candidateDifference    候选结果与目标应付金额的差值绝对值
     * @param candidatePayableAmount 候选结果应付金额
     * @param candidateScaleFactor   候选缩放系数
     * @param bestDifference         当前最优差值绝对值
     * @param bestPayableAmount      当前最优应付金额
     * @param bestScaleFactor        当前最优缩放系数
     * @param targetPayablePrice     目标应付金额
     * @return 候选结果是否更优
     */
    private boolean isBetterScaleCandidate(BigDecimal candidateDifference,
                                           BigDecimal candidatePayableAmount,
                                           BigDecimal candidateScaleFactor,
                                           BigDecimal bestDifference,
                                           BigDecimal bestPayableAmount,
                                           BigDecimal bestScaleFactor,
                                           BigDecimal targetPayablePrice) {
        if (bestDifference == null || bestPayableAmount == null) {
            return true;
        }
        int differenceCompare = candidateDifference.compareTo(bestDifference);
        if (differenceCompare != 0) {
            return differenceCompare < 0;
        }
        boolean candidateNotLessThanTarget = candidatePayableAmount.compareTo(targetPayablePrice) >= 0;
        boolean bestNotLessThanTarget = bestPayableAmount.compareTo(targetPayablePrice) >= 0;
        if (candidateNotLessThanTarget != bestNotLessThanTarget) {
            return candidateNotLessThanTarget;
        }
        int payableCompare = candidatePayableAmount.compareTo(bestPayableAmount);
        if (payableCompare != 0) {
            return payableCompare < 0;
        }
        return candidateScaleFactor.compareTo(bestScaleFactor) < 0;
    }

    /**
     * 计算指定缩放系数下的订单应付金额。
     *
     * @param items          订单项列表
     * @param couponSnapshot 多券快照
     * @param freightAmount  运费金额
     * @param scaleFactor    缩放系数
     * @return 缩放后的订单应付金额
     */
    private BigDecimal calculateScaledPayableAmount(List<MallOrderItem> items,
                                                    OrderCouponSelectionSnapshotDto couponSnapshot,
                                                    BigDecimal freightAmount,
                                                    BigDecimal scaleFactor) {
        CouponSettlementResultDto settlementResult = calculateScaledCouponSettlement(items, couponSnapshot, scaleFactor);
        return calculateOrderTotalAmount(
                settlementResult.getItemsAmount(),
                freightAmount,
                settlementResult.getCouponDeductAmount()
        );
    }

    /**
     * 计算缩放后的优惠券结算结果。
     *
     * @param items          订单项列表
     * @param couponSnapshot 多券快照
     * @param scaleFactor    缩放系数
     * @return 缩放后的优惠券结算结果
     */
    private CouponSettlementResultDto calculateScaledCouponSettlement(List<MallOrderItem> items,
                                                                      OrderCouponSelectionSnapshotDto couponSnapshot,
                                                                      BigDecimal scaleFactor) {
        List<OrderCouponSnapshotDto> selectedCoupons = normalizeSelectedCoupons(
                couponSnapshot == null ? null : couponSnapshot.getSelectedCoupons());
        BigDecimal targetItemsAmount = calculateOrderItemsAmount(items)
                .multiply(scaleFactor)
                .setScale(2, RoundingMode.HALF_UP);
        List<CouponSettlementItemDto> settlementItems = buildScaledSettlementItems(items, selectedCoupons, targetItemsAmount);
        CouponAutoSelectResultDto autoSelectResult = CouponAutoSelectCalculator.applyCouponsInOrder(selectedCoupons, settlementItems);
        return buildCouponSettlementResult(settlementItems, autoSelectResult);
    }

    /**
     * 构建缩放后的优惠券结算商品项列表。
     *
     * @param items             订单项列表
     * @param selectedCoupons   已选优惠券快照集合
     * @param targetItemsAmount 目标商品总金额
     * @return 缩放后的优惠券结算商品项列表
     */
    private List<CouponSettlementItemDto> buildScaledSettlementItems(List<MallOrderItem> items,
                                                                     List<OrderCouponSnapshotDto> selectedCoupons,
                                                                     BigDecimal targetItemsAmount) {
        Set<Long> eligibleProductIds = new LinkedHashSet<>();
        boolean allProductsEligible = false;
        if (!CollectionUtils.isEmpty(selectedCoupons)) {
            for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
                if (selectedCoupon == null) {
                    continue;
                }
                List<Long> currentEligibleProductIds = selectedCoupon.getEligibleProductIds();
                if (CollectionUtils.isEmpty(currentEligibleProductIds)) {
                    allProductsEligible = true;
                    break;
                }
                for (Long productId : currentEligibleProductIds) {
                    if (productId != null) {
                        eligibleProductIds.add(productId);
                    }
                }
            }
        }
        BigDecimal normalizedTargetItemsAmount = safeAmount(targetItemsAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal originalItemsAmount = calculateOrderItemsAmount(items);
        if (originalItemsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "订单商品金额异常，无法改价");
        }
        boolean allProductsEligibleFinal = allProductsEligible;
        BigDecimal remainingAmount = normalizedTargetItemsAmount;
        List<CouponSettlementItemDto> settlementItems = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            MallOrderItem item = items.get(i);
            BigDecimal itemScaledAmount;
            if (i == items.size() - 1) {
                itemScaledAmount = remainingAmount;
            } else {
                itemScaledAmount = safeAmount(item.getTotalPrice())
                        .multiply(normalizedTargetItemsAmount)
                        .divide(originalItemsAmount, 2, RoundingMode.HALF_UP);
                remainingAmount = remainingAmount.subtract(itemScaledAmount).setScale(2, RoundingMode.HALF_UP);
            }
            settlementItems.add(CouponSettlementItemDto.builder()
                    .itemKey(String.valueOf(item.getId()))
                    .productId(item.getProductId())
                    .totalAmount(itemScaledAmount)
                    .couponEnabled(allProductsEligibleFinal || eligibleProductIds.contains(item.getProductId()) ? 1 : 0)
                    .build());
        }
        return settlementItems;
    }

    /**
     * 规范化选中券快照集合，去除空值和重复券ID。
     *
     * @param selectedCoupons 原始选中券快照集合
     * @return 规范化后的选中券快照集合
     */
    private List<OrderCouponSnapshotDto> normalizeSelectedCoupons(List<OrderCouponSnapshotDto> selectedCoupons) {
        if (CollectionUtils.isEmpty(selectedCoupons)) {
            return List.of();
        }
        List<OrderCouponSnapshotDto> normalizedCoupons = new ArrayList<>();
        Set<Long> couponIdSet = new LinkedHashSet<>();
        for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
            if (selectedCoupon == null || selectedCoupon.getCouponId() == null) {
                continue;
            }
            if (couponIdSet.add(selectedCoupon.getCouponId())) {
                normalizedCoupons.add(selectedCoupon);
            }
        }
        return normalizedCoupons;
    }

    /**
     * 将自动选券结果转换为优惠结算结果。
     *
     * @param settlementItems  结算商品项列表
     * @param autoSelectResult 自动选券结果
     * @return 优惠结算结果
     */
    private CouponSettlementResultDto buildCouponSettlementResult(List<CouponSettlementItemDto> settlementItems,
                                                                  CouponAutoSelectResultDto autoSelectResult) {
        BigDecimal itemsAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal eligibleItemsAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (!CollectionUtils.isEmpty(settlementItems)) {
            for (CouponSettlementItemDto settlementItem : settlementItems) {
                if (settlementItem == null) {
                    continue;
                }
                BigDecimal totalAmount = safeAmount(settlementItem.getTotalAmount());
                itemsAmount = itemsAmount.add(totalAmount).setScale(2, RoundingMode.HALF_UP);
                if (Objects.equals(settlementItem.getCouponEnabled(), 1)) {
                    eligibleItemsAmount = eligibleItemsAmount.add(totalAmount).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        return CouponSettlementResultDto.builder()
                .itemsAmount(itemsAmount)
                .eligibleItemsAmount(eligibleItemsAmount)
                .couponDeductAmount(safeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponDeductAmount()))
                .couponConsumeAmount(safeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponConsumeAmount()))
                .couponWasteAmount(safeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponWasteAmount()))
                .allocations(autoSelectResult == null || autoSelectResult.getAllocations() == null
                        ? List.of()
                        : autoSelectResult.getAllocations())
                .build();
    }

    /**
     * 计算订单项商品总金额。
     *
     * @param items 订单项列表
     * @return 订单项商品总金额
     */
    private BigDecimal calculateOrderItemsAmount(List<MallOrderItem> items) {
        return items.stream()
                .map(MallOrderItem::getTotalPrice)
                .map(this::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按目标商品金额等比例缩放订单项。
     *
     * @param items             订单项列表
     * @param targetItemsAmount 目标商品金额
     * @param now               当前时间
     */
    private void scaleOrderItems(List<MallOrderItem> items, BigDecimal targetItemsAmount, Date now) {
        BigDecimal originalItemsAmount = calculateOrderItemsAmount(items);
        if (originalItemsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "订单商品金额异常，无法改价");
        }

        BigDecimal normalizedTargetItemsAmount = safeAmount(targetItemsAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = normalizedTargetItemsAmount;
        for (int i = 0; i < items.size(); i++) {
            MallOrderItem item = items.get(i);
            BigDecimal itemNewTotalPrice;
            if (i == items.size() - 1) {
                itemNewTotalPrice = remainingAmount;
            } else {
                itemNewTotalPrice = safeAmount(item.getTotalPrice())
                        .multiply(normalizedTargetItemsAmount)
                        .divide(originalItemsAmount, 2, RoundingMode.HALF_UP);
                remainingAmount = remainingAmount.subtract(itemNewTotalPrice).setScale(2, RoundingMode.HALF_UP);
            }
            item.setTotalPrice(itemNewTotalPrice);
            item.setCouponDeductAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setPayableAmount(itemNewTotalPrice);
            if (item.getQuantity() != null && item.getQuantity() > 0) {
                item.setPrice(itemNewTotalPrice.divide(new BigDecimal(item.getQuantity()), 2, RoundingMode.HALF_UP));
            }
            item.setUpdateTime(now);
        }
    }

    /**
     * 将优惠券分摊结果写回订单项。
     *
     * @param items            订单项列表
     * @param settlementResult 优惠券结算结果
     */
    private void applyCouponAllocations(List<MallOrderItem> items, CouponSettlementResultDto settlementResult) {
        Map<String, CouponSettlementAllocationDto> allocationMap = new HashMap<>();
        for (CouponSettlementAllocationDto allocation : settlementResult.getAllocations()) {
            allocationMap.put(allocation.getItemKey(), allocation);
        }
        for (MallOrderItem item : items) {
            CouponSettlementAllocationDto allocation = allocationMap.get(String.valueOf(item.getId()));
            if (allocation == null) {
                item.setCouponDeductAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                item.setPayableAmount(safeAmount(item.getTotalPrice()));
                continue;
            }
            item.setCouponDeductAmount(safeAmount(allocation.getCouponDeductAmount()));
            item.setPayableAmount(safeAmount(allocation.getPayableAmount()));
        }
    }

    /**
     * 计算订单最终应付金额。
     *
     * @param itemsAmount        商品原始总金额
     * @param freightAmount      运费金额
     * @param couponDeductAmount 优惠抵扣金额
     * @return 订单最终应付金额
     */
    private BigDecimal calculateOrderTotalAmount(BigDecimal itemsAmount,
                                                 BigDecimal freightAmount,
                                                 BigDecimal couponDeductAmount) {
        return safeAmount(itemsAmount)
                .add(safeAmount(freightAmount))
                .subtract(safeAmount(couponDeductAmount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 订单退款
     * <p>
     * 整个退款流程必须在事务中执行，确保订单状态更新和钱包充值的原子性。
     * 如果钱包退款成功但订单状态更新失败，会导致重复退款问题。
     * </p>
     *
     * @param request 订单退款参数
     * @return 是否退款成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "#request.orderNo")
    public boolean orderRefund(OrderRefundRequest request) {
        // 1. 加载并校验订单基本信息与可退款额度，校验失败会直接抛异常并终止流程。
        MallOrder mallOrder = loadRefundableOrder(request);
        PayTypeEnum payType = PayTypeEnum.fromCode(mallOrder.getPayType());
        if (payType == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "暂不支持该支付方式退款!");
        }

        // 2. 先更新订单状态，确保退款金额被记录，防止重复退款
        applyRefundSnapshot(mallOrder, request.getRefundAmount());
        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "更新订单退款状态失败, 请稍后重试!");
        }

        // 3. 当前仅支持钱包订单退款，其他支付方式直接拒绝处理。
        if (payType != PayTypeEnum.WALLET) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前仅支持钱包支付订单退款!");
        }
        processWalletRefund(mallOrder, request);

        // 4. 添加订单时间线记录
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .eventType(OrderEventTypeEnum.ORDER_REFUNDED.getType())
                .eventStatus(mallOrder.getOrderStatus())
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description("管理员发起订单退款")
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        return true;
    }

    /**
     * 取消订单
     * <p>
     * 取消逻辑：
     * 1. 如果订单未支付：直接取消并恢复库存
     * 2. 如果订单已支付：先全额退款，再取消订单
     * 3. 只有待支付、待发货状态的订单可以取消
     * </p>
     *
     * @param request 订单取消参数
     * @return 是否取消成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean cancelOrder(OrderCancelRequest request) {
        // 1. 查询订单并校验状态
        MallOrder mallOrder = getOrderById(request.getOrderId());
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());

        if (orderStatusEnum == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单状态异常");
        }

        // 只有待支付、待发货状态可以取消
        if (orderStatusEnum != OrderStatusEnum.PENDING_PAYMENT &&
                orderStatusEnum != OrderStatusEnum.PENDING_SHIPMENT) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许取消", orderStatusEnum.getName()));
        }

        // 3. 如果订单已支付，需要先退款
        if (Objects.equals(mallOrder.getPaid(), PAID_FLAG)) {
            log.info("订单{}已支付，执行全额退款", mallOrder.getOrderNo());

            BigDecimal refundAmount = mallOrder.getPayAmount();
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单支付金额异常，无法退款");
            }

            // 根据支付方式执行退款
            PayTypeEnum payType = PayTypeEnum.fromCode(mallOrder.getPayType());
            if (payType == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "支付方式异常");
            }

            switch (payType) {
                case WALLET -> {
                    // 钱包退款
                    Long userId = mallOrder.getUserId();
                    if (userId == null) {
                        throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单用户信息异常");
                    }
                    String walletRemark = String.format("订单取消退款（订单号：%s，退款金额：%s元）",
                            mallOrder.getOrderNo(), formatAmount(refundAmount));
                    boolean success = userWalletService.rechargeWallet(userId, refundAmount, walletRemark);
                    if (!success) {
                        throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包退款失败");
                    }
                }
                case COUPON -> log.info("订单{}为优惠券零元支付订单，取消时无需执行现金退款", mallOrder.getOrderNo());
                default -> throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前仅支持钱包支付订单取消退款");
            }

            // 更新退款信息
            mallOrder.setRefundPrice(refundAmount);
            mallOrder.setRefundTime(new Date());
            mallOrder.setRefundStatus(REFUND_STATUS_SUCCESS);
        }

        // 4. 更新订单状态为已取消
        String cancelReason = request.getCancelReason();
        if (!StringUtils.hasText(cancelReason)) {
            cancelReason = "管理员取消订单";
        }

        mallOrder.setOrderStatus(OrderStatusEnum.CANCELLED.getType());
        mallOrder.setPayType(PayTypeEnum.CANCELLED.getType());
        mallOrder.setCloseReason(cancelReason);
        mallOrder.setCloseTime(new Date());
        mallOrder.setUpdateTime(new Date());

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "取消订单失败");
        }

        // 5. 释放锁定优惠券并恢复库存。
        if (orderStatusEnum == OrderStatusEnum.PENDING_PAYMENT) {
            couponAdminService.releaseLockedCouponForOrder(mallOrder, SecurityUtils.getUsername(), cancelReason);
        }
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, mallOrder.getId())
                .list();

        if (!CollectionUtils.isEmpty(orderItems)) {
            for (MallOrderItem orderItem : orderItems) {
                if (orderItem != null && orderItem.getProductId() != null && orderItem.getQuantity() != null) {
                    mallInventoryService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                    log.info("恢复商品库存，商品ID：{}，数量：{}", orderItem.getProductId(), orderItem.getQuantity());
                }
            }
        }

        // 6. 添加订单时间线记录
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .eventType(OrderEventTypeEnum.ORDER_CANCELLED.getType())
                .eventStatus(OrderStatusEnum.CANCELLED.getType())
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description("管理员取消了订单：" + cancelReason)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        log.info("订单{}取消成功，是否退款：{}", mallOrder.getOrderNo(), Objects.equals(mallOrder.getPaid(), PAID_FLAG));
        return true;
    }

    /**
     * 根据退款请求加载订单并进行前置校验。
     *
     * @param request 退款请求参数
     * @return 可退款且校验通过的订单实体
     */
    private MallOrder loadRefundableOrder(OrderRefundRequest request) {
        // 订单号是定位订单的唯一凭证，这里使用业务异常兜底校验。
        Assert.isTrue(request != null, "订单退款参数不能为空");
        MallOrder mallOrder = getOrderByOrderNo(request.getOrderNo());
        BigDecimal refundAmount = request.getRefundAmount();
        if (refundAmount == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "退款金额不能为空");
        }
        if (!Objects.equals(mallOrder.getPaid(), PAID_FLAG)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单未支付，无法退款!");
        }

        ensureRefundAmountAllowed(mallOrder, refundAmount);
        return mallOrder;
    }

    /**
     * 校验退款金额是否合法：大于 0 且不超过剩余可退金额。
     */
    private void ensureRefundAmountAllowed(MallOrder mallOrder, BigDecimal refundAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "退款金额必须大于0!");
        }
        BigDecimal payAmount = safeAmount(mallOrder.getPayAmount());
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单支付金额异常，无法退款!");
        }
        BigDecimal alreadyRefunded = safeAmount(mallOrder.getRefundPrice());
        BigDecimal remainingAmount = payAmount.subtract(alreadyRefunded);
        if (refundAmount.compareTo(remainingAmount) > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "退款金额不能大于可退款金额!");
        }
    }

    /**
     * 钱包退款实现
     * <p>
     * 将退款金额返还到用户钱包余额中，并记录钱包流水
     * </p>
     *
     * @param mallOrder 订单信息
     * @param request   退款请求参数
     */
    private void processWalletRefund(MallOrder mallOrder, OrderRefundRequest request) {
        // 获取退款金额和用户ID
        BigDecimal refundAmount = request.getRefundAmount();
        Long userId = mallOrder.getUserId();

        if (userId == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单用户信息异常，无法退款");
        }

        // 构建退款原因描述
        String walletRemark = String.format("订单退款（订单号：%s，退款原因：%s，退款金额：%s元）",
                mallOrder.getOrderNo(),
                determineRefundReason(request.getRefundReason()),
                formatAmount(refundAmount));

        // 调用钱包服务进行余额充值（退款）
        boolean success = userWalletService.rechargeWallet(userId, refundAmount, walletRemark);

        if (!success) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包退款失败，请稍后重试");
        }

        log.info("钱包退款成功，订单号：{}，用户ID：{}，退款金额：{}",
                mallOrder.getOrderNo(), userId, refundAmount);
    }

    /**
     * 根据本次退款结果刷新订单对象的退款金额、时间与状态，确保后续查询能实时反映退款情况。
     */
    private void applyRefundSnapshot(MallOrder mallOrder, BigDecimal refundAmount) {
        BigDecimal alreadyRefunded = safeAmount(mallOrder.getRefundPrice());
        BigDecimal totalRefunded = alreadyRefunded.add(refundAmount);
        mallOrder.setRefundPrice(totalRefunded);
        mallOrder.setRefundTime(new Date());

        BigDecimal payableAmount = safeAmount(mallOrder.getPayAmount());
        boolean fullyRefunded = payableAmount.compareTo(totalRefunded) == 0;
        mallOrder.setRefundStatus(fullyRefunded ? REFUND_STATUS_SUCCESS : REFUND_STATUS_PARTIAL);

        if (fullyRefunded) {
            mallOrder.setOrderStatus(OrderStatusEnum.REFUNDED.getType());
        } else if (!Objects.equals(mallOrder.getOrderStatus(), OrderStatusEnum.REFUNDED.getType())) {
            mallOrder.setOrderStatus(OrderStatusEnum.AFTER_SALE.getType());
        }
    }

    /**
     * 空值保护，避免金额字段为 null 时触发 NPE。
     */
    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 若未传退款原因则回落到默认文案，方便审计与追责。
     */
    private String determineRefundReason(String refundReason) {
        return StringUtils.hasText(refundReason) ? refundReason : DEFAULT_REFUND_REASON;
    }

    /**
     * 将金额统一格式化为两位小数字符串，便于退款备注与日志复用。
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public Page<OrderWithProductDto> orderWithProduct(MallOrderListRequest request) {
        Page<OrderWithProductDto> orderWithProductDtoPage = request.toPage();
        Page<OrderWithProductDto> withProductDtoPage = mallOrderMapper.orderListWithProduct(orderWithProductDtoPage, request);
        List<OrderWithProductDto> records = withProductDtoPage.getRecords();
        if (records.isEmpty()) {
            return withProductDtoPage;
        }
        // 获取所有的商品ID
        List<Long> productIds = records.stream()
                .map(OrderWithProductDto::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return withProductDtoPage;
        }
        // 根据商品的ID获取商品的封面图片
        List<MallProductImage> images = mallProductImageService.getFirstImageByProductIds(productIds);
        if (images == null || images.isEmpty()) {
            return withProductDtoPage;
        }
        // 将图片URL映射到商品ID
        Map<Long, String> productImageMap = images.stream()
                .collect(Collectors.toMap(MallProductImage::getProductId, MallProductImage::getImageUrl, (existing, ignore) -> existing));

        // 为每个订单项设置商品图片URL
        records.forEach(orderWithProductDto -> {
            Long productId = orderWithProductDto.getProductId();
            orderWithProductDto.setProductImage(productImageMap.get(productId));
        });
        return withProductDtoPage;
    }

    @Override
    public List<MallOrder> getExpiredOrderClean(long expiredTime) {
        return mallOrderMapper.getExpiredOrderClean(expiredTime);
    }

    /**
     * 执行过期订单补偿关闭逻辑。
     *
     * @param orderNo 订单编号
     * @return 是否关闭成功
     */
    @Override
    public boolean closeExpiredOrderForCompensation(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            log.warn("收到空订单号的过期订单补偿关闭请求");
            return false;
        }
        String normalizedOrderNo = orderNo.trim();
        String lockName = buildOrderLockName(normalizedOrderNo);
        return distributedLockExecutor.tryExecuteOrElse(lockName,
                0L,
                -1L,
                () -> transactionTemplate.execute(status -> doCloseExpiredOrderForCompensation(normalizedOrderNo)),
                () -> false);
    }

    /**
     * 在事务内执行过期订单补偿关闭逻辑。
     *
     * @param orderNo 订单编号
     * @return 是否关闭成功
     */
    private boolean doCloseExpiredOrderForCompensation(String orderNo) {
        MallOrder order = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getOrderStatus, OrderStatusEnum.PENDING_PAYMENT.getType())
                .select(MallOrder::getId, MallOrder::getOrderNo, MallOrder::getVersion, MallOrder::getPayExpireTime)
                .one();

        if (order == null) {
            log.info("订单{}补偿关闭跳过，当前状态可能已变更", orderNo);
            return false;
        }

        Date now = new Date();
        Date payExpireTime = order.getPayExpireTime();
        if (payExpireTime != null && payExpireTime.after(now)) {
            log.info("订单{}补偿关闭跳过，当前支付过期时间尚未到达", orderNo);
            return false;
        }

        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, order.getId())
                .list();

        boolean updated = lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getVersion, order.getVersion())
                .set(MallOrder::getOrderStatus, OrderStatusEnum.EXPIRED.getType())
                .set(MallOrder::getCloseReason, ORDER_TIMEOUT_CLOSE_REASON)
                .set(MallOrder::getCloseTime, now)
                .set(MallOrder::getUpdateBy, SYSTEM_AUTO_CLOSE_OPERATOR)
                .set(MallOrder::getUpdateTime, now)
                .update();

        if (!updated) {
            log.info("订单{}补偿关闭失败，当前状态可能已变更", orderNo);
            return false;
        }

        if (!CollectionUtils.isEmpty(orderItems)) {
            for (MallOrderItem orderItem : orderItems) {
                if (orderItem != null && orderItem.getProductId() != null && orderItem.getQuantity() != null) {
                    mallInventoryService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                }
            }
        }

        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(order.getId())
                .eventType(OrderEventTypeEnum.ORDER_EXPIRED.getType())
                .eventStatus(OrderStatusEnum.EXPIRED.getType())
                .operatorType(OperatorTypeEnum.SYSTEM.getType())
                .description(ORDER_TIMEOUT_CLOSE_REASON)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
        log.info("订单{}已通过补偿任务自动关闭", orderNo);
        return true;
    }

    @Override
    public Page<MallOrder> getPaidOrderPage(Long userId, PageRequest request) {
        Page<MallOrder> page = request.toPage();
        return mallOrderMapper.getPaidOrderPage(page, userId);
    }

    @Override
    public UserOrderStatistics getOrderStatisticsByUserId(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        return userMapper.getOrderStatisticsByUserId(userId);
    }

    /**
     * 获取配送方式描述
     */
    private String getDeliveryTypeDesc(String deliveryType) {
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(deliveryType);
        return deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知";
    }

    /**
     * 在事务提交成功后发布订单支付超时延迟消息。
     *
     * @param orderNo 订单编号
     * @param delay   延迟时长
     * @param unit    延迟时间单位
     */
    private void publishOrderTimeoutAfterCommit(String orderNo, long delay, TimeUnit unit) {
        runAfterCommit(() -> orderTimeoutMessagePublisher.publishOrderTimeout(orderNo, delay, unit));
    }

    /**
     * 在事务提交成功后执行指定任务。
     *
     * @param task 需要延后执行的任务
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }


    /**
     * 获取支付方式描述
     */
    private String getPayTypeDesc(String payType) {
        PayTypeEnum payTypeEnum = PayTypeEnum.fromCode(payType);
        return payTypeEnum != null ? payTypeEnum.getType() : "未知";
    }

    private OrderDetailDto buildOrderDetailDto(OrderDetailRow row) {
        OrderDetailDto.UserInfo userInfo = null;
        if (row.getUserId() != null) {
            userInfo = OrderDetailDto.UserInfo.builder()
                    .userId(String.valueOf(row.getUserId()))
                    .nickname(row.getUserNickname())
                    .phoneNumber(row.getUserPhoneNumber())
                    .build();
        }

        OrderDetailDto.DeliveryInfo deliveryInfo = OrderDetailDto.DeliveryInfo.builder()
                .receiverName(row.getReceiverName())
                .receiverAddress(row.getReceiverDetail())
                .receiverPhone(row.getReceiverPhone())
                .deliveryMethod(getDeliveryTypeDesc(row.getDeliveryType()))
                .build();

        OrderDetailDto.OrderInfo orderInfo = OrderDetailDto.OrderInfo.builder()
                .orderNo(row.getOrderNo())
                .orderStatus(row.getOrderStatus())
                .payType(row.getPayType())
                .itemsAmount(row.getItemsAmount())
                .totalAmount(row.getTotalAmount())
                .payAmount(row.getPayAmount())
                .freightAmount(row.getFreightAmount())
                .couponId(row.getCouponId())
                .couponName(row.getCouponName())
                .couponDeductAmount(row.getCouponDeductAmount())
                .couponConsumeAmount(row.getCouponConsumeAmount())
                .build();

        return OrderDetailDto.builder()
                .userInfo(userInfo)
                .deliveryInfo(deliveryInfo)
                .orderInfo(orderInfo)
                .productInfo(new ArrayList<>())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean shipOrder(OrderShipRequest request) {
        // 1. 查询订单并校验状态
        MallOrder mallOrder = getOrderById(request.getOrderId());

        // 2. 校验订单状态是否允许发货
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单状态异常");
        }

        // 只有待发货状态可以发货
        if (orderStatusEnum != OrderStatusEnum.PENDING_SHIPMENT) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许发货", orderStatusEnum.getName()));
        }

        // 3. 更新订单状态为待收货
        Date now = new Date();
        mallOrder.setOrderStatus(OrderStatusEnum.PENDING_RECEIPT.getType());
        mallOrder.setDeliverTime(now);
        mallOrder.setUpdateTime(now);

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发货失败，请重试");
        }

        // 4. 创建物流记录
        MallOrderShipping shipping = MallOrderShipping.builder()
                .orderId(mallOrder.getId())
                .shippingNo(request.getTrackingNumber())
                .shippingCompany(request.getLogisticsCompany())
                .deliveryType(mallOrder.getDeliveryType())
                .status(ShippingStatusEnum.IN_TRANSIT.getType())
                .deliverTime(now)
                .shipmentNote(request.getShipmentNote())
                .createTime(now)
                .updateTime(now)
                .build();

        boolean shippingCreated = mallOrderShippingService.createShipping(shipping);
        if (!shippingCreated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "创建物流记录失败");
        }

        // 5. 添加订单时间线记录
        String description = String.format("管理员发货，物流公司：%s，物流单号：%s",
                request.getLogisticsCompany(), request.getTrackingNumber());
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .eventType(OrderEventTypeEnum.ORDER_SHIPPED.getType())
                .eventStatus(OrderStatusEnum.PENDING_RECEIPT.getType())
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        log.info("订单{}发货成功，物流公司：{}，物流单号：{}", mallOrder.getOrderNo(),
                request.getLogisticsCompany(), request.getTrackingNumber());
        return true;
    }

    @Override
    public OrderShippingVo getOrderShipping(Long orderId) {
        // 1. 查询订单基本信息
        MallOrder mallOrder = getOrderById(orderId);

        // 2. 查询物流信息
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(orderId);

        // 3. 获取订单状态名称
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        String orderStatusName = orderStatusEnum != null ? orderStatusEnum.getName() : "未知";

        // 4. 组装收货人信息
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(mallOrder.getDeliveryType());
        OrderShippingVo.ReceiverInfo receiverInfo = OrderShippingVo.ReceiverInfo.builder()
                .receiverName(mallOrder.getReceiverName())
                .receiverPhone(mallOrder.getReceiverPhone())
                .receiverDetail(mallOrder.getReceiverDetail())
                .deliveryType(mallOrder.getDeliveryType())
                .deliveryTypeName(deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知")
                .build();

        // 5. 组装返回VO
        OrderShippingVo.OrderShippingVoBuilder builder = OrderShippingVo.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .orderStatusName(orderStatusName)
                .receiverInfo(receiverInfo);

        // 6. 如果有物流信息，添加物流详情
        if (shipping != null) {
            ShippingStatusEnum statusEnum = ShippingStatusEnum.fromCode(shipping.getStatus());
            builder.logisticsCompany(shipping.getShippingCompany())
                    .trackingNumber(shipping.getShippingNo())
                    .shipmentNote(shipping.getShipmentNote())
                    .deliverTime(shipping.getDeliverTime())
                    .receiveTime(shipping.getReceiveTime())
                    .status(shipping.getStatus())
                    .statusName(statusEnum != null ? statusEnum.getName() : "未知")
                    .nodes(parseShippingNodes(shipping.getShippingInfo()));
        }

        return builder.build();
    }

    /**
     * 解析订单物流轨迹 JSON。
     *
     * @param shippingInfo 物流轨迹 JSON 字符串
     * @return 物流轨迹节点列表
     */
    private List<OrderShippingVo.ShippingNode> parseShippingNodes(String shippingInfo) {
        if (!StringUtils.hasText(shippingInfo)) {
            return List.of();
        }
        JsonElement element = JSONUtils.parseLenient(shippingInfo);
        JsonArray nodeArray = extractShippingNodeArray(element);
        if (nodeArray == null) {
            return List.of();
        }

        List<OrderShippingVo.ShippingNode> nodes = new ArrayList<>();
        for (JsonElement nodeElement : nodeArray) {
            if (!nodeElement.isJsonObject()) {
                continue;
            }
            JsonObject nodeObject = nodeElement.getAsJsonObject();
            String time = firstNonBlank(nodeObject, "time", "acceptTime", "timestamp", "createTime", "date");
            String content = firstNonBlank(nodeObject, "content", "description", "status", "remark",
                    "context", "acceptStation");
            String location = firstNonBlank(nodeObject, "location", "site", "address", "city", "nodeName");
            if (!StringUtils.hasText(time) && !StringUtils.hasText(content) && !StringUtils.hasText(location)) {
                continue;
            }
            nodes.add(OrderShippingVo.ShippingNode.builder()
                    .time(time)
                    .content(content)
                    .location(location)
                    .build());
        }
        return nodes;
    }

    /**
     * 提取物流节点数组。
     *
     * @param element JSON 节点
     * @return 物流节点数组
     */
    private JsonArray extractShippingNodeArray(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject jsonObject = element.getAsJsonObject();
        for (String key : List.of("traces", "nodes", "list", "data", "tracks", "shippingNodes")) {
            JsonElement candidate = jsonObject.get(key);
            if (candidate == null || candidate.isJsonNull()) {
                continue;
            }
            if (candidate.isJsonArray()) {
                return candidate.getAsJsonArray();
            }
            if (candidate.isJsonObject()) {
                JsonArray nested = extractShippingNodeArray(candidate);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * 读取 JSON 对象中首个非空文本字段。
     *
     * @param jsonObject JSON 对象
     * @param keys       字段名列表
     * @return 首个非空文本字段
     */
    private String firstNonBlank(JsonObject jsonObject, String... keys) {
        for (String key : keys) {
            JsonElement value = jsonObject.get(key);
            if (value == null || value.isJsonNull()) {
                continue;
            }
            String text = value.getAsString();
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    @Override
    public List<MallOrder> getOrdersForAutoConfirm(int daysAfterShipment) {
        // 计算N天前的时间点
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -daysAfterShipment);
        Date targetDate = calendar.getTime();

        // 查询发货时间在N天前且状态仍为待收货的订单
        return lambdaQuery()
                .eq(MallOrder::getOrderStatus, OrderStatusEnum.PENDING_RECEIPT.getType())
                .le(MallOrder::getDeliverTime, targetDate)
                .isNotNull(MallOrder::getDeliverTime)
                .list();
    }

    @Override
    public boolean autoConfirmReceipt(Long orderId) {
        String lockName = buildOrderLockName(getOrderById(orderId).getOrderNo());
        return distributedLockExecutor.tryExecuteOrElse(lockName,
                0L,
                -1L,
                () -> transactionTemplate.execute(status -> doAutoConfirmReceipt(orderId)),
                () -> false);
    }

    /**
     * 在事务内执行自动确认收货逻辑。
     *
     * @param orderId 订单 ID
     * @return 是否确认成功
     */
    private boolean doAutoConfirmReceipt(Long orderId) {
        // 1. 查询订单并校验状态
        MallOrder mallOrder = getOrderById(orderId);

        // 2. 校验订单状态
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum != OrderStatusEnum.PENDING_RECEIPT) {
            log.warn("订单{}状态不是待收货，无法自动确认收货，当前状态：{}", mallOrder.getOrderNo(), orderStatusEnum);
            return false;
        }

        // 3. 更新订单状态为已完成
        Date now = new Date();
        mallOrder.setOrderStatus(OrderStatusEnum.COMPLETED.getType());
        mallOrder.setReceiveTime(now);
        mallOrder.setFinishTime(now);
        mallOrder.setUpdateTime(now);

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "自动确认收货失败");
        }

        // 4. 更新物流状态为已签收
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(orderId);
        if (shipping != null) {
            shipping.setStatus(ShippingStatusEnum.DELIVERED.getType());
            shipping.setReceiveTime(now);
            shipping.setUpdateTime(now);
            mallOrderShippingService.updateById(shipping);
        }

        // 5. 添加订单时间线记录（标记为系统自动）
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .eventType(OrderEventTypeEnum.ORDER_RECEIVED.getType())
                .eventStatus(OrderStatusEnum.COMPLETED.getType())
                .operatorType(OperatorTypeEnum.SYSTEM.getType())
                .description("系统自动确认收货")
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        log.info("订单{}自动确认收货成功", mallOrder.getOrderNo());
        return true;
    }

    /**
     * 构建订单级分布式锁名称。
     *
     * @param orderNo 订单号
     * @return 订单级分布式锁名称
     */
    private String buildOrderLockName(String orderNo) {
        return String.format(RedisConstants.Lock.ORDER_KEY, orderNo.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "@mallDistributedLockKeyResolver.orderKeyById(#request.orderId)")
    public boolean manualConfirmReceipt(OrderReceiveRequest request) {
        // 1. 查询订单并校验
        MallOrder mallOrder = getOrderById(request.getOrderId());

        // 2. 校验订单状态是否允许确认收货
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单状态异常");
        }

        // 只有待收货状态可以确认收货
        if (orderStatusEnum != OrderStatusEnum.PENDING_RECEIPT) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许确认收货", orderStatusEnum.getName()));
        }

        // 3. 更新订单状态为已完成
        Date now = new Date();
        mallOrder.setOrderStatus(OrderStatusEnum.COMPLETED.getType());
        mallOrder.setReceiveTime(now);
        mallOrder.setFinishTime(now);
        mallOrder.setUpdateTime(now);

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "确认收货失败，请重试");
        }

        // 4. 更新物流状态为已签收
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(request.getOrderId());
        if (shipping != null) {
            shipping.setStatus(ShippingStatusEnum.DELIVERED.getType());
            shipping.setReceiveTime(now);
            shipping.setUpdateTime(now);
            mallOrderShippingService.updateById(shipping);
        }

        // 5. 添加订单时间线记录（标记为管理员操作）
        String username = SecurityUtils.getUsername();
        String description = String.format("管理员%s手动确认收货", username);
        if (request.getRemark() != null && !request.getRemark().trim().isEmpty()) {
            description += String.format("，备注：%s", request.getRemark());
        }

        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .eventType(OrderEventTypeEnum.ORDER_RECEIVED.getType())
                .eventStatus(OrderStatusEnum.COMPLETED.getType())
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        log.info("管理员{}手动确认收货成功，订单号：{}，备注：{}", username, mallOrder.getOrderNo(), request.getRemark());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrders(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "请选择要删除的订单");
        }

        List<MallOrder> orders = listByIds(ids);
        if (orders.size() != ids.size()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "存在无效订单ID，无法删除");
        }

        // 当订单状态为 已完成、已取消或已过期时，才允许删除
        for (MallOrder order : orders) {
            String orderStatus = order.getOrderStatus();
            boolean deletable = OrderStatusEnum.COMPLETED.getType().equals(orderStatus)
                    || OrderStatusEnum.CANCELLED.getType().equals(orderStatus)
                    || OrderStatusEnum.EXPIRED.getType().equals(orderStatus);
            if (!deletable) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "只有订单状态为已完成、已取消或已过期才能被删除!");
            }
        }

        boolean deleted = removeByIds(ids);
        if (!deleted) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "删除订单失败");
        }

        orders.forEach(order -> log.info("订单{}删除成功", order.getOrderNo()));
        return true;
    }

    @Override
    public List<OrderDetailDto> getOrderDetailByIds(List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return List.of();
        }

        // 1. 批量查询订单
        List<MallOrder> orders = listByIds(orderIds);
        if (orders.isEmpty()) {
            return List.of();
        }

        // 2. 批量查询用户信息
        List<Long> userIds = orders.stream()
                .map(MallOrder::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, user -> user, (existing, ignore) -> existing));
        }

        // 3. 批量查询订单商品
        List<Long> orderIdsFromOrders = orders.stream().map(MallOrder::getId).toList();
        List<MallOrderItem> allOrderItems = mallOrderItemService.lambdaQuery()
                .in(MallOrderItem::getOrderId, orderIdsFromOrders)
                .list();
        Map<Long, List<MallOrderItem>> orderItemMap = allOrderItems.stream()
                .collect(Collectors.groupingBy(MallOrderItem::getOrderId));

        // 4. 构建返回结果
        List<OrderDetailDto> result = new ArrayList<>();
        for (MallOrder mallOrder : orders) {
            // 获取用户信息
            User userInfo = userMap.get(mallOrder.getUserId());
            OrderDetailDto.UserInfo userInfoVo = null;
            if (userInfo != null) {
                userInfoVo = OrderDetailDto.UserInfo.builder()
                        .userId(userInfo.getId().toString())
                        .nickname(userInfo.getNickname())
                        .phoneNumber(userInfo.getPhoneNumber())
                        .build();
            }

            // 构建配送信息
            OrderDetailDto.DeliveryInfo deliveryInfo = OrderDetailDto.DeliveryInfo.builder()
                    .receiverName(mallOrder.getReceiverName())
                    .receiverAddress(mallOrder.getReceiverDetail())
                    .receiverPhone(mallOrder.getReceiverPhone())
                    .deliveryMethod(getDeliveryTypeDesc(mallOrder.getDeliveryType()))
                    .build();

            // 构建订单信息
            OrderDetailDto.OrderInfo orderInfo = OrderDetailDto.OrderInfo.builder()
                    .orderNo(mallOrder.getOrderNo())
                    .orderStatus(mallOrder.getOrderStatus())
                    .payType(mallOrder.getPayType())
                    .totalAmount(mallOrder.getTotalAmount())
                    .payAmount(mallOrder.getPayAmount())
                    .freightAmount(mallOrder.getFreightAmount())
                    .build();

            // 构建商品信息
            List<OrderDetailDto.ProductInfo> productInfoList = new ArrayList<>();
            List<MallOrderItem> orderItems = orderItemMap.getOrDefault(mallOrder.getId(), List.of());
            for (MallOrderItem item : orderItems) {
                OrderDetailDto.ProductInfo productInfo = OrderDetailDto.ProductInfo.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productImage(item.getImageUrl())
                        .productPrice(item.getPrice())
                        .productQuantity(item.getQuantity())
                        .productTotalAmount(item.getTotalPrice())
                        .build();
                productInfoList.add(productInfo);
            }

            result.add(OrderDetailDto.builder()
                    .userInfo(userInfoVo)
                    .deliveryInfo(deliveryInfo)
                    .orderInfo(orderInfo)
                    .productInfo(productInfoList)
                    .build());
        }

        return result;
    }

}
