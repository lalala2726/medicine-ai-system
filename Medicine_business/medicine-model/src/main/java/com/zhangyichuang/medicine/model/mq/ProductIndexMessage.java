package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品索引事件消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIndexMessage {

    /**
     * 操作类型：写入/删除。
     */
    private ProductIndexOperation operation;

    /**
     * 写入或更新索引的商品数据。
     */
    private ProductIndexPayload payload;

    /**
     * 删除索引时的商品ID列表。
     */
    private List<Long> productIds;
}
