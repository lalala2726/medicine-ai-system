package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.model.enums.DeliveryTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 购物车提交订单请求
 * <p>
 * 用于从购物车创建订单并锁定库存，订单创建后需要在30分钟内完成支付
 * </p>
 *
 * @author Chuang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "购物车提交订单请求参数")
public class CartSettleRequest {

    @NotEmpty(message = "购物车商品ID列表不能为空")
    @Schema(description = "购物车商品ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    private List<Long> cartIds;

    @NotNull(message = "收货地址ID不能为空")
    @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long addressId;

    @NotNull(message = "配送方式不能为空")
    @Schema(description = "配送方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "EXPRESS")
    private DeliveryTypeEnum deliveryType;

    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

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
}
