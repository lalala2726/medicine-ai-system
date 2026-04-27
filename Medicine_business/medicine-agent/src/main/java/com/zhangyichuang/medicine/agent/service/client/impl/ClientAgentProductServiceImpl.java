package com.zhangyichuang.medicine.agent.service.client.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentProductService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;
import com.zhangyichuang.medicine.rpc.client.ClientAgentProductRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户端智能体商品服务 Dubbo Consumer 实现。
 */
@Service
public class ClientAgentProductServiceImpl implements ClientAgentProductService {

    /**
     * 商品模块 Dubbo RPC 引用。
     */
    @DubboReference(group = "medicine-client", version = "1.0.0", check = false, timeout = 3000, retries = 0,
            url = "${dubbo.references.medicine-client.url:}")
    private ClientAgentProductRpcService clientAgentProductRpcService;

    /**
     * 调用商品模块执行搜索，并将 RPC 分页转换为 MyBatis Plus 分页对象。
     *
     * @param request 搜索参数
     * @return 商品分页结果
     */
    @Override
    public Page<ClientAgentProductSearchDto> searchProducts(ClientAgentProductSearchRequest request) {
        ClientAgentProductSearchRequest safeRequest = request == null ? new ClientAgentProductSearchRequest() : request;
        PageResult<ClientAgentProductSearchDto> result = clientAgentProductRpcService.searchProducts(safeRequest);
        return toPage(result);
    }

    /**
     * 调用商品模块查询商品搜索标签目录。
     *
     * @return 标签分组列表
     */
    @Override
    public List<ClientAgentProductSearchTagFilterDto> listProductSearchTagFilters() {
        List<ClientAgentProductSearchTagFilterDto> result = clientAgentProductRpcService.listProductSearchTagFilters();
        return result == null ? List.of() : result;
    }

    /**
     * 调用商品模块批量查询统一药品详情。
     *
     * @param productIds 商品ID列表
     * @return 商品详情列表
     */
    @Override
    public List<ClientAgentProductDetailDto> getProductDetails(List<Long> productIds) {
        return clientAgentProductRpcService.getProductDetails(productIds);
    }

    /**
     * 调用商品模块查询商品卡片补全结果。
     *
     * @param productIds 商品ID列表
     * @return 商品卡片补全结果
     */
    @Override
    public ClientAgentProductCardsDto getProductCards(List<Long> productIds) {
        ClientAgentProductCardsDto result = clientAgentProductRpcService.getProductCards(productIds);
        if (result == null) {
            return emptyProductCards();
        }
        if (result.getItems() == null) {
            result.setItems(List.of());
        }
        if (result.getTotalPrice() == null) {
            result.setTotalPrice("0.00");
        }
        return result;
    }

    /**
     * 调用商品模块查询商品购买卡片结果。
     *
     * @param items 商品购买项列表
     * @return 商品购买卡片结果
     */
    @Override
    public ClientAgentProductPurchaseCardsDto getProductPurchaseCards(List<ClientAgentProductPurchaseQueryDto> items) {
        ClientAgentProductPurchaseCardsDto result = clientAgentProductRpcService.getProductPurchaseCards(items);
        if (result == null) {
            return emptyProductPurchaseCards();
        }
        if (result.getItems() == null) {
            result.setItems(List.of());
        }
        if (result.getTotalPrice() == null) {
            result.setTotalPrice(new java.math.BigDecimal("0.00"));
        }
        return result;
    }

    /**
     * 将 RPC 分页结果转换为 MyBatis Plus Page，便于 controller 统一返回表格结构。
     *
     * @param result RPC 分页结果
     * @return MyBatis Plus 分页对象
     */
    private Page<ClientAgentProductSearchDto> toPage(PageResult<ClientAgentProductSearchDto> result) {
        if (result == null) {
            return new Page<>(1, 10, 0);
        }
        long pageNum = result.getPageNum() == null ? 1L : result.getPageNum();
        long pageSize = result.getPageSize() == null ? 10L : result.getPageSize();
        long total = result.getTotal() == null ? 0L : result.getTotal();
        Page<ClientAgentProductSearchDto> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(result.getRows() == null ? List.of() : result.getRows());
        return page;
    }

    /**
     * 构造空商品卡片结果。
     *
     * @return 空商品卡片结果
     */
    private ClientAgentProductCardsDto emptyProductCards() {
        return ClientAgentProductCardsDto.builder()
                .totalPrice("0.00")
                .items(List.of())
                .build();
    }

    /**
     * 构造空购买卡片结果。
     *
     * @return 空购买卡片结果
     */
    private ClientAgentProductPurchaseCardsDto emptyProductPurchaseCards() {
        return ClientAgentProductPurchaseCardsDto.builder()
                .totalPrice(new java.math.BigDecimal("0.00"))
                .items(List.of())
                .build();
    }
}
