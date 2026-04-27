package com.zhangyichuang.medicine.agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.request.AdminMallOrderListRequest;
import com.zhangyichuang.medicine.agent.model.vo.admin.OrderDetailVo;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;

import java.util.List;
import java.util.Map;

/**
 * 智能体订单服务接口。
 * <p>
 * 提供订单相关的查询服务，包括订单列表查询和订单详情查询。
 *
 * @author Chuang
 */
public interface MallOrderService {

    /**
     * 分页查询订单列表。
     *
     * @param request 查询请求参数，包含订单状态、时间范围等筛选条件
     * @return 订单与商品信息分页数据
     */
    Page<OrderWithProductDto> listOrders(AdminMallOrderListRequest request);

    /**
     * 根据订单编号批量查询订单详细信息。
     *
     * @param orderNos 订单编号列表
     * @return 订单详情列表
     */
    List<OrderDetailVo> getOrderDetail(List<String> orderNos);

    /**
     * 根据订单编号批量查询智能体订单上下文。
     *
     * @param orderNos 订单编号列表
     * @return 按订单编号分组的订单上下文
     */
    Map<String, OrderContextDto> getOrderContext(List<String> orderNos);
}
