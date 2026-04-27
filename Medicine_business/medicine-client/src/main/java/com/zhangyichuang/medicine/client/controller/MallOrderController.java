package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.*;
import com.zhangyichuang.medicine.client.model.vo.*;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单领域对外接口。
 *
 * <p>
 * 本控制器聚合了客户端订单创建、支付信息查询等订单能力，
 * 对外提供统一的订单业务入口。
 * </p>
 * <p>
 * created on 2025/10/31
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单管理")
public class MallOrderController extends BaseController {

    private final MallOrderService mallOrderService;

    public MallOrderController(MallOrderService mallOrderService) {
        this.mallOrderService = mallOrderService;
    }

    /**
     * 订单预览
     * <p>
     * 在用户提交订单前预览订单信息，包括商品详情、价格、运费等。
     * 支持两种场景：
     * - 单个商品购买：从商品详情页点击购买
     * - 购物车结算：从购物车选择多个商品结算
     * </p>
     *
     * @param request 订单预览请求
     * @return 订单预览信息
     */
    @PostMapping("/preview")
    @Operation(summary = "订单预览")
    public AjaxResult<OrderPreviewVo> previewOrder(@Validated @RequestBody OrderPreviewRequest request) {
        OrderPreviewVo orderPreviewVo = mallOrderService.previewOrder(request);
        return success(orderPreviewVo);
    }

    /**
     * 提交订单（创建订单并锁定库存）
     * <p>
     * 用户提交订单时创建订单并扣减库存，订单状态为待支付。
     * 订单创建后需要在30分钟内完成支付，否则订单将自动取消并恢复库存。
     * </p>
     *
     * @param request 订单提交请求
     * @return 订单提交结果
     */
    @PostMapping("/checkout")
    @Operation(summary = "提交订单（创建订单并锁定库存）")
    @PreventDuplicateSubmit
    public AjaxResult<OrderCheckoutVo> checkoutOrder(@Validated @RequestBody OrderCheckoutRequest request) {
        OrderCheckoutVo orderCheckoutVo = mallOrderService.checkoutOrder(request);
        return success(orderCheckoutVo);
    }

    /**
     * 获取订单支付信息
     * <p>
     * 收银台页面通过该接口展示订单支付信息，支持待支付和已支付订单。
     * </p>
     *
     * @param orderNo 订单号
     * @return 订单支付信息
     */
    @GetMapping("/pay_info/{orderNo}")
    @Operation(summary = "获取订单支付信息")
    public AjaxResult<OrderPayInfoVo> getOrderPayInfo(@PathVariable String orderNo) {
        OrderPayInfoVo orderPayInfoVo = mallOrderService.getOrderPayInfo(orderNo);
        return success(orderPayInfoVo);
    }

    /**
     * 订单支付
     * <p>
     * 对已创建的待支付订单进行支付操作，仅支持钱包支付：
     * - 钱包支付：同步扣款，订单状态变为待发货
     * </p>
     *
     * @param request 订单支付请求
     * @return 订单支付结果
     */
    @PostMapping("/pay")
    @Operation(summary = "订单支付")
    @PreventDuplicateSubmit
    public AjaxResult<OrderPayVo> payOrder(@Validated @RequestBody OrderPayRequest request) {
        OrderPayVo orderPayVo = mallOrderService.payOrder(request);
        return success(orderPayVo);
    }


    /**
     * 从购物车提交订单（创建订单并锁定库存）
     * <p>
     * 用户可以选择购物车中的多个商品进行结算，系统会校验商品状态和库存，
     * 扣减库存后创建订单，订单状态为待支付，自动删除已结算的购物车商品。
     * 订单创建后需要在30分钟内完成支付，否则订单将自动取消并恢复库存。
     * </p>
     *
     * @param request 购物车提交订单请求
     * @return 订单提交结果
     */
    @PostMapping("/create-from-cart")
    @Operation(summary = "从购物车提交订单（创建订单并锁定库存）")
    @PreventDuplicateSubmit
    public AjaxResult<OrderCheckoutVo> createOrderFromCart(@Valid @RequestBody CartSettleRequest request) {
        OrderCheckoutVo orderCheckoutVo = mallOrderService.createOrderFromCart(request);
        return success(orderCheckoutVo);
    }

    /**
     * 取消订单
     * <p>
     * 用户主动取消订单，需要提供取消原因。
     * 只有待支付状态的订单可以取消，取消后会恢复库存。
     * </p>
     *
     * @param request 订单取消请求参数
     * @return 取消结果
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消订单")
    @PreventDuplicateSubmit
    public AjaxResult<Void> cancelOrder(@Validated @RequestBody OrderCancelRequest request) {
        boolean result = mallOrderService.cancelOrder(request);
        return toAjax(result);
    }

    /**
     * 确认收货
     * <p>
     * 用户确认收到商品后，订单状态更新为已完成
     * </p>
     *
     * @param request 确认收货请求参数
     * @return 确认结果
     */
    @PostMapping("/confirm-receipt")
    @Operation(summary = "确认收货")
    @PreventDuplicateSubmit
    public AjaxResult<Void> confirmReceipt(@Validated @RequestBody OrderReceiveRequest request) {
        boolean result = mallOrderService.confirmReceipt(request);
        return toAjax(result);
    }

    /**
     * 查询订单物流信息
     * <p>
     * 用户可以查看订单的物流公司、物流单号等信息
     * </p>
     *
     * @param orderNo 订单编号
     * @return 物流信息
     */
    @GetMapping("/shipping/{orderNo}")
    @Operation(summary = "查询订单物流信息")
    public AjaxResult<OrderShippingVo> getOrderShipping(@PathVariable String orderNo) {
        OrderShippingVo orderShippingVo = mallOrderService.getOrderShipping(orderNo);
        return success(orderShippingVo);
    }

    /**
     * 分页查询用户订单列表
     * <p>
     * 用户可以查看自己的所有订单，支持按订单状态、订单编号、商品名称筛选
     * </p>
     *
     * @param request 查询条件
     * @return 订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询用户订单列表")
    public AjaxResult<TableDataResult> getOrderList(OrderListRequest request) {
        Page<OrderListVo> page = mallOrderService.getOrderList(request);
        return getTableData(page);
    }

    /**
     * 查询订单详情
     * <p>
     * 用户可以查看订单的详细信息，包括商品信息、收货地址、物流信息等
     * </p>
     *
     * @param orderNo 订单编号
     * @return 订单详情
     */
    @GetMapping("/detail/{orderNo}")
    @Operation(summary = "查询订单详情")
    public AjaxResult<OrderDetailVo> getOrderDetail(@PathVariable("orderNo") String orderNo) {
        OrderDetailVo orderDetailVo = mallOrderService.getOrderDetail(orderNo);
        return success(orderDetailVo);
    }
}
