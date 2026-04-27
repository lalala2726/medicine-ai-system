package com.zhangyichuang.medicine.rpc.client;

import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;

/**
 * 客户端智能体售后只读 RPC。
 */
public interface ClientAgentAfterSaleRpcService {

    /**
     * 根据售后单号查询当前用户售后详情。
     *
     * @param afterSaleNo 售后单号
     * @param userId      当前用户ID
     * @return 售后详情
     */
    AfterSaleDetailDto getAfterSaleDetail(String afterSaleNo, Long userId);

    /**
     * 校验当前用户订单或订单项是否满足售后资格。
     *
     * @param request 校验请求
     * @param userId  当前用户ID
     * @return 售后资格
     */
    ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId);

}
