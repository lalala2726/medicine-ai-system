package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.client.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.client.model.dto.MallOrderDto;
import com.zhangyichuang.medicine.client.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.client.model.request.*;
import com.zhangyichuang.medicine.client.model.vo.*;
import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import com.zhangyichuang.medicine.client.service.*;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.rabbitmq.publisher.OrderTimeoutMessagePublisher;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zhangyichuang.medicine.common.core.constants.Constants.ORDER_TIMEOUT_MINUTES;

/**
 * @author Chuang
 */
@Service
public class MallOrderServiceImpl extends ServiceImpl<MallOrderMapper, MallOrder> implements MallOrderService, BaseService {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(MallOrderServiceImpl.class);

    /**
     * 待支付订单状态编码。
     */
    private static final String ORDER_STATUS_WAIT_PAY = OrderStatusEnum.PENDING_PAYMENT.getType();

    /**
     * 待发货订单状态编码。
     */
    private static final String ORDER_STATUS_WAIT_SHIPMENT = OrderStatusEnum.PENDING_SHIPMENT.getType();

    /**
     * 待支付的支付类型编码。
     */
    private static final String WAIT_PAY = PayTypeEnum.WAIT_PAY.getType();

    /**
     * 触发销量索引同步所需的销量增量阈值。
     */
    private static final int SALES_SYNC_THRESHOLD = 5;

    /**
     * 订单允许取消的校验结果编码。
     */
    private static final String CANCEL_CHECK_REASON_CAN_CANCEL = "CAN_CANCEL";

    /**
     * 订单不存在时的校验结果编码。
     */
    private static final String CANCEL_CHECK_REASON_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    /**
     * 订单状态异常时的校验结果编码。
     */
    private static final String CANCEL_CHECK_REASON_STATUS_INVALID = "ORDER_STATUS_INVALID";

    /**
     * 订单状态不可取消时的校验结果编码。
     */
    private static final String CANCEL_CHECK_REASON_STATUS_NOT_CANCELABLE = "ORDER_STATUS_NOT_CANCELABLE";

    /**
     * 订单不存在或无访问权限时的提示语。
     */
    private static final String ORDER_NOT_FOUND_OR_NO_PERMISSION_MESSAGE = "订单不存在或无权访问";

    /**
     * 订单自动关闭的关闭原因文案。
     */
    private static final String ORDER_TIMEOUT_CLOSE_REASON = "订单支付超时，系统自动关闭";

    /**
     * 订单自动关闭时写入的操作人标识。
     */
    private static final String SYSTEM_AUTO_CLOSE_OPERATOR = "系统自动关闭";

    /**
     * 订单超时关闭时重新投递所需的最小延迟毫秒数。
     */
    private static final long MIN_REQUEUE_DELAY_MILLIS = 1000L;

    private final MallProductService mallProductService;
    private final MallOrderItemService mallOrderItemService;
    private final OrderTimeoutMessagePublisher orderTimeoutMessagePublisher;
    private final UserWalletService userWalletService;
    private final MallOrderTimelineService mallOrderTimelineService;
    private final MallOrderShippingService mallOrderShippingService;
    private final MallCartService mallCartService;
    private final UserAddressService userAddressService;
    private final RedisCache redisCache;
    private final MallProductSearchService mallProductSearchService;
    private final UserCouponService userCouponService;
    /**
     * 分布式锁执行器。
     */
    private final DistributedLockExecutor distributedLockExecutor;
    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造商城订单服务实现。
     *
     * @param mallProductService           商品服务
     * @param mallOrderItemService         订单项服务
     * @param orderTimeoutMessagePublisher 订单超时消息发布器
     * @param userWalletService            用户钱包服务
     * @param mallOrderTimelineService     订单时间线服务
     * @param mallOrderShippingService     订单物流服务
     * @param mallCartService              购物车服务
     * @param userAddressService           用户地址服务
     * @param redisCache                   Redis缓存
     * @param mallProductSearchService     商品搜索服务
     * @param userCouponService            用户优惠券服务
     * @param distributedLockExecutor      分布式锁执行器
     * @param transactionTemplate          事务模板
     */
    public MallOrderServiceImpl(MallProductService mallProductService,
                                MallOrderItemService mallOrderItemService,
                                OrderTimeoutMessagePublisher orderTimeoutMessagePublisher,
                                UserWalletService userWalletService,
                                MallOrderTimelineService mallOrderTimelineService,
                                MallOrderShippingService mallOrderShippingService,
                                MallCartService mallCartService,
                                UserAddressService userAddressService,
                                RedisCache redisCache,
                                MallProductSearchService mallProductSearchService,
                                UserCouponService userCouponService,
                                DistributedLockExecutor distributedLockExecutor,
                                TransactionTemplate transactionTemplate) {
        this.mallProductService = mallProductService;
        this.mallOrderItemService = mallOrderItemService;
        this.orderTimeoutMessagePublisher = orderTimeoutMessagePublisher;
        this.userWalletService = userWalletService;
        this.mallOrderTimelineService = mallOrderTimelineService;
        this.mallOrderShippingService = mallOrderShippingService;
        this.mallCartService = mallCartService;
        this.userAddressService = userAddressService;
        this.redisCache = redisCache;
        this.mallProductSearchService = mallProductSearchService;
        this.userCouponService = userCouponService;
        this.distributedLockExecutor = distributedLockExecutor;
        this.transactionTemplate = transactionTemplate;
    }


