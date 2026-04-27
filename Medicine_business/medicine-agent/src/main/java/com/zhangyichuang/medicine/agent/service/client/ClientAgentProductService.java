package com.zhangyichuang.medicine.agent.service.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;

import java.util.List;

/**
 * 客户端智能体商品服务接口。
 */
public interface ClientAgentProductService {

    /**
     * 搜索商品。
     *
     * @param request 搜索参数
     * @return 分页结果
     */
    Page<ClientAgentProductSearchDto> searchProducts(ClientAgentProductSearchRequest request);

    /**
     * 查询商品搜索标签目录。
     *
     * @return 标签分组列表
     */
    List<ClientAgentProductSearchTagFilterDto> listProductSearchTagFilters();

    /**
     * 批量查询统一药品详情。
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
