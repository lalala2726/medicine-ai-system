package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索结果VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MallProductSearchVo {

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品名称", example = "商品名称")
    private String productName;

    @Schema(description = "商品封面", example = "https://example.com/product.jpg")
    private String cover;

    @Schema(description = "药品通用名", example = "感冒灵颗粒")
    private String commonName;

    @Schema(description = "商品分类名称列表", example = "[\"感冒用药\",\"中成药\"]")
    private List<String> categoryNames;

    @Schema(description = "药品品牌", example = "999")
    private String brand;

    @Schema(description = "商品价格", example = "9.99")
    private BigDecimal price;

    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "1")
    private Integer drugCategory;

    @Schema(description = "商品标签名称列表", example = "[\"退烧\",\"止咳\"]")
    private List<String> tagNames;

    @Schema(description = "功能主治", example = "解热镇痛")
    private String efficacy;

    @Schema(description = "禁忌信息", example = "严重肝肾功能不全者慎用")
    private String taboo;
}
