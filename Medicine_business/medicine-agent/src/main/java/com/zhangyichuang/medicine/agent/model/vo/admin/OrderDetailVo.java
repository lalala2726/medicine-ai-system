package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端智能体订单详情。
 */
@Schema(description = "管理端智能体订单详情")
@FieldDescription(description = "管理端智能体订单详情")
@Data
public class OrderDetailVo {

    @Schema(description = "用户信息")
    @FieldDescription(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "配送信息")
    @FieldDescription(description = "配送信息")
    private DeliveryInfo deliveryInfo;

    @Schema(description = "订单信息")
    @FieldDescription(description = "订单信息")
    private OrderInfo orderInfo;

    @Schema(description = "商品信息")
    @FieldDescription(description = "商品信息")
    private List<ProductInfo> productInfo;

    @Data
    @FieldDescription(description = "用户信息")
    public static class UserInfo {

        @Schema(description = "用户ID")
        @FieldDescription(description = "用户ID")
        private String userId;

        @Schema(description = "用户昵称")
        @FieldDescription(description = "用户昵称")
        private String nickname;

        @Schema(description = "用户手机号")
        @FieldDescription(description = "用户手机号")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String phoneNumber;
    }

    @Data
    @FieldDescription(description = "配送信息")
    public static class DeliveryInfo {

        @Schema(description = "收货人")
        @FieldDescription(description = "收货人")
        private String receiverName;

        @Schema(description = "收货地址")
        @FieldDescription(description = "收货地址")
        private String receiverAddress;

        @Schema(description = "收货人电话")
        @FieldDescription(description = "收货人电话")
        private String receiverPhone;

        @Schema(description = "配送方式")
        @FieldDescription(description = "配送方式")
        private String deliveryMethod;
    }

    @Data
    @FieldDescription(description = "订单信息")
    public static class OrderInfo {

        @Schema(description = "订单编号")
        @FieldDescription(description = "订单编号")
        private String orderNo;

        @Schema(description = "订单状态")
        @FieldDescription(description = "订单状态")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
        private String orderStatus;

        @Schema(description = "支付方式")
        @FieldDescription(description = "支付方式")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_PAY_TYPE)
        private String payType;

        @Schema(description = "订单总金额")
        @FieldDescription(description = "订单总金额")
        private BigDecimal totalAmount;

        @Schema(description = "实际支付金额")
        @FieldDescription(description = "实际支付金额")
        private BigDecimal payAmount;

        @Schema(description = "运费金额")
        @FieldDescription(description = "运费金额")
        private BigDecimal freightAmount;
    }

    @Data
    @FieldDescription(description = "商品信息")
    public static class ProductInfo {

        @Schema(description = "商品ID")
        @FieldDescription(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        @FieldDescription(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        @FieldDescription(description = "商品图片")
        private String productImage;

        @Schema(description = "商品价格")
        @FieldDescription(description = "商品价格")
        private BigDecimal productPrice;

        @Schema(description = "商品数量")
        @FieldDescription(description = "商品数量")
        private Integer productQuantity;

        @Schema(description = "商品总价")
        @FieldDescription(description = "商品总价")
        private BigDecimal productTotalAmount;
    }
}
