package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单提交响应对象
 * <p>
 * 返回订单创建成功的基本信息，订单状态为待支付，需要在30分钟内完成支付
 * </p>
 *
 * @author Chuang
 * created on 2025/11/12
 */
@Data
@Builder
@Schema(description = "订单提交响应对象")
public class OrderCheckoutVo {

    @Schema(description = "订单号", example = "O2025103011223344")
    private String orderNo;

    @Schema(description = "订单金额", example = "128.50")
    private BigDecimal totalAmount;

    @Schema(description = "商品原始总金额", example = "158.50")
    private BigDecimal itemsAmount;

    @Schema(description = "使用的优惠券ID", example = "10001")
    private Long couponId;

    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    @Schema(description = "订单抵扣金额", example = "30.00")
    private BigDecimal couponDeductAmount;

    @Schema(description = "优惠券消耗金额", example = "30.00")
    private BigDecimal couponConsumeAmount;

    @Schema(description = "订单状态", example = "PENDING_PAYMENT")
    private String orderStatus;

    @Schema(description = "创建时间", example = "2025-10-30 13:22:33")
    private Date createTime;

    @Schema(description = "过期时间", example = "2025-10-30 13:37:33")
    private Date payExpireTime;

    @Schema(description = "商品摘要", example = "复方感冒灵颗粒 x2")
    private String productSummary;

    @Schema(description = "商品种类数量", example = "3")
    private Integer itemCount;
}
