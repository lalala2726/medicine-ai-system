package com.zhangyichuang.medicine.client.rpc;

import com.zhangyichuang.medicine.client.model.vo.OrderDetailVo;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.enums.OrderItemAfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import com.zhangyichuang.medicine.rpc.client.ClientAgentOrderRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 客户端智能体订单 RPC Provider。
 */
@DubboService(interfaceClass = ClientAgentOrderRpcService.class, group = "medicine-client", version = "1.0.0")
@RequiredArgsConstructor
public class ClientAgentOrderRpcServiceImpl implements ClientAgentOrderRpcService {

    /**
     * 客户端订单服务。
     */
    private final MallOrderService mallOrderService;

    /**
     * 按用户范围查询订单卡摘要。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单卡摘要 DTO
     */
    @Override
    public ClientAgentOrderCardSummaryDto getOrderCardSummary(String orderNo, Long userId) {
        return mallOrderService.getOrderCardSummary(orderNo, userId);
    }

    /**
     * 按用户范围查询订单详情并转换为共享 DTO。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单详情 DTO
     */
    @Override
    public ClientAgentOrderDetailDto getOrderDetail(String orderNo, Long userId) {
        OrderDetailVo detail = mallOrderService.getOrderDetail(orderNo, userId);
        return toDto(detail);
    }

    /**
     * 按用户范围查询订单物流并转换为共享 DTO。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单物流 DTO
     */
    @Override
    public ClientAgentOrderShippingDto getOrderShipping(String orderNo, Long userId) {
        OrderShippingVo shippingVo = mallOrderService.getOrderShipping(orderNo, userId);
        return toOrderShippingDto(shippingVo);
    }

    /**
     * 按用户范围查询订单时间线。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单时间线 DTO
     */
    @Override
    public ClientAgentOrderTimelineDto getOrderTimeline(String orderNo, Long userId) {
        return mallOrderService.getOrderTimeline(orderNo, userId);
    }

    /**
     * 校验当前用户订单是否允许取消。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 取消资格 DTO
     */
    @Override
    public ClientAgentOrderCancelCheckDto checkOrderCancelable(String orderNo, Long userId) {
        return mallOrderService.checkOrderCancelable(orderNo, userId);
    }

    /**
     * 将客户端订单详情转换为共享 DTO。
     *
     * @param source 客户端订单详情
     * @return 共享 DTO
     */
    private ClientAgentOrderDetailDto toDto(OrderDetailVo source) {
        if (source == null) {
            return null;
        }

        OrderItemAfterSaleStatusEnum afterSaleFlag = source.getAfterSaleFlag();
        return ClientAgentOrderDetailDto.builder()
                .id(source.getId())
                .orderNo(source.getOrderNo())
                .orderStatus(source.getOrderStatus())
                .orderStatusName(source.getOrderStatusName())
                .totalAmount(source.getTotalAmount())
                .payAmount(source.getPayAmount())
                .freightAmount(source.getFreightAmount())
                .payType(source.getPayType())
                .payTypeName(source.getPayTypeName())
                .deliveryType(source.getDeliveryType())
                .deliveryTypeName(source.getDeliveryTypeName())
                .paid(source.getPaid())
                .payExpireTime(source.getPayExpireTime())
                .payTime(source.getPayTime())
                .deliverTime(source.getDeliverTime())
                .receiveTime(source.getReceiveTime())
                .finishTime(source.getFinishTime())
                .createTime(source.getCreateTime())
                .note(source.getNote())
                .afterSaleFlag(afterSaleFlag == null ? null : afterSaleFlag.getStatus())
                .afterSaleFlagName(afterSaleFlag == null ? null : afterSaleFlag.getName())
                .refundStatus(source.getRefundStatus())
                .refundPrice(source.getRefundPrice())
                .refundTime(source.getRefundTime())
                .receiverInfo(toReceiverInfo(source.getReceiverInfo()))
                .items(toOrderItems(source.getItems()))
                .shippingInfo(toShippingInfo(source.getShippingInfo()))
                .build();
    }

