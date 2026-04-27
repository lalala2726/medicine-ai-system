package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentProductRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 管理端 Agent 商品 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentProductRpcService.class, group = "medicine-admin", version = "1.0.0")
@RequiredArgsConstructor
public class AdminAgentProductRpcServiceImpl implements AdminAgentProductRpcService {

    private final MallProductService mallProductService;

    @Override
    public PageResult<MallProductDetailDto> listProducts(MallProductListQueryRequest query) {
        MallProductListQueryRequest request = query == null ? new MallProductListQueryRequest() : query;
        Page<MallProductDetailDto> page = mallProductService.listMallProductWithCategory(request);
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    /**
     * 使用 Elasticsearch 搜索商品列表。
     *
     * @param query 商品查询参数
     * @return 分页商品结果
     */
    @Override
    public PageResult<MallProductDetailDto> searchProducts(MallProductListQueryRequest query) {
        MallProductListQueryRequest request = query == null ? new MallProductListQueryRequest() : query;
        Page<MallProductDetailDto> page = mallProductService.searchMallProductWithCategory(request);
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<MallProductDetailDto> getProductDetailsByIds(List<Long> productIds) {
        return mallProductService.getMallProductByIds(productIds);
    }

    @Override
    public List<AgentDrugDetailDto> getDrugDetailsByProductIds(List<Long> productIds) {
        return mallProductService.getDrugDetailByProductIds(productIds);
    }
}
