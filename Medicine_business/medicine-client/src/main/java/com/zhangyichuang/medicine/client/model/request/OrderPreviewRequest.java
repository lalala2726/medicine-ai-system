package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单预览请求参数
 * <p>
 * 用于在用户提交订单前预览订单信息,包括商品详情、价格、运费等
 * </p>
 *
 * @author Chuang
 * created on 2025/11/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "订单预览请求参数")
public class OrderPreviewRequest {

    @Schema(description = "预览类型: PRODUCT-单个商品, CART-购物车", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    @NotNull(message = "预览类型不能为空")
    private PreviewType type;

    @Schema(description = "商品ID(单个商品购买时必填)", example = "1")
    private Long productId;

    @Schema(description = "购买数量(单个商品购买时必填)", example = "2")
    @Min(value = 1, message = "购买数量不能小于1")
    private Integer quantity;

    @Schema(description = "购物车商品ID列表(购物车结算时必填)", example = "[1, 2, 3]")
    private List<Long> cartIds;

    @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "10001")
    private Long couponId;

    /**
     * 是否明确禁用优惠券。
     */
    @Schema(description = "是否明确禁用优惠券", example = "true")
    private Boolean disableCoupon;

    /**
     * 预览类型枚举
     */
    public enum PreviewType {
        /**
         * 单个商品购买
         */
        PRODUCT,
        /**
         * 购物车结算
         */
        CART
    }
}
