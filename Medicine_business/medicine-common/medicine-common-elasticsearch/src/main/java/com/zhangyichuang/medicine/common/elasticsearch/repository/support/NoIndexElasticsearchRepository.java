package com.zhangyichuang.medicine.common.elasticsearch.repository.support;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchEntityInformation;
import org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository;

/**
 * Disable auto index/mapping creation to avoid startup failures on incompatible clusters.
 */
public class NoIndexElasticsearchRepository<T, ID> extends SimpleElasticsearchRepository<T, ID> {

    public NoIndexElasticsearchRepository(ElasticsearchEntityInformation<T, ID> entityInformation,
                                          ElasticsearchOperations operations) {
        super(entityInformation, operations);
    }

    @Override
    public void createIndexAndMappingIfNeeded() {
        // No-op to skip index existence checks and auto-creation at startup.
    }
}
