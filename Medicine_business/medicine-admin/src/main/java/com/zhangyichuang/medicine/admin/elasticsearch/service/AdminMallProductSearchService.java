package com.zhangyichuang.medicine.admin.elasticsearch.service;

import com.zhangyichuang.medicine.admin.elasticsearch.model.dto.AdminMallProductSearchResult;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;

/**
 * 管理端商品 Elasticsearch 搜索服务。
 *
 * @author Chuang
 */
public interface AdminMallProductSearchService {

    /**
     * 按管理端商品查询条件执行 Elasticsearch 搜索。
     *
     * @param request 商品查询参数
     * @return 搜索结果
     */
    AdminMallProductSearchResult searchProducts(MallProductListQueryRequest request);
}
