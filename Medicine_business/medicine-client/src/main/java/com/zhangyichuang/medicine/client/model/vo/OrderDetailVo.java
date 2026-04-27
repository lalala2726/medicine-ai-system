package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import com.zhangyichuang.medicine.model.enums.OrderItemAfterSaleStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 客户端订单详情VO
 *
 * @author Chuang
 * created 2025/11/10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情VO")
public class OrderDetailVo {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "订单状态名称")
    private String orderStatusName;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "实际支付金额")
    private BigDecimal payAmount;

    @Schema(description = "商品原始总金额")
    private BigDecimal itemsAmount;

    @Schema(description = "运费金额")
    private BigDecimal freightAmount;

    @Schema(description = "使用的优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "订单抵扣金额")
    private BigDecimal couponDeductAmount;

    @Schema(description = "优惠券消耗金额")
    private BigDecimal couponConsumeAmount;

    @Schema(description = "支付方式")
    private String payType;

    @Schema(description = "支付方式名称")
    private String payTypeName;

    @Schema(description = "配送方式")
    private String deliveryType;

    @Schema(description = "配送方式名称")
    private String deliveryTypeName;

    @Schema(description = "是否已支付(0-否, 1-是)")
    private Integer paid;

    @Schema(description = "支付过期时间")
    private Date payExpireTime;

    @Schema(description = "支付时间")
    private Date payTime;

    @Schema(description = "发货时间")
    private Date deliverTime;

    @Schema(description = "确认收货时间")
    private Date receiveTime;

    @Schema(description = "完成时间")
    private Date finishTime;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "用户留言")
    private String note;

    @Schema(description = "是否存在售后")
    private OrderItemAfterSaleStatusEnum afterSaleFlag;

    @Schema(description = "退款状态")
    private String refundStatus;

    @Schema(description = "退款金额")
    private BigDecimal refundPrice;

    @Schema(description = "退款时间")
    private Date refundTime;

    @Schema(description = "收货人信息")
    private ReceiverInfo receiverInfo;

    @Schema(description = "订单商品列表")
    private List<OrderItemDetailVo> items;

    @Schema(description = "物流信息")
    private ShippingInfo shippingInfo;

    /**
     * 收货人信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "收货人信息")
    public static class ReceiverInfo {

        @Schema(description = "收货人姓名")
        private String receiverName;

        @Schema(description = "收货人电话")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String receiverPhone;

        @Schema(description = "收货详细地址")
        private String receiverDetail;
    }

    /**
     * 订单项详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单项详情")
    public static class OrderItemDetailVo {

        @Schema(description = "订单项ID")
        private Long id;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String imageUrl;

        @Schema(description = "购买数量")
        private Integer quantity;

        @Schema(description = "单价")
        private BigDecimal price;

        @Schema(description = "小计金额")
        private BigDecimal totalPrice;

        @Schema(description = "分摊优惠金额")
        private BigDecimal couponDeductAmount;

        @Schema(description = "应付金额")
        private BigDecimal payableAmount;

        @Schema(description = "售后状态")
        private String afterSaleStatus;

        @Schema(description = "售后状态名称")
        private String afterSaleStatusName;

        @Schema(description = "已退款金额")
        private BigDecimal refundedAmount;
    }

    /**
     * 物流信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "物流信息")
    public static class ShippingInfo {

        @Schema(description = "物流公司")
        private String logisticsCompany;

        @Schema(description = "物流单号")
        private String trackingNumber;

        @Schema(description = "物流状态")
        private String shippingStatus;

        @Schema(description = "物流状态名称")
        private String shippingStatusName;

        @Schema(description = "发货时间")
        private Date shipTime;
    }
}
