package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 退货退款风险商品项。
 */
@Data
@Schema(description = "退货退款风险商品项")
public class AnalyticsReturnRefundRiskProductVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "销量")
    private Long soldQuantity;

    @Schema(description = "退货退款件数")
    private Long returnRefundQuantity;

    @Schema(description = "退货退款率")
    private BigDecimal returnRefundRate;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;
}
