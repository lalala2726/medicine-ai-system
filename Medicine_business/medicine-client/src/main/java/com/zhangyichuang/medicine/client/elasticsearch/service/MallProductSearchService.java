package com.zhangyichuang.medicine.client.elasticsearch.service;

import com.zhangyichuang.medicine.client.model.dto.MallProductSearchQueryResult;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;

import java.util.List;

/**
 * 商品搜索服务，封装对 Elasticsearch 的读写操作。
 */
public interface MallProductSearchService {

    /**
     * 保存或更新商品文档。
     *
     * @param document 商品文档
     */
    void save(MallProductDocument document);

    /**
     * 批量保存或更新商品文档。
     *
     * @param documents 文档列表
     */
    void saveAll(List<MallProductDocument> documents);

    /**
     * 删除商品文档。
     *
     * @param productId 商品ID
     */
    void deleteById(Long productId);

    /**
     * 关键字搜索。
     *
     * @param request 搜索请求
     * @return 命中文档与动态筛选结果
     */
    MallProductSearchQueryResult search(MallProductSearchRequest request);

    /**
     * 搜索建议，仅命中名称/通用名/品牌（支持拼音）。
     */
    List<String> suggest(String keyword, int size);
}
