package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.admin.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentOrderRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        Map<String, OrderContextDto> result = new LinkedHashMap<>();
        for (String orderNo : orderNos) {
            MallOrder order = getOrderByOrderNo(orderNo);
            result.put(orderNo, buildOrderContext(order));
        }
        return result;
    }

    /**
     * 根据订单编号查询订单实体。
     *
     * @param orderNo 订单编号
     * @return 订单实体
     */
    private MallOrder getOrderByOrderNo(String orderNo) {
        Assert.notEmpty(orderNo, "订单编号不能为空");
        MallOrder order = mallOrderService.lambdaQuery()
                .eq(MallOrder::getOrderNo, orderNo)
                .one();
        Assert.notNull(order, "订单不存在: " + orderNo);
        return order;
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
     * 构建单个订单的智能体上下文。
     *
     * @param order 订单实体
     * @return 订单智能体上下文
     */
    private OrderContextDto buildOrderContext(MallOrder order) {
        OrderDetailDto detail = mallOrderService.orderDetail(order.getId());
        List<MallOrderTimeline> timeline = mallOrderTimelineService.getTimelineByOrderId(order.getId());
        OrderShippingVo shipping = mallOrderService.getOrderShipping(order.getId());
        boolean hasAfterSale = hasAfterSale(order);
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
     * @param shipping 订单物流 VO
     * @return 订单物流摘要
     */
    private OrderContextDto.ShippingSummary buildShippingSummary(OrderShippingVo shipping) {
        boolean shipped = shipping != null && shipping.getDeliverTime() != null;
        return OrderContextDto.ShippingSummary.builder()
                .shipped(shipped)
                .logisticsCompany(shipping == null ? null : shipping.getLogisticsCompany())
                .trackingNumber(shipping == null ? null : shipping.getTrackingNumber())
                .shipmentNote(shipping == null ? null : shipping.getShipmentNote())
                .statusCode(shipping == null ? null : shipping.getStatus())
                .statusText(shipping == null ? null : shipping.getStatusName())
                .deliverTime(shipping == null ? null : shipping.getDeliverTime())
                .receiveTime(shipping == null ? null : shipping.getReceiveTime())
                .nodes(buildShippingNodes(shipping))
                .build();
    }

    /**
     * 构建完整物流节点列表。
     *
     * @param shipping 订单物流 VO
     * @return 完整物流节点列表
     */
    private List<OrderContextDto.ShippingNodeSummary> buildShippingNodes(OrderShippingVo shipping) {
        if (shipping == null || CollectionUtils.isEmpty(shipping.getNodes())) {
            return List.of();
        }
        return shipping.getNodes().stream()
                .map(node -> OrderContextDto.ShippingNodeSummary.builder()
                        .time(node.getTime())
                        .content(node.getContent())
                        .location(node.getLocation())
                        .build())
                .toList();
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

    /**
     * 判断订单是否已有售后。
     *
     * @param order 订单实体
     * @return true 表示订单已有售后
     */
    private boolean hasAfterSale(MallOrder order) {
        Long afterSaleCount = mallAfterSaleService.lambdaQuery()
                .eq(MallAfterSale::getOrderNo, order.getOrderNo())
                .count();
        return afterSaleCount != null && afterSaleCount > 0;
    }
}
