package com.zhangyichuang.medicine.admin.task;

import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 过期订单清理定时任务
 *
 * @author Chuang
 * <p>
 * created on 2025/11/5
 */
@Slf4j
@Component
public class ExpiredOrderClean {

    private final MallOrderService mallOrderService;

    public ExpiredOrderClean(MallOrderService mallOrderService) {
        this.mallOrderService = mallOrderService;
    }


    /**
     * 定时清理过期订单任务
     * <p>
     * 订单主流程已经通过 RabbitMQ TTL + 死信机制驱动自动过期，
     * 本任务仅作为补偿机制定期扫描并关闭已过期但未被正常处理的待支付订单。
     * <p>
     * 扫描条件统一基于 pay_expire_time，避免再使用下单创建时间推导过期状态。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void clean() {
        long expiredTime = System.currentTimeMillis();
        List<MallOrder> expiredOrders = mallOrderService.getExpiredOrderClean(expiredTime);
        expiredOrders.forEach(order -> {
            boolean result = mallOrderService.closeExpiredOrderForCompensation(order.getOrderNo());
            if (result) {
                log.info("订单{}已通过补偿任务完成超时关闭", order.getOrderNo());
            }
        });
    }

}
