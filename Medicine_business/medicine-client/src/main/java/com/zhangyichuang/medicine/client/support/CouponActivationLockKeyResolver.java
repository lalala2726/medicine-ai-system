package com.zhangyichuang.medicine.client.support;

import com.zhangyichuang.medicine.shared.utils.CouponActivationCodeUtils;
import org.springframework.stereotype.Component;

/**
 * 激活码分布式锁键解析器。
 */
@Component("couponActivationLockKeyResolver")
public class CouponActivationLockKeyResolver {

    /**
     * 规范化激活码锁键。
     *
     * @param code 原始激活码
     * @return 规范化后的激活码
     */
    public String normalizeCode(String code) {
        return CouponActivationCodeUtils.normalizeCode(code);
    }
}
