package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 客户端智能体商品卡片。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体商品卡片")
@FieldDescription(description = "客户端智能体商品卡片")
public class ClientAgentProductCardsVo {

    @Schema(description = "整体价格", example = "36.70")
    @FieldDescription(description = "整体价格")
    private String totalPrice;

    @Schema(description = "商品列表")
    @FieldDescription(description = "商品列表")
    private List<ClientAgentProductItemVo> items;

    @Data
    @Schema(description = "商品卡片单项")
    @FieldDescription(description = "商品卡片单项")
    public static class ClientAgentProductItemVo {

        @Schema(description = "商品ID", example = "101")
        @FieldDescription(description = "商品ID")
        private String id;

        @Schema(description = "商品名称", example = "布洛芬缓释胶囊")
        @FieldDescription(description = "商品名称")
        private String name;

        @Schema(description = "商品主图", example = "https://example.com/images/101.png")
        @FieldDescription(description = "商品主图")
        private String image;

        @Schema(description = "商品销售价", example = "16.80")
        @FieldDescription(description = "商品销售价")
        private String price;

        @Schema(description = "规格", example = "24粒/盒")
        @FieldDescription(description = "规格")
        private String spec;

        @Schema(description = "功效/适应症", example = "缓解发热、头痛")
        @FieldDescription(description = "功效/适应症")
        private String efficacy;

        @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
        @FieldDescription(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）")
        private Integer drugCategory;

        @Schema(description = "库存", example = "56")
        @FieldDescription(description = "库存")
        private Integer stock;
    }
}
