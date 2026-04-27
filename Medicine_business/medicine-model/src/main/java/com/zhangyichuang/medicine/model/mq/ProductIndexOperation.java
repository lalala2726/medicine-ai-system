package com.zhangyichuang.medicine.model.mq;

/**
 * 商品索引的操作类型。
 */
public enum ProductIndexOperation {
    /**
     * 新增或更新索引。
     */
    UPSERT,
    /**
     * 删除索引。
     */
    DELETE
}
