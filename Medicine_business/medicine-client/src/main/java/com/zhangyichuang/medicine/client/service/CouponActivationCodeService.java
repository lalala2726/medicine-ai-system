package com.zhangyichuang.medicine.client.service;

import com.zhangyichuang.medicine.client.model.request.ActivationCodeRedeemRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.ActivationCodeRedeemVo;

/**
 * 客户端激活码服务。
 */
public interface CouponActivationCodeService {

    /**
     * 兑换当前用户激活码。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    ActivationCodeRedeemVo redeemCurrentUserCode(ActivationCodeRedeemRequest request);
}
