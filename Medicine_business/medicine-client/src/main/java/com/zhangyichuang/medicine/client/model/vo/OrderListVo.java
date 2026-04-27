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
 * 客户端订单列表VO
 *
 * @author Chuang
 * created 2025/11/10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单列表VO")
public class OrderListVo {

    @Schema(description = "订单ID", example = "1234567890")
    private Long id;

    @Schema(description = "订单编号", example = "ORD202501100001")
    private String orderNo;

    @Schema(description = "订单状态", example = "PENDING")
    private String orderStatus;

    @Schema(description = "订单状态名称", example = "待支付")
    private String orderStatusName;

    @Schema(description = "订单总金额", example = "299.99")
    private BigDecimal totalAmount;

    @Schema(description = "实际支付金额", example = "269.99")
    private BigDecimal payAmount;

    @Schema(description = "使用的优惠券ID", example = "10001")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    @Schema(description = "订单抵扣金额", example = "30.00")
    private BigDecimal couponDeductAmount;

    @Schema(description = "优惠券消耗金额", example = "30.00")
    private BigDecimal couponConsumeAmount;

    @Schema(description = "是否已支付(0-否, 1-是)", example = "1")
    private Integer paid;

    @Schema(description = "支付到期时间", example = "2025-01-10T22:30:00")
    private Date payExpireTime;

    @Schema(description = "支付时间", example = "2025-01-10T11:00:00")
    private Date payTime;

    @Schema(description = "是否存在售后", example = "NO_AFTER_SALE")
    private OrderItemAfterSaleStatusEnum afterSaleFlag;

    @Schema(description = "创建时间", example = "2025-01-10T10:30:00")
    private Date createTime;

    @Schema(description = "订单商品列表(只包含必要信息)")
    private List<OrderItemSimpleVo> items;

    @Schema(description = "收货信息")
    private ReceiverInfo receiverInfo;

    /**
     * 订单项简化信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单项简化信息")
    public static class OrderItemSimpleVo {

        @Schema(description = "订单项ID", example = "9876543210")
        private Long id;

        @Schema(description = "商品ID", example = "5001")
        private Long productId;

        @Schema(description = "商品名称", example = "999感冒灵颗粒")
        private String productName;

        @Schema(description = "商品图片", example = "https://example.com/images/product/999.jpg")
        private String imageUrl;

        @Schema(description = "购买数量", example = "2")
        private Integer quantity;

        @Schema(description = "单价", example = "25.50")
        private BigDecimal price;

        @Schema(description = "小计金额", example = "51.00")
        private BigDecimal totalPrice;

        @Schema(description = "售后状态", example = "NO_AFTER_SALE")
        private String afterSaleStatus;

        @Schema(description = "售后状态名称", example = "无售后")
        private String afterSaleStatusName;
    }

    /**
     * 收货信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "收货信息")
    public static class ReceiverInfo {

        @Schema(description = "收货人姓名", example = "张三")
        private String name;

        @Schema(description = "收货人电话", example = "13800000000")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String phone;

        @Schema(description = "收货人地址", example = "北京市海淀区中关村")
        private String address;
    }
}
