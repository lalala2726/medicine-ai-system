package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.MallOrderListVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderAddressVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderPriceVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderRemarkVo;
import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.admin.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.model.vo.MallOrderTimelineVo;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/31
 */
@RestController
@RequestMapping("/mall/order")
@Tag(name = "订单管理", description = "订单管理")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class MallOrderController extends BaseController {

    private final MallOrderService mallOrderService;
    private final MallOrderTimelineService mallOrderTimelineService;


    /**
     * 订单列表
     *
     * @param request 查询参数
     * @return 订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "订单列表")
    @PreAuthorize("hasAuthority('mall:order:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> orderList(MallOrderListRequest request) {
        Page<OrderWithProductDto> mallOrderPage = mallOrderService.orderWithProduct(request);
        List<MallOrderListVo> mallOrderListVos = mallOrderPage.getRecords().stream()
                .map(this::buildOrderListVo)
                .toList();
        return getTableData(mallOrderPage, mallOrderListVos);
    }

    /**
     * 订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("/detail/{orderId}")
    @Operation(summary = "订单详情")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<OrderDetailDto> orderDetail(@PathVariable Long orderId) {
        OrderDetailDto orderDetailDto = mallOrderService.orderDetail(orderId);
        return success(orderDetailDto);
    }

    /**
     * 获取订单地址信息
     *
     * @param orderId 订单ID
     * @return 订单地址信息
     */
    @GetMapping("/address/{orderId}")
    @Operation(summary = "获取订单地址信息")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<OrderAddressVo> getOrderAddress(@PathVariable Long orderId) {
        OrderAddressVo orderAddressVo = mallOrderService.getOrderAddress(orderId);
        return success(orderAddressVo);
    }

    /**
     * 修改订单地址
     *
     * @param request 修改参数
     * @return 修改结果
     */
    @PutMapping("/address")
    @Operation(summary = "修改订单地址")
    @PreAuthorize("hasAuthority('mall:order:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateOrderAddress(@Validated @RequestBody AddressUpdateRequest request) {
        boolean result = mallOrderService.updateOrderAddress(request);
        return toAjax(result);
    }

    /**
     * 获取订单备注信息
     *
     * @param orderId 订单ID
     * @return 订单备注信息
     */
    @GetMapping("/remark/{orderId}")
    @Operation(summary = "获取订单备注信息")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<OrderRemarkVo> getOrderRemark(@PathVariable Long orderId) {
        OrderRemarkVo orderRemarkVo = mallOrderService.getOrderRemark(orderId);
        return success(orderRemarkVo);
    }

    /**
     * 修改订单备注
     *
     * @param request 修改参数
     * @return 修改结果
     */
    @PutMapping("/remark")
    @Operation(summary = "修改订单备注")
    @PreAuthorize("hasAuthority('mall:order:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateOrderRemark(@Validated @RequestBody RemarkUpdateRequest request) {
        boolean result = mallOrderService.updateOrderRemark(request);
        return toAjax(result);
    }

    /**
     * 获取订单价格信息
     *
     * @param orderId 订单ID
     * @return 订单价格信息
     */
    @GetMapping("/price/{orderId}")
    @Operation(summary = "获取订单价格信息")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<OrderPriceVo> getOrderPrice(@PathVariable Long orderId) {
        OrderPriceVo orderPriceVo = mallOrderService.getOrderPrice(orderId);
        return success(orderPriceVo);
    }

    /**
     * 订单改价
     *
     * @param request 订单改价参数
     * @return 订单改价结果
     */
    @PutMapping("/price")
    @Operation(summary = "订单改价")
    @PreAuthorize("hasAuthority('mall:order:edit') or hasRole('super_admin')")
    public AjaxResult<Void> updateOrderPrice(@Validated @RequestBody OrderUpdatePriceRequest request) {
        boolean result = mallOrderService.updateOrderPrice(request);
        return toAjax(result);
    }


    /**
     * 订单退款
     *
     * @param request 订单退款参数
     * @return 订单退款结果
     */
    @PostMapping("/refund")
    @Operation(summary = "订单退款")
    @PreAuthorize("hasAuthority('mall:order:refund') or hasRole('super_admin')")
    public AjaxResult<Void> orderRefund(@RequestBody OrderRefundRequest request) {
        boolean result = mallOrderService.orderRefund(request);
        return toAjax(result);
    }

    /**
     * 取消订单
     * <p>
     * 如果订单已支付，会自动退款；如果未支付，直接取消并恢复库存
     * </p>
     *
     * @param request 订单取消参数
     * @return 取消结果
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消订单")
    @PreAuthorize("hasAuthority('mall:order:cancel') or hasRole('super_admin')")
    public AjaxResult<Void> cancelOrder(@Validated @RequestBody OrderCancelRequest request) {
        boolean result = mallOrderService.cancelOrder(request);
        return toAjax(result);
    }

    /**
     * 查询订单时间线
     *
     * @param orderId 订单ID
     * @return 订单时间线列表
     */
    @GetMapping("/timeline/{orderId}")
    @Operation(summary = "查询订单时间线")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<List<MallOrderTimelineVo>> getOrderTimeline(@PathVariable Long orderId) {
        List<MallOrderTimeline> timeline = mallOrderTimelineService.getTimelineByOrderId(orderId);
        List<MallOrderTimelineVo> mallOrderTimelineVos = copyListProperties(timeline, MallOrderTimelineVo.class);
        return success(mallOrderTimelineVos);
    }

    /**
     * 订单发货
     *
     * @param request 发货请求参数
     * @return 发货结果
     */
    @PostMapping("/ship")
    @Operation(summary = "订单发货")
    @PreAuthorize("hasAuthority('mall:order:ship') or hasRole('super_admin')")
    public AjaxResult<Void> shipOrder(@Validated @RequestBody OrderShipRequest request) {
        boolean result = mallOrderService.shipOrder(request);
        return toAjax(result);
    }

    /**
     * 查询订单物流信息
     *
     * @param orderId 订单ID
     * @return 物流信息
     */
    @GetMapping("/shipping/{orderId}")
    @Operation(summary = "查询订单物流信息")
    @PreAuthorize("hasAuthority('mall:order:query') or hasRole('super_admin')")
    public AjaxResult<OrderShippingVo> getOrderShipping(@PathVariable Long orderId) {
        OrderShippingVo orderShippingVo = mallOrderService.getOrderShipping(orderId);
        return success(orderShippingVo);
    }

    /**
     * 管理员手动确认收货
     *
     * @param request 确认收货请求
     * @return 操作结果
     */
    @PostMapping("/confirm-receipt")
    @Operation(summary = "管理员手动确认收货", description = "管理员手动确认订单收货，适用于特殊场景（如客户电话确认等）")
    @PreAuthorize("hasAuthority('mall:order:edit') or hasRole('super_admin')")
    public AjaxResult<Void> manualConfirmReceipt(@Validated @RequestBody OrderReceiveRequest request) {
        boolean result = mallOrderService.manualConfirmReceipt(request);
        return toAjax(result);
    }


    /**
     * 删除订单 订单状态为已完成、已取消或已过期才能被删除
     *
     * @param ids 订单ID
     * @return 删除结果
     */
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除订单")
    @PreAuthorize("hasAuthority('mall:order:delete') or hasRole('super_admin')")
    public AjaxResult<Void> deleteOrders(@PathVariable List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return error("请选择要删除的订单");
        }
        boolean result = mallOrderService.deleteOrders(ids);
        return toAjax(result);
    }

    /**
     * 构建订单列表VO
     *
     * @param source 源数据
     * @return 订单列表VO
     */
    private MallOrderListVo buildOrderListVo(OrderWithProductDto source) {
        MallOrderListVo target = BeanCotyUtils.copyProperties(source, MallOrderListVo.class);
        if (target == null) {
            return null;
        }
        if (source.getProductId() == null) {
            return target;
        }
        MallOrderListVo.ProductInfo productInfo = MallOrderListVo.ProductInfo.builder()
                .productName(source.getProductName())
                .productImage(source.getProductImage())
                .productPrice(source.getProductPrice())
                .productCategory(source.getProductCategory())
                .productId(source.getProductId())
                .quantity(source.getProductQuantity())
                .build();
        target.setProductInfo(productInfo);
        return target;
    }

}
