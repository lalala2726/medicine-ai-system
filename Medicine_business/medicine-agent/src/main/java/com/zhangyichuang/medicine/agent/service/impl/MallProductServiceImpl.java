package com.zhangyichuang.medicine.agent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentDrugDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductDetailVo;
import com.zhangyichuang.medicine.agent.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentProductRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 商品服务 Dubbo Consumer 实现。
 */
@Service
public class MallProductServiceImpl implements MallProductService {

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 3000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentProductRpcService adminAgentProductRpcService;

    @Override
    public Page<MallProductDetailDto> listProducts(MallProductListQueryRequest request) {
        MallProductListQueryRequest safeRequest = request == null ? new MallProductListQueryRequest() : request;
        PageResult<MallProductDetailDto> result = adminAgentProductRpcService.listProducts(safeRequest);
        return toPage(result);
    }

    /**
     * 调用管理端 RPC 执行 Elasticsearch 商品搜索。
     *
     * @param request 查询请求参数
     * @return 商品详情分页数据
     */
    @Override
    public Page<MallProductDetailDto> searchProducts(MallProductListQueryRequest request) {
        MallProductListQueryRequest safeRequest = request == null ? new MallProductListQueryRequest() : request;
        PageResult<MallProductDetailDto> result = adminAgentProductRpcService.searchProducts(safeRequest);
        return toPage(result);
    }

    @Override
    public List<AgentProductDetailVo> getProductDetail(List<Long> productIds) {
        List<MallProductDetailDto> products = adminAgentProductRpcService.getProductDetailsByIds(productIds);
        return BeanCotyUtils.copyListProperties(products, AgentProductDetailVo.class);
    }

    @Override
    public List<AgentDrugDetailVo> getDrugDetail(List<Long> productIds) {
        List<AgentDrugDetailDto> drugDetails = adminAgentProductRpcService.getDrugDetailsByProductIds(productIds);
        return BeanCotyUtils.copyListProperties(drugDetails, AgentDrugDetailVo.class);
    }

    /**
     * 将 RPC 分页结果转换为 MyBatis Plus Page。
     *
     * @param result RPC 分页结果
     * @return MyBatis Plus 分页对象
     */
    private Page<MallProductDetailDto> toPage(PageResult<MallProductDetailDto> result) {
        if (result == null) {
            return new Page<>(1, 10, 0);
        }
        long pageNum = result.getPageNum() == null ? 1L : result.getPageNum();
        long pageSize = result.getPageSize() == null ? 10L : result.getPageSize();
        long total = result.getTotal() == null ? 0L : result.getTotal();

        Page<MallProductDetailDto> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(result.getRows() == null ? List.of() : result.getRows());
        return page;
    }
}
