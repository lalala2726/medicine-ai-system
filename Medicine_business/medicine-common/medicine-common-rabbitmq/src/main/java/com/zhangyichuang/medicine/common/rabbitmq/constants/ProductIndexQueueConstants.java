package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 商品索引相关的交换机、队列和路由键常量。
 */
public final class ProductIndexQueueConstants {

    /**
     * 商品索引事件交换机。
     */
    public static final String EXCHANGE = "medicine.product.index.exchange";

    /**
     * 商品索引写入/删除队列。
     */
    public static final String QUEUE = "medicine.product.index.queue";

    /**
     * 写入或更新索引的路由键。
     */
    public static final String ROUTING_UPSERT = "product.index.upsert";

    /**
     * 删除索引的路由键。
     */
    public static final String ROUTING_DELETE = "product.index.delete";

    private ProductIndexQueueConstants() {
    }
}
