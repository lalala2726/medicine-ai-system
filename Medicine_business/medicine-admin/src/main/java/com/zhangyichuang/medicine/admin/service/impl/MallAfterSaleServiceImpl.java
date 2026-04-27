package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallAfterSaleMapper;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleAuditRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleProcessRequest;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 售后申请Service实现(管理端)
 *
 * @author Chuang
 * created 2025/11/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallAfterSaleServiceImpl extends ServiceImpl<MallAfterSaleMapper, MallAfterSale>
        implements MallAfterSaleService {

    /**
     * 全额退款完成后的退款状态编码。
     */
    private static final String REFUND_STATUS_SUCCESS = "SUCCESS";

    /**
     * 部分退款完成后的退款状态编码。
     */
    private static final String REFUND_STATUS_PARTIAL = "PARTIAL";

    private final MallAfterSaleMapper mallAfterSaleMapper;
    private final MallAfterSaleTimelineService mallAfterSaleTimelineService;
    private final MallOrderItemService mallOrderItemService;
    private final MallOrderTimelineService mallOrderTimelineService;
    private final MallOrderService mallOrderService;
    private final UserService userService;
    private final UserWalletService userWalletService;

    /**
     * 功能描述：分页查询售后列表并返回包含联表字段的 DTO 结果。
     *
     * @param request 查询条件对象，包含分页参数与售后筛选条件
     * @return 返回售后分页结果，记录类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当底层 Mapper 查询失败时抛出运行时异常
     */
    @Override
    public Page<MallAfterSaleListDto> getAfterSaleList(AfterSaleListRequest request) {
        Page<MallAfterSaleListDto> page = request.toPage();
        return mallAfterSaleMapper.selectAfterSaleList(page, request);
    }

    @Override
    public AfterSaleDetailVo getAfterSaleDetail(Long afterSaleId) {
        // 1. 查询售后申请
        MallAfterSale afterSale = getById(afterSaleId);
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        // 2. 查询用户信息
        User user = userService.getById(afterSale.getUserId());

        // 3. 查询订单项信息
        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());

        // 4. 构建售后详情
        AfterSaleTypeEnum afterSaleTypeEnum = AfterSaleTypeEnum.fromCode(afterSale.getAfterSaleType());
        AfterSaleStatusEnum afterSaleStatusEnum = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        AfterSaleReasonEnum afterSaleReasonEnum = AfterSaleReasonEnum.fromCode(afterSale.getApplyReason());
        ReceiveStatusEnum receiveStatusEnum = ReceiveStatusEnum.fromCode(afterSale.getReceiveStatus());

        List<String> evidenceImages = null;
        if (afterSale.getEvidenceImages() != null && !afterSale.getEvidenceImages().isEmpty()) {
            evidenceImages = JSONUtils.parseStringList(afterSale.getEvidenceImages());
        }

        AfterSaleDetailVo.ProductInfo productInfo = null;
        if (orderItem != null) {
            productInfo = AfterSaleDetailVo.ProductInfo.builder()
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .productImage(orderItem.getImageUrl())
                    .productPrice(orderItem.getPrice())
                    .quantity(orderItem.getQuantity())
                    .totalPrice(orderItem.getTotalPrice())
                    .build();
        }

        // 5. 查询时间线
        List<AfterSaleTimelineVo> timeline =
                mallAfterSaleTimelineService.getTimelineList(afterSaleId);

        return AfterSaleDetailVo.builder()
                .id(afterSale.getId())
                .afterSaleNo(afterSale.getAfterSaleNo())
                .orderId(afterSale.getOrderId())
                .orderNo(afterSale.getOrderNo())
                .orderItemId(afterSale.getOrderItemId())
                .userId(afterSale.getUserId())
                .userNickname(user != null ? user.getNickname() : "未知")
                .afterSaleType(afterSaleTypeEnum != null ? afterSaleTypeEnum.getType() : null)
                .afterSaleTypeName(afterSaleTypeEnum != null ? afterSaleTypeEnum.getName() : "未知")
                .afterSaleStatus(afterSaleStatusEnum != null ? afterSaleStatusEnum.getStatus() : null)
                .afterSaleStatusName(afterSaleStatusEnum != null ? afterSaleStatusEnum.getName() : "未知")
                .refundAmount(afterSale.getRefundAmount())
                .applyReason(afterSaleReasonEnum != null ? afterSaleReasonEnum.getReason() : null)
                .applyReasonName(afterSaleReasonEnum != null ? afterSaleReasonEnum.getName() : "未知")
                .applyDescription(afterSale.getApplyDescription())
                .evidenceImages(evidenceImages)
                .receiveStatus(receiveStatusEnum != null ? receiveStatusEnum.getStatus() : null)
                .receiveStatusName(receiveStatusEnum != null ? receiveStatusEnum.getName() : "未知")
                .rejectReason(afterSale.getRejectReason())
                .adminRemark(afterSale.getAdminRemark())
                .applyTime(afterSale.getApplyTime())
                .auditTime(afterSale.getAuditTime())
                .completeTime(afterSale.getCompleteTime())
                .productInfo(productInfo)
                .timeline(timeline)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.AFTER_SALE_KEY, key = "@mallDistributedLockKeyResolver.afterSaleKeyById(#request.afterSaleId)")
    public boolean auditAfterSale(AfterSaleAuditRequest request) {
        Long adminId = SecurityUtils.getUserId();
        String adminUsername = SecurityUtils.getUsername();

        // 1. 查询售后申请
        MallAfterSale afterSale = getById(request.getAfterSaleId());
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        // 2. 校验售后状态
        AfterSaleStatusEnum afterSaleStatus = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        if (afterSaleStatus != AfterSaleStatusEnum.PENDING) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前售后状态[%s]不允许审核", afterSaleStatus != null ? afterSaleStatus.getName() : "未知"));
        }

        Date now = new Date();

        if (request.getApproved()) {
            // 审核通过
            afterSale.setAfterSaleStatus(AfterSaleStatusEnum.APPROVED.getStatus());
            afterSale.setAdminRemark(request.getAdminRemark());
            afterSale.setAuditTime(now);
            afterSale.setUpdateTime(now);
            afterSale.setUpdateBy(adminUsername);

            boolean updated = updateById(afterSale);
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "审核失败，请重试");
            }

            // 添加售后时间线记录
            String description = String.format("管理员%s审核通过了售后申请", adminUsername);
            mallAfterSaleTimelineService.addTimeline(
                    afterSale.getId(),
                    OrderEventTypeEnum.AFTER_SALE_APPROVED.getType(),
                    AfterSaleStatusEnum.APPROVED.getStatus(),
                    OperatorTypeEnum.ADMIN.getType(),
                    adminId,
                    description
            );

            // 添加订单时间线记录
            OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                    .orderId(afterSale.getOrderId())
                    .eventType(OrderEventTypeEnum.AFTER_SALE_APPROVED.getType())
                    .eventStatus(mallOrderService.getById(afterSale.getOrderId()).getOrderStatus())
                    .operatorType(OperatorTypeEnum.ADMIN.getType())
                    .description(description)
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

            log.info("管理员{}审核通过售后申请，售后单号：{}", adminUsername, afterSale.getAfterSaleNo());

        } else {
            // 审核拒绝
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "拒绝原因不能为空");
            }

            afterSale.setAfterSaleStatus(AfterSaleStatusEnum.REJECTED.getStatus());
            afterSale.setRejectReason(request.getRejectReason());
            afterSale.setAdminRemark(request.getAdminRemark());
            afterSale.setAuditTime(now);
            afterSale.setCompleteTime(now);
            afterSale.setUpdateTime(now);
            afterSale.setUpdateBy(adminUsername);

            boolean updated = updateById(afterSale);
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "审核失败，请重试");
            }

            // 更新订单项售后状态为无售后
            MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());
            if (orderItem != null) {
                orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.NONE.getStatus());
                orderItem.setUpdateTime(now);
                mallOrderItemService.updateById(orderItem);
            }

            refreshOrderAfterSaleFlag(afterSale.getOrderId(), now);

            // 添加售后时间线记录
            String description = String.format("管理员%s拒绝了售后申请，原因：%s", adminUsername, request.getRejectReason());
            mallAfterSaleTimelineService.addTimeline(
                    afterSale.getId(),
                    OrderEventTypeEnum.AFTER_SALE_REJECTED.getType(),
                    AfterSaleStatusEnum.REJECTED.getStatus(),
                    OperatorTypeEnum.ADMIN.getType(),
                    adminId,
                    description
            );

            // 添加订单时间线记录
            OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                    .orderId(afterSale.getOrderId())
                    .eventType(OrderEventTypeEnum.AFTER_SALE_REJECTED.getType())
                    .eventStatus(mallOrderService.getById(afterSale.getOrderId()).getOrderStatus())
                    .operatorType(OperatorTypeEnum.ADMIN.getType())
                    .description(description)
                    .build();
            mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

            log.info("管理员{}拒绝售后申请，售后单号：{}，原因：{}", adminUsername, afterSale.getAfterSaleNo(), request.getRejectReason());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.AFTER_SALE_KEY, key = "@mallDistributedLockKeyResolver.afterSaleKeyById(#request.afterSaleId)")
    public boolean processRefund(AfterSaleProcessRequest request) {
        Long adminId = SecurityUtils.getUserId();
        String adminUsername = SecurityUtils.getUsername();

        // 1. 查询售后申请
        MallAfterSale afterSale = getById(request.getAfterSaleId());
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        // 2. 校验售后状态
        AfterSaleStatusEnum afterSaleStatus = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        if (afterSaleStatus != AfterSaleStatusEnum.APPROVED) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前售后状态[%s]不允许处理退款", afterSaleStatus != null ? afterSaleStatus.getName() : "未知"));
        }

        // 3. 校验售后类型
        AfterSaleTypeEnum afterSaleType = AfterSaleTypeEnum.fromCode(afterSale.getAfterSaleType());
        if (afterSaleType != AfterSaleTypeEnum.REFUND_ONLY && afterSaleType != AfterSaleTypeEnum.RETURN_REFUND) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "该售后类型不支持退款处理");
        }

        // 4. 查询订单信息
        MallOrder order = mallOrderService.getById(afterSale.getOrderId());
        if (order == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }

        // 校验订单是否已支付
        if (!Objects.equals(order.getPaid(), 1)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单未支付，无法退款");
        }

        // 5. 更新售后状态为处理中
        Date now = new Date();
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PROCESSING.getStatus());
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(adminUsername);
        updateById(afterSale);

        // 6. 当前仅支持钱包订单退款
        BigDecimal refundAmount = afterSale.getRefundAmount();
        PayTypeEnum payType = PayTypeEnum.fromCode(order.getPayType());
        if (payType != PayTypeEnum.WALLET) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前仅支持钱包支付订单退款");
        }

        try {
            processWalletRefund(afterSale, refundAmount);
        } catch (Exception e) {
            // 退款失败，恢复售后状态
            afterSale.setAfterSaleStatus(AfterSaleStatusEnum.APPROVED.getStatus());
            updateById(afterSale);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "退款失败：" + e.getMessage());
        }

        // 7. 先记录订单退款金额与退款时间，最终订单状态在售后完成后统一刷新
        BigDecimal orderRefundPrice = order.getRefundPrice() != null ? order.getRefundPrice() : BigDecimal.ZERO;
        BigDecimal totalRefunded = orderRefundPrice.add(refundAmount);
        order.setRefundPrice(totalRefunded);
        order.setRefundTime(now);

        // 8. 更新订单项已退款金额
        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());
        if (orderItem != null) {
            BigDecimal itemRefundedAmount = orderItem.getRefundedAmount() != null ? orderItem.getRefundedAmount() : BigDecimal.ZERO;
            orderItem.setRefundedAmount(itemRefundedAmount.add(refundAmount));
            orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.COMPLETED.getStatus());
            orderItem.setUpdateTime(now);
            mallOrderItemService.updateById(orderItem);
        }

        // 9. 更新售后状态为已完成
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.COMPLETED.getStatus());
        afterSale.setCompleteTime(now);
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(adminUsername);
        updateById(afterSale);

        refreshOrderAfterSaleFlag(order, now);

        // 10. 添加售后时间线记录
        String description = String.format("管理员%s完成了退款处理，退款金额：%.2f元", adminUsername, refundAmount);
        mallAfterSaleTimelineService.addTimeline(
                afterSale.getId(),
                OrderEventTypeEnum.AFTER_SALE_COMPLETED.getType(),
                AfterSaleStatusEnum.COMPLETED.getStatus(),
                OperatorTypeEnum.ADMIN.getType(),
                adminId,
                description
        );

        // 11. 添加订单时间线记录
        OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                .orderId(afterSale.getOrderId())
                .eventType(OrderEventTypeEnum.ORDER_REFUNDED.getType())
                .eventStatus(order.getOrderStatus())
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

        log.info("管理员{}完成售后退款，售后单号：{}，退款金额：{}", adminUsername, afterSale.getAfterSaleNo(), refundAmount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.AFTER_SALE_KEY, key = "@mallDistributedLockKeyResolver.afterSaleKeyById(#request.afterSaleId)")
    public boolean processExchange(AfterSaleProcessRequest request) {
        Long adminId = SecurityUtils.getUserId();
        String adminUsername = SecurityUtils.getUsername();

        // 1. 查询售后申请
        MallAfterSale afterSale = getById(request.getAfterSaleId());
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        // 2. 校验售后状态
        AfterSaleStatusEnum afterSaleStatus = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        if (afterSaleStatus != AfterSaleStatusEnum.APPROVED) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前售后状态[%s]不允许处理换货", afterSaleStatus != null ? afterSaleStatus.getName() : "未知"));
        }

        // 3. 校验售后类型
        AfterSaleTypeEnum afterSaleType = AfterSaleTypeEnum.fromCode(afterSale.getAfterSaleType());
        if (afterSaleType != AfterSaleTypeEnum.EXCHANGE) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "该售后类型不支持换货处理");
        }

        // 4. 更新售后状态为处理中
        Date now = new Date();
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PROCESSING.getStatus());
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(adminUsername);
        updateById(afterSale);

        // 5. 记录换货物流信息(如果提供)
        String exchangeInfo = "";
        if (request.getLogisticsCompany() != null && request.getTrackingNumber() != null) {
            exchangeInfo = String.format("，换货物流：%s，单号：%s", request.getLogisticsCompany(), request.getTrackingNumber());
        }

        // 6. 更新订单项售后状态为售后完成
        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());
        if (orderItem != null) {
            orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.COMPLETED.getStatus());
            orderItem.setUpdateTime(now);
            mallOrderItemService.updateById(orderItem);
        }

        // 7. 更新售后状态为已完成
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.COMPLETED.getStatus());
        afterSale.setCompleteTime(now);
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(adminUsername);
        updateById(afterSale);

        MallOrder order = mallOrderService.getById(afterSale.getOrderId());
        refreshOrderAfterSaleFlag(order, now);

        // 8. 添加售后时间线记录
        String description = String.format("管理员%s完成了换货处理%s", adminUsername, exchangeInfo);
        mallAfterSaleTimelineService.addTimeline(
                afterSale.getId(),
                OrderEventTypeEnum.AFTER_SALE_COMPLETED.getType(),
                AfterSaleStatusEnum.COMPLETED.getStatus(),
                OperatorTypeEnum.ADMIN.getType(),
                adminId,
                description
        );

        // 9. 添加订单时间线记录
        OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                .orderId(afterSale.getOrderId())
                .eventType(OrderEventTypeEnum.AFTER_SALE_COMPLETED.getType())
                .eventStatus(order != null ? order.getOrderStatus() : null)
                .operatorType(OperatorTypeEnum.ADMIN.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

        log.info("管理员{}完成售后换货，售后单号：{}{}", adminUsername, afterSale.getAfterSaleNo(), exchangeInfo);
        return true;
    }

    /**
     * 根据订单项售后状态刷新订单售后标记，并在售后结束后恢复订单业务状态。
     *
     * @param orderId 订单 ID
     * @param now     当前时间
     */
    private void refreshOrderAfterSaleFlag(Long orderId, Date now) {
        if (orderId == null) {
            return;
        }
        MallOrder order = mallOrderService.getById(orderId);
        if (order == null) {
            return;
        }
        refreshOrderAfterSaleFlag(order, now);
    }

    /**
     * 刷新订单的售后标记与订单状态。
     *
     * @param order 订单实体
     * @param now   当前时间
     */
    private void refreshOrderAfterSaleFlag(MallOrder order, Date now) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderItemAfterSaleStatusEnum newFlag = resolveOrderAfterSaleFlag(order.getId());
        order.setAfterSaleFlag(newFlag);
        if (isOrderFullyRefunded(order)) {
            order.setRefundStatus(REFUND_STATUS_SUCCESS);
            order.setOrderStatus(OrderStatusEnum.REFUNDED.getType());
        } else {
            if (defaultAmount(order.getRefundPrice()).compareTo(BigDecimal.ZERO) > 0) {
                order.setRefundStatus(REFUND_STATUS_PARTIAL);
            }
            if (newFlag == OrderItemAfterSaleStatusEnum.IN_PROGRESS) {
                order.setOrderStatus(OrderStatusEnum.AFTER_SALE.getType());
            } else {
                order.setOrderStatus(resolveOrderStatusAfterAfterSale(order).getType());
            }
        }
        order.setUpdateTime(now);
        boolean updated = mallOrderService.updateById(order);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "刷新订单售后状态失败，请重试");
        }
    }

    /**
     * 解析订单维度的售后标记。
     *
     * @param orderId 订单 ID
     * @return 返回订单售后标记
     */
    private OrderItemAfterSaleStatusEnum resolveOrderAfterSaleFlag(Long orderId) {
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, orderId)
                .list();
        boolean hasCompleted = false;
        for (MallOrderItem orderItem : orderItems) {
            OrderItemAfterSaleStatusEnum afterSaleStatusEnum =
                    OrderItemAfterSaleStatusEnum.fromCode(orderItem.getAfterSaleStatus());
            if (afterSaleStatusEnum == OrderItemAfterSaleStatusEnum.IN_PROGRESS) {
                return OrderItemAfterSaleStatusEnum.IN_PROGRESS;
            }
            if (afterSaleStatusEnum == OrderItemAfterSaleStatusEnum.COMPLETED) {
                hasCompleted = true;
            }
        }
        return hasCompleted ? OrderItemAfterSaleStatusEnum.COMPLETED : OrderItemAfterSaleStatusEnum.NONE;
    }

    /**
     * 在售后流程结束后恢复订单应有的业务状态。
     *
     * @param order 订单实体
     * @return 返回售后结束后的订单状态
     */
    private OrderStatusEnum resolveOrderStatusAfterAfterSale(MallOrder order) {
        if (order.getReceiveTime() != null || order.getFinishTime() != null) {
            return OrderStatusEnum.COMPLETED;
        }
        if (order.getDeliverTime() != null) {
            return OrderStatusEnum.PENDING_RECEIPT;
        }
        return OrderStatusEnum.PENDING_SHIPMENT;
    }

    /**
     * 判断订单是否已经完成全额退款。
     *
     * @param order 订单实体
     * @return 返回是否全额退款
     */
    private boolean isOrderFullyRefunded(MallOrder order) {
        BigDecimal payAmount = defaultAmount(order.getPayAmount());
        BigDecimal refundedAmount = defaultAmount(order.getRefundPrice());
        return payAmount.compareTo(BigDecimal.ZERO) > 0 && refundedAmount.compareTo(payAmount) >= 0;
    }

    /**
     * 统一处理金额空值，避免空指针影响售后状态回写。
     *
     * @param amount 原始金额
     * @return 返回非空金额
     */
    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 处理钱包退款
     *
     * @param afterSale    售后申请
     * @param refundAmount 退款金额
     */
    private void processWalletRefund(MallAfterSale afterSale, BigDecimal refundAmount) {
        String walletRemark = String.format("售后退款（售后单号：%s，退款金额：%s元）",
                afterSale.getAfterSaleNo(), formatAmount(refundAmount));
        boolean success = userWalletService.rechargeWallet(afterSale.getUserId(), refundAmount, walletRemark);
        if (!success) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包退款失败");
        }
        log.info("钱包退款成功，售后单号：{}，退款金额：{}", afterSale.getAfterSaleNo(), refundAmount);
    }

    /**
     * 格式化金额为两位小数字符串。
     *
     * @param amount 金额对象
     * @return 格式化后的金额字符串
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
