package com.zhangyichuang.medicine.client.elasticsearch.repository;

import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品搜索索引仓储。
 */
@Repository
public interface MallProductSearchRepository extends ElasticsearchRepository<MallProductDocument, Long> {
}
