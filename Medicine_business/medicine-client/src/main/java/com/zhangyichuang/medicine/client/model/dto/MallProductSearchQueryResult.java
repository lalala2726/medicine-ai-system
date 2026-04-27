package com.zhangyichuang.medicine.client.model.dto;

import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import lombok.Data;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

/**
 * 商品搜索查询结果对象。
 *
 * @author Chuang
 */
@Data
public class MallProductSearchQueryResult {

    /**
     * 商品搜索命中文档。
     */
    private SearchHits<MallProductDocument> searchHits;

    /**
     * 动态标签筛选列表。
     */
    private List<MallProductSearchTagFilterVo> tagFilters;
}
