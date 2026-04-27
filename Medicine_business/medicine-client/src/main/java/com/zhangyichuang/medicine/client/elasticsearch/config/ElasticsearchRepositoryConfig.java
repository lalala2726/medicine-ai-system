package com.zhangyichuang.medicine.client.elasticsearch.config;

import com.zhangyichuang.medicine.common.elasticsearch.repository.support.NoIndexElasticsearchRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * 启用商城 Elasticsearch 仓储扫描。
 */
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.zhangyichuang.medicine.client.elasticsearch.repository",
        repositoryBaseClass = NoIndexElasticsearchRepository.class
)
public class ElasticsearchRepositoryConfig {
}
