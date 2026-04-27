package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
public interface MallProductMapper extends BaseMapper<MallProduct> {

    /**
     * 获取商城商品列表
     *
     * @param page    分页参数
     * @param request 查询参数
     * @return 分页的商城商品列表
     */
    Page<MallProduct> listMallProduct(Page<MallProduct> page, @Param("request") MallProductListQueryRequest request);

    /**
     * 获取商城商品列表（包含分类名称）
     *
     * @param page    分页参数
     * @param request 查询参数
     * @return 分页的商城商品列表（包含分类名称）
     */
    Page<MallProductDetailDto> listMallProductWithCategory(Page<MallProductDetailDto> page, @Param("request") MallProductListQueryRequest request);

    /**
     * 根据ID获取商城商品详情（包含图片信息）
     *
     * @param id 商品ID
     * @return 商城商品详情
     */
    MallProductDetailDto getMallProductDetailById(@Param("id") Long id);

    /**
     * 根据ID列表批量获取商城商品详情
     *
     * @param ids 商品ID列表
     * @return 商城商品详情列表
     */
    java.util.List<MallProductDetailDto> getMallProductDetailByIds(@Param("ids") java.util.List<Long> ids);
}
