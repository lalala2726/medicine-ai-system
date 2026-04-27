package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.UserCouponListRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.OrderCouponOptionVo;
import com.zhangyichuang.medicine.client.model.vo.coupon.UserCouponVo;
import com.zhangyichuang.medicine.model.coupon.CouponAutoSelectResultDto;
import com.zhangyichuang.medicine.model.coupon.CouponSettlementItemDto;
import com.zhangyichuang.medicine.model.coupon.OrderCouponSelectionSnapshotDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;

import java.util.List;

/**
 * 用户优惠券服务。
 */
public interface UserCouponService {

    /**
     * 查询当前用户优惠券列表。
     *
     * @param request 查询请求
     * @return 优惠券分页结果
     */
    Page<UserCouponVo> listCurrentUserCoupons(UserCouponListRequest request);

    /**
     * 删除当前用户的优惠券。
     *
     * @param couponId 用户优惠券ID
     * @return 是否删除成功
     */
    boolean deleteCurrentUserCoupon(Long couponId);

    /**
     * 统计客户端应展示的优惠券数量。
     *
     * @param userId 用户ID
     * @return 客户端应展示的优惠券数量
     */
    long countDisplayableCoupons(Long userId);

    /**
     * 查询当前订单可选优惠券列表。
     *
     * @param userId 用户ID
     * @param items  商品项列表
     * @return 当前订单可选优惠券列表
     */
    List<OrderCouponOptionVo> listMatchedCoupons(Long userId, List<CouponSettlementItemDto> items);

    /**
     * 查询指定优惠券在当前订单下的可用信息。
     *
     * @param userId   用户ID
     * @param couponId 用户优惠券ID
     * @param items    商品项列表
     * @return 当前订单优惠券信息
     */
    OrderCouponOptionVo getSelectedCouponOption(Long userId, Long couponId, List<CouponSettlementItemDto> items);

    /**
     * 自动选择优惠券组合。
     *
     * @param userId 用户ID
     * @param items  商品项列表
     * @return 自动选券结果
     */
    CouponAutoSelectResultDto autoSelectCoupons(Long userId, List<CouponSettlementItemDto> items);

    /**
     * 批量锁定优惠券并返回订单快照。
     *
     * @param userId    用户ID
     * @param couponIds 用户优惠券ID列表
     * @param items     商品项列表
     * @param orderNo   订单号
     * @return 订单优惠券快照
     */
    OrderCouponSelectionSnapshotDto lockCoupons(Long userId,
                                                List<Long> couponIds,
                                                List<CouponSettlementItemDto> items,
                                                String orderNo);

    /**
     * 消耗订单已锁定的优惠券。
     *
     * @param order 订单实体
     */
    void consumeCouponsForOrder(MallOrder order);

    /**
     * 批量释放订单已锁定的优惠券。
     *
     * @param order      订单实体
     * @param operatorId 操作人标识
     * @param reason     操作原因
     */
    void releaseCouponsForOrder(MallOrder order, String operatorId, String reason);
}