    /**
     * 提交订单（创建订单并锁定库存）
     * <p>
     * 用户提交订单时创建订单并扣减库存，订单状态为待支付。
     * 订单创建后需要在30分钟内完成支付，否则订单将自动取消并恢复库存。
     * </p>
     *
     * @param request 订单提交请求参数
     * @return 订单提交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_SUBMIT_USER_KEY, key = "@mallDistributedLockKeyResolver.currentUserId()")
    public OrderCheckoutVo checkoutOrder(OrderCheckoutRequest request) {
        // 1. 查询商品详情并校验上架状态
        MallProductWithImageDto mallProductWithImageDto = mallProductService.getProductWithImagesById(request.getProductId());
        if (mallProductWithImageDto == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }

        // 2. 校验商品状态与库存。
        final Integer productStatusOnSale = 1;
        if (!Objects.equals(mallProductWithImageDto.getStatus(), productStatusOnSale)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品未上架或已下架");
        }
        Integer stock = mallProductWithImageDto.getStock();
        if (stock == null || stock < request.getQuantity()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("商品库存不足，当前库存：%d", stock == null ? 0 : stock));
        }

        // 3. 计算商品金额与优惠券金额。
        BigDecimal price = mallProductWithImageDto.getPrice();
        if (price == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品价格未配置");
        }
        BigDecimal itemsAmount = normalizeOrderAmount(price.multiply(BigDecimal.valueOf(request.getQuantity())));
        String orderNo = generateOrderNo();
        Date now = new Date();
        Date expireTime = buildOrderPayExpireTime(now);
        String deliveryTypeCode = request.getDeliveryType().getType();
        Long userId = SecurityUtils.getUserId();
        UserAddress userAddress = getUserAddressOrThrow(userId, request.getAddressId());
        String receiverDetail = buildReceiverDetail(userAddress);
        BigDecimal freightAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<CouponSettlementItemDto> settlementItems = List.of(buildSettlementItem(
                "PRODUCT_" + mallProductWithImageDto.getId(),
                mallProductWithImageDto.getId(),
                itemsAmount,
                mallProductWithImageDto.getCouponEnabled()
        ));
        List<Long> selectedCouponIds = resolveCheckoutCouponIds(
                userId,
                request.getCouponId(),
                request.getDisableCoupon(),
                settlementItems
        );
        OrderCouponSelectionSnapshotDto couponSelectionSnapshot = userCouponService.lockCoupons(
                userId,
                selectedCouponIds,
                settlementItems,
                orderNo
        );
        couponSelectionSnapshot.setAutoSelected(shouldAutoSelectCoupon(request.getCouponId(), request.getDisableCoupon()));
        BigDecimal totalAmount = calculateOrderTotalAmount(
                itemsAmount,
                normalizeOrderAmount(couponSelectionSnapshot.getCouponDeductAmount())
        );
        OrderCouponSnapshotDto firstSelectedCoupon = couponSelectionSnapshot.getSelectedCoupons() == null
                || couponSelectionSnapshot.getSelectedCoupons().isEmpty()
                ? null
                : couponSelectionSnapshot.getSelectedCoupons().getFirst();

        // 4. 扣减库存，内部包含乐观锁控制。
        mallProductService.deductStock(request.getProductId(), request.getQuantity());

        // 5. 创建订单主表快照。
        MallOrder order = MallOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .itemsAmount(itemsAmount)
                .totalAmount(totalAmount)
                .payAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .freightAmount(freightAmount)
                .couponId(firstSelectedCoupon == null ? null : firstSelectedCoupon.getCouponId())
                .couponName(firstSelectedCoupon == null ? null : firstSelectedCoupon.getCouponName())
                .couponDeductAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponDeductAmount()))
                .couponConsumeAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponConsumeAmount()))
                .couponWasteAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponWasteAmount()))
                .couponSnapshotJson(buildCouponSnapshotJson(couponSelectionSnapshot))
                .payType(WAIT_PAY)
                .orderStatus(ORDER_STATUS_WAIT_PAY)
                .deliveryType(deliveryTypeCode)
                .addressId(userAddress.getId())
                .receiverName(userAddress.getReceiverName())
                .receiverPhone(userAddress.getReceiverPhone())
                .receiverDetail(receiverDetail)
                .note(request.getRemark())
                .payExpireTime(expireTime)
                .afterSaleFlag(OrderItemAfterSaleStatusEnum.NONE)
                .createTime(now)
                .updateTime(now)
                .build();

        // 6. 保存订单主表。
        if (!save(order)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "创建订单失败，请稍后再试");
        }

        MallProductImage mallProductImage = null;
        if (mallProductWithImageDto.getProductImages() != null && !mallProductWithImageDto.getProductImages().isEmpty()) {
            mallProductImage = mallProductWithImageDto.getProductImages().getFirst();
        }

        MallOrderItem mallOrderItem = MallOrderItem.builder()
                .orderId(order.getId())
                .productId(mallProductWithImageDto.getId())
                .productName(mallProductWithImageDto.getName())
                .quantity(request.getQuantity())
                .price(mallProductWithImageDto.getPrice())
                .imageUrl(mallProductImage == null ? "" : mallProductImage.getImageUrl())
                .totalPrice(itemsAmount)
                .couponDeductAmount(findCouponDeductAmount(couponSelectionSnapshot.getAllocations(),
                        "PRODUCT_" + mallProductWithImageDto.getId()))
                .payableAmount(itemsAmount.subtract(findCouponDeductAmount(couponSelectionSnapshot.getAllocations(),
                        "PRODUCT_" + mallProductWithImageDto.getId())).setScale(2, RoundingMode.HALF_UP))
                .createTime(now)
                .updateTime(now)
                .build();

        // 7. 保存订单项。
        if (!mallOrderItemService.save(mallOrderItem)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "创建订单失败，请稍后再试");
        }

        // 8. 添加订单创建时间线记录。
        String username = getUsername();
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(order.getId())
                .eventType(OrderEventTypeEnum.ORDER_CREATED.getType())
                .eventStatus(order.getOrderStatus())
                .operatorType(OperatorTypeEnum.USER.getType())
                .description(String.format("用户%s创建了订单", username))
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        // 9. 0元订单直接完成支付，非0元订单继续等待支付。
        String finalOrderStatus = ORDER_STATUS_WAIT_PAY;
        Date finalPayExpireTime = expireTime;
        if (isZeroAmount(totalAmount)) {
            boolean paid = markOrderPaid(orderNo, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), PayTypeEnum.COUPON.getType());
            if (!paid) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "零元订单自动完成失败，请重试");
            }
            finalOrderStatus = ORDER_STATUS_WAIT_SHIPMENT;
            finalPayExpireTime = null;
        } else {
            publishOrderTimeoutAfterCommit(orderNo);
        }

        return OrderCheckoutVo.builder()
                .orderNo(orderNo)
                .itemsAmount(itemsAmount)
                .totalAmount(totalAmount)
                .couponId(order.getCouponId())
                .couponName(order.getCouponName())
                .couponDeductAmount(order.getCouponDeductAmount())
                .couponConsumeAmount(order.getCouponConsumeAmount())
                .orderStatus(finalOrderStatus)
                .createTime(now)
                .payExpireTime(finalPayExpireTime)
                .productSummary(buildProductSummary(mallProductWithImageDto, request.getQuantity()))
                .itemCount(1)
                .build();
    }

    @Override
    public OrderPayInfoVo getOrderPayInfo(String orderNo) {
        MallOrder order = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .one();

        if (order == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        Long userId = getUserId();
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单信息不存在!");
        }

        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, order.getId())
                .orderByAsc(MallOrderItem::getId)
                .list();
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        PayTypeEnum payTypeEnum = PayTypeEnum.fromCode(order.getPayType());

        return OrderPayInfoVo.builder()
                .orderNo(order.getOrderNo())
                .itemsAmount(order.getItemsAmount())
                .totalAmount(order.getTotalAmount())
                .couponId(order.getCouponId())
                .couponName(order.getCouponName())
                .couponDeductAmount(order.getCouponDeductAmount())
                .couponConsumeAmount(order.getCouponConsumeAmount())
                .orderStatus(order.getOrderStatus())
                .orderStatusName(orderStatusEnum != null ? orderStatusEnum.getName() : "未知")
                .paid(order.getPaid())
                .payType(order.getPayType())
                .payTypeName(payTypeEnum != null ? payTypeEnum.getDescription() : "未知")
                .payTime(order.getPayTime())
                .createTime(order.getCreateTime())
                .payExpireTime(order.getPayExpireTime())
                .productSummary(buildProductSummary(orderItems))
                .itemCount(orderItems == null ? 0 : orderItems.size())
                .build();
    }

    /**
     * 订单支付
     * <p>
     * 对已创建的待支付订单进行支付操作，仅支持钱包支付：
     * - 钱包支付：同步扣款，订单状态变为待发货
     * </p>
     *
     * @param request 订单支付请求参数
     * @return 订单支付结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "#request.orderNo")
    public OrderPayVo payOrder(OrderPayRequest request) {
        // 1. 查询订单并校验状态
        MallOrder order = lambdaQuery()
                .eq(MallOrder::getOrderNo, request.getOrderNo())
                .one();

        if (order == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        // 2. 校验订单所属用户
        Long orderUserId = order.getUserId();
        Long userId = getUserId();
        if (!Objects.equals(orderUserId, userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单信息不存在!");
        }

        // 3. 校验订单状态（必须是待支付状态）
        if (!Objects.equals(order.getOrderStatus(), ORDER_STATUS_WAIT_PAY)) {
            OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
            String statusName = orderStatusEnum != null ? orderStatusEnum.getName() : "未知";
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许支付", statusName));
        }

        // 4. 根据支付方式处理支付
        BigDecimal payAmount = order.getTotalAmount();

        if (request.getPayMethod() != PayTypeEnum.WALLET) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前仅支持钱包支付");
        }

        boolean result = userWalletService.deductBalance(userId, payAmount,
                String.format("订单支付-%s", request.getOrderNo()));
        if (!result) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包余额不足");
        }

        markOrderPaid(request.getOrderNo(), payAmount, PayTypeEnum.WALLET.getType());

        return OrderPayVo.builder()
                .orderNo(request.getOrderNo())
                .payAmount(payAmount)
                .orderStatus(ORDER_STATUS_WAIT_SHIPMENT)
                .paymentMethod(PayTypeEnum.WALLET.getType())
                .paymentStatus("SUCCESS")
                .paymentData(null)
                .build();
    }

    /**
     * 标记订单为已支付（通用方法）
     * <p>
     * 更新订单状态为待发货，设置支付方式、支付金额、支付时间等信息，并添加时间线记录
     * </p>
     *
     * @param orderNo   订单号
     * @param payAmount 支付金额
     * @param payType   支付方式
     * @return 是否更新成功
     */
    private boolean markOrderPaid(String orderNo, BigDecimal payAmount, String payType) {
        final int PAID = 1;

        // 查询订单信息
        MallOrder order = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .one();
        if (order == null) {
            log.warn("订单不存在，订单号：{}", orderNo);
            return false;
        }

        // 如果订单状态不是待支付，说明已经处理过了
        if (!Objects.equals(order.getOrderStatus(), ORDER_STATUS_WAIT_PAY)) {
            log.info("订单状态不是待支付，跳过处理，订单号：{}，当前状态：{}", orderNo, order.getOrderStatus());
            return true;
        }

        // 计算最终支付金额
        Date now = new Date();
        BigDecimal finalPayAmount = payAmount != null ? payAmount : order.getTotalAmount();
        log.info("订单支付成功，订单号：{}，支付方式：{}，支付金额：{}", orderNo, payType, finalPayAmount);

        // 更新订单状态
        boolean updated = lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .set(MallOrder::getOrderStatus, ORDER_STATUS_WAIT_SHIPMENT)
                .set(MallOrder::getPayType, payType)
                .set(MallOrder::getPayAmount, finalPayAmount)
                .set(MallOrder::getPayTime, now)
                .set(MallOrder::getUpdateTime, now)
                .set(MallOrder::getPaid, PAID)
                .update();

        // 添加订单支付时间线记录
        if (updated) {
            userCouponService.consumeCouponsForOrder(order);
            // todo 这边需要从订单信息拿到用户的信息
            OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                    .orderId(order.getId())
                    .eventType(OrderEventTypeEnum.ORDER_PAID.getType())
                    .eventStatus(ORDER_STATUS_WAIT_SHIPMENT)
                    .operatorType(OperatorTypeEnum.USER.getType())
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
        }

        return updated;
    }

