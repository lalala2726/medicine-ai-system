package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销商品项。
 */
@Data
@Schema(description = "热销商品项")
public class AnalyticsTopSellingProductVo implements Serializable {

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

    @Schema(description = "成交金额")
    private BigDecimal paidAmount;
}
