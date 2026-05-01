package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleTimelineService;
import com.zhangyichuang.medicine.admin.service.MallOrderItemService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleTimelineDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallAfterSaleTimeline;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.AfterSaleReasonEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleTypeEnum;
import com.zhangyichuang.medicine.model.enums.OperatorTypeEnum;
import com.zhangyichuang.medicine.model.enums.OrderEventTypeEnum;
import com.zhangyichuang.medicine.model.enums.ReceiveStatusEnum;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAfterSaleRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 Agent 售后 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentAfterSaleRpcService.class, group = "medicine-admin", version = "1.0.0")
@RequiredArgsConstructor
public class AdminAgentAfterSaleRpcServiceImpl implements AdminAgentAfterSaleRpcService {

    /**
     * AI Context 工具单次批量查询上限。
     */
    private static final int CONTEXT_BATCH_LIMIT = 20;

    /**
     * AI Context 售后时间线最多返回节点数。
     */
    private static final int CONTEXT_TIMELINE_LIMIT = 5;

    private final MallAfterSaleService mallAfterSaleService;
    private final MallAfterSaleTimelineService mallAfterSaleTimelineService;
    private final MallOrderItemService mallOrderItemService;
    private final UserService userService;

    /**
     * 功能描述：提供给 Agent 端的售后列表分页查询能力。
     *
     * @param query 售后分页查询参数，包含分页参数与筛选条件
     * @return 返回售后分页结果，记录类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当管理端售后服务查询异常时抛出运行时异常
     */
    @Override
    public Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest query) {
        AfterSaleListRequest afterSaleListRequest = BeanCotyUtils.copyProperties(query, AfterSaleListRequest.class);
        return mallAfterSaleService.getAfterSaleList(afterSaleListRequest);
    }

    /**
     * 功能描述：根据售后单号批量查询售后详情并返回给 Agent。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回售后详情 DTO 列表
     * @throws RuntimeException 异常说明：当售后详情查询异常时抛出运行时异常
     */
    @Override
    public List<AfterSaleDetailDto> getAfterSaleDetailsByAfterSaleNos(List<String> afterSaleNos) {
        Assert.notEmpty(afterSaleNos, "售后单号不能为空");
        List<String> normalizedAfterSaleNos = normalizeAfterSaleNos(afterSaleNos);
        Assert.notEmpty(normalizedAfterSaleNos, "售后单号不能为空");
        Map<String, MallAfterSale> afterSaleMap = loadAfterSaleMap(normalizedAfterSaleNos);
        BatchAfterSaleContext batchContext = loadBatchAfterSaleContext(afterSaleMap);
        return normalizedAfterSaleNos.stream()
                .map(afterSaleNo -> {
                    MallAfterSale afterSale = afterSaleMap.get(afterSaleNo);
                    Assert.notNull(afterSale, "售后单不存在: " + afterSaleNo);
                    return buildAfterSaleDetailDto(afterSale, batchContext);
                })
                .toList();
    }

    /**
     * 功能描述：根据售后单号批量查询智能体售后上下文。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回按售后单号分组的售后上下文
     * @throws RuntimeException 异常说明：当售后单号为空、超过上限或不存在时抛出运行时异常
     */
    @Override
    public Map<String, AfterSaleContextDto> getAfterSaleContextsByAfterSaleNos(List<String> afterSaleNos) {
        validateContextAfterSaleNos(afterSaleNos);
        List<AfterSaleDetailDto> details = getAfterSaleDetailsByAfterSaleNos(afterSaleNos);
        Map<String, AfterSaleContextDto> result = new LinkedHashMap<>();
        for (AfterSaleDetailDto detail : details) {
            result.put(detail.getAfterSaleNo(), buildAfterSaleContext(detail));
        }
        return result;
    }

    /**
     * 归一化售后单号列表。
     *
     * @param afterSaleNos 原始售后单号列表
     * @return 去空、去重后的售后单号列表
     */
    private List<String> normalizeAfterSaleNos(List<String> afterSaleNos) {
        return afterSaleNos.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 批量加载售后实体并按售后单号索引。
     *
     * @param afterSaleNos 售后单号列表
     * @return 售后单号到售后实体的映射
     */
    private Map<String, MallAfterSale> loadAfterSaleMap(List<String> afterSaleNos) {
        List<MallAfterSale> afterSales = mallAfterSaleService.lambdaQuery()
                .in(MallAfterSale::getAfterSaleNo, afterSaleNos)
                .list();
        if (CollectionUtils.isEmpty(afterSales)) {
            return Map.of();
        }
        return afterSales.stream()
                .filter(afterSale -> StringUtils.hasText(afterSale.getAfterSaleNo()))
                .collect(Collectors.toMap(MallAfterSale::getAfterSaleNo, afterSale -> afterSale,
                        (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载售后详情依赖数据。
     *
     * @param afterSaleMap 售后实体映射
     * @return 售后批量上下文
     */
    private BatchAfterSaleContext loadBatchAfterSaleContext(Map<String, MallAfterSale> afterSaleMap) {
        if (afterSaleMap.isEmpty()) {
            return new BatchAfterSaleContext(Map.of(), Map.of(), Map.of());
        }
        List<Long> userIds = afterSaleMap.values().stream()
                .map(MallAfterSale::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> orderItemIds = afterSaleMap.values().stream()
                .map(MallAfterSale::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> afterSaleIds = afterSaleMap.values().stream()
                .map(MallAfterSale::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return new BatchAfterSaleContext(
                loadUserMap(userIds),
                loadOrderItemMap(orderItemIds),
                loadTimelineMap(afterSaleIds)
        );
    }

    /**
     * 批量加载用户并按用户 ID 索引。
     *
     * @param userIds 用户 ID 列表
     * @return 用户 ID 到用户实体的映射
     */
    private Map<Long, User> loadUserMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }
        List<User> users = userService.lambdaQuery()
                .in(User::getId, userIds)
                .list();
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载订单项并按订单项 ID 索引。
     *
     * @param orderItemIds 订单项 ID 列表
     * @return 订单项 ID 到订单项实体的映射
     */
    private Map<Long, MallOrderItem> loadOrderItemMap(List<Long> orderItemIds) {
        if (CollectionUtils.isEmpty(orderItemIds)) {
            return Map.of();
        }
        List<MallOrderItem> orderItems = mallOrderItemService.lambdaQuery()
                .in(MallOrderItem::getId, orderItemIds)
                .list();
        if (CollectionUtils.isEmpty(orderItems)) {
            return Map.of();
        }
        return orderItems.stream()
                .filter(orderItem -> orderItem.getId() != null)
                .collect(Collectors.toMap(MallOrderItem::getId, orderItem -> orderItem,
                        (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载售后时间线并按售后 ID 分组。
     *
     * @param afterSaleIds 售后 ID 列表
     * @return 售后 ID 到时间线 DTO 列表的映射
     */
    private Map<Long, List<AfterSaleTimelineDto>> loadTimelineMap(List<Long> afterSaleIds) {
        if (CollectionUtils.isEmpty(afterSaleIds)) {
            return Map.of();
        }
        List<MallAfterSaleTimeline> timelines = mallAfterSaleTimelineService.lambdaQuery()
                .in(MallAfterSaleTimeline::getAfterSaleId, afterSaleIds)
                .orderByDesc(MallAfterSaleTimeline::getCreateTime)
                .list();
        if (CollectionUtils.isEmpty(timelines)) {
            return Map.of();
        }
        return timelines.stream()
                .filter(timeline -> timeline.getAfterSaleId() != null)
                .collect(Collectors.groupingBy(MallAfterSaleTimeline::getAfterSaleId, LinkedHashMap::new,
                        Collectors.mapping(this::toTimelineDto, Collectors.toList())));
    }

    /**
     * 构建售后详情 DTO。
     *
     * @param afterSale    售后实体
     * @param batchContext 批量查询上下文
     * @return 售后详情 DTO
     */
    private AfterSaleDetailDto buildAfterSaleDetailDto(MallAfterSale afterSale, BatchAfterSaleContext batchContext) {
        User user = batchContext.userMap().get(afterSale.getUserId());
        MallOrderItem orderItem = batchContext.orderItemMap().get(afterSale.getOrderItemId());
        AfterSaleTypeEnum typeEnum = AfterSaleTypeEnum.fromCode(afterSale.getAfterSaleType());
        AfterSaleStatusEnum statusEnum = AfterSaleStatusEnum.fromCode(afterSale.getAfterSaleStatus());
        AfterSaleReasonEnum reasonEnum = AfterSaleReasonEnum.fromCode(afterSale.getApplyReason());
        ReceiveStatusEnum receiveStatusEnum = ReceiveStatusEnum.fromCode(afterSale.getReceiveStatus());
        return AfterSaleDetailDto.builder()
                .id(afterSale.getId())
                .afterSaleNo(afterSale.getAfterSaleNo())
                .orderId(afterSale.getOrderId())
                .orderNo(afterSale.getOrderNo())
                .orderItemId(afterSale.getOrderItemId())
                .userId(afterSale.getUserId())
                .userNickname(user == null ? "未知" : user.getNickname())
                .afterSaleType(typeEnum == null ? null : typeEnum.getType())
                .afterSaleTypeName(typeEnum == null ? "未知" : typeEnum.getName())
                .afterSaleStatus(statusEnum == null ? null : statusEnum.getStatus())
                .afterSaleStatusName(statusEnum == null ? "未知" : statusEnum.getName())
                .refundAmount(afterSale.getRefundAmount())
                .applyReason(reasonEnum == null ? null : reasonEnum.getReason())
                .applyReasonName(reasonEnum == null ? "未知" : reasonEnum.getName())
                .applyDescription(afterSale.getApplyDescription())
                .evidenceImages(parseEvidenceImages(afterSale.getEvidenceImages()))
                .receiveStatus(receiveStatusEnum == null ? null : receiveStatusEnum.getStatus())
                .receiveStatusName(receiveStatusEnum == null ? "未知" : receiveStatusEnum.getName())
                .rejectReason(afterSale.getRejectReason())
                .adminRemark(afterSale.getAdminRemark())
                .applyTime(afterSale.getApplyTime())
                .auditTime(afterSale.getAuditTime())
                .completeTime(afterSale.getCompleteTime())
                .productInfo(toProductInfoDto(orderItem))
                .timeline(batchContext.timelineMap().getOrDefault(afterSale.getId(), List.of()))
                .build();
    }

    /**
     * 解析售后凭证图片。
     *
     * @param evidenceImages 售后凭证 JSON
     * @return 凭证图片列表
     */
    private List<String> parseEvidenceImages(String evidenceImages) {
        if (!StringUtils.hasText(evidenceImages)) {
            return null;
        }
        return JSONUtils.parseStringList(evidenceImages);
    }

    /**
     * 将订单项转换为售后商品 DTO。
     *
     * @param orderItem 订单项实体
     * @return 售后商品 DTO
     */
    private AfterSaleDetailDto.ProductInfo toProductInfoDto(MallOrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        return AfterSaleDetailDto.ProductInfo.builder()
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .productImage(orderItem.getImageUrl())
                .productPrice(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }

    /**
     * 将售后时间线实体转换为 DTO。
     *
     * @param timeline 售后时间线实体
     * @return 售后时间线 DTO
     */
    private AfterSaleTimelineDto toTimelineDto(MallAfterSaleTimeline timeline) {
        OrderEventTypeEnum eventTypeEnum = OrderEventTypeEnum.fromCode(timeline.getEventType());
        OperatorTypeEnum operatorTypeEnum = OperatorTypeEnum.fromCode(timeline.getOperatorType());
        return AfterSaleTimelineDto.builder()
                .id(timeline.getId())
                .eventType(timeline.getEventType())
                .eventTypeName(eventTypeEnum == null ? "未知" : eventTypeEnum.getName())
                .eventStatus(timeline.getEventStatus())
                .operatorType(timeline.getOperatorType())
                .operatorTypeName(operatorTypeEnum == null ? "未知" : operatorTypeEnum.getName())
                .description(timeline.getDescription())
                .createTime(timeline.getCreateTime())
                .build();
    }

    /**
     * 功能描述：校验售后 Context 批量查询参数。
     *
     * @param afterSaleNos 售后单号列表
     * @throws RuntimeException 异常说明：当售后单号为空或超过上限时抛出运行时异常
     */
    private void validateContextAfterSaleNos(List<String> afterSaleNos) {
        Assert.notEmpty(afterSaleNos, "售后单号不能为空");
        Assert.isParamTrue(afterSaleNos.size() <= CONTEXT_BATCH_LIMIT, "售后单号最多支持20个");
    }

    /**
     * 功能描述：构建单个售后单的智能体上下文。
     *
     * @param detail 售后详情 DTO
     * @return 返回售后智能体上下文
     * @throws RuntimeException 异常说明：当售后详情为空时抛出运行时异常
     */
    private AfterSaleContextDto buildAfterSaleContext(AfterSaleDetailDto detail) {
        Assert.notNull(detail, "售后详情不能为空");
        AfterSaleStatusEnum statusEnum = AfterSaleStatusEnum.fromCode(detail.getAfterSaleStatus());
        return AfterSaleContextDto.builder()
                .afterSaleNo(detail.getAfterSaleNo())
                .orderNo(detail.getOrderNo())
                .statusCode(detail.getAfterSaleStatus())
                .statusText(detail.getAfterSaleStatusName())
                .typeCode(detail.getAfterSaleType())
                .typeText(detail.getAfterSaleTypeName())
                .refundAmount(detail.getRefundAmount())
                .reasonText(detail.getApplyReasonName())
                .productSummary(buildProductSummary(detail.getProductInfo()))
                .evidenceSummary(buildEvidenceSummary(detail.getEvidenceImages()))
                .timelineSummary(buildTimelineSummary(detail.getTimeline()))
                .aiHints(buildAfterSaleAiHints(statusEnum))
                .build();
    }

    /**
     * 功能描述：构建售后商品摘要。
     *
     * @param productInfo 售后商品信息 DTO
     * @return 返回售后商品摘要
     */
    private AfterSaleContextDto.ProductSummary buildProductSummary(AfterSaleDetailDto.ProductInfo productInfo) {
        if (productInfo == null) {
            return null;
        }
        return AfterSaleContextDto.ProductSummary.builder()
                .productId(productInfo.getProductId())
                .productName(productInfo.getProductName())
                .productImage(productInfo.getProductImage())
                .quantity(productInfo.getQuantity())
                .totalPrice(productInfo.getTotalPrice())
                .build();
    }

    /**
     * 功能描述：构建售后凭证摘要。
     *
     * @param evidenceImages 售后凭证图片列表
     * @return 返回凭证数量与首图摘要
     */
    private AfterSaleContextDto.EvidenceSummary buildEvidenceSummary(List<String> evidenceImages) {
        int evidenceCount = CollectionUtils.isEmpty(evidenceImages) ? 0 : evidenceImages.size();
        String firstEvidenceImage = CollectionUtils.isEmpty(evidenceImages) ? null : evidenceImages.get(0);
        return AfterSaleContextDto.EvidenceSummary.builder()
                .evidenceCount(evidenceCount)
                .firstEvidenceImage(firstEvidenceImage)
                .build();
    }

    /**
     * 功能描述：构建最近售后处理时间线摘要。
     *
     * @param timeline 售后时间线 DTO 列表
     * @return 返回最多 5 条最近处理节点
     */
    private List<AfterSaleContextDto.TimelineItem> buildTimelineSummary(List<AfterSaleTimelineDto> timeline) {
        if (CollectionUtils.isEmpty(timeline)) {
            return List.of();
        }
        return timeline.stream()
                .limit(CONTEXT_TIMELINE_LIMIT)
                .map(item -> AfterSaleContextDto.TimelineItem.builder()
                        .eventType(item.getEventType())
                        .eventTypeName(item.getEventTypeName())
                        .eventStatus(item.getEventStatus())
                        .operatorType(item.getOperatorType())
                        .operatorTypeName(item.getOperatorTypeName())
                        .description(item.getDescription())
                        .eventTime(item.getCreateTime())
                        .build())
                .toList();
    }

    /**
     * 功能描述：构建售后智能体决策提示。
     *
     * @param statusEnum 售后状态枚举
     * @return 返回智能体决策提示
     */
    private AfterSaleContextDto.AiHints buildAfterSaleAiHints(AfterSaleStatusEnum statusEnum) {
        return AfterSaleContextDto.AiHints.builder()
                .waitingAudit(statusEnum == AfterSaleStatusEnum.PENDING)
                .processing(statusEnum == AfterSaleStatusEnum.APPROVED || statusEnum == AfterSaleStatusEnum.PROCESSING)
                .completed(statusEnum == AfterSaleStatusEnum.COMPLETED)
                .rejected(statusEnum == AfterSaleStatusEnum.REJECTED)
                .canCancel(statusEnum == AfterSaleStatusEnum.PENDING || statusEnum == AfterSaleStatusEnum.APPROVED)
                .canReapply(statusEnum == AfterSaleStatusEnum.REJECTED || statusEnum == AfterSaleStatusEnum.CANCELLED)
                .build();
    }

    /**
     * 售后批量查询上下文。
     *
     * @param userMap      用户映射
     * @param orderItemMap 订单项映射
     * @param timelineMap  售后时间线映射
     */
    private record BatchAfterSaleContext(Map<Long, User> userMap,
                                         Map<Long, MallOrderItem> orderItemMap,
                                         Map<Long, List<AfterSaleTimelineDto>> timelineMap) {
    }
}
