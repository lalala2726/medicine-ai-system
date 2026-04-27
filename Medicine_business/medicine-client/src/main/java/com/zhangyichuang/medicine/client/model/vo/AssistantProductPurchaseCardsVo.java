package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天商品购买卡片补全结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天商品购买卡片补全结果")
public class AssistantProductPurchaseCardsVo {

    @Schema(description = "整体价格", example = "36.70")
    private String totalPrice;

    @Schema(description = "商品列表")
    private List<AssistantProductPurchaseItemVo> items;

    /**
     * 商品购买卡片单项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商品购买卡片单项")
    public static class AssistantProductPurchaseItemVo {

        @Schema(description = "商品ID", example = "101")
        private String id;

        @Schema(description = "商品名称", example = "布洛芬缓释胶囊")
        private String name;

        @Schema(description = "商品主图", example = "https://example.com/images/101.png")
        private String image;

        @Schema(description = "商品销售价", example = "16.80")
        private String price;

        @Schema(description = "规格", example = "24粒/盒")
        private String spec;

        @Schema(description = "功效/适应症", example = "缓解发热、头痛")
        private String efficacy;

        @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "1")
        private Integer drugCategory;

        @Schema(description = "库存", example = "56")
        private Integer stock;
    }
}
