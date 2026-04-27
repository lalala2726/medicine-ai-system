package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单预览响应对象
 * <p>
 * 返回订单的详细信息供用户确认,包括商品列表、价格明细等
 * </p>
 *
 * @author Chuang
 * created on 2025/11/12
 */
@Data
@Builder
@Schema(description = "订单预览响应对象")
public class OrderPreviewVo {

    @Schema(description = "商品列表")
    private List<OrderItemPreview> items;

    @Schema(description = "商品总金额", example = "256.00")
    private BigDecimal itemsAmount;

    @Schema(description = "优惠金额", example = "0.00")
    private BigDecimal discountAmount;

    @Schema(description = "使用的优惠券ID", example = "10001")
    private Long couponId;

    @Schema(description = "订单抵扣金额", example = "30.00")
    private BigDecimal couponDeductAmount;

    @Schema(description = "优惠券消耗金额", example = "30.00")
    private BigDecimal couponConsumeAmount;

    @Schema(description = "优惠券浪费金额", example = "0.00")
    private BigDecimal couponWasteAmount;

    @Schema(description = "当前选中的优惠券")
    private OrderCouponOptionVo selectedCoupon;

    @Schema(description = "当前自动或手动选中的优惠券集合")
    private List<OrderCouponOptionVo> selectedCoupons;

    @Schema(description = "是否自动选券", example = "true")
    private Boolean autoCouponSelected;

    @Schema(description = "当前可选的优惠券列表")
    private List<OrderCouponOptionVo> couponCandidates;

    @Schema(description = "订单总金额", example = "256.00")
    private BigDecimal totalAmount;

    @Schema(description = "收货地址")
    private String address;

    @Schema(description = "配送方式", example = "EXPRESS")
    private String deliveryType;

    @Schema(description = "配送方式名称", example = "快递配送")
    private String deliveryTypeName;

    @Schema(description = "预计送达时间", example = "预计3-5天送达")
    private String estimatedDeliveryTime;

    /**
     * 订单商品项预览
     */
    @Data
    @Builder
    @Schema(description = "订单商品项预览")
    public static class OrderItemPreview {

        @Schema(description = "商品ID", example = "1")
        private Long productId;

        @Schema(description = "商品名称", example = "复方感冒灵颗粒")
        private String productName;

        @Schema(description = "商品图片", example = "https://example.com/image.jpg")
        private String imageUrl;

        @Schema(description = "商品价格", example = "128.00")
        private BigDecimal price;

        @Schema(description = "购买数量", example = "2")
        private Integer quantity;

        @Schema(description = "小计金额", example = "256.00")
        private BigDecimal subtotal;

        @Schema(description = "商品库存", example = "100")
        private Integer stock;

        @Schema(description = "商品状态", example = "1")
        private Integer status;

        @Schema(description = "商品状态描述", example = "在售")
        private String statusDesc;
    }
}
