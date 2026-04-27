package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author Chuang
 */
@Validated
public interface MallProductImageService extends IService<MallProductImage> {

    /**
     * 获取商品封面图片
     *
     * @param productId 商品ID
     * @return 商品封面图片
     */
    String getProductCoverImage(@NotNull(message = "商品ID不能为空") Long productId);

    /**
     * 获取商品图片列表
     *
     * @param productIds 商品ID集合
     * @return 商品图片列表
     */
    List<MallProductImage> getProductCoverImage(@NotNull(message = "商品ID不能为空") List<Long> productIds);
}
