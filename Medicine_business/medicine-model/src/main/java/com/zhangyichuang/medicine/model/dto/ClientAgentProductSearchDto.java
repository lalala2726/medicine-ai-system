package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 客户端智能体商品搜索结果 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体商品搜索结果")
public class ClientAgentProductSearchDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称", example = "999感冒灵颗粒")
    private String productName;

    /**
     * 药品通用名。
     */
    @Schema(description = "药品通用名", example = "感冒灵颗粒")
    private String commonName;

    /**
     * 商品分类名称列表。
     */
    @Schema(description = "商品分类名称列表", example = "[\"感冒用药\",\"中成药\"]")
    private List<String> categoryNames;

    /**
     * 药品品牌。
     */
    @Schema(description = "药品品牌", example = "999")
    private String brand;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
    private Integer drugCategory;

    /**
     * 商品标签名称列表。
     */
    @Schema(description = "商品标签名称列表", example = "[\"退烧\",\"止咳\"]")
    private List<String> tagNames;

    /**
     * 功能主治。
     */
    @Schema(description = "功能主治", example = "解热镇痛")
    private String efficacy;

    /**
     * 禁忌信息。
     */
    @Schema(description = "禁忌信息", example = "严重肝肾功能不全者慎用")
    private String taboo;

    /**
     * 商品价格。
     */
    @Schema(description = "商品价格", example = "29.90")
    private BigDecimal price;
}
