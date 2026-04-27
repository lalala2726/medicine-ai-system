package com.zhangyichuang.medicine.agent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.service.MallAfterSaleService;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAfterSaleRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 售后服务 Dubbo Consumer 实现。
 */
@Service
public class MallAfterSaleServiceImpl implements MallAfterSaleService {

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 10000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentAfterSaleRpcService adminAgentAfterSaleRpcService;

    /**
     * 功能描述：通过 Dubbo 调用管理端服务，查询售后分页数据。
     *
     * @param request 售后分页查询参数，包含分页信息与筛选条件
     * @return 返回售后分页结果，记录类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当 Dubbo 远程调用失败时抛出运行时异常
     */
    @Override
    public Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest request) {
        return adminAgentAfterSaleRpcService.listAfterSales(request);
    }

    /**
     * 功能描述：通过 Dubbo 调用管理端服务批量查询售后详情。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回售后详情 DTO 列表
     * @throws RuntimeException 异常说明：当 Dubbo 远程调用失败时抛出运行时异常
     */
    @Override
    public List<AfterSaleDetailDto> getAfterSaleDetails(List<String> afterSaleNos) {
        return adminAgentAfterSaleRpcService.getAfterSaleDetailsByAfterSaleNos(afterSaleNos);
    }

    /**
     * 功能描述：通过 Dubbo 调用管理端服务批量查询售后聚合上下文。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回按售后单号分组的售后上下文
     * @throws RuntimeException 异常说明：当 Dubbo 远程调用失败时抛出运行时异常
     */
    @Override
    public Map<String, AfterSaleContextDto> getAfterSaleContexts(List<String> afterSaleNos) {
        return adminAgentAfterSaleRpcService.getAfterSaleContextsByAfterSaleNos(afterSaleNos);
    }
}
