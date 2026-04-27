package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.request.AdminMallOrderListRequest;
import com.zhangyichuang.medicine.agent.model.vo.admin.AdminMallOrderListVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.MallOrderProductInfoVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.OrderDetailVo;
import com.zhangyichuang.medicine.agent.service.MallOrderService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.OrderContextDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端智能体订单工具控制器。
 * <p>
 * 提供给管理端智能体使用的订单查询工具接口，
 * 支持订单列表查询和订单详情查询等功能。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/agent/admin/order")
@Tag(name = "管理端智能体订单工具", description = "用于管理端智能体订单查询接口")
@RequiredArgsConstructor
public class AgentOrderController extends BaseController {

    private final MallOrderService agentOrderService;

    /**
     * 根据条件分页查询订单列表。
     * <p>
     * 支持按订单状态、时间范围等条件筛选订单，
     * 返回订单基本信息及首个商品信息，按创建时间倒序排列。
     *
     * @param request 查询请求参数
     * @return 订单列表分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "获取订单列表", description = "分页获取订单列表，默认按创建时间倒序")
    @PreAuthorize("hasAuthority('mall:order:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getOrderList(AdminMallOrderListRequest request) {
        AdminMallOrderListRequest safeRequest = request == null ? new AdminMallOrderListRequest() : request;
        Page<OrderWithProductDto> orderPage = agentOrderService.listOrders(safeRequest);
        List<AdminMallOrderListVo> orderListVos = orderPage.getRecords().stream()
                .map(this::buildOrderListVo)
                .toList();
        return getTableData(orderPage, orderListVos);
    }

    /**
     * 根据订单编号批量查询订单聚合上下文。
     * <p>
     * 用于智能体优先判断订单状态、售后、物流和下一步动作，
     * 避免默认连续调用详情、时间线和发货记录等细粒度接口。
     *
     * @param orderNos 订单编号列表
     * @return 按订单编号分组的订单聚合上下文
     */
    @GetMapping("/context/{orderNos}")
    @Operation(summary = "获取订单聚合上下文", description = "根据订单编号批量获取智能体订单聚合上下文")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<Map<String, OrderContextDto>> getOrderContext(
            @Parameter(description = "订单编号，多个可用逗号分隔")
            @PathVariable List<String> orderNos
    ) {
        return success(agentOrderService.getOrderContext(orderNos));
    }

    /**
     * 根据订单编号查询订单详情
     * <p>
     * 返回订单的详细信息，包括订单基本信息、收货地址、
     * 商品明细、支付信息等完整订单数据。
     *
     * @param orderNos 订单编号列表
     * @return 订单详情列表
     */
    @GetMapping("/{orderNos}")
    @Operation(summary = "获取订单详情", description = "根据订单编号获取详细信息")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<List<OrderDetailVo>> getOrderDetail(
            @Parameter(description = "订单编号，多个可用逗号分隔")
            @PathVariable List<String> orderNos
    ) {
        return success(agentOrderService.getOrderDetail(orderNos));
    }

    private AdminMallOrderListVo buildOrderListVo(OrderWithProductDto source) {
        if (source == null) {
            return null;
        }
        AdminMallOrderListVo target = new AdminMallOrderListVo();
        target.setId(source.getId());
        target.setOrderNo(source.getOrderNo());
        target.setTotalAmount(source.getTotalAmount());
        target.setPayType(source.getPayType());
        target.setOrderStatus(source.getOrderStatus());
        target.setPayTime(source.getPayTime());
        target.setCreateTime(source.getCreateTime());

        if (source.getProductId() == null) {
            return target;
        }

        MallOrderProductInfoVo productInfo = new MallOrderProductInfoVo();
        productInfo.setProductName(source.getProductName());
        productInfo.setProductImage(source.getProductImage());
        productInfo.setProductPrice(source.getProductPrice());
        productInfo.setProductCategory(source.getProductCategory());
        productInfo.setProductId(source.getProductId());
        productInfo.setQuantity(source.getProductQuantity());
        target.setProductInfo(productInfo);
        return target;
    }
}
