package com.zhangyichuang.medicine.agent.controller.client;

import com.zhangyichuang.medicine.agent.model.vo.client.*;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentOrderService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端智能体订单工具控制器。
 */
@RestController
@RequestMapping("/agent/client/order")
@Tag(name = "客户端智能体订单工具", description = "用于客户端智能体订单查询接口")
@RequiredArgsConstructor
public class AgentClientOrderController extends BaseController {

    /**
     * 客户端智能体订单服务。
     */
    private final ClientAgentOrderService clientAgentOrderService;

    /**
     * 根据订单编号查询当前登录用户订单卡摘要。
     *
     * @param orderNo 订单编号
     * @return 订单卡摘要
     */
    @GetMapping("/summary/{orderNo}")
    @Operation(summary = "获取订单卡摘要", description = "根据订单编号获取当前登录用户的订单卡摘要")
    public AjaxResult<ClientAgentOrderCardSummaryVo> getOrderCardSummary(
            @Parameter(description = "订单编号", required = true)
            @PathVariable String orderNo
    ) {
        ClientAgentOrderCardSummaryDto summary = clientAgentOrderService.getOrderCardSummary(orderNo, getUserId());
        ClientAgentOrderCardSummaryVo target = copyProperties(summary, ClientAgentOrderCardSummaryVo.class);
        if (summary.getPreviewProduct() != null) {
            target.setPreviewProduct(copyProperties(summary.getPreviewProduct(), ClientAgentOrderCardSummaryVo.PreviewProduct.class));
        }
        return success(target);
    }

    /**
     * 根据订单编号查询当前登录用户订单详情。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "获取订单详情", description = "根据订单编号获取当前登录用户的订单详情")
    public AjaxResult<ClientAgentOrderDetailVo> getOrderDetail(
            @Parameter(description = "订单编号", required = true)
            @PathVariable String orderNo
    ) {
        ClientAgentOrderDetailDto detail = clientAgentOrderService.getOrderDetail(orderNo, getUserId());
        ClientAgentOrderDetailVo target = copyProperties(detail, ClientAgentOrderDetailVo.class);
        if (detail.getReceiverInfo() != null) {
            target.setReceiverInfo(copyProperties(detail.getReceiverInfo(), ClientAgentOrderDetailVo.ReceiverInfo.class));
        }
        if (detail.getItems() != null) {
            target.setItems(copyListProperties(detail.getItems(), ClientAgentOrderDetailVo.OrderItemDetail.class));
        }
        if (detail.getShippingInfo() != null) {
            target.setShippingInfo(copyProperties(detail.getShippingInfo(), ClientAgentOrderDetailVo.ShippingInfo.class));
        }
        return success(target);
    }

    /**
     * 根据订单编号查询当前登录用户订单物流。
     *
     * @param orderNo 订单编号
     * @return 订单物流
     */
    @GetMapping("/shipping/{orderNo}")
    @Operation(summary = "获取订单物流", description = "根据订单编号获取当前登录用户的订单物流")
    public AjaxResult<ClientAgentOrderShippingVo> getOrderShipping(
            @Parameter(description = "订单编号", required = true)
            @PathVariable String orderNo
    ) {
        ClientAgentOrderShippingDto shipping = clientAgentOrderService.getOrderShipping(orderNo, getUserId());
        ClientAgentOrderShippingVo target = copyProperties(shipping, ClientAgentOrderShippingVo.class);
        target.setReceiverInfo(copyProperties(shipping.getReceiverInfo(), ClientAgentOrderShippingVo.ReceiverInfo.class));
        target.setNodes(copyListProperties(shipping.getNodes(), ClientAgentOrderShippingVo.ShippingNode.class));
        return success(target);
    }

    /**
     * 根据订单编号查询当前登录用户订单时间线。
     *
     * @param orderNo 订单编号
     * @return 订单时间线
     */
    @GetMapping("/timeline/{orderNo}")
    @Operation(summary = "获取订单时间线", description = "根据订单编号获取当前登录用户的订单时间线")
    public AjaxResult<ClientAgentOrderTimelineVo> getOrderTimeline(
            @Parameter(description = "订单编号", required = true)
            @PathVariable String orderNo
    ) {
        ClientAgentOrderTimelineDto timeline = clientAgentOrderService.getOrderTimeline(orderNo, getUserId());
        ClientAgentOrderTimelineVo target = copyProperties(timeline, ClientAgentOrderTimelineVo.class);
        target.setTimeline(copyListProperties(timeline.getTimeline(), ClientAgentOrderTimelineVo.TimelineNode.class));
        return success(target);
    }

    /**
     * 校验当前登录用户订单是否允许取消。
     *
     * @param orderNo 订单编号
     * @return 取消资格
     */
    @GetMapping("/cancel-check/{orderNo}")
    @Operation(summary = "校验是否可取消订单", description = "根据订单编号校验当前登录用户订单是否允许取消")
    public AjaxResult<ClientAgentOrderCancelCheckVo> checkOrderCancelable(
            @Parameter(description = "订单编号", required = true)
            @PathVariable String orderNo
    ) {
        ClientAgentOrderCancelCheckDto result = clientAgentOrderService.checkOrderCancelable(orderNo, getUserId());
        return success(copyProperties(result, ClientAgentOrderCancelCheckVo.class));
    }

}
