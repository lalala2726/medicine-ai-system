package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户端智能体商品购买卡片结果。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体商品购买卡片结果")
@FieldDescription(description = "客户端智能体商品购买卡片结果")
public class ClientAgentProductPurchaseCardsVo {

    /**
     * 整体总价。
     */
    @Schema(description = "整体总价", example = "53.50")
    @FieldDescription(description = "整体总价")
    private BigDecimal totalPrice;

    /**
     * 商品列表。
     */
    @Schema(description = "商品列表")
    @FieldDescription(description = "商品列表")
    private List<ClientAgentProductPurchaseItemVo> items;

    /**
     * 商品购买卡片单项。
     */
    @Data
    @Schema(description = "商品购买卡片单项")
    @FieldDescription(description = "商品购买卡片单项")
    public static class ClientAgentProductPurchaseItemVo {

        /**
         * 商品ID。
         */
        @Schema(description = "商品ID", example = "101")
        @FieldDescription(description = "商品ID")
        private String id;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称", example = "布洛芬缓释胶囊")
        @FieldDescription(description = "商品名称")
        private String name;

        /**
         * 商品主图。
         */
        @Schema(description = "商品主图", example = "https://example.com/images/101.png")
        @FieldDescription(description = "商品主图")
        private String image;

        /**
         * 商品销售价。
         */
        @Schema(description = "商品销售价", example = "16.80")
        @FieldDescription(description = "商品销售价")
        private BigDecimal price;

        /**
         * 购买数量。
         */
        @Schema(description = "购买数量", example = "2")
        @FieldDescription(description = "购买数量")
        private Integer quantity;

        /**
         * 规格。
         */
        @Schema(description = "规格", example = "24粒/盒")
        @FieldDescription(description = "规格")
        private String spec;

        /**
         * 功效/适应症。
         */
        @Schema(description = "功效/适应症", example = "缓解发热、头痛")
        @FieldDescription(description = "功效/适应症")
        private String efficacy;

        /**
         * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
         */
        @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "1")
        @FieldDescription(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）")
        private Integer drugCategory;

        /**
         * 库存。
         */
        @Schema(description = "库存", example = "56")
        @FieldDescription(description = "库存")
        private Integer stock;
    }
}
