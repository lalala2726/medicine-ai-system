package com.zhangyichuang.medicine.client.elasticsearch.mq;

import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.ProductIndexQueueConstants;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.mq.ProductIndexMessage;
import com.zhangyichuang.medicine.model.mq.ProductIndexOperation;
import com.zhangyichuang.medicine.model.mq.ProductIndexPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * 订阅商品索引事件，落库至 Elasticsearch。
 *
 * @author Chuang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallProductIndexMessageListener {

    /**
     * 商品搜索服务。
     */
    private final MallProductSearchService mallProductSearchService;

    /**
     * 监听商品索引消息。
     *
     * @param message 商品索引消息
     */
    @RabbitListener(queues = ProductIndexQueueConstants.QUEUE)
    public void onMessage(ProductIndexMessage message) {
        if (message == null || message.getOperation() == null) {
            log.warn("跳过商品索引消息: {}", message);
            return;
        }
        if (message.getOperation() == ProductIndexOperation.UPSERT) {
            handleUpsert(message.getPayload());
        } else if (message.getOperation() == ProductIndexOperation.DELETE) {
            handleDelete(message.getProductIds());
        }
    }

    /**
     * 处理商品索引新增或更新。
     *
     * @param payload 商品索引载荷
     */
    private void handleUpsert(ProductIndexPayload payload) {
        if (payload == null || payload.getId() == null) {
            log.warn("跳过商品索引更新操作，payload 为空");
            return;
        }
        mallProductSearchService.save(toDocument(payload));
    }

    /**
     * 处理商品索引删除。
     *
     * @param productIds 商品ID集合
     */
    private void handleDelete(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return;
        }
        productIds.forEach(mallProductSearchService::deleteById);
    }

    /**
     * 将索引载荷转换为 ES 文档。
     *
     * @param payload 商品索引载荷
     * @return ES 文档
     */
    private MallProductDocument toDocument(ProductIndexPayload payload) {
        return MallProductDocument.builder()
                .id(payload.getId())
                .name(payload.getName())
                .categoryNames(payload.getCategoryNames())
                .categoryIds(payload.getCategoryIds())
                .price(payload.getPrice())
                .drugCategory(payload.getDrugCategory())
                .status(payload.getStatus())
                .brand(payload.getBrand())
                .commonName(payload.getCommonName())
                .keywordSuggest(completion(payload.getKeywordSuggestInputs()))
                .efficacy(payload.getEfficacy())
                .tagIds(payload.getTagIds())
                .tagNames(payload.getTagNames())
                .tagTypeBindings(payload.getTagTypeBindings())
                .sales(payload.getSales())
                .instruction(payload.getInstruction())
                .taboo(payload.getTaboo())
                .coverImage(payload.getCoverImage())
                .build();
    }

    /**
     * 构建自动补全字段。
     *
     * @param values 补全输入列表
     * @return 自动补全对象
     */
    private Completion completion(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        List<String> normalizedValues = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedValues.isEmpty()) {
            return null;
        }
        return new Completion(normalizedValues);
    }
}
