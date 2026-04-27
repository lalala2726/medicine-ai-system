package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/16
 */
@Data
@Schema(description = "商品推荐视图对象")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecommendListVo {

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "商品名称")
    private String productName;

    @Schema(description = "商品封面", example = "https://example.com/product.jpg")
    private String cover;

    @Schema(description = "商品价格", example = "9.99")
    private BigDecimal price;

    @Schema(description = "商品销量", example = "128")
    private Integer sales;

    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
    private Integer drugCategory;

}
