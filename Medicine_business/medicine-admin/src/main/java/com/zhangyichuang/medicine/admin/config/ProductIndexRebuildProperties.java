package com.zhangyichuang.medicine.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商品索引重建配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.elasticsearch.product-rebuild")
public class ProductIndexRebuildProperties {

    /**
     * 是否启用启动后自动检查并触发商品索引重建。
     */
    private boolean startupEnabled = true;

    /**
     * 全量重建时单批次处理的商品数量。
     */
    private int batchSize = 100;

    /**
     * 获取分布式锁时的等待时间（毫秒）。
     */
    private long lockWaitMillis = 0L;

    /**
     * 商品索引重建分布式锁租约时间（毫秒）。
     */
    private long lockLeaseMillis = 30 * 60 * 1000L;
}
