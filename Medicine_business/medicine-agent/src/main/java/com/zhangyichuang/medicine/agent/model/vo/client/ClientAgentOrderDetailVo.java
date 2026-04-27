package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 客户端智能体订单详情。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体订单详情")
@FieldDescription(description = "客户端智能体订单详情")
public class ClientAgentOrderDetailVo {

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID")
    @FieldDescription(description = "订单ID")
    private Long id;

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
     * 订单总金额。
     */
    @Schema(description = "订单总金额")
    @FieldDescription(description = "订单总金额")
    private BigDecimal totalAmount;

    /**
     * 实际支付金额。
     */
    @Schema(description = "实际支付金额")
    @FieldDescription(description = "实际支付金额")
    private BigDecimal payAmount;

    /**
     * 运费金额。
     */
    @Schema(description = "运费金额")
    @FieldDescription(description = "运费金额")
    private BigDecimal freightAmount;

    /**
     * 支付方式编码。
     */
    @Schema(description = "支付方式编码")
    @FieldDescription(description = "支付方式编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_PAY_TYPE)
    private String payType;

    /**
     * 支付方式名称。
     */
    @Schema(description = "支付方式名称")
    @FieldDescription(description = "支付方式名称")
    private String payTypeName;

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

    /**
     * 是否已支付。
     */
    @Schema(description = "是否已支付(0-否, 1-是)")
    @FieldDescription(description = "是否已支付")
    private Integer paid;

    /**
     * 支付过期时间。
     */
    @Schema(description = "支付过期时间")
    @FieldDescription(description = "支付过期时间")
    private Date payExpireTime;

    /**
     * 支付时间。
     */
    @Schema(description = "支付时间")
    @FieldDescription(description = "支付时间")
    private Date payTime;

    /**
     * 发货时间。
     */
    @Schema(description = "发货时间")
    @FieldDescription(description = "发货时间")
    private Date deliverTime;

    /**
     * 确认收货时间。
     */
    @Schema(description = "确认收货时间")
    @FieldDescription(description = "确认收货时间")
    private Date receiveTime;

    /**
     * 完成时间。
     */
    @Schema(description = "完成时间")
    @FieldDescription(description = "完成时间")
    private Date finishTime;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    @FieldDescription(description = "创建时间")
    private Date createTime;

    /**
     * 用户留言。
     */
    @Schema(description = "用户留言")
    @FieldDescription(description = "用户留言")
    private String note;

    /**
     * 订单售后标记编码。
     */
    @Schema(description = "订单售后标记编码")
    @FieldDescription(description = "订单售后标记编码")
    private String afterSaleFlag;

    /**
     * 订单售后标记名称。
     */
    @Schema(description = "订单售后标记名称")
    @FieldDescription(description = "订单售后标记名称")
    private String afterSaleFlagName;

    /**
     * 退款状态。
     */
    @Schema(description = "退款状态")
    @FieldDescription(description = "退款状态")
    private String refundStatus;

    /**
     * 退款金额。
     */
    @Schema(description = "退款金额")
    @FieldDescription(description = "退款金额")
    private BigDecimal refundPrice;

    /**
     * 退款时间。
     */
    @Schema(description = "退款时间")
    @FieldDescription(description = "退款时间")
    private Date refundTime;

    /**
     * 收货人信息。
     */
    @Schema(description = "收货人信息")
    @FieldDescription(description = "收货人信息")
    private ReceiverInfo receiverInfo;

    /**
     * 订单商品列表。
     */
    @Schema(description = "订单商品列表")
    @FieldDescription(description = "订单商品列表")
    private List<OrderItemDetail> items;

    /**
     * 物流信息。
     */
    @Schema(description = "物流信息")
    @FieldDescription(description = "物流信息")
    private ShippingInfo shippingInfo;

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
    }

    @Data
    @Schema(description = "订单商品项")
    @FieldDescription(description = "订单商品项")
    public static class OrderItemDetail {

        /**
         * 订单项ID。
         */
        @Schema(description = "订单项ID")
        @FieldDescription(description = "订单项ID")
        private Long id;

        /**
         * 商品ID。
         */
        @Schema(description = "商品ID")
        @FieldDescription(description = "商品ID")
        private Long productId;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称")
        @FieldDescription(description = "商品名称")
        private String productName;

        /**
         * 商品图片地址。
         */
        @Schema(description = "商品图片")
        @FieldDescription(description = "商品图片")
        private String imageUrl;

        /**
         * 购买数量。
         */
        @Schema(description = "购买数量")
        @FieldDescription(description = "购买数量")
        private Integer quantity;

        /**
         * 商品单价。
         */
        @Schema(description = "商品单价")
        @FieldDescription(description = "商品单价")
        private BigDecimal price;

        /**
         * 小计金额。
         */
        @Schema(description = "小计金额")
        @FieldDescription(description = "小计金额")
        private BigDecimal totalPrice;

        /**
         * 订单项售后状态编码。
         */
        @Schema(description = "订单项售后状态编码")
        @FieldDescription(description = "订单项售后状态编码")
        private String afterSaleStatus;

        /**
         * 订单项售后状态名称。
         */
        @Schema(description = "订单项售后状态名称")
        @FieldDescription(description = "订单项售后状态名称")
        private String afterSaleStatusName;

        /**
         * 已退款金额。
         */
        @Schema(description = "已退款金额")
        @FieldDescription(description = "已退款金额")
        private BigDecimal refundedAmount;
    }

    @Data
    @Schema(description = "物流信息")
    @FieldDescription(description = "物流信息")
    public static class ShippingInfo {

        /**
         * 物流公司。
         */
        @Schema(description = "物流公司")
        @FieldDescription(description = "物流公司")
        private String logisticsCompany;

        /**
         * 物流单号。
         */
        @Schema(description = "物流单号")
        @FieldDescription(description = "物流单号")
        private String trackingNumber;

        /**
         * 物流状态编码。
         */
        @Schema(description = "物流状态编码")
        @FieldDescription(description = "物流状态编码")
        private String shippingStatus;

        /**
         * 物流状态名称。
         */
        @Schema(description = "物流状态名称")
        @FieldDescription(description = "物流状态名称")
        private String shippingStatusName;

        /**
         * 发货时间。
         */
        @Schema(description = "发货时间")
        @FieldDescription(description = "发货时间")
        private Date shipTime;
    }
}
