package com.zhangyichuang.medicine.agent.service.client;

import com.zhangyichuang.medicine.model.dto.*;

/**
 * 客户端智能体订单服务接口。
 */
public interface ClientAgentOrderService {

    /**
     * 查询订单卡摘要。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单卡摘要
     */
    ClientAgentOrderCardSummaryDto getOrderCardSummary(String orderNo, Long userId);

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单详情
     */
    ClientAgentOrderDetailDto getOrderDetail(String orderNo, Long userId);

    /**
     * 查询订单物流。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单物流
     */
    ClientAgentOrderShippingDto getOrderShipping(String orderNo, Long userId);

    /**
     * 查询订单时间线。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 订单时间线
     */
    ClientAgentOrderTimelineDto getOrderTimeline(String orderNo, Long userId);

    /**
     * 校验订单是否允许取消。
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     * @return 取消资格
     */
    ClientAgentOrderCancelCheckDto checkOrderCancelable(String orderNo, Long userId);
}
