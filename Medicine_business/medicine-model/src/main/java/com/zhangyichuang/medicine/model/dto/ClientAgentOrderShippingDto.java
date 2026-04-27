package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 客户端智能体订单物流 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体订单物流")
public class ClientAgentOrderShippingDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID")
    private Long orderId;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    private String orderNo;

    /**
     * 订单状态编码。
     */
    @Schema(description = "订单状态编码")
    private String orderStatus;

    /**
     * 订单状态名称。
     */
    @Schema(description = "订单状态名称")
    private String orderStatusName;

    /**
     * 物流状态编码。
     */
    @Schema(description = "物流状态编码")
    private String shippingStatus;

    /**
     * 物流状态名称。
     */
    @Schema(description = "物流状态名称")
    private String shippingStatusName;

    /**
     * 物流公司。
     */
    @Schema(description = "物流公司")
    private String logisticsCompany;

    /**
     * 运单号。
     */
    @Schema(description = "运单号")
    private String trackingNumber;

    /**
     * 发货备注。
     */
    @Schema(description = "发货备注")
    private String shipmentNote;

    /**
     * 发货时间。
     */
    @Schema(description = "发货时间")
    private Date deliverTime;

    /**
     * 签收时间。
     */
    @Schema(description = "签收时间")
    private Date receiveTime;

    /**
     * 收货人信息。
     */
    @Schema(description = "收货人信息")
    private ReceiverInfo receiverInfo;

    /**
     * 物流轨迹节点。
     */
    @Schema(description = "物流轨迹节点")
    private List<ShippingNode> nodes;

    /**
     * 收货人信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "收货人信息")
    public static class ReceiverInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 收货人姓名。
         */
        @Schema(description = "收货人姓名")
        private String receiverName;

        /**
         * 收货人电话。
         */
        @Schema(description = "收货人电话")
        private String receiverPhone;

        /**
         * 收货详细地址。
         */
        @Schema(description = "收货详细地址")
        private String receiverDetail;

        /**
         * 配送方式编码。
         */
        @Schema(description = "配送方式编码")
        private String deliveryType;

        /**
         * 配送方式名称。
         */
        @Schema(description = "配送方式名称")
        private String deliveryTypeName;
    }

    /**
     * 物流轨迹节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "物流轨迹节点")
    public static class ShippingNode implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 节点时间。
         */
        @Schema(description = "节点时间")
        private String time;

        /**
         * 节点内容。
         */
        @Schema(description = "节点内容")
        private String content;

        /**
         * 节点位置。
         */
        @Schema(description = "节点位置")
        private String location;
    }
}
