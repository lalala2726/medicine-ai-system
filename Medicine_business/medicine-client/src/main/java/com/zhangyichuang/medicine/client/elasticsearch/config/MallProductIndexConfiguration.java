package com.zhangyichuang.medicine.client.elasticsearch.config;

import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * 初始化商城商品索引，确保自定义分析器设置生效。
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.elasticsearch.index.init", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MallProductIndexConfiguration {

    private final ElasticsearchOperations elasticsearchOperations;

    @Bean
    public CommandLineRunner initMallProductIndex() {
        return args -> {
            var indexOps = elasticsearchOperations.indexOps(MallProductDocument.class);
            try {
                if (!indexOps.exists()) {
                    indexOps.create();
                    indexOps.putMapping(indexOps.createMapping());
                }
            } catch (Exception ex) {
                log.warn("MallProduct index init skipped due to Elasticsearch error: {}", ex.getMessage());
            }
        };
    }
}
