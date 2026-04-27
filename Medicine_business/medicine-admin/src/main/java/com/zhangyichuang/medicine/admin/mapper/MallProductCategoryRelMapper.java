package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.MallProductCategoryRel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 商品分类关联 Mapper。
 *
 * @author Chuang
 */
public interface MallProductCategoryRelMapper extends BaseMapper<MallProductCategoryRel> {

    /**
     * 按商品ID物理删除分类关联。
     *
     * @param productId 商品ID
     * @return 影响行数
     */
    @Delete("DELETE FROM mall_product_category_rel WHERE product_id = #{productId}")
    int physicalDeleteByProductId(@Param("productId") Long productId);

    /**
     * 按商品ID列表批量物理删除分类关联。
     *
     * @param productIds 商品ID列表
     * @return 影响行数
     */
    @Delete({
            "<script>",
            "DELETE FROM mall_product_category_rel",
            "WHERE product_id IN",
            "<foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "#{productId}",
            "</foreach>",
            "</script>"
    })
    int physicalDeleteByProductIds(@Param("productIds") java.util.List<Long> productIds);
}