    /**
     * 映射收货人信息。
     *
     * @param source 客户端收货人信息
     * @return 共享 DTO 收货人信息
     */
    private ClientAgentOrderDetailDto.ReceiverInfo toReceiverInfo(OrderDetailVo.ReceiverInfo source) {
        if (source == null) {
            return null;
        }
        return ClientAgentOrderDetailDto.ReceiverInfo.builder()
                .receiverName(source.getReceiverName())
                .receiverPhone(source.getReceiverPhone())
                .receiverDetail(source.getReceiverDetail())
                .build();
    }

    /**
     * 映射订单项列表。
     *
     * @param source 客户端订单项列表
     * @return 共享 DTO 订单项列表
     */
    private List<ClientAgentOrderDetailDto.OrderItemDetail> toOrderItems(List<OrderDetailVo.OrderItemDetailVo> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(item -> ClientAgentOrderDetailDto.OrderItemDetail.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getTotalPrice())
                .afterSaleStatus(item.getAfterSaleStatus())
                .afterSaleStatusName(item.getAfterSaleStatusName())
                .refundedAmount(item.getRefundedAmount())
                .build()).toList();
    }

    /**
     * 映射物流信息。
     *
     * @param source 客户端物流信息
     * @return 共享 DTO 物流信息
     */
    private ClientAgentOrderDetailDto.ShippingInfo toShippingInfo(OrderDetailVo.ShippingInfo source) {
        if (source == null) {
            return null;
        }
        return ClientAgentOrderDetailDto.ShippingInfo.builder()
                .logisticsCompany(source.getLogisticsCompany())
                .trackingNumber(source.getTrackingNumber())
                .shippingStatus(source.getShippingStatus())
                .shippingStatusName(source.getShippingStatusName())
                .shipTime(source.getShipTime())
                .build();
    }

    /**
     * 映射订单物流 DTO。
     *
     * @param source 客户端订单物流
     * @return 共享 DTO 订单物流
     */
    private ClientAgentOrderShippingDto toOrderShippingDto(OrderShippingVo source) {
        if (source == null) {
            return null;
        }
        return ClientAgentOrderShippingDto.builder()
                .orderId(source.getOrderId())
                .orderNo(source.getOrderNo())
                .orderStatus(source.getOrderStatus())
                .orderStatusName(source.getOrderStatusName())
                .shippingStatus(source.getStatus())
                .shippingStatusName(source.getStatusName())
                .logisticsCompany(source.getLogisticsCompany())
                .trackingNumber(source.getTrackingNumber())
                .shipmentNote(source.getShipmentNote())
                .deliverTime(source.getDeliverTime())
                .receiveTime(source.getReceiveTime())
                .receiverInfo(toShippingReceiverInfo(source.getReceiverInfo()))
                .nodes(toShippingNodes(source.getNodes()))
                .build();
    }

    /**
     * 映射物流收货人信息。
     *
     * @param source 客户端物流收货人信息
     * @return 共享 DTO 收货人信息
     */
    private ClientAgentOrderShippingDto.ReceiverInfo toShippingReceiverInfo(OrderShippingVo.ReceiverInfo source) {
        if (source == null) {
            return null;
        }
        return ClientAgentOrderShippingDto.ReceiverInfo.builder()
                .receiverName(source.getReceiverName())
                .receiverPhone(source.getReceiverPhone())
                .receiverDetail(source.getReceiverDetail())
                .deliveryType(source.getDeliveryType())
                .deliveryTypeName(source.getDeliveryTypeName())
                .build();
    }

    /**
     * 映射物流轨迹节点列表。
     *
     * @param source 客户端物流轨迹节点列表
     * @return 共享 DTO 物流轨迹节点列表
     */
    private List<ClientAgentOrderShippingDto.ShippingNode> toShippingNodes(List<OrderShippingVo.ShippingNode> source) {
        if (source == null) {
            return List.of();
        }
        return source.stream().map(node -> ClientAgentOrderShippingDto.ShippingNode.builder()
                .time(node.getTime())
                .content(node.getContent())
                .location(node.getLocation())
                .build()).toList();
    }
}
