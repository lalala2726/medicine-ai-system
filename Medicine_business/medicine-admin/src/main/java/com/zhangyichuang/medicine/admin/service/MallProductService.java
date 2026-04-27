package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.request.MallProductAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductUpdateRequest;

import java.util.List;

/**
 * 商城商品服务接口
 * <p>
 * 提供商城商品的业务逻辑处理，包括商品的增删改查、
 * 商品列表查询、商品详情获取等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
public interface MallProductService extends IService<MallProduct> {

    /**
     * 获取商城商品列表
     *
     * @param request 查询参数
     * @return 分页的商城商品列表
     */
    Page<MallProduct> listMallProduct(MallProductListQueryRequest request);

    /**
     * 获取商城商品列表（包含分类名称）
     *
     * @param request 查询参数
     * @return 分页的商城商品列表（包含分类名称）
     */
    Page<MallProductDetailDto> listMallProductWithCategory(MallProductListQueryRequest request);

    /**
     * 使用 Elasticsearch 搜索商城商品并补齐分类等展示信息。
     *
     * @param request 查询参数
     * @return 分页的商城商品列表（基于 Elasticsearch 命中结果）
     */
    Page<MallProductDetailDto> searchMallProductWithCategory(MallProductListQueryRequest request);

    /**
     * 根据ID获取商城商品
     *
     * @param id 商品ID
     * @return 商城商品信息
     */
    MallProductDetailDto getMallProductById(Long id);

    /**
     * 根据ID列表批量获取商城商品
     *
     * @param ids 商品ID列表
     * @return 商城商品列表
     */
    List<MallProductDetailDto> getMallProductByIds(List<Long> ids);

    /**
     * 根据商品ID列表批量获取药品详情
     *
     * @param productIds 商品ID列表
     * @return 药品详情列表
     */
    List<AgentDrugDetailDto> getDrugDetailByProductIds(List<Long> productIds);

    /**
     * 添加商城商品
     *
     * @param request 添加参数
     * @return 添加结果
     */
    boolean addMallProduct(MallProductAddRequest request);

    /**
     * 修改商城商品
     *
     * @param request 修改参数
     * @return 修改结果
     */
    boolean updateMallProduct(MallProductUpdateRequest request);

    /**
     * 删除商城商品
     *
     * @param ids 商品ID列表
     * @return 删除结果
     */
    boolean deleteMallProduct(List<Long> ids);

}
