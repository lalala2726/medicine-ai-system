package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallProductImageMapper extends BaseMapper<MallProductImage> {

    /**
     * 获取商品封面图片
     *
     * @param productIds 商品ID集合
     * @return 商品封面图片列表
     */
    List<MallProductImage> getProductCoverImage(@Param("productIds") List<Long> productIds);
}




