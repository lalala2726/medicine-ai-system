package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单价格信息VO
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单价格信息")
public class OrderPriceVo {

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "订单号", example = "ORDER20230725001")
    private String orderNo;

    @Schema(description = "订单总金额", example = "99.99")
    private BigDecimal totalAmount;

    @Schema(description = "商品原始总金额", example = "129.99")
    private BigDecimal itemsAmount;

    @Schema(description = "使用的优惠券ID", example = "10001")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    @Schema(description = "订单抵扣金额", example = "30.00")
    private BigDecimal couponDeductAmount;

}
