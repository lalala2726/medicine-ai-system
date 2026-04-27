package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单支付信息响应。
 * <p>
 * 用于收银台页面展示订单支付信息，支持待支付和已支付订单。
 * </p>
 */
@Data
@Builder
@Schema(description = "订单支付信息响应")
public class OrderPayInfoVo {

    @Schema(description = "订单号", example = "O20251113112233445566")
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

    @Schema(description = "订单状态名称", example = "待支付")
    private String orderStatusName;

    @Schema(description = "是否已支付(0-否, 1-是)", example = "0")
    private Integer paid;

    @Schema(description = "支付方式编码", example = "WALLET")
    private String payType;

    @Schema(description = "支付方式名称", example = "使用钱包余额进行支付")
    private String payTypeName;

    @Schema(description = "支付时间", example = "2025-11-13 10:12:00")
    private Date payTime;

    @Schema(description = "创建时间", example = "2025-11-13 10:00:00")
    private Date createTime;

    @Schema(description = "过期时间", example = "2025-11-13 10:30:00")
    private Date payExpireTime;

    @Schema(description = "商品摘要", example = "布洛芬缓释胶囊 x2 等2件")
    private String productSummary;

    @Schema(description = "商品种类数量", example = "2")
    private Integer itemCount;
}
