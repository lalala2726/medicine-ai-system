package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 客户端智能体订单物流。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体订单物流")
@FieldDescription(description = "客户端智能体订单物流")
public class ClientAgentOrderShippingVo {

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID")
    @FieldDescription(description = "订单ID")
    private Long orderId;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    @FieldDescription(description = "订单编号")
    private String orderNo;

    /**
     * 订单状态编码。
     */
    @Schema(description = "订单状态编码")
    @FieldDescription(description = "订单状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
    private String orderStatus;

    /**
     * 订单状态名称。
     */
    @Schema(description = "订单状态名称")
    @FieldDescription(description = "订单状态名称")
    private String orderStatusName;

    /**
     * 物流状态编码。
     */
    @Schema(description = "物流状态编码")
    @FieldDescription(description = "物流状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_SHIPPING_STATUS)
    private String shippingStatus;

    /**
     * 物流状态名称。
     */
    @Schema(description = "物流状态名称")
    @FieldDescription(description = "物流状态名称")
    private String shippingStatusName;

    /**
     * 物流公司。
     */
    @Schema(description = "物流公司")
    @FieldDescription(description = "物流公司")
    private String logisticsCompany;

    /**
     * 运单号。
     */
    @Schema(description = "运单号")
    @FieldDescription(description = "运单号")
    private String trackingNumber;

    /**
     * 发货备注。
     */
    @Schema(description = "发货备注")
    @FieldDescription(description = "发货备注")
    private String shipmentNote;

    /**
     * 发货时间。
     */
    @Schema(description = "发货时间")
    @FieldDescription(description = "发货时间")
    private Date deliverTime;

    /**
     * 签收时间。
     */
    @Schema(description = "签收时间")
    @FieldDescription(description = "签收时间")
    private Date receiveTime;

    /**
     * 收货人信息。
     */
    @Schema(description = "收货人信息")
    @FieldDescription(description = "收货人信息")
    private ReceiverInfo receiverInfo;

    /**
     * 物流轨迹节点。
     */
    @Schema(description = "物流轨迹节点")
    @FieldDescription(description = "物流轨迹节点")
    private List<ShippingNode> nodes;

    /**
     * 收货人信息。
     */
    @Data
    @Schema(description = "收货人信息")
    @FieldDescription(description = "收货人信息")
    public static class ReceiverInfo {

        /**
         * 收货人姓名。
         */
        @Schema(description = "收货人姓名")
        @FieldDescription(description = "收货人姓名")
        private String receiverName;

        /**
         * 收货人电话。
         */
        @Schema(description = "收货人电话")
        @FieldDescription(description = "收货人电话")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String receiverPhone;

        /**
         * 收货详细地址。
         */
        @Schema(description = "收货详细地址")
        @FieldDescription(description = "收货详细地址")
        private String receiverDetail;

        /**
         * 配送方式编码。
         */
        @Schema(description = "配送方式编码")
        @FieldDescription(description = "配送方式编码")
        private String deliveryType;

        /**
         * 配送方式名称。
         */
        @Schema(description = "配送方式名称")
        @FieldDescription(description = "配送方式名称")
        private String deliveryTypeName;
    }

    /**
     * 物流轨迹节点。
     */
    @Data
    @Schema(description = "物流轨迹节点")
    @FieldDescription(description = "物流轨迹节点")
    public static class ShippingNode {

        /**
         * 节点时间。
         */
        @Schema(description = "节点时间")
        @FieldDescription(description = "节点时间")
        private String time;

        /**
         * 节点内容。
         */
        @Schema(description = "节点内容")
        @FieldDescription(description = "节点内容")
        private String content;

        /**
         * 节点位置。
         */
        @Schema(description = "节点位置")
        @FieldDescription(description = "节点位置")
        private String location;
    }
}
