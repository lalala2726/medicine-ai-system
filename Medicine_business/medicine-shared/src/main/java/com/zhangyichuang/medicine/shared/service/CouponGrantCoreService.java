package com.zhangyichuang.medicine.shared.service;

import com.zhangyichuang.medicine.model.coupon.CouponGrantCommand;
import com.zhangyichuang.medicine.model.coupon.CouponGrantResultDto;

/**
 * 优惠券共享发放核心服务。
 */
public interface CouponGrantCoreService {

    /**
     * 按模板发放用户优惠券。
     *
     * @param command 发券命令
     * @return 发券结果
     */
    CouponGrantResultDto grantCoupon(CouponGrantCommand command);
}
