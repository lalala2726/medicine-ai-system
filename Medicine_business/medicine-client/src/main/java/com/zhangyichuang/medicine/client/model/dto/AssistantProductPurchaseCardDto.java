package com.zhangyichuang.medicine.client.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 聊天商品购买卡片商品查询 DTO。
 */
@Data
public class AssistantProductPurchaseCardDto {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品主图
     */
    private String image;

    /**
     * 商品销售价
     */
    private BigDecimal price;

    /**
     * 规格
     */
    private String spec;

    /**
     * 功效/适应症
     */
    private String efficacy;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）
     */
    private Integer drugCategory;

    /**
     * 库存
     */
    private Integer stock;
}
