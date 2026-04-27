package com.zhangyichuang.medicine.agent.service.client;

import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;

/**
 * 客户端智能体售后服务接口。
 */
public interface ClientAgentAfterSaleService {

    /**
     * 查询售后详情。
     *
     * @param afterSaleNo 售后单号
     * @param userId      当前用户ID
     * @return 售后详情
     */
    AfterSaleDetailDto getAfterSaleDetail(String afterSaleNo, Long userId);

    /**
     * 校验售后资格。
     *
     * @param request 校验请求
     * @param userId  当前用户ID
     * @return 售后资格
     */
    ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId);

}
