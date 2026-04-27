package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品及图片传输对象。
 *
 * @author Chuang
 * created on 2025/11/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallProductWithImageDto extends MallProduct {

    /**
     * 商品图片列表。
     */
    private List<MallProductImage> productImages;

    /**
     * 药品详细信息。
     */
    private DrugDetailDto drugDetail;

    /**
     * 商品标签列表。
     */
    private List<MallProductTagVo> tags;

    /**
     * 商品销量（已完成订单数量汇总）。
     */
    private Integer sales;
}
