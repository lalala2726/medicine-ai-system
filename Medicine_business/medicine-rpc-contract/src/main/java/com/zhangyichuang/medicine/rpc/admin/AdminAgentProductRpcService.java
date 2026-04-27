package com.zhangyichuang.medicine.rpc.admin;

import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;

import java.util.List;

/**
 * 管理端 Agent 商品只读 RPC。
 */
public interface AdminAgentProductRpcService {

    /**
     * 分页查询商品列表（包含分类等展示信息）。
     *
     * @param query 商品查询参数
     * @return 分页商品结果
     */
    PageResult<MallProductDetailDto> listProducts(MallProductListQueryRequest query);

    /**
     * 使用 Elasticsearch 分页搜索商品列表。
     *
     * @param query 商品查询参数
     * @return 分页商品结果
     */
    PageResult<MallProductDetailDto> searchProducts(MallProductListQueryRequest query);

    /**
     * 根据商品 ID 列表批量查询商品详情。
     *
     * @param productIds 商品 ID 列表
     * @return 商品详情列表
     */
    List<MallProductDetailDto> getProductDetailsByIds(List<Long> productIds);

    /**
     * 根据商品 ID 列表批量查询药品说明信息。
     *
     * @param productIds 商品 ID 列表
     * @return 药品详情列表
     */
    List<AgentDrugDetailDto> getDrugDetailsByProductIds(List<Long> productIds);
}
