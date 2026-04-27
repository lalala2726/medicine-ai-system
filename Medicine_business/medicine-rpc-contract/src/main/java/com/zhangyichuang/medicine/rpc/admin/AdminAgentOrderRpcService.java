package com.zhangyichuang.medicine.rpc.admin;

import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;

import java.util.List;
import java.util.Map;

/**
 * 管理端 Agent 订单只读 RPC。
 */
public interface AdminAgentOrderRpcService {

    /**
     * 分页查询订单及关联商品信息。
     *
     * @param query 订单查询参数
     * @return 分页订单结果
     */
    PageResult<OrderWithProductDto> listOrders(MallOrderListRequest query);

    /**
     * 根据订单编号列表批量查询订单详情。
     *
     * @param orderNos 订单编号列表
     * @return 订单详情列表
     */
    List<OrderDetailDto> getOrderDetailsByOrderNos(List<String> orderNos);

    /**
     * 根据订单编号列表批量查询智能体订单上下文。
     *
     * @param orderNos 订单编号列表
     * @return 按订单编号分组的订单上下文
     */
    Map<String, OrderContextDto> getOrderContextsByOrderNos(List<String> orderNos);
}
