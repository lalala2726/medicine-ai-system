package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.client.model.dto.AssistantProductPurchaseCardDto;
import com.zhangyichuang.medicine.client.model.dto.RecommendProductDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductWithImageDto;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallProductMapper extends BaseMapper<MallProduct> {

    /**
     * 根据乐观锁版本号更新库存
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @param version   版本号
     * @return 更新记录数
     */
    int updateStockWithVersion(@Param("productId") Long productId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    /**
     * 根据商品ID获取商品详情
     *
     * @param productId 商品ID
     * @return 商品详情
     */
    MallProductWithImageDto getProductWithImagesById(@Param("productId") Long productId);

    /**
     * 获取商品详情（含药品详情、封面）
     */
    MallProductDetailDto getProductAndDrugInfoById(@Param("productId") Long productId);

    /**
     * 批量查询聊天商品购买卡片商品信息。
     *
     * @param productIds 商品ID集合
     * @return 商品购买卡片商品信息
     */
    List<AssistantProductPurchaseCardDto> listAssistantProductPurchaseCardsByIds(@Param("productIds") List<Long> productIds);

    /**
     * 根据销量与浏览量推荐商品
     */
    List<RecommendProductDto> listRecommendProducts();
}



