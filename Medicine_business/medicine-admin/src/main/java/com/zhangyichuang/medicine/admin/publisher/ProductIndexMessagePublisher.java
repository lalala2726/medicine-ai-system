package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.rabbitmq.constants.ProductIndexQueueConstants;
import com.zhangyichuang.medicine.model.mq.ProductIndexMessage;
import com.zhangyichuang.medicine.model.mq.ProductIndexOperation;
import com.zhangyichuang.medicine.model.mq.ProductIndexPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 商品索引事件发布器，admin 端只负责发消息，具体索引同步由消费者处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布商品写入/更新索引事件。
     */
    public void publishUpsert(ProductIndexPayload payload) {
        if (payload == null || payload.getId() == null) {
            log.warn("Skip publish product index event, payload is empty");
            return;
        }
        ProductIndexMessage message = ProductIndexMessage.builder()
                .operation(ProductIndexOperation.UPSERT)
                .payload(payload)
                .build();
        rabbitTemplate.convertAndSend(
                ProductIndexQueueConstants.EXCHANGE,
                ProductIndexQueueConstants.ROUTING_UPSERT,
                message
        );
    }

    /**
     * 发布商品索引删除事件。
     */
    public void publishDelete(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return;
        }
        List<Long> ids = productIds.stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        ProductIndexMessage message = ProductIndexMessage.builder()
                .operation(ProductIndexOperation.DELETE)
                .productIds(ids)
                .build();
        rabbitTemplate.convertAndSend(
                ProductIndexQueueConstants.EXCHANGE,
                ProductIndexQueueConstants.ROUTING_DELETE,
                message
        );
    }
}
