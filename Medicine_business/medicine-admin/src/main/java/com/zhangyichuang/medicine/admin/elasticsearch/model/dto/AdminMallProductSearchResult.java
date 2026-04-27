package com.zhangyichuang.medicine.admin.elasticsearch.model.dto;

import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import lombok.Data;

import java.util.List;

/**
 * 管理端商品 Elasticsearch 搜索结果。
 *
 * @author Chuang
 */
@Data
public class AdminMallProductSearchResult {

    /**
     * 当前页码。
     */
    private Long pageNum;

    /**
     * 每页数量。
     */
    private Long pageSize;

    /**
     * 总命中数。
     */
    private Long total;

    /**
     * 当前页命中的商品文档列表。
     */
    private List<MallProductDocument> documents;
}