    @Override
    public void closeOrderIfUnpaid(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            log.warn("收到空订单号的超时关单请求");
            return;
        }
        String normalizedOrderNo = orderNo.trim();
        String lockName = buildOrderLockName(normalizedOrderNo);
        distributedLockExecutor.tryExecuteOrElse(lockName,
                0L,
                -1L,
                () -> {
                    transactionTemplate.execute(transactionStatus -> {
                        doCloseOrderIfUnpaid(normalizedOrderNo);
                        return null;
                    });
                    return null;
                },
                () -> {
                    orderTimeoutMessagePublisher.publishOrderTimeout(normalizedOrderNo, MIN_REQUEUE_DELAY_MILLIS, TimeUnit.MILLISECONDS);
                    log.info("订单 {} 关单时分布式锁竞争失败，已重新投递短延迟消息", normalizedOrderNo);
                    return null;
                });
    }

    /**
     * 在事务内执行未支付订单关闭逻辑。
     *
     * @param orderNo 订单号
     */
    private void doCloseOrderIfUnpaid(String orderNo) {
        log.info("订单 {} 未支付，准备关闭", orderNo);

        // 先查询订单当前版本和状态
        MallOrder order = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getOrderStatus, ORDER_STATUS_WAIT_PAY)
                .select(MallOrder::getId, MallOrder::getOrderNo, MallOrder::getUserId, MallOrder::getCouponId,
                        MallOrder::getCouponDeductAmount, MallOrder::getCouponWasteAmount,
                        MallOrder::getVersion, MallOrder::getPayExpireTime)
                .one();

        if (order == null) {
            log.info("订单 {} 未执行关闭，当前状态可能已变更", orderNo);
            return;
        }

        Date now = new Date();
        Date payExpireTime = order.getPayExpireTime();
        if (payExpireTime != null && payExpireTime.after(now)) {
            long remainingDelayMillis = Math.max(payExpireTime.getTime() - now.getTime(), MIN_REQUEUE_DELAY_MILLIS);
            orderTimeoutMessagePublisher.publishOrderTimeout(orderNo, remainingDelayMillis, TimeUnit.MILLISECONDS);
            log.info("订单 {} 尚未达到最新支付过期时间，已重新投递延迟关闭，剩余 {} ms", orderNo, remainingDelayMillis);
            return;
        }

        // 查询订单项，用于恢复库存
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, order.getId())
                .list();

        // 使用版本号进行乐观锁更新
        boolean updated = lambdaUpdate()
                .eq(MallOrder::getId, order.getId())
                .eq(MallOrder::getVersion, order.getVersion())
                .set(MallOrder::getOrderStatus, OrderStatusEnum.EXPIRED.getType())
                .set(MallOrder::getCloseReason, ORDER_TIMEOUT_CLOSE_REASON)
                .set(MallOrder::getCloseTime, new Date())
                .set(MallOrder::getUpdateBy, SYSTEM_AUTO_CLOSE_OPERATOR)
                .set(MallOrder::getUpdateTime, new Date())
                .update();

        if (updated) {
            log.info("订单 {} 已自动关闭", orderNo);
            userCouponService.releaseCouponsForOrder(order, SYSTEM_AUTO_CLOSE_OPERATOR, ORDER_TIMEOUT_CLOSE_REASON);
            // 恢复库存
            if (orderItems != null) {
                for (MallOrderItem orderItem : orderItems) {
                    if (orderItem != null && orderItem.getProductId() != null && orderItem.getQuantity() != null) {
                        mallProductService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                    }
                }
            }

            // 添加订单过期时间线记录
            OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                    .orderId(order.getId())
                    .eventType(OrderEventTypeEnum.ORDER_EXPIRED.getType())
                    .eventStatus(OrderStatusEnum.EXPIRED.getType())
                    .operatorType(OperatorTypeEnum.SYSTEM.getType())
                    .description(ORDER_TIMEOUT_CLOSE_REASON)
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(timelineDto);
        } else {
            log.info("订单 {} 未执行关闭，当前状态可能已变更", orderNo);
        }
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

    /**
     * 组装商品摘要，方便前端展示订单信息。
     */
    private String buildProductSummary(MallProduct product, int quantity) {
        String unit = product.getUnit();
        if (unit != null && !unit.isBlank()) {
            return product.getName() + " " + quantity + unit;
        }
        return product.getName() + " x" + quantity;
    }

    /**
     * 根据订单项构建商品摘要。
     */
    private String buildProductSummary(List<MallOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "订单商品";
        }
        MallOrderItem first = items.getFirst();
        String name = StringUtils.hasText(first.getProductName()) ? first.getProductName() : "商品";
        Integer quantity = first.getQuantity();
        String base = quantity != null && quantity > 0 ? name + " x" + quantity : name;
        if (items.size() > 1) {
            base = base + " 等" + items.size() + "件";
        }
        return base;
    }


    /**
     * 生成业务唯一的订单编号。
     */
    private String generateOrderNo() {
        String prefix = "O";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", (int) (Math.random() * 1000000));
        return prefix + datePart + randomPart;
    }

    /**
     * 用户取消订单
     * <p>
     * 用户主动取消订单，需要提供取消原因。
     * 只有待支付状态的订单可以取消，取消后会恢复库存。
     * </p>
     *
     * @param request 订单取消请求参数
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "#request.orderNo")
    public boolean cancelOrder(OrderCancelRequest request) {
        // 1. 查询订单并校验所属用户
        MallOrder mallOrder = lambdaQuery()
                .eq(MallOrder::getOrderNo, request.getOrderNo())
                .one();

        if (mallOrder == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        // 2. 校验订单所属用户
        Long orderUserId = mallOrder.getUserId();
        Long userId = getUserId();
        if (!Objects.equals(orderUserId, userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单信息不存在!");
        }

        // 3. 校验订单状态（只有待支付状态可以取消）
        if (!Objects.equals(mallOrder.getOrderStatus(), ORDER_STATUS_WAIT_PAY)) {
            OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
            String statusName = orderStatusEnum != null ? orderStatusEnum.getName() : "未知";
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许取消", statusName));
        }

        // 4. 更新订单状态为已取消
        Date now = new Date();
        mallOrder.setOrderStatus(OrderStatusEnum.CANCELLED.getType());
        mallOrder.setPayType(PayTypeEnum.CANCELLED.getType());
        mallOrder.setCloseReason(request.getCancelReason());
        mallOrder.setCloseTime(now);
        mallOrder.setUpdateTime(now);

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "取消订单失败，请重试");
        }

        // 5. 释放优惠券并恢复库存。
        userCouponService.releaseCouponsForOrder(mallOrder, getUsername(), request.getCancelReason());
        Long orderId = mallOrder.getId();
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, orderId)
                .list();

        if (orderItems != null && !orderItems.isEmpty()) {
            for (MallOrderItem orderItem : orderItems) {
                if (orderItem != null && orderItem.getProductId() != null && orderItem.getQuantity() != null) {
                    mallProductService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                }
            }
        }

        // 6. 添加订单时间线记录
        String username = getUsername();
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(orderId)
                .eventType(OrderEventTypeEnum.ORDER_CANCELLED.getType())
                .eventStatus(OrderStatusEnum.CANCELLED.getType())
                .operatorType(OperatorTypeEnum.USER.getType())
                .description(String.format("用户%s取消了订单，原因：%s", username, request.getCancelReason()))
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        log.info("用户{}取消订单成功，订单号：{}，原因：{}", username, mallOrder.getOrderNo(), request.getCancelReason());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "#request.orderNo")
    public boolean confirmReceipt(OrderReceiveRequest request) {
        // 1. 查询订单并校验所属用户
        MallOrder mallOrder = lambdaQuery()
                .eq(MallOrder::getOrderNo, request.getOrderNo())
                .one();

        if (mallOrder == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        // 2. 校验订单所属用户
        Long orderUserId = mallOrder.getUserId();
        Long userId = getUserId();
        if (!Objects.equals(orderUserId, userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单信息不存在!");
        }

        // 3. 校验订单状态是否允许确认收货
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单状态异常");
        }

        // 只有待收货状态可以确认收货
        if (orderStatusEnum != OrderStatusEnum.PENDING_RECEIPT) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前订单状态[%s]不允许确认收货", orderStatusEnum.getName()));
        }

        // 4. 更新订单状态为已完成
        Date now = new Date();
        mallOrder.setOrderStatus(OrderStatusEnum.COMPLETED.getType());
        mallOrder.setReceiveTime(now);
        mallOrder.setFinishTime(now);
        mallOrder.setUpdateTime(now);

        boolean updated = updateById(mallOrder);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "确认收货失败，请重试");
        }

        // 5. 更新物流状态为已签收
        Long orderId = mallOrder.getId();
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(orderId);
        if (shipping != null) {
            shipping.setStatus(ShippingStatusEnum.DELIVERED.getType());
            shipping.setReceiveTime(now);
            shipping.setUpdateTime(now);
            mallOrderShippingService.updateById(shipping);
        }

        // 6. 添加订单时间线记录（标记为用户操作）
        String username = getUsername();
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(orderId)
                .eventType(OrderEventTypeEnum.ORDER_RECEIVED.getType())
                .eventStatus(OrderStatusEnum.COMPLETED.getType())
                .operatorType(OperatorTypeEnum.USER.getType())
                .description(String.format("用户%s确认收货", username))
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(timelineDto);

        // 订单完成后按销量增量阈值刷新商品索引
        runAfterCommit(() -> {
            try {
                syncSalesIndexIfNeeded(orderId);
            } catch (Exception ex) {
                log.warn("订单{}销量索引同步失败: {}", orderId, ex.getMessage(), ex);
            }
        });

        log.info("用户{}确认收货成功，订单号：{}", username, mallOrder.getOrderNo());
        return true;
    }

    @Override
    public OrderShippingVo getOrderShipping(String orderNo) {
        return getOrderShipping(orderNo, getUserId());
    }

    /**
     * 按订单号和指定用户ID查询订单物流。
     *
     * @param orderNo 订单编号
     * @param userId  指定用户ID
     * @return 订单物流
     */
    @Override
    public OrderShippingVo getOrderShipping(String orderNo, Long userId) {
        MallOrder mallOrder = getOwnedOrderByOrderNo(orderNo, userId);
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(mallOrder.getId());
        return buildOrderShippingVo(mallOrder, shipping);
    }

    /**
     * 按订单号和指定用户ID查询订单时间线。
     *
     * @param orderNo 订单编号
     * @param userId  指定用户ID
     * @return 订单时间线
     */
    @Override
    public ClientAgentOrderTimelineDto getOrderTimeline(String orderNo, Long userId) {
        MallOrder mallOrder = getOwnedOrderByOrderNo(orderNo, userId);
        List<MallOrderTimeline> timelineList = mallOrderTimelineService.getTimelineByOrderId(mallOrder.getId());
        List<ClientAgentOrderTimelineDto.TimelineNode> timeline = timelineList == null ? List.of() : timelineList.stream()
                .map(this::toOrderTimelineNode)
                .toList();

        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        return ClientAgentOrderTimelineDto.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .orderStatusName(orderStatusEnum != null ? orderStatusEnum.getName() : "未知")
                .timeline(timeline)
                .build();
    }

    /**
     * 校验订单是否允许取消。
     *
     * @param orderNo 订单编号
     * @param userId  指定用户ID
     * @return 取消资格
     */
    @Override
    public ClientAgentOrderCancelCheckDto checkOrderCancelable(String orderNo, Long userId) {
        MallOrder mallOrder = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId)
                .one();
        if (mallOrder == null) {
            return buildOrderCancelCheck(orderNo, null, null, false,
                    CANCEL_CHECK_REASON_ORDER_NOT_FOUND, ORDER_NOT_FOUND_OR_NO_PERMISSION_MESSAGE);
        }

        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        if (orderStatusEnum == null) {
            return buildOrderCancelCheck(mallOrder.getOrderNo(), mallOrder.getOrderStatus(), "未知", false,
                    CANCEL_CHECK_REASON_STATUS_INVALID, "当前订单状态异常，暂不支持取消");
        }
        if (orderStatusEnum == OrderStatusEnum.PENDING_PAYMENT) {
            return buildOrderCancelCheck(mallOrder.getOrderNo(), mallOrder.getOrderStatus(), orderStatusEnum.getName(), true,
                    CANCEL_CHECK_REASON_CAN_CANCEL, "当前订单允许取消");
        }
        return buildOrderCancelCheck(mallOrder.getOrderNo(), mallOrder.getOrderStatus(), orderStatusEnum.getName(), false,
                CANCEL_CHECK_REASON_STATUS_NOT_CANCELABLE,
                String.format("当前订单状态[%s]不允许取消", orderStatusEnum.getName()));
    }


    @Override
    public Page<OrderListVo> getOrderList(OrderListRequest request) {
        Long userId = getUserId();
        Page<MallOrderDto> page = request.toPage();

        // 查询订单列表DTO
        Page<MallOrderDto> orderDtoPage = baseMapper.selectOrderList(page, request, userId);

        // 将DTO转换为VO
        List<OrderListVo> orderVoList = BeanCotyUtils.copyListProperties(orderDtoPage.getRecords(), OrderListVo.class);


        // 查询每个订单的商品项
        for (OrderListVo orderVo : orderVoList) {
            // 获取对应的DTO
            MallOrderDto orderDto = orderDtoPage.getRecords().stream()
                    .filter(dto -> dto.getId().equals(orderVo.getId()))
                    .findFirst()
                    .orElse(null);

            // 获取订单状态名称
            OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(orderVo.getOrderStatus());
            orderVo.setOrderStatusName(orderStatusEnum != null ? orderStatusEnum.getName() : "未知");

            // 设置收货人信息
            if (orderDto != null && (orderDto.getReceiverName() != null || orderDto.getReceiverPhone() != null || orderDto.getReceiverDetail() != null)) {
                OrderListVo.ReceiverInfo receiverInfo = OrderListVo.ReceiverInfo.builder()
                        .name(orderDto.getReceiverName())
                        .phone(orderDto.getReceiverPhone())
                        .address(orderDto.getReceiverDetail())
                        .build();
                orderVo.setReceiverInfo(receiverInfo);
            }

            // 查询订单项
            List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                    .eq(MallOrderItem::getOrderId, orderVo.getId())
                    .list();

            // 一个订单可能有多个商品项,需要遍历处理 需要转换成VO
            List<OrderListVo.OrderItemSimpleVo> itemVos = new ArrayList<>();
            for (MallOrderItem item : orderItems) {
                OrderItemAfterSaleStatusEnum afterSaleStatusEnum =
                        OrderItemAfterSaleStatusEnum.fromCode(item.getAfterSaleStatus());

                OrderListVo.OrderItemSimpleVo itemVo = OrderListVo.OrderItemSimpleVo.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .imageUrl(item.getImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .totalPrice(item.getTotalPrice())
                        .afterSaleStatus(item.getAfterSaleStatus())
                        .afterSaleStatusName(afterSaleStatusEnum != null ? afterSaleStatusEnum.getName() : "未知")
                        .build();
                itemVos.add(itemVo);
            }
            orderVo.setItems(itemVos);
        }

        // 构建返回的Page对象
        Page<OrderListVo> resultPage = new Page<>(orderDtoPage.getCurrent(), orderDtoPage.getSize(), orderDtoPage.getTotal());
        resultPage.setRecords(orderVoList);

        return resultPage;
    }

    @Override
    public OrderDetailVo getOrderDetail(String orderNo) {
        return getOrderDetail(orderNo, getUserId());
    }

    /**
     * 按订单号和指定用户ID查询订单详情，供 Dubbo 场景显式传入用户范围。
     *
     * @param orderNo 订单编号
     * @param userId  指定用户ID
     * @return 订单详情
     */
    @Override
    public OrderDetailVo getOrderDetail(String orderNo, Long userId) {
        // 1. 查询订单详情
        OrderDetailDto orderDetailDto = baseMapper.getOrderDetailByOrderNo(orderNo, userId);
        if (orderDetailDto == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        // 2. 设置订单状态名称
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(orderDetailDto.getOrderStatus());

        // 3. 设置支付方式名称
        PayTypeEnum payTypeEnum = PayTypeEnum.fromCode(orderDetailDto.getPayType());

        // 4. 设置配送方式名称
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(orderDetailDto.getDeliveryType());

        // 5. 从 SQL 查询结果中获取收货人信息
        OrderDetailVo.ReceiverInfo receiverInfo = OrderDetailVo.ReceiverInfo.builder()
                .receiverName(orderDetailDto.getReceiverName())
                .receiverPhone(orderDetailDto.getReceiverPhone())
                .receiverDetail(orderDetailDto.getReceiverDetail())
                .build();

        // 6. 查询订单项
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, orderDetailDto.getId())
                .list();

        List<OrderDetailVo.OrderItemDetailVo> itemDetailVos = new ArrayList<>();
        for (MallOrderItem item : orderItems) {
            OrderItemAfterSaleStatusEnum afterSaleStatusEnum =
                    OrderItemAfterSaleStatusEnum.fromCode(item.getAfterSaleStatus());

            OrderDetailVo.OrderItemDetailVo itemDetailVo = OrderDetailVo.OrderItemDetailVo.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .imageUrl(item.getImageUrl())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .totalPrice(item.getTotalPrice())
                    .couponDeductAmount(item.getCouponDeductAmount())
                    .payableAmount(item.getPayableAmount())
                    .afterSaleStatus(item.getAfterSaleStatus())
                    .afterSaleStatusName(afterSaleStatusEnum != null ? afterSaleStatusEnum.getName() : "未知")
                    .refundedAmount(item.getRefundedAmount())
                    .build();
            itemDetailVos.add(itemDetailVo);
        }

        // 7. 查询物流信息
        MallOrderShipping shipping = mallOrderShippingService.getByOrderId(orderDetailDto.getId());
        OrderDetailVo.ShippingInfo shippingInfo = null;
        if (shipping != null) {
            ShippingStatusEnum shippingStatusEnum = ShippingStatusEnum.fromCode(shipping.getStatus());
            shippingInfo = OrderDetailVo.ShippingInfo.builder()
                    .logisticsCompany(shipping.getShippingCompany())
                    .trackingNumber(shipping.getShippingNo())
                    .shippingStatus(shipping.getStatus())
                    .shippingStatusName(shippingStatusEnum != null ? shippingStatusEnum.getName() : "未知")
                    .shipTime(shipping.getDeliverTime())
                    .build();
        }

        // 8. 构建并返回 OrderDetailVo
        return OrderDetailVo.builder()
                .id(orderDetailDto.getId())
                .orderNo(orderDetailDto.getOrderNo())
                .orderStatus(orderDetailDto.getOrderStatus())
                .orderStatusName(orderStatusEnum != null ? orderStatusEnum.getName() : "未知")
                .itemsAmount(orderDetailDto.getItemsAmount())
                .totalAmount(orderDetailDto.getTotalAmount())
                .payAmount(orderDetailDto.getPayAmount())
                .freightAmount(orderDetailDto.getFreightAmount())
                .couponId(orderDetailDto.getCouponId())
                .couponName(orderDetailDto.getCouponName())
                .couponDeductAmount(orderDetailDto.getCouponDeductAmount())
                .couponConsumeAmount(orderDetailDto.getCouponConsumeAmount())
                .payType(orderDetailDto.getPayType())
                .payTypeName(payTypeEnum != null ? payTypeEnum.getType() : "未知")
                .deliveryType(orderDetailDto.getDeliveryType())
                .deliveryTypeName(deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知")
                .paid(orderDetailDto.getPaid())
                .payExpireTime(orderDetailDto.getPayExpireTime())
                .payTime(orderDetailDto.getPayTime())
                .deliverTime(orderDetailDto.getDeliverTime())
                .receiveTime(orderDetailDto.getReceiveTime())
                .finishTime(orderDetailDto.getFinishTime())
                .createTime(orderDetailDto.getCreateTime())
                .note(orderDetailDto.getNote())
                .afterSaleFlag(OrderItemAfterSaleStatusEnum.fromCode(orderDetailDto.getAfterSaleFlag()))
                .refundStatus(orderDetailDto.getRefundStatus())
                .refundPrice(orderDetailDto.getRefundPrice())
                .refundTime(orderDetailDto.getRefundTime())
                .receiverInfo(receiverInfo)
                .items(itemDetailVos)
                .shippingInfo(shippingInfo)
                .build();
    }

    /**
     * 按订单编号和指定用户ID查询订单卡摘要。
     *
     * @param orderNo 订单编号
     * @param userId  指定用户ID
     * @return 订单卡摘要
     */
    @Override
    public ClientAgentOrderCardSummaryDto getOrderCardSummary(String orderNo, Long userId) {
        MallOrder mallOrder = getOwnedOrderByOrderNo(orderNo, userId);
        List<MallOrderItem> sortedOrderItems = mallOrderItemService.getOrderItemByOrderId(mallOrder.getId()).stream()
                .sorted(Comparator.comparing(MallOrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (sortedOrderItems.isEmpty()) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单商品不存在");
        }

        MallOrderItem previewItem = sortedOrderItems.getFirst();
        int productCount = sortedOrderItems.stream()
                .map(MallOrderItem::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());

        return ClientAgentOrderCardSummaryDto.builder()
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .orderStatusText(orderStatusEnum == null ? null : orderStatusEnum.getName())
                .previewProduct(ClientAgentOrderCardSummaryDto.PreviewProduct.builder()
                        .productId(previewItem.getProductId())
                        .productName(previewItem.getProductName())
                        .imageUrl(previewItem.getImageUrl())
                        .build())
                .productCount(productCount)
                .payAmount(mallOrder.getPayAmount())
                .totalAmount(mallOrder.getTotalAmount())
                .createTime(mallOrder.getCreateTime())
                .build();
    }

    /**
     * 查询当前用户范围内的订单。
     *
     * @param orderNo 订单编号
     * @param userId  用户ID
     * @return 订单实体
     */
    private MallOrder getOwnedOrderByOrderNo(String orderNo, Long userId) {
        MallOrder mallOrder = lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId)
                .one();
        if (mallOrder == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }
        return mallOrder;
    }

    /**
     * 构建订单物流返回对象。
     *
     * @param mallOrder 订单实体
     * @param shipping  物流实体
     * @return 订单物流
     */
    private OrderShippingVo buildOrderShippingVo(MallOrder mallOrder, MallOrderShipping shipping) {
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(mallOrder.getOrderStatus());
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(mallOrder.getDeliveryType());
        OrderShippingVo.ReceiverInfo receiverInfo = OrderShippingVo.ReceiverInfo.builder()
                .receiverName(mallOrder.getReceiverName())
                .receiverPhone(mallOrder.getReceiverPhone())
                .receiverDetail(mallOrder.getReceiverDetail())
                .deliveryType(mallOrder.getDeliveryType())
                .deliveryTypeName(deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知")
                .build();

        ShippingStatusEnum shippingStatusEnum = shipping == null
                ? ShippingStatusEnum.NOT_SHIPPED
                : ShippingStatusEnum.fromCode(shipping.getStatus());

        OrderShippingVo.OrderShippingVoBuilder builder = OrderShippingVo.builder()
                .orderId(mallOrder.getId())
                .orderNo(mallOrder.getOrderNo())
                .orderStatus(mallOrder.getOrderStatus())
                .orderStatusName(orderStatusEnum != null ? orderStatusEnum.getName() : "未知")
                .status(shipping == null ? ShippingStatusEnum.NOT_SHIPPED.getType() : shipping.getStatus())
                .statusName(shippingStatusEnum != null ? shippingStatusEnum.getName() : "未知")
                .receiverInfo(receiverInfo)
                .nodes(shipping == null ? List.of() : parseShippingNodes(shipping.getShippingInfo()));

        if (shipping != null) {
            builder.logisticsCompany(shipping.getShippingCompany())
                    .trackingNumber(shipping.getShippingNo())
                    .shipmentNote(shipping.getShipmentNote())
                    .deliverTime(shipping.getDeliverTime())
                    .receiveTime(shipping.getReceiveTime());
        }
        return builder.build();
    }

    /**
     * 构建订单时间线节点。
     *
     * @param source 时间线实体
     * @return 时间线节点
     */
    private ClientAgentOrderTimelineDto.TimelineNode toOrderTimelineNode(MallOrderTimeline source) {
        if (source == null) {
            return null;
        }
        OrderEventTypeEnum eventTypeEnum = OrderEventTypeEnum.fromCode(source.getEventType());
        OrderStatusEnum eventStatusEnum = OrderStatusEnum.fromCode(source.getEventStatus());
        OperatorTypeEnum operatorTypeEnum = OperatorTypeEnum.fromCode(source.getOperatorType());
        return ClientAgentOrderTimelineDto.TimelineNode.builder()
                .id(source.getId())
                .eventType(source.getEventType())
                .eventTypeName(eventTypeEnum != null ? eventTypeEnum.getName() : "未知")
                .eventStatus(source.getEventStatus())
                .eventStatusName(eventStatusEnum != null ? eventStatusEnum.getName() : "未知")
                .operatorType(source.getOperatorType())
                .operatorTypeName(operatorTypeEnum != null ? operatorTypeEnum.getName() : "未知")
                .description(source.getDescription())
                .createdTime(source.getCreatedTime())
                .build();
    }

    /**
     * 构建订单取消校验结果。
     *
     * @param orderNo         订单编号
     * @param orderStatus     订单状态编码
     * @param orderStatusName 订单状态名称
     * @param cancelable      是否可取消
     * @param reasonCode      结果编码
     * @param reasonMessage   结果说明
     * @return 取消校验结果
     */
    private ClientAgentOrderCancelCheckDto buildOrderCancelCheck(String orderNo,
                                                                 String orderStatus,
                                                                 String orderStatusName,
                                                                 boolean cancelable,
                                                                 String reasonCode,
                                                                 String reasonMessage) {
        return ClientAgentOrderCancelCheckDto.builder()
                .orderNo(orderNo)
                .orderStatus(orderStatus)
                .orderStatusName(orderStatusName)
                .cancelable(cancelable)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .build();
    }

    /**
     * 解析物流轨迹 JSON。
     *
     * @param shippingInfo 物流轨迹 JSON
     * @return 物流轨迹节点列表
     */
    private List<OrderShippingVo.ShippingNode> parseShippingNodes(String shippingInfo) {
        if (!StringUtils.hasText(shippingInfo)) {
            return List.of();
        }
        try {
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
        } catch (Exception ex) {
            log.warn("解析订单物流轨迹失败，shippingInfo={}", shippingInfo, ex);
            return List.of();
        }
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
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_SUBMIT_USER_KEY, key = "@mallDistributedLockKeyResolver.currentUserId()")
    public OrderCheckoutVo createOrderFromCart(CartSettleRequest request) {
        Long userId = SecurityUtils.getUserId();

        // 1. 查询购物车商品。
        List<MallCart> cartItems = mallCartService.lambdaQuery()
                .eq(MallCart::getUserId, userId)
                .in(MallCart::getId, request.getCartIds())
                .list();
        if (cartItems.isEmpty()) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "购物车商品不存在");
        }

        // 2. 校验商品并构建订单草稿。
        BigDecimal itemsAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<CartOrderDraftItem> draftItems = new ArrayList<>();
        List<CouponSettlementItemDto> settlementItems = new ArrayList<>();
        List<MallOrderItem> orderItems = new ArrayList<>();
        for (MallCart cartItem : cartItems) {
            MallProductWithImageDto product = mallProductService.getProductWithImagesById(cartItem.getProductId());
            if (product == null) {
                throw new ServiceException(ResponseCode.RESULT_IS_NULL,
                        String.format("商品[%s]不存在", cartItem.getProductName()));
            }
            if (product.getStatus() != 1) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        String.format("商品[%s]已下架", product.getName()));
            }
            if (product.getStock() < cartItem.getCartNum()) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        String.format("商品[%s]库存不足，当前库存：%d", product.getName(), product.getStock()));
            }
            BigDecimal itemTotal = product.getPrice()
                    .multiply(new BigDecimal(cartItem.getCartNum()))
                    .setScale(2, RoundingMode.HALF_UP);
            itemsAmount = itemsAmount.add(itemTotal).setScale(2, RoundingMode.HALF_UP);
            String imageUrl = null;
            if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
                imageUrl = product.getProductImages().getFirst().getImageUrl();
            }
            draftItems.add(CartOrderDraftItem.builder()
                    .cartId(cartItem.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(imageUrl)
                    .price(product.getPrice())
                    .quantity(cartItem.getCartNum())
                    .totalPrice(itemTotal)
                    .couponEnabled(product.getCouponEnabled())
                    .build());
            settlementItems.add(buildSettlementItem(
                    "CART_" + cartItem.getId(),
                    product.getId(),
                    itemTotal,
                    product.getCouponEnabled()
            ));
        }

        // 3. 生成订单号并锁定优惠券。
        String orderNo = generateOrderNo();
        Date now = new Date();
        Date expireTime = buildOrderPayExpireTime(now);
        UserAddress userAddress = getUserAddressOrThrow(userId, request.getAddressId());
        String receiverDetail = buildReceiverDetail(userAddress);
        BigDecimal freightAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<Long> selectedCouponIds = resolveCheckoutCouponIds(
                userId,
                request.getCouponId(),
                request.getDisableCoupon(),
                settlementItems
        );
        OrderCouponSelectionSnapshotDto couponSelectionSnapshot = userCouponService.lockCoupons(
                userId,
                selectedCouponIds,
                settlementItems,
                orderNo
        );
        couponSelectionSnapshot.setAutoSelected(shouldAutoSelectCoupon(request.getCouponId(), request.getDisableCoupon()));
        BigDecimal totalAmount = calculateOrderTotalAmount(
                itemsAmount,
                normalizeOrderAmount(couponSelectionSnapshot.getCouponDeductAmount())
        );
        OrderCouponSnapshotDto firstSelectedCoupon = couponSelectionSnapshot.getSelectedCoupons() == null
                || couponSelectionSnapshot.getSelectedCoupons().isEmpty()
                ? null
                : couponSelectionSnapshot.getSelectedCoupons().getFirst();

        // 4. 扣减库存并组装订单项。
        for (CartOrderDraftItem draftItem : draftItems) {
            mallProductService.deductStock(draftItem.getProductId(), draftItem.getQuantity());
            BigDecimal itemCouponDeductAmount = findCouponDeductAmount(couponSelectionSnapshot.getAllocations(),
                    "CART_" + draftItem.getCartId());
            orderItems.add(MallOrderItem.builder()
                    .productId(draftItem.getProductId())
                    .productName(draftItem.getProductName())
                    .imageUrl(draftItem.getImageUrl())
                    .price(draftItem.getPrice())
                    .quantity(draftItem.getQuantity())
                    .totalPrice(draftItem.getTotalPrice())
                    .couponDeductAmount(itemCouponDeductAmount)
                    .payableAmount(draftItem.getTotalPrice().subtract(itemCouponDeductAmount).setScale(2, RoundingMode.HALF_UP))
                    .afterSaleStatus(OrderItemAfterSaleStatusEnum.NONE.getStatus())
                    .build());
        }

        // 5. 创建订单主表。
        MallOrder order = MallOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .itemsAmount(itemsAmount)
                .totalAmount(totalAmount)
                .payAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .freightAmount(freightAmount)
                .couponId(firstSelectedCoupon == null ? null : firstSelectedCoupon.getCouponId())
                .couponName(firstSelectedCoupon == null ? null : firstSelectedCoupon.getCouponName())
                .couponDeductAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponDeductAmount()))
                .couponConsumeAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponConsumeAmount()))
                .couponWasteAmount(normalizeOrderAmount(couponSelectionSnapshot.getCouponWasteAmount()))
                .couponSnapshotJson(buildCouponSnapshotJson(couponSelectionSnapshot))
                .orderStatus(ORDER_STATUS_WAIT_PAY)
                .payType(WAIT_PAY)
                .paid(0)
                .deliveryType(request.getDeliveryType().getType())
                .addressId(userAddress.getId())
                .receiverName(userAddress.getReceiverName())
                .receiverPhone(userAddress.getReceiverPhone())
                .receiverDetail(receiverDetail)
                .note(request.getRemark())
                .payExpireTime(expireTime)
                .afterSaleFlag(OrderItemAfterSaleStatusEnum.NONE)
                .createTime(now)
                .updateTime(now)
                .build();
        boolean orderSaved = save(order);
        if (!orderSaved) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单创建失败");
        }

        // 6. 保存订单项。
        for (MallOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setCreateTime(now);
            item.setUpdateTime(now);
        }
        boolean itemsSaved = mallOrderItemService.saveBatch(orderItems);
        if (!itemsSaved) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单项保存失败");
        }

        // 7. 添加订单时间线。
        OrderTimelineDto timelineDto = OrderTimelineDto.builder()
                .orderId(order.getId())
                .eventType(OrderEventTypeEnum.ORDER_CREATED.getType())
                .eventStatus(ORDER_STATUS_WAIT_PAY)
                .operatorType(OperatorTypeEnum.USER.getType())
                .description("用户创建订单")
                .build();
        mallOrderTimelineService.addTimeline(timelineDto);

        // 8. 删除已结算的购物车商品。
        mallCartService.removeCartItems(request.getCartIds());

        // 9. 构建商品摘要并处理支付状态。
        String productSummary = orderItems.stream()
                .map(MallOrderItem::getProductName)
                .collect(Collectors.joining("、"));
        String finalOrderStatus = ORDER_STATUS_WAIT_PAY;
        Date finalPayExpireTime = expireTime;
        if (isZeroAmount(totalAmount)) {
            boolean paid = markOrderPaid(orderNo, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), PayTypeEnum.COUPON.getType());
            if (!paid) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "零元订单自动完成失败，请重试");
            }
            finalOrderStatus = ORDER_STATUS_WAIT_SHIPMENT;
            finalPayExpireTime = null;
        } else {
            publishOrderTimeoutAfterCommit(orderNo);
        }

        log.info("用户{}从购物车创建订单成功，订单号：{}，共{}件商品",
                userId, orderNo, orderItems.size());

        return OrderCheckoutVo.builder()
                .orderNo(orderNo)
                .itemsAmount(itemsAmount)
                .totalAmount(totalAmount)
                .couponId(order.getCouponId())
                .couponName(order.getCouponName())
                .couponDeductAmount(order.getCouponDeductAmount())
                .couponConsumeAmount(order.getCouponConsumeAmount())
                .productSummary(productSummary)
                .itemCount(orderItems.size())
                .orderStatus(finalOrderStatus)
                .createTime(now)
                .payExpireTime(finalPayExpireTime)
                .build();
    }

    @Override
    public OrderPreviewVo previewOrder(OrderPreviewRequest request) {
        Long userId = getUserId();
        UserAddress userAddress = getUserAddressOrThrow(userId, request.getAddressId());
        // 根据预览类型处理
        if (request.getType() == OrderPreviewRequest.PreviewType.PRODUCT) {
            // 单个商品购买预览
            return previewSingleProduct(request, userAddress);
        } else if (request.getType() == OrderPreviewRequest.PreviewType.CART) {
            // 购物车结算预览
            return previewCartItems(request, userId, userAddress);
        } else {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "不支持的预览类型");
        }
    }

    /**
     * 预览单个商品购买
     */
    private OrderPreviewVo previewSingleProduct(OrderPreviewRequest request, UserAddress userAddress) {
        if (request.getProductId() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品ID不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "购买数量必须大于0");
        }

        // 查询商品详情
        MallProductWithImageDto product = mallProductService.getProductWithImagesById(request.getProductId());
        if (product == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }

        // 校验商品状态
        final Integer PRODUCT_STATUS_ON_SALE = 1;
        if (!Objects.equals(product.getStatus(), PRODUCT_STATUS_ON_SALE)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品未上架或已下架");
        }

        // 构建商品项预览
        String imageUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrl = product.getProductImages().getFirst().getImageUrl();
        }

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        OrderPreviewVo.OrderItemPreview itemPreview = OrderPreviewVo.OrderItemPreview.builder()
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(imageUrl)
                .price(product.getPrice())
                .quantity(request.getQuantity())
                .subtotal(subtotal)
                .stock(product.getStock())
                .status(product.getStatus())
                .statusDesc(product.getStatus() == 1 ? "在售" : "已下架")
                .build();

        // 计算价格与优惠券信息。
        List<CouponSettlementItemDto> settlementItems = List.of(buildSettlementItem(
                "PREVIEW_PRODUCT_" + product.getId(),
                product.getId(),
                subtotal,
                product.getCouponEnabled()
        ));
        List<OrderCouponOptionVo> couponCandidates = hasCouponEligibleItem(settlementItems)
                ? userCouponService.listMatchedCoupons(getUserId(), settlementItems)
                : List.of();
        PreviewCouponSelectionContext couponSelection = resolvePreviewCouponSelection(
                getUserId(),
                request.getCouponId(),
                request.getDisableCoupon(),
                settlementItems,
                couponCandidates
        );
        OrderCouponOptionVo selectedCoupon = couponSelection.selectedCoupon();
        BigDecimal couponDeductAmount = couponSelection.couponDeductAmount();
        BigDecimal couponConsumeAmount = couponSelection.couponConsumeAmount();
        BigDecimal couponWasteAmount = couponSelection.couponWasteAmount();
        BigDecimal totalAmount = calculateOrderTotalAmount(subtotal, couponDeductAmount);

        // 获取配送方式信息
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromLegacyCode(product.getDeliveryType());
        String deliveryType = deliveryTypeEnum != null ? deliveryTypeEnum.getType() : "UNKNOWN";
        String deliveryTypeName = deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知";

        return OrderPreviewVo.builder()
                .items(List.of(itemPreview))
                .itemsAmount(subtotal)
                .discountAmount(couponDeductAmount)
                .couponId(selectedCoupon == null ? null : selectedCoupon.getCouponId())
                .couponDeductAmount(couponDeductAmount)
                .couponConsumeAmount(couponConsumeAmount)
                .couponWasteAmount(couponWasteAmount)
                .selectedCoupon(selectedCoupon)
                .selectedCoupons(couponSelection.selectedCoupons())
                .autoCouponSelected(couponSelection.autoSelected())
                .couponCandidates(couponCandidates)
                .totalAmount(totalAmount)
                .address(buildReceiverDetail(userAddress))
                .deliveryType(deliveryType)
                .deliveryTypeName(deliveryTypeName)
                .estimatedDeliveryTime(getEstimatedDeliveryTime(deliveryType))
                .build();
    }

    /**
     * 预览购物车商品
     */
    private OrderPreviewVo previewCartItems(OrderPreviewRequest request, Long userId, UserAddress userAddress) {
        if (request.getCartIds() == null || request.getCartIds().isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "购物车商品ID列表不能为空");
        }

        // 查询购物车商品
        List<MallCart> cartItems = mallCartService.lambdaQuery()
                .eq(MallCart::getUserId, userId)
                .in(MallCart::getId, request.getCartIds())
                .list();

        if (cartItems.isEmpty()) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "购物车商品不存在");
        }

        // 构建商品项预览列表
        List<OrderPreviewVo.OrderItemPreview> itemPreviews = new ArrayList<>();
        List<CouponSettlementItemDto> settlementItems = new ArrayList<>();
        BigDecimal itemsAmount = BigDecimal.ZERO;
        String orderDeliveryType = null;

        for (MallCart cartItem : cartItems) {
            // 查询商品详情
            MallProductWithImageDto product = mallProductService.getProductWithImagesById(cartItem.getProductId());
            if (product == null) {
                throw new ServiceException(ResponseCode.RESULT_IS_NULL,
                        String.format("商品[%s]不存在", cartItem.getProductName()));
            }

            // 校验商品状态
            if (product.getStatus() != 1) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        String.format("商品[%s]已下架", product.getName()));
            }

            // 校验并统一配送方式
            orderDeliveryType = getString(orderDeliveryType, product);

            // 构建商品项预览
            String imageUrl = null;
            if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
                imageUrl = product.getProductImages().getFirst().getImageUrl();
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getCartNum()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderPreviewVo.OrderItemPreview itemPreview = OrderPreviewVo.OrderItemPreview.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(imageUrl)
                    .price(product.getPrice())
                    .quantity(cartItem.getCartNum())
                    .subtotal(subtotal)
                    .stock(product.getStock())
                    .status(product.getStatus())
                    .statusDesc(product.getStatus() == 1 ? "在售" : "已下架")
                    .build();

            itemPreviews.add(itemPreview);
            itemsAmount = itemsAmount.add(subtotal);
            settlementItems.add(buildSettlementItem(
                    "PREVIEW_CART_" + cartItem.getId(),
                    product.getId(),
                    subtotal,
                    product.getCouponEnabled()
            ));
        }

        // 计算价格与优惠券信息。
        List<OrderCouponOptionVo> couponCandidates = hasCouponEligibleItem(settlementItems)
                ? userCouponService.listMatchedCoupons(userId, settlementItems)
                : List.of();
        PreviewCouponSelectionContext couponSelection = resolvePreviewCouponSelection(
                userId,
                request.getCouponId(),
                request.getDisableCoupon(),
                settlementItems,
                couponCandidates
        );
        OrderCouponOptionVo selectedCoupon = couponSelection.selectedCoupon();
        BigDecimal couponDeductAmount = couponSelection.couponDeductAmount();
        BigDecimal couponConsumeAmount = couponSelection.couponConsumeAmount();
        BigDecimal couponWasteAmount = couponSelection.couponWasteAmount();
        BigDecimal totalAmount = calculateOrderTotalAmount(itemsAmount, couponDeductAmount);

        // 获取配送方式信息
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(orderDeliveryType);
        String deliveryTypeName = deliveryTypeEnum != null ? deliveryTypeEnum.getName() : "未知";

        return OrderPreviewVo.builder()
                .items(itemPreviews)
                .itemsAmount(itemsAmount)
                .discountAmount(couponDeductAmount)
                .couponId(selectedCoupon == null ? null : selectedCoupon.getCouponId())
                .couponDeductAmount(couponDeductAmount)
                .couponConsumeAmount(couponConsumeAmount)
                .couponWasteAmount(couponWasteAmount)
                .selectedCoupon(selectedCoupon)
                .selectedCoupons(couponSelection.selectedCoupons())
                .autoCouponSelected(couponSelection.autoSelected())
                .couponCandidates(couponCandidates)
                .totalAmount(totalAmount)
                .address(buildReceiverDetail(userAddress))
                .deliveryType(orderDeliveryType)
                .deliveryTypeName(deliveryTypeName)
                .estimatedDeliveryTime(getEstimatedDeliveryTime(orderDeliveryType))
                .build();
    }

    /**
     * 解析预览阶段的优惠券选择结果。
     *
     * @param userId           用户ID
     * @param couponId         手选优惠券ID
     * @param disableCoupon    显式禁用优惠券标记
     * @param settlementItems  结算商品项
     * @param couponCandidates 当前可选优惠券集合
     * @return 预览优惠券选择结果
     */
    private PreviewCouponSelectionContext resolvePreviewCouponSelection(Long userId,
                                                                        Long couponId,
                                                                        Boolean disableCoupon,
                                                                        List<CouponSettlementItemDto> settlementItems,
                                                                        List<OrderCouponOptionVo> couponCandidates) {
        if (isCouponDisabled(disableCoupon)) {
            return new PreviewCouponSelectionContext(
                    null,
                    List.of(),
                    normalizeOrderAmount(null),
                    normalizeOrderAmount(null),
                    normalizeOrderAmount(null),
                    Boolean.FALSE
            );
        }
        if (couponId != null) {
            OrderCouponOptionVo selectedCoupon = userCouponService.getSelectedCouponOption(userId, couponId, settlementItems);
            List<OrderCouponOptionVo> selectedCoupons = selectedCoupon == null ? List.of() : List.of(selectedCoupon);
            return new PreviewCouponSelectionContext(
                    selectedCoupon,
                    selectedCoupons,
                    selectedCoupon == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeOrderAmount(selectedCoupon.getCouponDeductAmount()),
                    selectedCoupon == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeOrderAmount(selectedCoupon.getCouponConsumeAmount()),
                    selectedCoupon == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalizeOrderAmount(selectedCoupon.getCouponWasteAmount()),
                    Boolean.FALSE
            );
        }
        CouponAutoSelectResultDto autoResult = userCouponService.autoSelectCoupons(userId, settlementItems);
        List<OrderCouponOptionVo> selectedCoupons = buildSelectedCouponOptions(couponCandidates, autoResult);
        OrderCouponOptionVo selectedCoupon = selectedCoupons.isEmpty() ? null : selectedCoupons.getFirst();
        return new PreviewCouponSelectionContext(
                selectedCoupon,
                selectedCoupons,
                normalizeOrderAmount(autoResult.getCouponDeductAmount()),
                normalizeOrderAmount(autoResult.getCouponConsumeAmount()),
                normalizeOrderAmount(autoResult.getCouponWasteAmount()),
                Boolean.TRUE
        );
    }

    /**
     * 判断当前请求是否明确禁用优惠券。
     *
     * @param disableCoupon 显式禁用优惠券标记
     * @return 是否禁用优惠券
     */
    private boolean isCouponDisabled(Boolean disableCoupon) {
        return Boolean.TRUE.equals(disableCoupon);
    }

    /**
     * 判断当前请求是否需要自动选券。
     *
     * @param couponId       手选优惠券ID
     * @param disableCoupon  显式禁用优惠券标记
     * @return 是否需要自动选券
     */
    private boolean shouldAutoSelectCoupon(Long couponId, Boolean disableCoupon) {
        return !isCouponDisabled(disableCoupon) && couponId == null;
    }

    /**
     * 解析提交订单时实际需要锁定的优惠券集合。
     *
     * @param userId          用户ID
     * @param couponId        手选优惠券ID
     * @param disableCoupon   显式禁用优惠券标记
     * @param settlementItems 结算商品项
     * @return 需要锁定的优惠券ID集合
     */
    private List<Long> resolveCheckoutCouponIds(Long userId,
                                                Long couponId,
                                                Boolean disableCoupon,
                                                List<CouponSettlementItemDto> settlementItems) {
        if (isCouponDisabled(disableCoupon)) {
            return List.of();
        }
        if (couponId != null) {
            return List.of(couponId);
        }
        return extractSelectedCouponIds(userCouponService.autoSelectCoupons(userId, settlementItems));
    }

    /**
     * 将自动选券结果映射为前端可直接展示的优惠券对象集合。
     *
     * @param couponCandidates 当前可选优惠券集合
     * @param autoResult       自动选券结果
     * @return 自动选中的优惠券展示集合
     */
    private List<OrderCouponOptionVo> buildSelectedCouponOptions(List<OrderCouponOptionVo> couponCandidates,
                                                                 CouponAutoSelectResultDto autoResult) {
        if (autoResult == null || autoResult.getAppliedCoupons() == null || autoResult.getAppliedCoupons().isEmpty()) {
            return List.of();
        }
        Map<Long, OrderCouponOptionVo> candidateMap = (couponCandidates == null ? List.<OrderCouponOptionVo>of() : couponCandidates).stream()
                .filter(option -> option.getCouponId() != null)
                .collect(Collectors.toMap(OrderCouponOptionVo::getCouponId, option -> option, (left, right) -> left));
        List<OrderCouponOptionVo> selectedCoupons = new ArrayList<>();
        for (CouponAppliedDetailDto appliedDetail : autoResult.getAppliedCoupons()) {
            if (appliedDetail == null || appliedDetail.getCouponId() == null) {
                continue;
            }
            OrderCouponOptionVo candidate = candidateMap.get(appliedDetail.getCouponId());
            if (candidate != null) {
                selectedCoupons.add(OrderCouponOptionVo.builder()
                        .couponId(candidate.getCouponId())
                        .couponName(candidate.getCouponName())
                        .thresholdAmount(candidate.getThresholdAmount())
                        .availableAmount(candidate.getAvailableAmount())
                        .continueUseEnabled(candidate.getContinueUseEnabled())
                        .couponStatus(candidate.getCouponStatus())
                        .effectiveTime(candidate.getEffectiveTime())
                        .expireTime(candidate.getExpireTime())
                        .matched(Boolean.TRUE)
                        .unusableReason(null)
                        .couponDeductAmount(normalizeOrderAmount(appliedDetail.getCouponDeductAmount()))
                        .couponConsumeAmount(normalizeOrderAmount(appliedDetail.getCouponConsumeAmount()))
                        .couponWasteAmount(normalizeOrderAmount(appliedDetail.getCouponWasteAmount()))
                        .build());
            } else {
                selectedCoupons.add(OrderCouponOptionVo.builder()
                        .couponId(appliedDetail.getCouponId())
                        .couponName(appliedDetail.getCouponName())
                        .matched(Boolean.TRUE)
                        .couponDeductAmount(normalizeOrderAmount(appliedDetail.getCouponDeductAmount()))
                        .couponConsumeAmount(normalizeOrderAmount(appliedDetail.getCouponConsumeAmount()))
                        .couponWasteAmount(normalizeOrderAmount(appliedDetail.getCouponWasteAmount()))
                        .build());
            }
        }
        return selectedCoupons;
    }

    /**
     * 从自动选券结果中提取选中的优惠券ID集合。
     *
     * @param autoResult 自动选券结果
     * @return 选中的优惠券ID集合
     */
    private List<Long> extractSelectedCouponIds(CouponAutoSelectResultDto autoResult) {
        if (autoResult == null || autoResult.getAppliedCoupons() == null || autoResult.getAppliedCoupons().isEmpty()) {
            return List.of();
        }
        return autoResult.getAppliedCoupons().stream()
                .map(CouponAppliedDetailDto::getCouponId)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 根据ID获取并校验用户收货地址
     *
     * @param userId    当前用户ID
     * @param addressId 地址ID
     * @return 收货地址
     */
    private UserAddress getUserAddressOrThrow(Long userId, Long addressId) {
        if (addressId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "请选择收货地址");
        }
        UserAddress address = userAddressService.getById(addressId);
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "收货地址不存在");
        }
        return address;
    }

    /**
     * 构建完整收货地址
     *
     * @param address 收货地址
     * @return 完整地址
     */
    private String buildReceiverDetail(UserAddress address) {
        if (address == null) {
            return null;
        }
        StringBuilder detailBuilder = new StringBuilder();
        if (StringUtils.hasText(address.getAddress())) {
            detailBuilder.append(address.getAddress());
        }
        if (StringUtils.hasText(address.getDetailAddress())) {
            if (!detailBuilder.isEmpty()) {
                detailBuilder.append(" ");
            }
            detailBuilder.append(address.getDetailAddress());
        }
        return detailBuilder.toString();
    }

    /**
     * 获取配送方式信息
     */
    private String getString(String orderDeliveryType, MallProductWithImageDto product) {
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromLegacyCode(product.getDeliveryType());
        if (deliveryTypeEnum == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("商品[%s]配送方式配置异常", product.getName()));
        }

        String productDeliveryType = deliveryTypeEnum.getType();
        if (orderDeliveryType == null) {
            orderDeliveryType = productDeliveryType;
        } else if (!orderDeliveryType.equals(productDeliveryType)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    "购物车中的商品配送方式不一致，请分开下单");
        }
        return orderDeliveryType;
    }

    /**
     * 获取预计送达时间
     */
    private String getEstimatedDeliveryTime(String deliveryType) {
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromCode(deliveryType);
        if (deliveryTypeEnum == null) {
            return "未知";
        }

        return switch (deliveryTypeEnum) {
            case EXPRESS -> "预计3-5天送达";
            case SELF_PICKUP -> "下单后可到店自提";
            default -> "未知";
        };
    }

    /**
     * 构建优惠券结算商品项。
     *
     * @param itemKey       商品项业务键
     * @param productId     商品ID
     * @param totalAmount   商品金额
     * @param couponEnabled 是否允许使用优惠券
     * @return 优惠券结算商品项
     */
    private CouponSettlementItemDto buildSettlementItem(String itemKey,
                                                        Long productId,
                                                        BigDecimal totalAmount,
                                                        Integer couponEnabled) {
        return CouponSettlementItemDto.builder()
                .itemKey(itemKey)
                .productId(productId)
                .totalAmount(normalizeOrderAmount(totalAmount))
                .couponEnabled(resolveCouponEnabledFlag(couponEnabled))
                .build();
    }

    /**
     * 查找指定商品项的优惠分摊金额。
     *
     * @param allocations 商品项优惠分摊集合
     * @param itemKey     商品项业务键
     * @return 指定商品项的优惠分摊金额
     */
    private BigDecimal findCouponDeductAmount(List<CouponSettlementAllocationDto> allocations, String itemKey) {
        if (allocations == null || allocations.isEmpty() || !StringUtils.hasText(itemKey)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        for (CouponSettlementAllocationDto allocation : allocations) {
            if (allocation != null && itemKey.equals(allocation.getItemKey())) {
                return normalizeOrderAmount(allocation.getCouponDeductAmount());
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算订单最终应付金额。
     *
     * @param itemsAmount        商品原始总金额
     * @param couponDeductAmount 优惠抵扣金额
     * @return 订单最终应付金额
     */
    private BigDecimal calculateOrderTotalAmount(BigDecimal itemsAmount,
                                                 BigDecimal couponDeductAmount) {
        BigDecimal totalAmount = normalizeOrderAmount(itemsAmount)
                .subtract(normalizeOrderAmount(couponDeductAmount))
                .setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalAmount;
    }

    /**
     * 规范化订单金额字段。
     *
     * @param amount 原始金额
     * @return 规范化后的金额
     */
    private BigDecimal normalizeOrderAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 序列化订单优惠券快照。
     *
     * @param couponSnapshot 订单优惠券快照
     * @return 订单优惠券快照JSON
     */
    private String buildCouponSnapshotJson(OrderCouponSelectionSnapshotDto couponSnapshot) {
        if (couponSnapshot == null) {
            return null;
        }
        return JSONUtils.toJson(couponSnapshot);
    }

    /**
     * 判断当前订单是否为0元订单。
     *
     * @param amount 订单金额
     * @return 是否为0元订单
     */
    private boolean isZeroAmount(BigDecimal amount) {
        return normalizeOrderAmount(amount).compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    /**
     * 判断当前订单是否存在可用券商品。
     *
     * @param settlementItems 优惠券结算商品项列表
     * @return 是否存在可用券商品
     */
    private boolean hasCouponEligibleItem(List<CouponSettlementItemDto> settlementItems) {
        return settlementItems != null && settlementItems.stream()
                .anyMatch(item -> Objects.equals(item.getCouponEnabled(), 1));
    }

    /**
     * 归一化商品是否允许使用优惠券标记。
     *
     * @param couponEnabled 原始标记
     * @return 归一化后的标记
     */
    private Integer resolveCouponEnabledFlag(Integer couponEnabled) {
        return Objects.equals(couponEnabled, 1) ? 1 : 0;
    }

    /**
     * 在事务提交成功后发布订单支付超时延迟消息。
     *
     * @param orderNo 订单编号
     */
    private void publishOrderTimeoutAfterCommit(String orderNo) {
        runAfterCommit(() -> orderTimeoutMessagePublisher.publishOrderTimeout(orderNo, com.zhangyichuang.medicine.common.core.constants.Constants.ORDER_TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    private void syncSalesIndexIfNeeded(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<MallOrderItem> orderItems = mallOrderItemService.getOrderItemByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        Map<Long, Integer> incrementMap = orderItems.stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() != null)
                .collect(Collectors.toMap(
                        MallOrderItem::getProductId,
                        MallOrderItem::getQuantity,
                        Integer::sum
                ));
        if (incrementMap.isEmpty()) {
            return;
        }

        List<Long> productIdsToSync = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : incrementMap.entrySet()) {
            Integer increment = entry.getValue();
            if (increment == null || increment <= 0) {
                continue;
            }
            String counterKey = String.format(RedisConstants.MallProductIndex.SALES_SYNC_COUNTER_KEY, entry.getKey());
            Long counter = redisCache.redisTemplate.opsForValue().increment(counterKey, increment);
            long current = counter != null ? counter : increment;
            long previous = current - increment;
            if (previous / SALES_SYNC_THRESHOLD < current / SALES_SYNC_THRESHOLD) {
                productIdsToSync.add(entry.getKey());
            }
        }

        if (productIdsToSync.isEmpty()) {
            return;
        }

        Map<Long, Integer> salesMap = mallOrderItemService.getCompletedSalesByProductIds(productIdsToSync);
        List<MallProductDocument> documents = new ArrayList<>();
        for (Long productId : productIdsToSync) {
            try {
                MallProductDetailDto detail = mallProductService.getProductAndDrugInfoById(productId);
                if (detail == null) {
                    continue;
                }
                detail.setSales(salesMap.getOrDefault(productId, 0));
                MallProductDocument document = toSearchDocument(detail);
                if (document != null) {
                    documents.add(document);
                }
            } catch (Exception ex) {
                log.warn("商品{}销量索引同步失败: {}", productId, ex.getMessage(), ex);
            }
        }
        if (!documents.isEmpty()) {
            mallProductSearchService.saveAll(documents);
        }
    }

    private MallProductDocument toSearchDocument(MallProductDetailDto detail) {
        if (detail == null) {
            return null;
        }
        DrugDetailDto drugDetail = detail.getDrugDetail();
        String coverImage = detail.getImages() != null && !detail.getImages().isEmpty() ? detail.getImages().getFirst() : null;
        return MallProductDocument.builder()
                .id(detail.getId())
                .name(detail.getName())
                .categoryNames(detail.getCategoryNames())
                .categoryIds(detail.getCategoryIds())
                .price(detail.getPrice())
                .sales(detail.getSales())
                .drugCategory(drugDetail != null ? drugDetail.getDrugCategory() : null)
                .status(detail.getStatus())
                .brand(drugDetail != null ? drugDetail.getBrand() : null)
                .commonName(drugDetail != null ? drugDetail.getCommonName() : null)
                .efficacy(drugDetail != null ? drugDetail.getEfficacy() : null)
                .tagIds(extractTagIds(detail.getTags()))
                .tagTypeBindings(extractTagTypeBindings(detail.getTags()))
                .tagNames(extractTagNames(detail.getTags()))
                .instruction(drugDetail != null ? drugDetail.getInstruction() : null)
                .coverImage(coverImage)
                .keywordSuggest(completion(buildKeywordSuggestInputs(detail)))
                .build();
    }

    /**
     * 构建商品关键字补全输入列表。
     *
     * @param detail 商品详情
     * @return 商品关键字补全输入列表
     */
    private List<String> buildKeywordSuggestInputs(MallProductDetailDto detail) {
        if (detail == null) {
            return List.of();
        }
        List<String> inputs = new ArrayList<>();
        addKeywordSuggestInput(inputs, detail.getName());
        if (detail.getCategoryNames() != null) {
            detail.getCategoryNames().forEach(categoryName -> addKeywordSuggestInput(inputs, categoryName));
        }
        if (detail.getDrugDetail() != null) {
            addKeywordSuggestInput(inputs, detail.getDrugDetail().getBrand());
            addKeywordSuggestInput(inputs, detail.getDrugDetail().getCommonName());
        }
        extractTagNames(detail.getTags()).forEach(tagName -> addKeywordSuggestInput(inputs, tagName));
        return inputs;
    }

    /**
     * 构建自动补全字段。
     *
     * @param values 自动补全输入列表
     * @return 自动补全字段
     */
    private Completion completion(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return new Completion(values);
    }

    /**
     * 追加关键字补全输入项。
     *
     * @param inputs 关键字补全输入列表
     * @param value  补全输入项
     */
    private void addKeywordSuggestInput(List<String> inputs, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalizedValue = value.trim();
        if (!inputs.contains(normalizedValue)) {
            inputs.add(normalizedValue);
        }
    }

    /**
     * 提取标签ID列表。
     *
     * @param tags 标签列表
     * @return 标签ID列表
     */
    private List<Long> extractTagIds(List<MallProductTagVo> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(MallProductTagVo::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 提取标签类型绑定列表。
     *
     * @param tags 标签列表
     * @return 标签类型绑定列表
     */
    private List<String> extractTagTypeBindings(List<MallProductTagVo> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .filter(tag -> tag.getId() != null && StringUtils.hasText(tag.getTypeCode()))
                .map(tag -> tag.getTypeCode() + MallProductTagConstants.TYPE_BINDING_SEPARATOR + tag.getId())
                .distinct()
                .toList();
    }

    /**
     * 提取标签名称列表。
     *
     * @param tags 标签列表
     * @return 标签名称列表
     */
    private List<String> extractTagNames(List<MallProductTagVo> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(MallProductTagVo::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 基于当前时间构建订单支付过期时间。
     *
     * @param baseTime 基准时间
     * @return 支付过期时间
     */
    private Date buildOrderPayExpireTime(Date baseTime) {
        return DateUtils.addMinutes(baseTime, ORDER_TIMEOUT_MINUTES);
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
     * 预览阶段优惠券选择上下文。
     *
     * @param selectedCoupon      当前主选优惠券（用于兼容单券展示位）
     * @param selectedCoupons     当前选中优惠券集合
     * @param couponDeductAmount  聚合抵扣金额
     * @param couponConsumeAmount 聚合消耗金额
     * @param couponWasteAmount   聚合浪费金额
     * @param autoSelected        是否自动选券
     */
    private record PreviewCouponSelectionContext(
            OrderCouponOptionVo selectedCoupon,
            List<OrderCouponOptionVo> selectedCoupons,
            BigDecimal couponDeductAmount,
            BigDecimal couponConsumeAmount,
            BigDecimal couponWasteAmount,
            Boolean autoSelected
    ) {
    }

    /**
     * 购物车下单草稿项。
     */
    @lombok.Data
    @lombok.Builder
    private static class CartOrderDraftItem {

        /**
         * 购物车ID。
         */
        private Long cartId;

        /**
         * 商品ID。
         */
        private Long productId;

        /**
         * 商品名称。
         */
        private String productName;

        /**
         * 商品图片。
         */
        private String imageUrl;

        /**
         * 商品单价。
         */
        private BigDecimal price;

        /**
         * 购买数量。
         */
        private Integer quantity;

        /**
         * 商品原始总金额。
         */
        private BigDecimal totalPrice;

        /**
         * 商品是否允许使用优惠券。
         */
        private Integer couponEnabled;
    }
}
