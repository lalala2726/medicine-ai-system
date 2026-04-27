package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品浏览记录视图对象
 */
@Data
public class ViewHistoryVo {

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "999感冒灵颗粒")
    private String productName;

    @Schema(description = "商品封面图", example = "https://example.com/images/product/cover.jpg")
    private String coverImage;

    @Schema(description = "商品价格", example = "29.90")
    private BigDecimal price;

    @Schema(description = "商品销量", example = "1580")
    private Integer sales;

    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
    private Integer drugCategory;

    @Schema(description = "最后浏览时间", example = "2024-01-20T15:45:00")
    private Date lastViewTime;
}
