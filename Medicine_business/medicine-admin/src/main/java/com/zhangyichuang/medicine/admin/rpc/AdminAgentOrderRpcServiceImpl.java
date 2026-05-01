package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.admin.service.MallOrderShippingService;
import com.zhangyichuang.medicine.admin.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderShipping;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import com.zhangyichuang.medicine.model.enums.ShippingStatusEnum;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentOrderRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端 Agent 订单 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentOrderRpcService.class, group = "medicine-admin", version = "1.0.0", timeout = 10000)
@RequiredArgsConstructor
public class AdminAgentOrderRpcServiceImpl implements AdminAgentOrderRpcService {

    /**
     * AI Context 工具单次批量查询上限。
     */
    private static final int CONTEXT_BATCH_LIMIT = 20;

    /**
     * AI Context 商品名称摘要最多返回数量。
     */
    private static final int PRODUCT_NAME_SUMMARY_LIMIT = 3;

    private final MallOrderService mallOrderService;
    private final MallOrderTimelineService mallOrderTimelineService;
    private final MallOrderShippingService mallOrderShippingService;
    private final MallAfterSaleService mallAfterSaleService;

    @Override
    public PageResult<OrderWithProductDto> listOrders(MallOrderListRequest query) {
        MallOrderListRequest request = query == null ? new MallOrderListRequest() : query;
        Page<OrderWithProductDto> page = mallOrderService.orderWithProduct(request);
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<OrderDetailDto> getOrderDetailsByOrderNos(List<String> orderNos) {
        return mallOrderService.getOrderByOrderNo(orderNos);
    }

    /**
     * 根据订单编号批量查询智能体订单上下文。
     *
     * @param orderNos 订单编号列表
     * @return 按订单编号分组的订单上下文
     */
    @Override
    public Map<String, OrderContextDto> getOrderContextsByOrderNos(List<String> orderNos) {
        validateContextOrderNos(orderNos);
        List<String> normalizedOrderNos = normalizeOrderNos(orderNos);
        Assert.notEmpty(normalizedOrderNos, "订单编号不能为空");
        Map<String, MallOrder> orderMap = loadOrderMap(normalizedOrderNos);
        Map<String, OrderDetailDto> detailMap = loadOrderDetailMap(normalizedOrderNos);
        List<Long> orderIds = orderMap.values().stream()
                .map(MallOrder::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<MallOrderTimeline>> timelineMap = loadTimelineMap(orderIds);
        Map<Long, MallOrderShipping> shippingMap = loadShippingMap(orderIds);
        Set<String> afterSaleOrderNos = loadAfterSaleOrderNos(normalizedOrderNos);

        Map<String, OrderContextDto> result = new LinkedHashMap<>();
        for (String orderNo : normalizedOrderNos) {
            MallOrder order = orderMap.get(orderNo);
            Assert.notNull(order, "订单不存在: " + orderNo);
            result.put(orderNo, buildOrderContext(
                    order,
                    detailMap.get(orderNo),
                    timelineMap.getOrDefault(order.getId(), List.of()),
                    shippingMap.get(order.getId()),
                    afterSaleOrderNos.contains(orderNo)
            ));
        }
        return result;
    }

    /**
     * 归一化订单编号列表。
     *
     * @param orderNos 原始订单编号列表
     * @return 去空、去重后的订单编号列表
     */
    private List<String> normalizeOrderNos(List<String> orderNos) {
        return orderNos.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 校验订单 Context 批量查询参数。
     *
     * @param orderNos 订单编号列表
     */
    private void validateContextOrderNos(List<String> orderNos) {
        Assert.notEmpty(orderNos, "订单编号不能为空");
        Assert.isParamTrue(orderNos.size() <= CONTEXT_BATCH_LIMIT, "订单编号最多支持20个");
    }

    /**
     * 批量加载订单实体并按订单编号索引。
     *
     * @param orderNos 订单编号列表
     * @return 订单编号到订单实体的映射
     */
    private Map<String, MallOrder> loadOrderMap(List<String> orderNos) {
        List<MallOrder> orders = mallOrderService.lambdaQuery()
                .in(MallOrder::getOrderNo, orderNos)
                .list();
        if (CollectionUtils.isEmpty(orders)) {
            return Map.of();
        }
        return orders.stream()
                .filter(order -> StringUtils.hasText(order.getOrderNo()))
                .collect(Collectors.toMap(MallOrder::getOrderNo, order -> order, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载订单详情并按订单编号索引。
     *
     * @param orderNos 订单编号列表
     * @return 订单编号到订单详情 DTO 的映射
     */
    private Map<String, OrderDetailDto> loadOrderDetailMap(List<String> orderNos) {
        List<OrderDetailDto> details = mallOrderService.getOrderByOrderNo(orderNos);
        if (CollectionUtils.isEmpty(details)) {
            return Map.of();
        }
        return details.stream()
                .filter(detail -> detail.getOrderInfo() != null)
                .filter(detail -> StringUtils.hasText(detail.getOrderInfo().getOrderNo()))
                .collect(Collectors.toMap(detail -> detail.getOrderInfo().getOrderNo(), detail -> detail,
                        (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载订单时间线并按订单 ID 分组。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到时间线列表的映射
     */
    private Map<Long, List<MallOrderTimeline>> loadTimelineMap(List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Map.of();
        }
        List<MallOrderTimeline> timelines = mallOrderTimelineService.lambdaQuery()
                .in(MallOrderTimeline::getOrderId, orderIds)
                .orderByDesc(MallOrderTimeline::getCreatedTime)
                .list();
        if (CollectionUtils.isEmpty(timelines)) {
            return Map.of();
        }
        return timelines.stream()
                .filter(timeline -> timeline.getOrderId() != null)
                .collect(Collectors.groupingBy(MallOrderTimeline::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 批量加载订单物流并按订单 ID 索引。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到物流记录的映射
     */
    private Map<Long, MallOrderShipping> loadShippingMap(List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Map.of();
        }
        List<MallOrderShipping> shippings = mallOrderShippingService.lambdaQuery()
                .in(MallOrderShipping::getOrderId, orderIds)
                .list();
        if (CollectionUtils.isEmpty(shippings)) {
            return Map.of();
        }
        return shippings.stream()
                .filter(shipping -> shipping.getOrderId() != null)
                .collect(Collectors.toMap(MallOrderShipping::getOrderId, shipping -> shipping,
                        (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载存在售后的订单编号。
     *
     * @param orderNos 订单编号列表
     * @return 已存在售后记录的订单编号集合
     */
    private Set<String> loadAfterSaleOrderNos(List<String> orderNos) {
        List<MallAfterSale> afterSales = mallAfterSaleService.lambdaQuery()
                .in(MallAfterSale::getOrderNo, orderNos)
                .list();
        if (CollectionUtils.isEmpty(afterSales)) {
            return Set.of();
        }
        return afterSales.stream()
                .map(MallAfterSale::getOrderNo)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    /**
     * 构建单个订单的智能体上下文。
     *
     * @param order        订单实体
     * @param detail       订单详情 DTO
     * @param timeline     订单时间线列表
     * @param shipping     订单物流记录
     * @param hasAfterSale 是否存在售后
     * @return 订单智能体上下文
     */
    private OrderContextDto buildOrderContext(MallOrder order,
                                              OrderDetailDto detail,
                                              List<MallOrderTimeline> timeline,
                                              MallOrderShipping shipping,
                                              boolean hasAfterSale) {
        OrderStatusEnum statusEnum = OrderStatusEnum.fromCode(order.getOrderStatus());
        return OrderContextDto.builder()
                .orderNo(order.getOrderNo())
                .statusCode(order.getOrderStatus())
                .statusText(statusEnum != null ? statusEnum.getName() : null)
                .payType(order.getPayType())
                .amountSummary(buildAmountSummary(order))
                .receiverSummary(buildReceiverSummary(order, detail))
                .productSummary(buildProductSummary(detail, hasAfterSale))
                .shippingSummary(buildShippingSummary(shipping))
                .timeline(buildTimeline(timeline))
                .aiHints(buildOrderAiHints(statusEnum, hasAfterSale))
                .build();
    }

    /**
     * 构建订单金额摘要。
     *
     * @param order 订单实体
     * @return 订单金额摘要
     */
    private OrderContextDto.AmountSummary buildAmountSummary(MallOrder order) {
        return OrderContextDto.AmountSummary.builder()
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .freightAmount(order.getFreightAmount())
                .build();
    }

    /**
     * 构建订单收货摘要。
     *
     * @param order  订单实体
     * @param detail 订单详情 DTO
     * @return 订单收货摘要
     */
    private OrderContextDto.ReceiverSummary buildReceiverSummary(MallOrder order, OrderDetailDto detail) {
        OrderDetailDto.DeliveryInfo deliveryInfo = detail == null ? null : detail.getDeliveryInfo();
        return OrderContextDto.ReceiverSummary.builder()
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .deliveryMethod(deliveryInfo == null ? order.getDeliveryType() : deliveryInfo.getDeliveryMethod())
                .receiverAddress(order.getReceiverDetail())
                .build();
    }

    /**
     * 构建订单商品摘要。
     *
     * @param detail       订单详情 DTO
     * @param hasAfterSale 是否存在售后
     * @return 订单商品摘要
     */
    private OrderContextDto.ProductSummary buildProductSummary(OrderDetailDto detail, boolean hasAfterSale) {
        List<OrderDetailDto.ProductInfo> products = detail == null ? List.of() : detail.getProductInfo();
        int productLineCount = CollectionUtils.isEmpty(products) ? 0 : products.size();
        int productCount = CollectionUtils.isEmpty(products) ? 0 : products.stream()
                .map(OrderDetailDto.ProductInfo::getProductQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<String> topProductNames = CollectionUtils.isEmpty(products) ? List.of() : products.stream()
                .map(OrderDetailDto.ProductInfo::getProductName)
                .filter(Objects::nonNull)
                .limit(PRODUCT_NAME_SUMMARY_LIMIT)
                .toList();
        return OrderContextDto.ProductSummary.builder()
                .productCount(productCount)
                .productLineCount(productLineCount)
                .topProductNames(topProductNames)
                .hasAfterSale(hasAfterSale)
                .build();
    }

    /**
     * 构建订单物流摘要。
     *
     * @param shipping 订单物流记录
     * @return 订单物流摘要
     */
    private OrderContextDto.ShippingSummary buildShippingSummary(MallOrderShipping shipping) {
        boolean shipped = shipping != null && shipping.getDeliverTime() != null;
        ShippingStatusEnum statusEnum = shipping == null ? null : ShippingStatusEnum.fromCode(shipping.getStatus());
        return OrderContextDto.ShippingSummary.builder()
                .shipped(shipped)
                .logisticsCompany(shipping == null ? null : shipping.getShippingCompany())
                .trackingNumber(shipping == null ? null : shipping.getShippingNo())
                .shipmentNote(shipping == null ? null : shipping.getShipmentNote())
                .statusCode(shipping == null ? null : shipping.getStatus())
                .statusText(statusEnum == null ? null : statusEnum.getName())
                .deliverTime(shipping == null ? null : shipping.getDeliverTime())
                .receiveTime(shipping == null ? null : shipping.getReceiveTime())
                .nodes(buildShippingNodes(shipping == null ? null : shipping.getShippingInfo()))
                .build();
    }

    /**
     * 构建完整物流节点列表。
     *
     * @param shippingInfo 物流轨迹 JSON 字符串
     * @return 完整物流节点列表
     */
    private List<OrderContextDto.ShippingNodeSummary> buildShippingNodes(String shippingInfo) {
        if (!StringUtils.hasText(shippingInfo)) {
            return List.of();
        }
        JsonElement element = JSONUtils.parseLenient(shippingInfo);
        JsonArray nodeArray = extractShippingNodeArray(element);
        if (nodeArray == null) {
            return List.of();
        }
        List<OrderContextDto.ShippingNodeSummary> nodes = new ArrayList<>();
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
            nodes.add(OrderContextDto.ShippingNodeSummary.builder()
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

    /**
     * 构建订单完整时间线。
     *
     * @param timeline 订单时间线列表
     * @return 订单完整时间线
     */
    private List<OrderContextDto.TimelineItem> buildTimeline(List<MallOrderTimeline> timeline) {
        if (CollectionUtils.isEmpty(timeline)) {
            return List.of();
        }
        return timeline.stream()
                .map(item -> OrderContextDto.TimelineItem.builder()
                        .id(item.getId())
                        .eventType(item.getEventType())
                        .eventStatus(item.getEventStatus())
                        .operatorType(item.getOperatorType())
                        .description(item.getDescription())
                        .eventTime(item.getCreatedTime())
                        .build())
                .toList();
    }

    /**
     * 构建订单智能体决策提示。
     *
     * @param statusEnum   订单状态枚举
     * @param hasAfterSale 是否存在售后
     * @return 订单智能体决策提示
     */
    private OrderContextDto.AiHints buildOrderAiHints(OrderStatusEnum statusEnum, boolean hasAfterSale) {
        return OrderContextDto.AiHints.builder()
                .canCancel(statusEnum == OrderStatusEnum.PENDING_PAYMENT || statusEnum == OrderStatusEnum.PENDING_SHIPMENT)
                .canApplyAfterSale((statusEnum == OrderStatusEnum.PENDING_RECEIPT || statusEnum == OrderStatusEnum.COMPLETED) && !hasAfterSale)
                .needsPayment(statusEnum == OrderStatusEnum.PENDING_PAYMENT)
                .needsShipment(statusEnum == OrderStatusEnum.PENDING_SHIPMENT)
                .needsReceipt(statusEnum == OrderStatusEnum.PENDING_RECEIPT)
                .hasAfterSale(hasAfterSale)
                .build();
    }
}
