package com.zhangyichuang.medicine.admin.task;

import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券过期扫描任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireTask {

    /**
     * 管理端优惠券服务。
     */
    private final CouponAdminService couponAdminService;

    /**
     * 分布式锁执行器。
     */
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * 定时扫描并过期可用优惠券。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void expireCoupons() {
        Integer expiredCount = distributedLockExecutor.tryExecuteOrElse(RedisConstants.Lock.COUPON_EXPIRE_TASK_KEY,
                0L,
                -1L,
                couponAdminService::expireAvailableCoupons,
                () -> {
                    log.info("优惠券过期任务已由其他节点执行，本节点跳过本轮扫描");
                    return 0;
                });
        if (expiredCount > 0) {
            log.info("本次共过期 {} 张优惠券", expiredCount);
        }
    }
}
