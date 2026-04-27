package com.zhangyichuang.medicine.client.listener;

import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.OrderTimeoutQueueConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单支付超时消息监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutMessageListener {

    /**
     * 商城订单服务。
     */
    private final MallOrderService mallOrderService;

    /**
     * 消费订单支付超时消息并触发未支付关单逻辑。
     *
     * @param message RabbitMQ 原始消息
     */
    @RabbitListener(queues = OrderTimeoutQueueConstants.PROCESS_QUEUE)
    public void handleOrderTimeout(Message message) {
        String orderNo = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("收到订单超时处理消息，订单号：{}", orderNo);
        mallOrderService.closeOrderIfUnpaid(orderNo);
    }
}
