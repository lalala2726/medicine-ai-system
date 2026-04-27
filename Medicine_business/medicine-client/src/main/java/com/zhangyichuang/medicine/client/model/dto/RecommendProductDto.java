package com.zhangyichuang.medicine.client.model.dto;

import com.zhangyichuang.medicine.model.entity.MallProduct;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推荐商品基础信息，附带销量、浏览量。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendProductDto extends MallProduct {

    /**
     * 完成订单的销量
     */
    private Integer sales;

    /**
     * 浏览次数
     */
    private Long views;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    private Integer drugCategory;
}
