package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供搜索索引使用的商品基础数据。
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIndexPayload {

    /**
     * 商品ID。
     */
    private Long id;

    /**
     * 商品名称。
     */
    private String name;

    /**
     * 分类名称列表。
     */
    private List<String> categoryNames;

    /**
     * 分类ID列表。
     */
    private List<Long> categoryIds;

    /**
     * 价格。
     */
    private BigDecimal price;

    /**
     * 商品销量。
     */
    private Integer sales;

    /**
     * 商品状态。
     */
    private Integer status;

    /**
     * 药品品牌。
     */
    private String brand;

    /**
     * 药品通用名。
     */
    private String commonName;

    /**
     * 药品功效。
     */
    private String efficacy;

    /**
     * 全量标签ID列表。
     */
    private List<Long> tagIds;

    /**
     * 聚合后的标签名称列表。
     */
    private List<String> tagNames;

    /**
     * 商品关键字补全输入列表。
     */
    private List<String> keywordSuggestInputs;

    /**
     * 标签类型绑定列表，格式为 typeCode:tagId。
     */
    private List<String> tagTypeBindings;

    /**
     * 提醒信息。
     */
    private String warmTips;

    /**
     * 使用说明。
     */
    private String instruction;

    /**
     * 禁忌信息。
     */
    private String taboo;

    /**
     * 封面图片。
     */
    private String coverImage;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    private Integer drugCategory;
}
