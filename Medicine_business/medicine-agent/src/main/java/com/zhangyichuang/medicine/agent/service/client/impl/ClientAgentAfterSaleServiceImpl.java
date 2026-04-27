package com.zhangyichuang.medicine.agent.service.client.impl;

import com.zhangyichuang.medicine.agent.service.client.ClientAgentAfterSaleService;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.rpc.client.ClientAgentAfterSaleRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 客户端智能体售后服务 Dubbo Consumer 实现。
 */
@Service
public class ClientAgentAfterSaleServiceImpl implements ClientAgentAfterSaleService {

    /**
     * 售后模块 Dubbo RPC 引用。
     */
    @DubboReference(group = "medicine-client", version = "1.0.0", check = false, timeout = 10000, retries = 0,
            url = "${dubbo.references.medicine-client.url:}")
    private ClientAgentAfterSaleRpcService clientAgentAfterSaleRpcService;

    /**
     * 调用售后模块查询当前用户售后详情。
     *
     * @param afterSaleNo 售后单号
     * @param userId      当前用户ID
     * @return 售后详情
     */
    @Override
    public AfterSaleDetailDto getAfterSaleDetail(String afterSaleNo, Long userId) {
        return clientAgentAfterSaleRpcService.getAfterSaleDetail(afterSaleNo, userId);
    }

    /**
     * 调用售后模块校验当前用户售后资格。
     *
     * @param request 校验请求
     * @param userId  当前用户ID
     * @return 售后资格
     */
    @Override
    public ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId) {
        return clientAgentAfterSaleRpcService.checkAfterSaleEligibility(request, userId);
    }
}
