package com.zhangyichuang.medicine.admin.elasticsearch.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.zhangyichuang.medicine.admin.elasticsearch.model.dto.AdminMallProductSearchResult;
import com.zhangyichuang.medicine.admin.elasticsearch.service.AdminMallProductSearchService;
import com.zhangyichuang.medicine.admin.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 管理端商品 Elasticsearch 搜索服务实现。
 *
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class AdminMallProductSearchServiceImpl implements AdminMallProductSearchService {

    /**
     * Elasticsearch 查询结果窗口上限。
     */
    private static final int MAX_RESULT_WINDOW = 500;

    /**
     * Elasticsearch 单次查询允许的最大页大小。
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 商品关键词全文匹配字段。
     */
    private static final List<String> KEYWORD_MATCH_FIELDS = List.of(
            "name^5",
            "categoryNames^4",
            "tagNames^4",
            "brand^3",
            "commonName^4",
            "efficacy^2",
            "instruction"
    );

    /**
     * 商品关键词前缀匹配字段。
     */
    private static final List<String> KEYWORD_PREFIX_FIELDS = List.of(
            "name^5",
            "categoryNames^4",
            "tagNames^4",
            "brand^3",
            "commonName^4"
    );

    /**
     * Elasticsearch 操作对象。
     */
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 按管理端商品查询条件执行 Elasticsearch 搜索。
     *
     * @param request 商品查询参数
     * @return 搜索结果
     */
    @Override
    public AdminMallProductSearchResult searchProducts(MallProductListQueryRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        normalizeRequest(request);
        mallProductTagService.fillTagFilterGroups(request);
        validateSearchRequest(request);

        SearchHits<MallProductDocument> searchHits = elasticsearchOperations.search(
                buildSearchQuery(request),
                MallProductDocument.class
        );

        AdminMallProductSearchResult result = new AdminMallProductSearchResult();
        result.setPageNum((long) request.getPageNum());
        result.setPageSize((long) request.getPageSize());
        result.setTotal(searchHits.getTotalHits());
        result.setDocuments(searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .filter(Objects::nonNull)
                .filter(document -> document.getId() != null)
                .toList());
        return result;
    }

    /**
     * 规范化搜索请求。
     *
     * @param request 搜索请求
     */
    private void normalizeRequest(MallProductListQueryRequest request) {
        if (StringUtils.hasText(request.getName())) {
            request.setName(request.getName().trim());
        }
        request.setPageNum(Math.max(request.getPageNum(), 1));
        request.setPageSize(Math.min(Math.max(request.getPageSize(), 1), MAX_PAGE_SIZE));
    }

    /**
     * 校验搜索请求是否合法。
     *
     * @param request 搜索请求
     */
    private void validateSearchRequest(MallProductListQueryRequest request) {
        long offset = (long) (request.getPageNum() - 1) * request.getPageSize();
        if (offset + request.getPageSize() > MAX_RESULT_WINDOW) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "查询数据总数不能超过" + MAX_RESULT_WINDOW + "条");
        }
        if (!hasSearchCondition(request)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "搜索条件不能为空");
        }
        BigDecimal minPrice = request.getMinPrice();
        BigDecimal maxPrice = request.getMaxPrice();
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "最低价格不能大于最高价格");
        }
    }

    /**
     * 构建 Elasticsearch 搜索查询。
     *
     * @param request 搜索请求
     * @return Elasticsearch 查询对象
     */
    private NativeQuery buildSearchQuery(MallProductListQueryRequest request) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(buildRootQuery(request))
                .withPageable(PageRequest.of(request.getPageNum() - 1, request.getPageSize()))
                .withTrackTotalHits(true);
        return queryBuilder.build();
    }

    /**
     * 构建根查询。
     *
     * @param request 搜索请求
     * @return 根查询对象
     */
    private Query buildRootQuery(MallProductListQueryRequest request) {
        List<Query> mustQueries = buildMustQueries(request);
        List<Query> filterQueries = buildFilterQueries(request);
        return Query.of(queryBuilder -> queryBuilder.bool(boolBuilder -> {
            if (!mustQueries.isEmpty()) {
                boolBuilder.must(mustQueries);
            }
            if (!filterQueries.isEmpty()) {
                boolBuilder.filter(filterQueries);
            }
            return boolBuilder;
        }));
    }

    /**
     * 构建必须命中的查询条件。
     *
     * @param request 搜索请求
     * @return 必须命中的查询条件列表
     */
    private List<Query> buildMustQueries(MallProductListQueryRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        if (StringUtils.hasText(request.getName())) {
            mustQueries.add(buildKeywordQuery(request.getName()));
        }
        return mustQueries;
    }

    /**
     * 构建过滤条件列表。
     *
     * @param request 搜索请求
     * @return 过滤条件列表
     */
    private List<Query> buildFilterQueries(MallProductListQueryRequest request) {
        List<Query> filterQueries = new ArrayList<>();
        if (request.getId() != null) {
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.ids(idsQueryBuilder ->
                    idsQueryBuilder.values(String.valueOf(request.getId()))
            )));
        }
        if (request.getCategoryId() != null) {
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                    termBuilder.field("categoryIds").value(request.getCategoryId())
            )));
        }
        if (request.getStatus() != null) {
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                    termBuilder.field("status").value(request.getStatus())
            )));
        }
        appendPriceFilters(filterQueries, request);
        appendTagFilters(filterQueries, request.getTagFilterGroups());
        return filterQueries;
    }

    /**
     * 构建关键词查询。
     *
     * @param keyword 搜索关键词
     * @return 关键词查询
     */
    private Query buildKeywordQuery(String keyword) {
        List<Query> shouldQueries = new ArrayList<>();
        shouldQueries.add(Query.of(queryBuilder -> queryBuilder.multiMatch(multiMatchBuilder ->
                multiMatchBuilder.query(keyword)
                        .fields(KEYWORD_MATCH_FIELDS)
                        .type(TextQueryType.BestFields)
        )));
        if (!containsWhitespace(keyword)) {
            shouldQueries.add(Query.of(queryBuilder -> queryBuilder.multiMatch(multiMatchBuilder ->
                    multiMatchBuilder.query(keyword)
                            .fields(KEYWORD_PREFIX_FIELDS)
                            .type(TextQueryType.BoolPrefix)
            )));
        }
        return Query.of(queryBuilder -> queryBuilder.bool(boolBuilder ->
                boolBuilder.should(shouldQueries).minimumShouldMatch("1")
        ));
    }

    /**
     * 追加价格过滤条件。
     *
     * @param filterQueries 过滤条件列表
     * @param request       搜索请求
     */
    private void appendPriceFilters(List<Query> filterQueries, MallProductListQueryRequest request) {
        if (request.getMinPrice() != null) {
            filterQueries.add(buildNumberRangeQuery(request.getMinPrice(), true));
        }
        if (request.getMaxPrice() != null) {
            filterQueries.add(buildNumberRangeQuery(request.getMaxPrice(), false));
        }
    }

    /**
     * 构建单边价格区间查询。
     *
     * @param price    价格边界
     * @param isMinHit 是否为最低价条件
     * @return 区间查询
     */
    private Query buildNumberRangeQuery(BigDecimal price, boolean isMinHit) {
        return Query.of(queryBuilder -> queryBuilder.range(rangeBuilder ->
                rangeBuilder.number(numberRangeBuilder -> {
                    numberRangeBuilder.field("price");
                    if (isMinHit) {
                        numberRangeBuilder.gte(price.doubleValue());
                    } else {
                        numberRangeBuilder.lte(price.doubleValue());
                    }
                    return numberRangeBuilder;
                })
        ));
    }

    /**
     * 追加标签过滤条件。
     *
     * @param filterQueries 过滤条件列表
     * @param filterGroups  标签筛选分组
     */
    private void appendTagFilters(List<Query> filterQueries,
                                  List<MallProductTagFilterGroup> filterGroups) {
        for (MallProductTagFilterGroup filterGroup : normalizeFilterGroups(filterGroups)) {
            List<Query> shouldQueries = filterGroup.getTagIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(tagId -> Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                            termBuilder.field("tagTypeBindings").value(
                                    filterGroup.getTypeCode() + MallProductTagConstants.TYPE_BINDING_SEPARATOR + tagId
                            )
                    )))
                    .toList();
            if (!shouldQueries.isEmpty()) {
                filterQueries.add(Query.of(queryBuilder -> queryBuilder.bool(boolBuilder ->
                        boolBuilder.should(shouldQueries).minimumShouldMatch("1")
                )));
            }
        }
    }

    /**
     * 规范化标签筛选分组。
     *
     * @param filterGroups 原始标签筛选分组
     * @return 规范化后的标签筛选分组
     */
    private List<MallProductTagFilterGroup> normalizeFilterGroups(
            List<MallProductTagFilterGroup> filterGroups
    ) {
        if (filterGroups == null || filterGroups.isEmpty()) {
            return List.of();
        }
        return filterGroups.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeFilterGroup)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 规范化单个标签筛选分组。
     *
     * @param filterGroup 原始标签筛选分组
     * @return 规范化后的标签筛选分组
     */
    private MallProductTagFilterGroup normalizeFilterGroup(
            MallProductTagFilterGroup filterGroup
    ) {
        if (filterGroup == null || !StringUtils.hasText(filterGroup.getTypeCode()) || filterGroup.getTagIds() == null) {
            return null;
        }
        List<Long> normalizedTagIds = filterGroup.getTagIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedTagIds.isEmpty()) {
            return null;
        }
        MallProductTagFilterGroup normalizedGroup =
                new MallProductTagFilterGroup();
        normalizedGroup.setTypeId(filterGroup.getTypeId());
        normalizedGroup.setTypeCode(filterGroup.getTypeCode().trim());
        normalizedGroup.setTagIds(normalizedTagIds);
        return normalizedGroup;
    }

    /**
     * 判断是否提供了有效搜索条件。
     *
     * @param request 搜索请求
     * @return 是否存在有效搜索条件
     */
    private boolean hasSearchCondition(MallProductListQueryRequest request) {
        return request.getId() != null
                || StringUtils.hasText(request.getName())
                || request.getCategoryId() != null
                || request.getStatus() != null
                || request.getMinPrice() != null
                || request.getMaxPrice() != null
                || (request.getTagIds() != null && !request.getTagIds().isEmpty());
    }

    /**
     * 判断关键词是否包含空白字符。
     *
     * @param value 待判断字符串
     * @return 是否包含空白字符
     */
    private boolean containsWhitespace(String value) {
        return value != null && value.chars().anyMatch(Character::isWhitespace);
    }
}
