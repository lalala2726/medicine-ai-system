package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端智能体订单详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情")
public class OrderDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "配送信息")
    private DeliveryInfo deliveryInfo;

    @Schema(description = "订单信息")
    private OrderInfo orderInfo;

    @Schema(description = "商品信息")
    private List<ProductInfo> productInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "用户手机号")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "配送信息")
    public static class DeliveryInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "收货人")
        private String receiverName;

        @Schema(description = "收货地址")
        private String receiverAddress;

        @Schema(description = "收货人电话")
        private String receiverPhone;

        @Schema(description = "配送方式")
        private String deliveryMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单信息")
    public static class OrderInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "订单编号")
        private String orderNo;

        @Schema(description = "订单状态")
        private String orderStatus;

        @Schema(description = "支付方式")
        private String payType;

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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商品信息")
    public static class ProductInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "商品价格")
        private BigDecimal productPrice;

        @Schema(description = "商品数量")
        private Integer productQuantity;

        @Schema(description = "商品总价")
        private BigDecimal productTotalAmount;

        @Schema(description = "分摊优惠金额")
        private BigDecimal couponDeductAmount;

        @Schema(description = "应付金额")
        private BigDecimal payableAmount;
    }
}
