package com.zhangyichuang.medicine.rpc.client;

import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;

import java.util.List;

/**
 * 客户端智能体商品只读 RPC。
 */
public interface ClientAgentProductRpcService {

    /**
     * 按关键词分页搜索商品。
     *
     * @param request 商品搜索参数
     * @return 商品分页结果
     */
    PageResult<ClientAgentProductSearchDto> searchProducts(ClientAgentProductSearchRequest request);

    /**
     * 查询商品搜索标签目录。
     *
     * @return 标签分组列表
     */
    List<ClientAgentProductSearchTagFilterDto> listProductSearchTagFilters();

    /**
     * 批量查询商品统一药品详情。
     *
     * @param productIds 商品ID列表
     * @return 商品详情列表
     */
    List<ClientAgentProductDetailDto> getProductDetails(List<Long> productIds);

    /**
     * 查询商品卡片补全结果。
     *
     * @param productIds 商品ID列表
     * @return 商品卡片补全结果
     */
    ClientAgentProductCardsDto getProductCards(List<Long> productIds);

    /**
     * 查询商品购买卡片结果。
     *
     * @param items 商品购买项列表
     * @return 商品购买卡片结果
     */
    ClientAgentProductPurchaseCardsDto getProductPurchaseCards(List<ClientAgentProductPurchaseQueryDto> items);

}
