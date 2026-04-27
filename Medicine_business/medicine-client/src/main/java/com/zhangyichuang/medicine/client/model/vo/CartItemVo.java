package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车商品VO
 *
 * @author Chuang
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "购物车商品信息")
public class CartItemVo {

    @Schema(description = "购物车ID", example = "1")
    private Long id;

    @Schema(description = "商品ID", example = "1001")
    private Long productId;

    @Schema(description = "商品名称", example = "布洛芬缓释胶囊")
    private String productName;

    @Schema(description = "商品图片", example = "https://example.com/product/image.jpg")
    private String productImage;

    @Schema(description = "商品单价", example = "25.80")
    private BigDecimal price;

    @Schema(description = "购买数量", example = "2")
    private Integer cartNum;

    @Schema(description = "小计金额", example = "51.60")
    private BigDecimal subtotal;

    @Schema(description = "库存", example = "100")
    private Integer stock;
}

