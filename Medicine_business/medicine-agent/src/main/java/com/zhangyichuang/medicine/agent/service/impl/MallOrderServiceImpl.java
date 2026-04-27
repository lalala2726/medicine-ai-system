package com.zhangyichuang.medicine.agent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.request.AdminMallOrderListRequest;
import com.zhangyichuang.medicine.agent.model.vo.admin.OrderDetailVo;
import com.zhangyichuang.medicine.agent.service.MallOrderService;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentOrderRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 订单服务 Dubbo Consumer 实现。
 */
@Service
public class MallOrderServiceImpl implements MallOrderService {

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 10000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentOrderRpcService adminAgentOrderRpcService;

    @Override
    public Page<OrderWithProductDto> listOrders(AdminMallOrderListRequest request) {
        AdminMallOrderListRequest safeRequest = request == null ? new AdminMallOrderListRequest() : request;
        MallOrderListRequest query = BeanCotyUtils.copyProperties(safeRequest, MallOrderListRequest.class);
        PageResult<OrderWithProductDto> result = adminAgentOrderRpcService.listOrders(query);
        return toPage(result);
    }

    @Override
    public List<OrderDetailVo> getOrderDetail(List<String> orderNos) {
        List<OrderDetailDto> details = adminAgentOrderRpcService.getOrderDetailsByOrderNos(orderNos);
        return details.stream().map(this::toAdminOrderDetail).toList();
    }

    /**
     * 根据订单编号批量查询智能体订单上下文。
     *
     * @param orderNos 订单编号列表
     * @return 按订单编号分组的订单上下文
     */
    @Override
    public Map<String, OrderContextDto> getOrderContext(List<String> orderNos) {
        return adminAgentOrderRpcService.getOrderContextsByOrderNos(orderNos);
    }

    private Page<OrderWithProductDto> toPage(PageResult<OrderWithProductDto> result) {
        if (result == null) {
            return new Page<>(1, 10, 0);
        }
        long pageNum = result.getPageNum() == null ? 1L : result.getPageNum();
        long pageSize = result.getPageSize() == null ? 10L : result.getPageSize();
        long total = result.getTotal() == null ? 0L : result.getTotal();
        Page<OrderWithProductDto> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(result.getRows() == null ? List.of() : result.getRows());
        return page;
    }

    private OrderDetailVo toAdminOrderDetail(OrderDetailDto source) {
        if (source == null) {
            return null;
        }
        OrderDetailVo target = new OrderDetailVo();
        target.setUserInfo(BeanCotyUtils.copyProperties(source.getUserInfo(), OrderDetailVo.UserInfo.class));
        target.setDeliveryInfo(BeanCotyUtils.copyProperties(source.getDeliveryInfo(), OrderDetailVo.DeliveryInfo.class));
        target.setOrderInfo(BeanCotyUtils.copyProperties(source.getOrderInfo(), OrderDetailVo.OrderInfo.class));
        target.setProductInfo(BeanCotyUtils.copyListProperties(source.getProductInfo(), OrderDetailVo.ProductInfo.class));
        return target;
    }
}
