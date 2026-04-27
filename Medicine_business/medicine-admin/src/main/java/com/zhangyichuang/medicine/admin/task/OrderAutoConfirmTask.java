package com.zhangyichuang.medicine.admin.task;

import com.zhangyichuang.medicine.admin.service.MallOrderService;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单自动确认收货定时任务
 * <p>
 * 每天凌晨2点执行，自动确认发货后超过15天仍未确认收货的订单
 * </p>
 *
 * @author Chuang
 * created 2025/11/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmTask {

    /**
     * 自动确认收货天数（发货后N天自动确认）
     */
    private static final int AUTO_CONFIRM_DAYS = 15;
    private final MallOrderService mallOrderService;

    /**
     * 自动确认收货定时任务
     * <p>
     * 每天凌晨2点执行
     * </p>
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoConfirmOrders() {
        log.info("开始执行订单自动确认收货任务");

        try {
            // 查询发货后超过15天仍未确认收货的订单
            List<MallOrder> orders = mallOrderService.getOrdersForAutoConfirm(AUTO_CONFIRM_DAYS);

            if (orders == null || orders.isEmpty()) {
                log.info("没有需要自动确认收货的订单");
                return;
            }

            log.info("找到{}个需要自动确认收货的订单", orders.size());

            int successCount = 0;
            int failCount = 0;

            for (MallOrder order : orders) {
                try {
                    boolean success = mallOrderService.autoConfirmReceipt(order.getId());
                    if (success) {
                        successCount++;
                        log.info("自动确认收货成功，订单号：{}", order.getOrderNo());
                    } else {
                        failCount++;
                        log.warn("自动确认收货失败，订单号：{}", order.getOrderNo());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("自动确认收货异常，订单号：{}", order.getOrderNo(), e);
                }
            }

            log.info("订单自动确认收货任务执行完成，成功：{}个，失败：{}个", successCount, failCount);

        } catch (Exception e) {
            log.error("订单自动确认收货任务执行异常", e);
        }
    }
}

