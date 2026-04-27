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
 * 客户端智能体商品购买卡片结果 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体商品购买卡片结果")
public class ClientAgentProductPurchaseCardsDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 整体总价。
     */
    @Schema(description = "整体总价", example = "53.50")
    private BigDecimal totalPrice;

    /**
     * 商品列表。
     */
    @Schema(description = "商品列表")
    private List<ClientAgentProductPurchaseItemDto> items;

    /**
     * 商品购买卡片单项 DTO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商品购买卡片单项")
    public static class ClientAgentProductPurchaseItemDto implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品ID。
         */
        @Schema(description = "商品ID", example = "101")
        private String id;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称", example = "布洛芬缓释胶囊")
        private String name;

        /**
         * 商品主图。
         */
        @Schema(description = "商品主图", example = "https://example.com/images/101.png")
        private String image;

        /**
         * 商品销售价。
         */
        @Schema(description = "商品销售价", example = "16.80")
        private BigDecimal price;

        /**
         * 购买数量。
         */
        @Schema(description = "购买数量", example = "2")
        private Integer quantity;

        /**
         * 规格。
         */
        @Schema(description = "规格", example = "24粒/盒")
        private String spec;

        /**
         * 功效/适应症。
         */
        @Schema(description = "功效/适应症", example = "缓解发热、头痛")
        private String efficacy;

        /**
         * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
         */
        @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "1")
        private Integer drugCategory;

        /**
         * 库存。
         */
        @Schema(description = "库存", example = "56")
        private Integer stock;
    }
}
