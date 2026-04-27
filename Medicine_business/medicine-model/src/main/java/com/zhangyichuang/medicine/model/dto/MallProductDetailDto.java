package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品详情传输对象。
 *
 * @author Chuang
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallProductDetailDto extends MallProduct {

    /**
     * 商品分类ID列表。
     */
    private List<Long> categoryIds;

    /**
     * 商品分类名称列表。
     */
    private List<String> categoryNames;

    /**
     * 商品销量。
     */
    private Integer sales;

    /**
     * 商品图片列表。
     */
    private List<String> images;

    /**
     * 商品标签列表。
     */
    private List<MallProductTagVo> tags;

    /**
     * 药品详细信息。
     */
    private DrugDetailDto drugDetail;
}
