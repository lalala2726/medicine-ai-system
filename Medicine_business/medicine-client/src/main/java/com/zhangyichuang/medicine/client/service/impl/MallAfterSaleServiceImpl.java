package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallAfterSaleMapper;
import com.zhangyichuang.medicine.client.model.bo.AfterSaleEligibilityBo.CommandBo;
import com.zhangyichuang.medicine.client.model.bo.AfterSaleEligibilityBo.FailureBo;
import com.zhangyichuang.medicine.client.model.bo.AfterSaleEligibilityBo.ItemResultBo;
import com.zhangyichuang.medicine.client.model.bo.AfterSaleEligibilityBo.SnapshotBo;
import com.zhangyichuang.medicine.client.model.request.*;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleApplyResultVo;
import com.zhangyichuang.medicine.client.model.vo.AfterSaleEligibilityVo;
import com.zhangyichuang.medicine.client.service.MallAfterSaleService;
import com.zhangyichuang.medicine.client.service.MallAfterSaleTimelineService;
import com.zhangyichuang.medicine.client.service.MallOrderItemService;
import com.zhangyichuang.medicine.client.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleListVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 售后申请Service实现
 *
 * @author Chuang
 * created 2025/11/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallAfterSaleServiceImpl extends ServiceImpl<MallAfterSaleMapper, MallAfterSale>
        implements MallAfterSaleService, BaseService {

    /**
     * 已完成订单可申请售后的有效月数。
     */
    private static final int AFTER_SALE_VALID_MONTHS = 3;

    /**
     * 售后资格校验结果编码：满足售后条件。
     */
    private static final String ELIGIBILITY_REASON_OK = "ELIGIBLE";

    /**
     * 售后资格校验结果编码：订单不存在或不属于当前用户。
     */
    private static final String ELIGIBILITY_REASON_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    /**
     * 售后资格校验结果编码：订单项不存在或不属于当前订单。
     */
    private static final String ELIGIBILITY_REASON_ORDER_ITEM_NOT_FOUND = "ORDER_ITEM_NOT_FOUND";

    /**
     * 售后资格校验结果编码：订单尚未支付。
     */
    private static final String ELIGIBILITY_REASON_ORDER_NOT_PAID = "ORDER_NOT_PAID";

    /**
     * 售后资格校验结果编码：订单状态异常。
     */
    private static final String ELIGIBILITY_REASON_ORDER_STATUS_INVALID = "ORDER_STATUS_INVALID";

    /**
     * 售后资格校验结果编码：订单状态不允许发起售后。
     */
    private static final String ELIGIBILITY_REASON_ORDER_STATUS_NOT_ELIGIBLE = "ORDER_STATUS_NOT_ELIGIBLE";

    /**
     * 售后资格校验结果编码：存在进行中的售后记录。
     */
    private static final String ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS = "AFTER_SALE_IN_PROGRESS";

    /**
     * 售后资格校验结果编码：存在历史售后冲突。
     */
    private static final String ELIGIBILITY_REASON_HISTORY_CONFLICT = "HISTORY_CONFLICT";

    /**
     * 售后资格校验结果编码：没有可退款金额。
     */
    private static final String ELIGIBILITY_REASON_NO_REFUNDABLE_AMOUNT = "NO_REFUNDABLE_AMOUNT";

    /**
     * 售后资格校验结果编码：已超过售后有效期。
     */
    private static final String ELIGIBILITY_REASON_AFTER_SALE_EXPIRED = "AFTER_SALE_EXPIRED";

    /**
     * 售后列表查询 Mapper。
     */
    private final MallAfterSaleMapper mallAfterSaleMapper;

    /**
     * 售后时间线服务。
     */
    private final MallAfterSaleTimelineService mallAfterSaleTimelineService;

    /**
     * 订单项服务。
     */
    private final MallOrderItemService mallOrderItemService;

    /**
     * 订单时间线服务。
     */
    private final MallOrderTimelineService mallOrderTimelineService;

    /**
     * 订单服务。
     */
    private final com.zhangyichuang.medicine.client.service.MallOrderService mallOrderService;

    /**
     * 用户服务。
     */
    private final com.zhangyichuang.medicine.client.service.UserService userService;

    /**
     * 统一处理用户端售后申请。
     *
     * @param request 售后申请请求参数
     * @return 返回申请结果，包含请求范围、实际生效范围和生成的售后单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.ORDER_KEY, key = "#request.orderNo")
    public AfterSaleApplyResultVo applyAfterSale(AfterSaleApplyRequest request) {
        validateApplyRequest(request);
        Long userId = getUserId();
        SnapshotBo snapshot = buildEligibilitySnapshot(CommandBo.builder()
                .orderNo(request.getOrderNo())
                .requestedScope(request.getScope())
                .requestedOrderItemId(request.getOrderItemId())
                .userId(userId)
                .build());

        if (!snapshot.getEligible()) {
            throw buildEligibilityException(snapshot);
        }
        if (snapshot.getResolvedScope() == AfterSaleScopeEnum.ORDER
                && request.getAfterSaleType() != AfterSaleTypeEnum.REFUND_ONLY) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "整单申请仅支持仅退款");
        }

        ReceiveStatusEnum receiveStatus = resolveReceiveStatus(snapshot.getOrder());
        String evidenceImagesJson = buildEvidenceImagesJson(request.getEvidenceImages());
        Date now = new Date();

        List<String> afterSaleNos = new ArrayList<>();
        List<Long> orderItemIds = new ArrayList<>();
        if (snapshot.getResolvedScope() == AfterSaleScopeEnum.ORDER) {
            for (ItemResultBo itemResult : getApplicableOrderScopeItems(snapshot)) {
                String afterSaleNo = createOrUpdateAfterSale(snapshot.getOrder(), itemResult.getOrderItem(), userId,
                        request.getAfterSaleType(), itemResult.getRefundableAmount(), request.getApplyReason(),
                        request.getApplyDescription(), evidenceImagesJson, receiveStatus, now);
                afterSaleNos.add(afterSaleNo);
                orderItemIds.add(itemResult.getOrderItem().getId());
            }
        } else {
            ItemResultBo selectedItem = requireSelectedItem(snapshot);
            BigDecimal refundAmount = validateItemRefundAmount(request.getRefundAmount(), selectedItem.getRefundableAmount());
            String afterSaleNo = createOrUpdateAfterSale(snapshot.getOrder(), selectedItem.getOrderItem(), userId,
                    request.getAfterSaleType(), refundAmount, request.getApplyReason(), request.getApplyDescription(),
                    evidenceImagesJson, receiveStatus, now);
            afterSaleNos.add(afterSaleNo);
            orderItemIds.add(selectedItem.getOrderItem().getId());
        }

        return AfterSaleApplyResultVo.builder()
                .orderNo(snapshot.getOrder().getOrderNo())
                .requestedScope(snapshot.getRequestedScope().getScope())
                .resolvedScope(snapshot.getResolvedScope().getScope())
                .afterSaleNos(afterSaleNos)
                .orderItemIds(orderItemIds)
                .build();
    }

    /**
     * 查询当前用户订单或订单项的售后资格信息。
     *
     * @param request 售后资格校验请求参数
     * @return 返回包含商品列表、金额信息和资格结果的视图对象
     */
    @Override
    public AfterSaleEligibilityVo getAfterSaleEligibility(AfterSaleEligibilityRequest request) {
        AfterSaleEligibilityRequest safeRequest = request == null ? new AfterSaleEligibilityRequest() : request;
        SnapshotBo snapshot = buildEligibilitySnapshot(CommandBo.builder()
                .orderNo(safeRequest.getOrderNo())
                .requestedScope(safeRequest.getScope() == null ? AfterSaleScopeEnum.ORDER : safeRequest.getScope())
                .requestedOrderItemId(safeRequest.getOrderItemId())
                .userId(getUserId())
                .build());
        return toEligibilityVo(snapshot);
    }

    /**
     * 重新提交已被拒绝的售后申请。
     *
     * @param request 售后重新申请参数
     * @return 返回重新提交后的售后单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.AFTER_SALE_KEY, key = "#request.afterSaleNo")
    public String reapplyAfterSale(AfterSaleReapplyRequest request) {
        Long userId = getUserId();

        MallAfterSale afterSale = lambdaQuery()
                .eq(MallAfterSale::getAfterSaleNo, request.getAfterSaleNo())
                .one();
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "售后申请不存在");
        }

        AfterSaleStatusEnum afterSaleStatusEnum = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        if (afterSaleStatusEnum != AfterSaleStatusEnum.REJECTED) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前售后状态[%s]不允许再次申请", afterSaleStatusEnum != null ? afterSaleStatusEnum.getName() : "未知"));
        }

        long approvedCount = lambdaQuery()
                .eq(MallAfterSale::getOrderItemId, afterSale.getOrderItemId())
                .eq(MallAfterSale::getUserId, userId)
                .ne(MallAfterSale::getId, afterSale.getId())
                .in(MallAfterSale::getAfterSaleStatus,
                        AfterSaleStatusEnum.APPROVED.getStatus(),
                        AfterSaleStatusEnum.PROCESSING.getStatus(),
                        AfterSaleStatusEnum.COMPLETED.getStatus())
                .count();
        if (approvedCount > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "该商品已存在通过的售后记录，禁止重复申请");
        }

        String evidenceImagesJson = buildEvidenceImagesJson(request.getEvidenceImages());

        Date now = new Date();
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PENDING.getStatus());
        afterSale.setApplyReason(request.getApplyReason().getReason());
        afterSale.setApplyDescription(request.getApplyDescription());
        afterSale.setEvidenceImages(evidenceImagesJson);
        afterSale.setApplyTime(now);
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(getUsername());
        afterSale.setRejectReason(null);
        afterSale.setAdminRemark(null);
        afterSale.setAuditTime(null);
        afterSale.setCompleteTime(null);

        boolean updated = updateById(afterSale);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "重新申请售后失败，请稍后重试");
        }

        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());
        if (orderItem != null) {
            orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.IN_PROGRESS.getStatus());
            orderItem.setUpdateTime(now);
            mallOrderItemService.updateById(orderItem);
        }

        MallOrder order = mallOrderService.getById(afterSale.getOrderId());
        if (order != null) {
            markOrderAfterSaleInProgress(order, now);
        }

        String username = getUsername();
        String description = String.format("用户%s重新提交售后申请", username);
        mallAfterSaleTimelineService.addTimeline(
                afterSale.getId(),
                OrderEventTypeEnum.AFTER_SALE_APPLIED.getType(),
                AfterSaleStatusEnum.PENDING.getStatus(),
                OperatorTypeEnum.USER.getType(),
                userId,
                description
        );

        OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                .orderId(afterSale.getOrderId())
                .eventType(OrderEventTypeEnum.AFTER_SALE_APPLIED.getType())
                .eventStatus(order != null ? order.getOrderStatus() : null)
                .operatorType(OperatorTypeEnum.USER.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

        log.info("用户{}重新申请售后成功，售后单号：{}", username, afterSale.getAfterSaleNo());
        return afterSale.getAfterSaleNo();
    }

    /**
     * 取消当前用户待审核状态的售后申请。
     *
     * @param request 取消售后请求参数
     * @return 返回是否取消成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(prefix = RedisConstants.Lock.AFTER_SALE_KEY, key = "@mallDistributedLockKeyResolver.afterSaleKeyById(#request.afterSaleId)")
    public boolean cancelAfterSale(AfterSaleCancelRequest request) {
        Long userId = getUserId();

        MallAfterSale afterSale = getById(request.getAfterSaleId());
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }

        if (!Objects.equals(afterSale.getUserId(), userId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "售后申请不存在");
        }

        AfterSaleStatusEnum afterSaleStatus = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        if (afterSaleStatus != AfterSaleStatusEnum.PENDING) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("当前售后状态[%s]不允许取消", afterSaleStatus != null ? afterSaleStatus.getName() : "未知"));
        }

        Date now = new Date();
        afterSale.setAfterSaleStatus(AfterSaleStatusEnum.CANCELLED.getStatus());
        afterSale.setCompleteTime(now);
        afterSale.setUpdateTime(now);
        afterSale.setUpdateBy(getUsername());

        boolean updated = updateById(afterSale);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "取消售后失败，请重试");
        }

        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());
        if (orderItem != null) {
            orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.NONE.getStatus());
            orderItem.setUpdateTime(now);
            mallOrderItemService.updateById(orderItem);
        }

        refreshOrderAfterSaleFlag(afterSale.getOrderId(), now);

        String username = getUsername();
        String description = String.format("用户%s取消了售后申请", username);
        if (request.getCancelReason() != null && !request.getCancelReason().trim().isEmpty()) {
            description += String.format("，原因：%s", request.getCancelReason());
        }
        mallAfterSaleTimelineService.addTimeline(
                afterSale.getId(),
                OrderEventTypeEnum.ORDER_CANCELLED.getType(),
                AfterSaleStatusEnum.CANCELLED.getStatus(),
                OperatorTypeEnum.USER.getType(),
                userId,
                description
        );

        log.info("用户{}取消售后成功，售后单号：{}", username, afterSale.getAfterSaleNo());
        return true;
    }

    /**
     * 分页查询当前用户的售后列表。
     *
     * @param request 售后列表查询条件
     * @return 返回售后分页列表
     */
    @Override
    public Page<AfterSaleListVo> getAfterSaleList(AfterSaleListRequest request) {
        AfterSaleListRequest safeRequest = request == null ? new AfterSaleListRequest() : request;
        Page<AfterSaleListVo> page = safeRequest.toPage();
        return mallAfterSaleMapper.selectAfterSaleList(page, safeRequest, getUserId());
    }

    /**
     * 按售后单号和用户 ID 查询售后详情。
     *
     * @param afterSaleNo 售后单号
     * @param userId      用户 ID
     * @return 返回售后详情视图对象
     */
    @Override
    public AfterSaleDetailVo getAfterSaleDetail(String afterSaleNo, Long userId) {
        MallAfterSale afterSale = lambdaQuery()
                .eq(MallAfterSale::getAfterSaleNo, afterSaleNo)
                .eq(MallAfterSale::getUserId, userId)
                .one();
        if (afterSale == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }
        return buildAfterSaleDetail(afterSale);
    }

    /**
     * 为客户端智能体提供售后资格判断结果。
     *
     * @param request 智能体售后资格校验请求
     * @param userId  当前用户 ID
     * @return 返回智能体侧使用的售后资格 DTO
     */
    @Override
    public ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId) {
        ClientAgentAfterSaleEligibilityRequest safeRequest =
                request == null ? new ClientAgentAfterSaleEligibilityRequest() : request;
        AfterSaleScopeEnum requestedScope = safeRequest.getOrderItemId() == null ? AfterSaleScopeEnum.ORDER : AfterSaleScopeEnum.ITEM;
        SnapshotBo snapshot = buildEligibilitySnapshot(CommandBo.builder()
                .orderNo(safeRequest.getOrderNo())
                .requestedScope(requestedScope)
                .requestedOrderItemId(safeRequest.getOrderItemId())
                .userId(userId)
                .build());

        return ClientAgentAfterSaleEligibilityDto.builder()
                .orderNo(snapshot.getOrderNo())
                .orderItemId(snapshot.getSelectedOrderItemId())
                .scope(snapshot.getResolvedScope().getScope())
                .orderStatus(snapshot.getOrderStatus())
                .orderStatusName(snapshot.getOrderStatusName())
                .eligible(snapshot.getEligible())
                .reasonCode(snapshot.getReasonCode())
                .reasonMessage(snapshot.getReasonMessage())
                .refundableAmount(snapshot.getSelectedRefundableAmount())
                .build();
    }

    /**
     * 校验统一售后申请请求中的关键字段是否合法。
     *
     * @param request 售后申请请求参数
     */
    private void validateApplyRequest(AfterSaleApplyRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "申请参数不能为空");
        }
        if (request.getOrderNo() == null || request.getOrderNo().isBlank()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单编号不能为空");
        }
        if (request.getScope() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "申请范围不能为空");
        }
        if (request.getAfterSaleType() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "售后类型不能为空");
        }
        if (request.getApplyReason() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "申请原因不能为空");
        }
        if (request.getScope() == AfterSaleScopeEnum.ITEM && request.getOrderItemId() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单项ID不能为空");
        }
        if (request.getScope() == AfterSaleScopeEnum.ORDER && request.getAfterSaleType() != AfterSaleTypeEnum.REFUND_ONLY) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "整单申请仅支持仅退款");
        }
    }

    /**
     * 构建指定订单范围下的售后资格快照。
     *
     * @param command 售后资格计算命令
     * @return 返回包含订单、商品、金额和结果编码的资格快照
     */
    private SnapshotBo buildEligibilitySnapshot(CommandBo command) {
        AfterSaleScopeEnum requestedScope = command.getRequestedScope() == null ? AfterSaleScopeEnum.ORDER : command.getRequestedScope();
        MallOrder order = mallOrderService.lambdaQuery()
                .eq(MallOrder::getOrderNo, command.getOrderNo())
                .eq(MallOrder::getUserId, command.getUserId())
                .one();
        if (order == null) {
            return buildNotFoundSnapshot(command.getOrderNo(), requestedScope, command.getRequestedOrderItemId()
            );
        }

        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .eq(MallOrderItem::getOrderId, order.getId())
                .list();
        if (orderItems == null) {
            orderItems = Collections.emptyList();
        }

        AfterSaleScopeEnum resolvedScope = resolveScope(requestedScope, command.getRequestedOrderItemId(), orderItems);
        Date deadlineTime = resolveAfterSaleDeadline(order);
        FailureBo orderFailure = evaluateOrderFailure(order, deadlineTime);
        List<ItemResultBo> itemResults = buildItemEligibilityResults(order, orderItems, command.getUserId(), orderFailure);
        BigDecimal totalRefundableAmount = positive(calculateOrderRefundableAmount(order));

        ItemResultBo selectedItem = resolveSelectedItem(itemResults, command.getRequestedOrderItemId());
        if (command.getRequestedOrderItemId() != null && selectedItem == null && resolvedScope == AfterSaleScopeEnum.ITEM) {
            return SnapshotBo.builder()
                    .orderNo(order.getOrderNo())
                    .requestedScope(requestedScope)
                    .resolvedScope(resolvedScope)
                    .requestedOrderItemId(command.getRequestedOrderItemId())
                    .selectedOrderItemId(command.getRequestedOrderItemId())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(resolveOrderStatusName(order.getOrderStatus()))
                    .eligible(false)
                    .reasonCode(ELIGIBILITY_REASON_ORDER_ITEM_NOT_FOUND)
                    .reasonMessage("订单商品不存在或不属于当前订单")
                    .selectedRefundableAmount(BigDecimal.ZERO)
                    .totalRefundableAmount(totalRefundableAmount)
                    .afterSaleDeadlineTime(deadlineTime)
                    .order(order)
                    .orderItems(orderItems)
                    .itemResults(itemResults)
                    .build();
        }

        if (resolvedScope == AfterSaleScopeEnum.ORDER && requestedScope == AfterSaleScopeEnum.ITEM
                && orderItems.size() == 1 && selectedItem == null && !orderItems.isEmpty()) {
            selectedItem = itemResults.isEmpty() ? null : itemResults.getFirst();
        }

        boolean eligible;
        String reasonCode;
        String reasonMessage;
        BigDecimal selectedRefundableAmount;
        Long selectedOrderItemId;

        if (resolvedScope == AfterSaleScopeEnum.ORDER) {
            FailureBo orderScopeFailure = orderFailure != null ? orderFailure : evaluateOrderScopeFailure(itemResults, totalRefundableAmount);
            eligible = orderScopeFailure == null;
            reasonCode = eligible ? ELIGIBILITY_REASON_OK : orderScopeFailure.getReasonCode();
            reasonMessage = eligible ? "订单满足售后条件" : orderScopeFailure.getReasonMessage();
            selectedRefundableAmount = totalRefundableAmount;
            selectedOrderItemId = selectedItem != null ? selectedItem.getOrderItem().getId() : command.getRequestedOrderItemId();
        } else if (selectedItem != null) {
            eligible = Boolean.TRUE.equals(selectedItem.getEligible());
            reasonCode = eligible ? ELIGIBILITY_REASON_OK : selectedItem.getReasonCode();
            reasonMessage = eligible ? "该商品满足售后条件" : selectedItem.getReasonMessage();
            selectedRefundableAmount = selectedItem.getRefundableAmount();
            selectedOrderItemId = selectedItem.getOrderItem().getId();
        } else {
            ItemResultBo firstEligible = findFirstEligibleItem(itemResults);
            eligible = firstEligible != null;
            reasonCode = eligible ? ELIGIBILITY_REASON_OK
                    : itemResults.stream().findFirst().map(ItemResultBo::getReasonCode).orElse(ELIGIBILITY_REASON_ORDER_ITEM_NOT_FOUND);
            reasonMessage = eligible ? "订单存在可申请售后的商品"
                    : itemResults.stream().findFirst().map(ItemResultBo::getReasonMessage).orElse("订单商品不存在");
            selectedRefundableAmount = BigDecimal.ZERO;
            selectedOrderItemId = null;
        }

        return SnapshotBo.builder()
                .orderNo(order.getOrderNo())
                .requestedScope(requestedScope)
                .resolvedScope(resolvedScope)
                .requestedOrderItemId(command.getRequestedOrderItemId())
                .selectedOrderItemId(selectedOrderItemId)
                .orderStatus(order.getOrderStatus())
                .orderStatusName(resolveOrderStatusName(order.getOrderStatus()))
                .eligible(eligible)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .selectedRefundableAmount(selectedRefundableAmount)
                .totalRefundableAmount(totalRefundableAmount)
                .afterSaleDeadlineTime(deadlineTime)
                .order(order)
                .orderItems(orderItems)
                .itemResults(itemResults)
                .build();
    }

    /**
     * 构建订单不存在场景下的默认资格快照。
     *
     * @param orderNo              订单编号
     * @param requestedScope       请求范围
     * @param requestedOrderItemId 请求订单项 ID
     * @return 返回不可售后的资格快照
     */
    private SnapshotBo buildNotFoundSnapshot(String orderNo,
                                             AfterSaleScopeEnum requestedScope,
                                             Long requestedOrderItemId) {
        return SnapshotBo.builder()
                .orderNo(orderNo)
                .requestedScope(requestedScope)
                .resolvedScope(requestedScope)
                .requestedOrderItemId(requestedOrderItemId)
                .selectedOrderItemId(requestedOrderItemId)
                .eligible(false)
                .reasonCode(MallAfterSaleServiceImpl.ELIGIBILITY_REASON_ORDER_NOT_FOUND)
                .reasonMessage("订单不存在或无权访问")
                .selectedRefundableAmount(BigDecimal.ZERO)
                .totalRefundableAmount(BigDecimal.ZERO)
                .itemResults(Collections.emptyList())
                .orderItems(Collections.emptyList())
                .build();
    }

    /**
     * 解析本次售后申请最终生效的范围。
     *
     * @param requestedScope       前端请求范围
     * @param requestedOrderItemId 前端请求的订单项 ID
     * @param orderItems           订单下的全部商品列表
     * @return 返回最终生效的售后范围
     */
    private AfterSaleScopeEnum resolveScope(AfterSaleScopeEnum requestedScope,
                                            Long requestedOrderItemId,
                                            List<MallOrderItem> orderItems) {
        if (requestedScope != AfterSaleScopeEnum.ITEM || orderItems.size() != 1) {
            return requestedScope;
        }
        Long onlyItemId = orderItems.getFirst().getId();
        if (requestedOrderItemId == null || Objects.equals(requestedOrderItemId, onlyItemId)) {
            return AfterSaleScopeEnum.ORDER;
        }
        return requestedScope;
    }

    /**
     * 为订单下的每个商品构建售后资格结果。
     *
     * @param order        订单实体
     * @param orderItems   订单商品列表
     * @param userId       当前用户 ID
     * @param orderFailure 订单级失败原因
     * @return 返回商品维度的资格结果列表
     */
    private List<ItemResultBo> buildItemEligibilityResults(MallOrder order,
                                                           List<MallOrderItem> orderItems,
                                                           Long userId,
                                                           FailureBo orderFailure) {
        List<ItemResultBo> results = new ArrayList<>();
        for (MallOrderItem orderItem : orderItems) {
            BigDecimal refundableAmount = positive(calculateOrderItemRefundableAmount(orderItem));
            if (orderFailure != null) {
                results.add(buildItemEligibilityResult(orderItem, false, orderFailure.getReasonCode(),
                        orderFailure.getReasonMessage(), refundableAmount));
                continue;
            }

            OrderItemAfterSaleStatusEnum itemStatusEnum = resolveOrderItemStatus(orderItem.getAfterSaleStatus());
            if (itemStatusEnum == OrderItemAfterSaleStatusEnum.IN_PROGRESS || hasActiveAfterSale(order.getId(), orderItem.getId(), userId)) {
                results.add(buildItemEligibilityResult(orderItem, false, ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS,
                        "该商品当前存在处理中售后记录", refundableAmount));
                continue;
            }
            if (itemStatusEnum == OrderItemAfterSaleStatusEnum.COMPLETED) {
                results.add(buildItemEligibilityResult(orderItem, false, ELIGIBILITY_REASON_HISTORY_CONFLICT,
                        "该商品已存在完成售后记录，暂不支持重复申请", refundableAmount));
                continue;
            }
            if (refundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                results.add(buildItemEligibilityResult(orderItem, false, ELIGIBILITY_REASON_NO_REFUNDABLE_AMOUNT,
                        "该商品已无可退款金额", BigDecimal.ZERO));
                continue;
            }
            results.add(buildItemEligibilityResult(orderItem, true, ELIGIBILITY_REASON_OK,
                    "该商品满足售后条件", refundableAmount));
        }
        return results;
    }

    /**
     * 构建单个订单项的售后资格结果。
     *
     * @param orderItem        订单项实体
     * @param eligible         是否满足资格
     * @param reasonCode       结果编码
     * @param reasonMessage    结果说明
     * @param refundableAmount 当前可退金额
     * @return 返回订单项资格结果
     */
    private ItemResultBo buildItemEligibilityResult(MallOrderItem orderItem,
                                                    boolean eligible,
                                                    String reasonCode,
                                                    String reasonMessage,
                                                    BigDecimal refundableAmount) {
        return ItemResultBo.builder()
                .orderItem(orderItem)
                .eligible(eligible)
                .reasonCode(reasonCode)
                .reasonMessage(reasonMessage)
                .refundableAmount(refundableAmount)
                .build();
    }

    /**
     * 计算订单级售后失败原因。
     *
     * @param order        订单实体
     * @param deadlineTime 售后截止时间
     * @return 返回订单级失败原因，若为空表示订单级校验通过
     */
    private FailureBo evaluateOrderFailure(MallOrder order, Date deadlineTime) {
        if (!Objects.equals(order.getPaid(), 1)) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_ORDER_NOT_PAID)
                    .reasonMessage("订单未支付，暂不支持申请售后")
                    .build();
        }

        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        if (orderStatusEnum == null) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_ORDER_STATUS_INVALID)
                    .reasonMessage("订单状态异常，暂不支持申请售后")
                    .build();
        }
        if (!isOrderStatusEligible(orderStatusEnum)) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_ORDER_STATUS_NOT_ELIGIBLE)
                    .reasonMessage(String.format("当前订单状态[%s]不允许申请售后", orderStatusEnum.getName()))
                    .build();
        }
        if (deadlineTime != null && deadlineTime.before(new Date())) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_AFTER_SALE_EXPIRED)
                    .reasonMessage("订单确认收货已超过3个月，无法申请售后")
                    .build();
        }
        return null;
    }

    /**
     * 计算整单申请场景下的失败原因。
     *
     * @param itemResults           商品维度资格结果
     * @param totalRefundableAmount 整单可退金额
     * @return 返回整单失败原因，若为空表示整单可申请
     */
    private FailureBo evaluateOrderScopeFailure(List<ItemResultBo> itemResults, BigDecimal totalRefundableAmount) {
        boolean hasInProgress = itemResults.stream()
                .anyMatch(item -> ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS.equals(item.getReasonCode()));
        if (hasInProgress) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS)
                    .reasonMessage("订单存在售后中的商品，暂不支持整单申请售后")
                    .build();
        }

        boolean hasHistoryConflict = itemResults.stream()
                .anyMatch(item -> ELIGIBILITY_REASON_HISTORY_CONFLICT.equals(item.getReasonCode()));
        if (hasHistoryConflict) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_HISTORY_CONFLICT)
                    .reasonMessage("订单包含已完成售后的商品，暂不支持整单再次申请")
                    .build();
        }

        boolean hasApplicableItems = itemResults.stream()
                .anyMatch(item -> item.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0
                        && !ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS.equals(item.getReasonCode())
                        && !ELIGIBILITY_REASON_HISTORY_CONFLICT.equals(item.getReasonCode()));
        if (totalRefundableAmount.compareTo(BigDecimal.ZERO) <= 0 || !hasApplicableItems) {
            return FailureBo.builder()
                    .reasonCode(ELIGIBILITY_REASON_NO_REFUNDABLE_AMOUNT)
                    .reasonMessage("订单已无可退款金额")
                    .build();
        }
        return null;
    }

    /**
     * 获取整单申请场景下实际可创建售后的商品列表。
     *
     * @param snapshot 售后资格快照
     * @return 返回可用于整单拆单创建售后的商品资格结果列表
     */
    private List<ItemResultBo> getApplicableOrderScopeItems(SnapshotBo snapshot) {
        List<ItemResultBo> results = new ArrayList<>();
        for (ItemResultBo itemResult : snapshot.getItemResults()) {
            if (itemResult.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0
                    && !ELIGIBILITY_REASON_AFTER_SALE_IN_PROGRESS.equals(itemResult.getReasonCode())
                    && !ELIGIBILITY_REASON_HISTORY_CONFLICT.equals(itemResult.getReasonCode())) {
                results.add(itemResult);
            }
        }
        if (results.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单已无可退款金额");
        }
        return results;
    }

    /**
     * 获取本次申请选中的订单项资格结果，若不存在则抛出异常。
     *
     * @param snapshot 售后资格快照
     * @return 返回选中的订单项资格结果
     */
    private ItemResultBo requireSelectedItem(SnapshotBo snapshot) {
        ItemResultBo selectedItem = resolveSelectedItem(snapshot.getItemResults(), snapshot.getSelectedOrderItemId());
        if (selectedItem == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单商品不存在");
        }
        return selectedItem;
    }

    /**
     * 按订单项 ID 从资格结果列表中查找目标商品。
     *
     * @param itemResults 商品资格结果列表
     * @param orderItemId 订单项 ID
     * @return 返回匹配的订单项资格结果，未找到则返回 {@code null}
     */
    private ItemResultBo resolveSelectedItem(List<ItemResultBo> itemResults, Long orderItemId) {
        if (orderItemId == null) {
            return null;
        }
        return itemResults.stream()
                .filter(item -> Objects.equals(item.getOrderItem().getId(), orderItemId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找首个满足售后资格的商品。
     *
     * @param itemResults 商品资格结果列表
     * @return 返回首个可申请的商品资格结果，未找到则返回 {@code null}
     */
    private ItemResultBo findFirstEligibleItem(List<ItemResultBo> itemResults) {
        return itemResults.stream()
                .filter(item -> Boolean.TRUE.equals(item.getEligible()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验单商品申请场景下的退款金额是否合法。
     *
     * @param refundAmount    前端传入的退款金额
     * @param maxRefundAmount 当前商品最大可退金额
     * @return 返回通过校验后的退款金额
     */
    private BigDecimal validateItemRefundAmount(BigDecimal refundAmount, BigDecimal maxRefundAmount) {
        if (refundAmount == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "退款金额不能为空");
        }
        if (refundAmount.compareTo(maxRefundAmount) > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("退款金额不能超过可退款金额%.2f元", maxRefundAmount));
        }
        return refundAmount;
    }

    /**
     * 根据资格快照构建统一的售后申请异常。
     *
     * @param snapshot 售后资格快照
     * @return 返回适用于当前失败场景的业务异常
     */
    private ServiceException buildEligibilityException(SnapshotBo snapshot) {
        ResponseCode responseCode = resolveResponseCode(snapshot.getReasonCode());
        return new ServiceException(responseCode, snapshot.getReasonMessage());
    }

    /**
     * 将资格结果编码映射为统一响应码。
     *
     * @param reasonCode 资格结果编码
     * @return 返回对应的响应码枚举
     */
    private ResponseCode resolveResponseCode(String reasonCode) {
        if (ELIGIBILITY_REASON_ORDER_NOT_FOUND.equals(reasonCode) || ELIGIBILITY_REASON_ORDER_ITEM_NOT_FOUND.equals(reasonCode)) {
            return ResponseCode.RESULT_IS_NULL;
        }
        return ResponseCode.OPERATION_ERROR;
    }

    /**
     * 根据订单状态推导售后申请中的收货状态。
     *
     * @param order 订单实体
     * @return 返回推导后的收货状态枚举
     */
    private ReceiveStatusEnum resolveReceiveStatus(MallOrder order) {
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        if (orderStatusEnum == OrderStatusEnum.COMPLETED) {
            return ReceiveStatusEnum.RECEIVED;
        }
        return ReceiveStatusEnum.NOT_RECEIVED;
    }

    /**
     * 创建或更新单个订单项对应的售后记录，并同步订单项、订单时间线状态。
     *
     * @param order              订单实体
     * @param orderItem          订单项实体
     * @param userId             当前用户 ID
     * @param afterSaleType      售后类型
     * @param refundAmount       退款金额
     * @param applyReason        申请原因
     * @param applyDescription   申请说明
     * @param evidenceImagesJson 凭证图片 JSON 字符串
     * @param receiveStatus      收货状态
     * @param now                当前时间
     * @return 返回售后单号
     */
    private String createOrUpdateAfterSale(MallOrder order,
                                           MallOrderItem orderItem,
                                           Long userId,
                                           AfterSaleTypeEnum afterSaleType,
                                           BigDecimal refundAmount,
                                           AfterSaleReasonEnum applyReason,
                                           String applyDescription,
                                           String evidenceImagesJson,
                                           ReceiveStatusEnum receiveStatus,
                                           Date now) {
        MallAfterSale existingAfterSale = lambdaQuery()
                .eq(MallAfterSale::getOrderId, order.getId())
                .eq(MallAfterSale::getOrderItemId, orderItem.getId())
                .eq(MallAfterSale::getUserId, userId)
                .orderByDesc(MallAfterSale::getId)
                .last("limit 1")
                .one();

        MallAfterSale afterSale;
        String afterSaleNo;
        if (existingAfterSale != null) {
            afterSale = existingAfterSale;
            afterSaleNo = existingAfterSale.getAfterSaleNo();
            afterSale.setAfterSaleType(afterSaleType.getType());
            afterSale.setAfterSaleStatus(AfterSaleStatusEnum.PENDING.getStatus());
            afterSale.setRefundAmount(refundAmount);
            afterSale.setApplyReason(applyReason.getReason());
            afterSale.setApplyDescription(applyDescription);
            afterSale.setEvidenceImages(evidenceImagesJson);
            afterSale.setReceiveStatus(receiveStatus.getStatus());
            afterSale.setApplyTime(now);
            afterSale.setUpdateTime(now);
            afterSale.setUpdateBy(getUsername());

            boolean updated = updateById(afterSale);
            if (!updated) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "申请售后失败，请稍后重试");
            }
        } else {
            afterSaleNo = generateAfterSaleNo();
            afterSale = MallAfterSale.builder()
                    .afterSaleNo(afterSaleNo)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .orderItemId(orderItem.getId())
                    .userId(userId)
                    .afterSaleType(afterSaleType.getType())
                    .afterSaleStatus(AfterSaleStatusEnum.PENDING.getStatus())
                    .refundAmount(refundAmount)
                    .applyReason(applyReason.getReason())
                    .applyDescription(applyDescription)
                    .evidenceImages(evidenceImagesJson)
                    .receiveStatus(receiveStatus.getStatus())
                    .applyTime(now)
                    .createTime(now)
                    .updateTime(now)
                    .createBy(getUsername())
                    .build();

            boolean saved = save(afterSale);
            if (!saved) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "申请售后失败，请稍后重试");
            }
        }

        orderItem.setAfterSaleStatus(OrderItemAfterSaleStatusEnum.IN_PROGRESS.getStatus());
        orderItem.setUpdateTime(now);
        mallOrderItemService.updateById(orderItem);

        markOrderAfterSaleInProgress(order, now);

        String username = getUsername();
        String description = String.format("用户%s申请%s", username, afterSaleType.getName());
        mallAfterSaleTimelineService.addTimeline(
                afterSale.getId(),
                OrderEventTypeEnum.AFTER_SALE_APPLIED.getType(),
                AfterSaleStatusEnum.PENDING.getStatus(),
                OperatorTypeEnum.USER.getType(),
                userId,
                description
        );

        OrderTimelineDto orderTimelineDto = OrderTimelineDto.builder()
                .orderId(order.getId())
                .eventType(OrderEventTypeEnum.AFTER_SALE_APPLIED.getType())
                .eventStatus(order.getOrderStatus())
                .operatorType(OperatorTypeEnum.USER.getType())
                .description(description)
                .build();
        mallOrderTimelineService.addTimelineIfNotExists(orderTimelineDto);

        log.info("用户{}申请售后成功，售后单号：{}", username, afterSaleNo);
        return afterSaleNo;
    }

    /**
     * 将凭证图片列表序列化为 JSON 字符串。
     *
     * @param evidenceImages 凭证图片列表
     * @return 返回 JSON 字符串，若为空则返回 {@code null}
     */
    private String buildEvidenceImagesJson(List<String> evidenceImages) {
        return evidenceImages != null && !evidenceImages.isEmpty() ? JSONUtils.toJson(evidenceImages) : null;
    }

    /**
     * 将内部资格快照转换为前端使用的资格视图对象。
     *
     * @param snapshot 售后资格快照
     * @return 返回前端售后资格视图对象
     */
    private AfterSaleEligibilityVo toEligibilityVo(SnapshotBo snapshot) {
        List<AfterSaleEligibilityVo.ItemEligibility> items = new ArrayList<>();
        for (ItemResultBo itemResult : snapshot.getItemResults()) {
            MallOrderItem orderItem = itemResult.getOrderItem();
            items.add(AfterSaleEligibilityVo.ItemEligibility.builder()
                    .orderItemId(orderItem.getId())
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .imageUrl(orderItem.getImageUrl())
                    .quantity(orderItem.getQuantity())
                    .price(orderItem.getPrice())
                    .totalPrice(orderItem.getTotalPrice())
                    .refundedAmount(defaultAmount(orderItem.getRefundedAmount()))
                    .refundableAmount(itemResult.getRefundableAmount())
                    .afterSaleStatus(resolveOrderItemAfterSaleStatus(orderItem))
                    .eligible(itemResult.getEligible())
                    .reasonCode(itemResult.getReasonCode())
                    .reasonMessage(itemResult.getReasonMessage())
                    .build());
        }
        return AfterSaleEligibilityVo.builder()
                .orderNo(snapshot.getOrderNo())
                .requestedScope(snapshot.getRequestedScope().getScope())
                .resolvedScope(snapshot.getResolvedScope().getScope())
                .eligible(snapshot.getEligible())
                .reasonCode(snapshot.getReasonCode())
                .reasonMessage(snapshot.getReasonMessage())
                .orderStatus(snapshot.getOrderStatus())
                .orderStatusName(snapshot.getOrderStatusName())
                .selectedOrderItemId(snapshot.getSelectedOrderItemId())
                .selectedRefundableAmount(snapshot.getSelectedRefundableAmount())
                .totalRefundableAmount(snapshot.getTotalRefundableAmount())
                .afterSaleDeadlineTime(snapshot.getAfterSaleDeadlineTime())
                .items(items)
                .build();
    }

    /**
     * 统一解析订单项售后状态，空值时默认为无售后。
     *
     * @param orderItem 订单项实体
     * @return 返回订单项售后状态编码
     */
    private String resolveOrderItemAfterSaleStatus(MallOrderItem orderItem) {
        return orderItem.getAfterSaleStatus() == null ? OrderItemAfterSaleStatusEnum.NONE.getStatus() : orderItem.getAfterSaleStatus();
    }

    /**
     * 组装售后详情视图对象。
     *
     * @param afterSale 售后实体
     * @return 返回售后详情视图对象
     */
    private AfterSaleDetailVo buildAfterSaleDetail(MallAfterSale afterSale) {
        User user = userService.getById(afterSale.getUserId());
        MallOrderItem orderItem = mallOrderItemService.getById(afterSale.getOrderItemId());

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

        List<AfterSaleTimelineVo> timeline = mallAfterSaleTimelineService.getTimelineList(afterSale.getId());

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

    /**
     * 生成新的售后单号。
     *
     * @return 返回售后单号字符串
     */
    private String generateAfterSaleNo() {
        String prefix = "AS";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", (int) (Math.random() * 1000000));
        return prefix + datePart + randomPart;
    }

    /**
     * 根据订单项售后状态刷新订单上的售后标记。
     *
     * @param orderId 订单 ID
     */
    private void refreshOrderAfterSaleFlag(Long orderId, Date now) {
        if (orderId == null) {
            return;
        }
        MallOrder order = mallOrderService.getById(orderId);
        if (order == null) {
            return;
        }

        OrderItemAfterSaleStatusEnum newFlag = resolveOrderAfterSaleFlag(orderId);
        order.setAfterSaleFlag(newFlag);
        if (newFlag == OrderItemAfterSaleStatusEnum.IN_PROGRESS) {
            order.setOrderStatus(OrderStatusEnum.AFTER_SALE.getType());
        } else {
            order.setOrderStatus(resolveOrderStatusAfterAfterSale(order).getType());
        }
        order.setUpdateTime(now);
        boolean updated = mallOrderService.updateById(order);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "刷新订单售后状态失败，请稍后重试");
        }
    }

    /**
     * 将订单标记为售后处理中。
     *
     * @param order 订单实体
     * @param now   当前时间
     */
    private void markOrderAfterSaleInProgress(MallOrder order, Date now) {
        order.setAfterSaleFlag(OrderItemAfterSaleStatusEnum.IN_PROGRESS);
        order.setOrderStatus(OrderStatusEnum.AFTER_SALE.getType());
        order.setUpdateTime(now);
        boolean updated = mallOrderService.updateById(order);
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "更新订单售后状态失败，请稍后重试");
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
            OrderItemAfterSaleStatusEnum afterSaleStatusEnum = resolveOrderItemStatus(orderItem.getAfterSaleStatus());
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
        if (isOrderFullyRefunded(order)) {
            return OrderStatusEnum.REFUNDED;
        }
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
     * 判断订单状态是否允许发起售后。
     *
     * @param orderStatusEnum 订单状态枚举
     * @return 返回是否允许售后
     */
    private boolean isOrderStatusEligible(OrderStatusEnum orderStatusEnum) {
        return orderStatusEnum == OrderStatusEnum.PENDING_SHIPMENT
                || orderStatusEnum == OrderStatusEnum.PENDING_RECEIPT
                || orderStatusEnum == OrderStatusEnum.COMPLETED;
    }

    /**
     * 计算整单当前可退款金额。
     *
     * @param order 订单实体
     * @return 返回整单可退款金额
     */
    private BigDecimal calculateOrderRefundableAmount(MallOrder order) {
        BigDecimal payAmount = defaultAmount(order.getPayAmount());
        BigDecimal refundedAmount = defaultAmount(order.getRefundPrice());
        return payAmount.subtract(refundedAmount);
    }

    /**
     * 计算单个订单项当前可退款金额。
     *
     * @param orderItem 订单项实体
     * @return 返回订单项可退款金额
     */
    private BigDecimal calculateOrderItemRefundableAmount(MallOrderItem orderItem) {
        BigDecimal totalPrice = defaultAmount(orderItem.getTotalPrice());
        BigDecimal refundedAmount = defaultAmount(orderItem.getRefundedAmount());
        return totalPrice.subtract(refundedAmount);
    }

    /**
     * 判断指定订单或订单项是否存在进行中的售后记录。
     *
     * @param orderId     订单 ID
     * @param orderItemId 订单项 ID，可为空
     * @param userId      当前用户 ID
     * @return 返回是否存在进行中的售后
     */
    private boolean hasActiveAfterSale(Long orderId, Long orderItemId, Long userId) {
        var query = lambdaQuery()
                .eq(MallAfterSale::getOrderId, orderId)
                .eq(MallAfterSale::getUserId, userId)
                .in(MallAfterSale::getAfterSaleStatus,
                        AfterSaleStatusEnum.PENDING.getStatus(),
                        AfterSaleStatusEnum.APPROVED.getStatus(),
                        AfterSaleStatusEnum.PROCESSING.getStatus());
        if (orderItemId != null) {
            query.eq(MallAfterSale::getOrderItemId, orderItemId);
        }
        return query.count() > 0;
    }

    /**
     * 将订单项售后状态编码转换为枚举，空值时默认无售后。
     *
     * @param afterSaleStatus 订单项售后状态编码
     * @return 返回订单项售后状态枚举
     */
    private OrderItemAfterSaleStatusEnum resolveOrderItemStatus(String afterSaleStatus) {
        String safeStatus = afterSaleStatus == null ? OrderItemAfterSaleStatusEnum.NONE.getStatus() : afterSaleStatus;
        return OrderItemAfterSaleStatusEnum.fromCode(safeStatus);
    }

    /**
     * 根据订单状态编码解析状态名称。
     *
     * @param orderStatus 订单状态编码
     * @return 返回订单状态名称，未匹配时返回“未知”
     */
    private String resolveOrderStatusName(String orderStatus) {
        OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromCode(orderStatus);
        return orderStatusEnum == null ? "未知" : orderStatusEnum.getName();
    }

    /**
     * 计算订单的售后截止时间。
     *
     * @param order 订单实体
     * @return 返回售后截止时间，若当前订单不受售后期限限制则返回 {@code null}
     */
    private Date resolveAfterSaleDeadline(MallOrder order) {
        if (order == null || !Objects.equals(order.getOrderStatus(), OrderStatusEnum.COMPLETED.getType())) {
            return null;
        }
        Date anchor = order.getReceiveTime() != null ? order.getReceiveTime() : order.getFinishTime();
        if (anchor == null) {
            return null;
        }
        LocalDateTime deadline = LocalDateTime.ofInstant(anchor.toInstant(), ZoneId.systemDefault())
                .plusMonths(AFTER_SALE_VALID_MONTHS);
        return Date.from(deadline.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将金额归一化为不小于 0 的数值。
     *
     * @param amount 原始金额
     * @return 返回归一化后的金额
     */
    private BigDecimal positive(BigDecimal amount) {
        BigDecimal safeAmount = defaultAmount(amount);
        return safeAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : safeAmount;
    }

    /**
     * 将空金额转换为 0。
     *
     * @param amount 原始金额
     * @return 返回非空金额
     */
    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
