package com.zhangyichuang.medicine.agent.service.client.impl;

import com.zhangyichuang.medicine.agent.service.client.ClientAgentOrderService;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.rpc.client.ClientAgentOrderRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 客户端智能体订单服务 Dubbo Consumer 实现。
 */
@Service
public class ClientAgentOrderServiceImpl implements ClientAgentOrderService {

    /**
     * 订单模块 Dubbo RPC 引用。
     */
    @DubboReference(group = "medicine-client", version = "1.0.0", check = false, timeout = 10000, retries = 0,
            url = "${dubbo.references.medicine-client.url:}")
    private ClientAgentOrderRpcService clientAgentOrderRpcService;

    /**
     * 调用订单模块查询当前用户订单卡摘要。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单卡摘要
     */
    @Override
    public ClientAgentOrderCardSummaryDto getOrderCardSummary(String orderNo, Long userId) {
        return clientAgentOrderRpcService.getOrderCardSummary(orderNo, userId);
    }

    /**
     * 调用订单模块查询当前用户订单详情。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单详情
     */
    @Override
    public ClientAgentOrderDetailDto getOrderDetail(String orderNo, Long userId) {
        return clientAgentOrderRpcService.getOrderDetail(orderNo, userId);
    }

    /**
     * 调用订单模块查询当前用户订单物流。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单物流
     */
    @Override
    public ClientAgentOrderShippingDto getOrderShipping(String orderNo, Long userId) {
        return clientAgentOrderRpcService.getOrderShipping(orderNo, userId);
    }

    /**
     * 调用订单模块查询当前用户订单时间线。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单时间线
     */
    @Override
    public ClientAgentOrderTimelineDto getOrderTimeline(String orderNo, Long userId) {
        return clientAgentOrderRpcService.getOrderTimeline(orderNo, userId);
    }

    /**
     * 调用订单模块校验当前用户订单是否允许取消。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 取消资格
     */
    @Override
    public ClientAgentOrderCancelCheckDto checkOrderCancelable(String orderNo, Long userId) {
        return clientAgentOrderRpcService.checkOrderCancelable(orderNo, userId);
    }
}
