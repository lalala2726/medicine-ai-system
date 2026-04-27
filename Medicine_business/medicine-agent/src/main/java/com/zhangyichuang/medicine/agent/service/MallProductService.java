package com.zhangyichuang.medicine.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentDrugDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductDetailVo;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;

import java.util.List;

/**
 * 智能体商品服务接口。
 * <p>
 * 提供商品相关的查询服务，包括商品列表查询、商品详情查询和药品详情查询。
 *
 * @author Chuang
 */
public interface MallProductService {

    /**
     * 分页查询商品列表。
     *
     * @param request 查询请求参数，包含关键词、分类等筛选条件
     * @return 商品详情分页数据
     */
    Page<MallProductDetailDto> listProducts(MallProductListQueryRequest request);

    /**
     * 使用 Elasticsearch 分页搜索商品列表。
     *
     * @param request 查询请求参数，包含关键词、分类、标签等筛选条件
     * @return 商品详情分页数据
     */
    Page<MallProductDetailDto> searchProducts(MallProductListQueryRequest request);

    /**
     * 批量查询商品详情。
     *
     * @param productIds 商品 ID 列表
     * @return 商品详情列表
     */
    List<AgentProductDetailVo> getProductDetail(List<Long> productIds);

    /**
     * 批量查询药品详情。
     *
     * @param productIds 商品 ID 列表
     * @return 药品详情列表
     */
    List<AgentDrugDetailVo> getDrugDetail(List<Long> productIds);
}
