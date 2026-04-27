package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.AdminUserCouponVo;
import com.zhangyichuang.medicine.admin.model.vo.CouponLogVo;
import com.zhangyichuang.medicine.admin.model.vo.CouponTemplateVo;
import com.zhangyichuang.medicine.model.coupon.CouponSettlementResultDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.enums.CouponTemplateDeleteModeEnum;
import com.zhangyichuang.medicine.model.mq.CouponBatchIssueMessage;
import com.zhangyichuang.medicine.model.mq.CouponIssueMessage;

import java.util.List;

/**
 * 管理端优惠券服务。
 */
public interface CouponAdminService {

    /**
     * 查询优惠券模板列表。
     *
     * @param request 查询请求
     * @return 优惠券模板分页结果
     */
    Page<CouponTemplateVo> listTemplates(CouponTemplateListRequest request);

    /**
     * 查询优惠券模板详情。
     *
     * @param id 模板ID
     * @return 优惠券模板详情
     */
    CouponTemplateVo getTemplate(Long id);

    /**
     * 新增优惠券模板。
     *
     * @param request 新增请求
     * @return 是否新增成功
     */
    boolean addTemplate(CouponTemplateAddRequest request);

    /**
     * 修改优惠券模板。
     *
     * @param request 修改请求
     * @return 是否修改成功
     */
    boolean updateTemplate(CouponTemplateUpdateRequest request);

    /**
     * 删除优惠券模板。
     *
     * @param id         模板ID
     * @param deleteMode 删除模式
     * @return 是否删除成功
     */
    boolean deleteTemplate(Long id, CouponTemplateDeleteModeEnum deleteMode);

    /**
     * 管理端发券。
     *
     * @param request 发券请求
     * @return 是否发券成功
     */
    boolean issueCoupon(CouponIssueRequest request);

    /**
     * 消费批量发券消息并拆分单用户发券消息。
     *
     * @param message 批量发券消息
     */
    void consumeBatchIssueCoupon(CouponBatchIssueMessage message);

    /**
     * 消费发券消息并落库。
     *
     * @param message 发券消息
     */
    void consumeIssueCoupon(CouponIssueMessage message);

    /**
     * 查询用户优惠券列表。
     *
     * @param request 查询请求
     * @return 用户优惠券分页结果
     */
    Page<AdminUserCouponVo> listUserCoupons(AdminUserCouponListRequest request);

    /**
     * 删除指定用户优惠券。
     *
     * @param userId     用户ID
     * @param couponId   用户优惠券ID
     * @param operatorId 操作人标识
     * @return 是否删除成功
     */
    boolean deleteUserCoupon(Long userId, Long couponId, String operatorId);

    /**
     * 查询指定用户优惠券日志列表。
     *
     * @param userId  用户ID
     * @param request 查询请求
     * @return 用户优惠券日志分页结果
     */
    Page<CouponLogVo> listUserCouponLogs(Long userId, CouponLogListRequest request);

    /**
     * 查询优惠券日志列表。
     *
     * @param request 查询请求
     * @return 优惠券日志分页结果
     */
    Page<CouponLogVo> listCouponLogs(CouponLogListRequest request);

    /**
     * 扫描并过期可用优惠券。
     *
     * @return 本次过期数量
     */
    int expireAvailableCoupons();

    /**
     * 为待支付订单重算已锁定优惠券。
     *
     * @param order      订单实体
     * @param orderItems 订单项列表
     * @return 优惠券重算结果
     */
    CouponSettlementResultDto recalculateLockedCoupon(MallOrder order, List<MallOrderItem> orderItems);

    /**
     * 释放订单已锁定优惠券。
     *
     * @param order      订单实体
     * @param operatorId 操作人标识
     * @param reason     操作原因
     */
    void releaseLockedCouponForOrder(MallOrder order, String operatorId, String reason);
}
