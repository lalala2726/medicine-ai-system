package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.dto.UserOrderStatistics;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.OrderAddressVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderPriceVo;
import com.zhangyichuang.medicine.admin.model.vo.OrderRemarkVo;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.dto.OrderWithProductDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.request.MallOrderListRequest;
import com.zhangyichuang.medicine.model.vo.OrderShippingVo;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallOrderService extends IService<MallOrder> {


    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单
     */
    MallOrder getOrderByOrderNo(String orderNo);

    /**
     * 根据订单号列表查询订单详情。
     *
     * @param orderNos 订单号列表
     * @return 订单详情列表
     */
    List<OrderDetailDto> getOrderByOrderNo(List<String> orderNos);

    /**
     * 根据ID查询订单
     *
     * @param id 订单ID
     * @return 订单
     */
    MallOrder getOrderById(Long id);

    /**
     * 订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailDto orderDetail(Long orderId);

    /**
     * 获取订单地址信息
     *
     * @param orderId 订单ID
     * @return 订单地址信息
     */
    OrderAddressVo getOrderAddress(Long orderId);

    /**
     * 更新订单地址
     *
     * @param request 更新参数
     * @return 是否成功
     */
    boolean updateOrderAddress(AddressUpdateRequest request);

    /**
     * 获取订单备注信息
     *
     * @param orderId 订单ID
     * @return 订单备注信息
     */
    OrderRemarkVo getOrderRemark(Long orderId);

    /**
     * 更新订单备注
     *
     * @param request 更新参数
     * @return 是否成功
     */
    boolean updateOrderRemark(RemarkUpdateRequest request);

    /**
     * 获取订单价格信息
     *
     * @param orderId 订单ID
     * @return 订单价格信息
     */
    OrderPriceVo getOrderPrice(Long orderId);

    /**
     * 更新订单价格
     *
     * @param request 订单价格更新参数
     * @return 是否成功
     */
    boolean updateOrderPrice(OrderUpdatePriceRequest request);

    /**
     * 订单退款
     *
     * @param request 订单退款参数
     * @return 是否成功
     */
    boolean orderRefund(OrderRefundRequest request);

    /**
     * 取消订单
     * <p>
     * 如果订单已支付，会自动退款；如果未支付，直接取消并恢复库存
     * </p>
     *
     * @param request 订单取消参数
     * @return 是否成功
     */
    boolean cancelOrder(OrderCancelRequest request);

    /**
     * 订单列表（带商品信息）
     *
     * @param request 订单列表参数
     * @return 订单列表
     */
    Page<OrderWithProductDto> orderWithProduct(MallOrderListRequest request);

    /**
     * 获取过期订单
     *
     * @param expiredTime 过期时间点, 毫秒级时间戳, 小于该时间点的订单视为过期
     * @return 过期订单
     */
    List<MallOrder> getExpiredOrderClean(long expiredTime);

    /**
     * 执行过期订单补偿关闭。
     *
     * @param orderNo 订单编号
     * @return 是否关闭成功
     */
    boolean closeExpiredOrderForCompensation(String orderNo);

    /**
     * 获取用户订单
     *
     * @param userId  用户ID
     * @param request 查询参数
     * @return 用户订单
     */
    Page<MallOrder> getPaidOrderPage(Long userId, PageRequest request);

    /**
     * 获取用户订单统计信息
     *
     * @param userId 用户ID
     * @return 订单统计信息
     */
    UserOrderStatistics getOrderStatisticsByUserId(Long userId);

    /**
     * 订单发货
     *
     * @param request 发货请求参数
     * @return 是否成功
     */
    boolean shipOrder(OrderShipRequest request);

    /**
     * 获取订单物流信息
     *
     * @param orderId 订单ID
     * @return 物流信息
     */
    OrderShippingVo getOrderShipping(Long orderId);

    /**
     * 查询待自动确认收货的订单（发货N天后）
     *
     * @param daysAfterShipment 发货后天数
     * @return 待自动确认收货的订单列表
     */
    List<MallOrder> getOrdersForAutoConfirm(int daysAfterShipment);

    /**
     * 自动确认收货
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean autoConfirmReceipt(Long orderId);

    /**
     * 管理员手动确认收货
     *
     * @param request 确认收货请求参数
     * @return 是否成功
     */
    boolean manualConfirmReceipt(OrderReceiveRequest request);

    /**
     * 删除订单
     *
     * @param ids 订单ID列表
     * @return 是否成功
     */
    boolean deleteOrders(List<Long> ids);

    /**
     * 批量获取订单详情
     *
     * @param orderIds 订单ID列表
     * @return 订单详情列表
     */
    List<OrderDetailDto> getOrderDetailByIds(List<Long> orderIds);
}
