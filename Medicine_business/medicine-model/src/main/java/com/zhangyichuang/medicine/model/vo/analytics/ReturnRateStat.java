package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "商品退货率")
public class ReturnRateStat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "售出数量")
    private Long soldQuantity;

    @Schema(description = "售后/退货数量")
    private Long returnQuantity;

    @Schema(description = "退货率，0-1")
    private BigDecimal returnRate;
}
