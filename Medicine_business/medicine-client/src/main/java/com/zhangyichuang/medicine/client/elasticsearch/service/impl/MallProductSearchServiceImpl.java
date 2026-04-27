package com.zhangyichuang.medicine.client.elasticsearch.service.impl;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.zhangyichuang.medicine.client.elasticsearch.repository.MallProductSearchRepository;
import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.client.model.dto.MallProductSearchQueryResult;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.suggest.response.CompletionSuggestion;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 商品搜索服务实现。
 *
 * @author Chuang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallProductSearchServiceImpl implements MallProductSearchService {

    /**
     * 查询结果窗口上限。
     */
    private static final int MAX_RESULT_WINDOW = 500;

    /**
     * 单次查询允许的最大页大小。
     */
    private static final int MAX_PAGE_SIZE = 50;
    /**
     * 标签绑定聚合名称。
     */
    private static final String TAG_BINDING_AGGREGATION_NAME = "tag_type_binding_counts";

    /**
     * 自动补全允许的最大返回数量。
     */
    private static final int MAX_SUGGEST_SIZE = 20;
    /**
     * 标签绑定聚合桶数量上限。
     */
    private static final int TAG_BINDING_AGGREGATION_SIZE = 500;
    /**
     * 关键字补全名称。
     */
    private static final String KEYWORD_SUGGEST_NAME = "keyword_suggest";
    /**
     * 关键字全文匹配字段。
     */
    private static final List<String> KEYWORD_MATCH_FIELDS = List.of(
            "name^5",
            "categoryNames^4",
            "brand^3",
            "commonName^4",
            "tagNames^4",
            "efficacy^2",
            "instruction"
    );
    /**
     * 关键字前缀匹配字段。
     */
    private static final List<String> KEYWORD_PREFIX_FIELDS = List.of(
            "name^5",
            "categoryNames^4",
            "brand^3",
            "commonName^4",
            "tagNames^4"
    );
    /**
     * 用途匹配字段。
     */
    private static final List<String> USAGE_MATCH_FIELDS = List.of(
            "efficacy^2",
            "instruction"
    );
    /**
     * 索引初始化锁对象。
     */
    private final Object indexInitializationMonitor = new Object();

    /**
     * 商品搜索仓库。
     */
    private final MallProductSearchRepository searchRepository;

    /**
     * Elasticsearch 操作对象。
     */
    private final ElasticsearchOperations elasticsearchOperations;
    /**
     * 商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 保存商品索引文档。
     *
     * @param document 商品索引文档
     */
    @Override
    public void save(MallProductDocument document) {
        if (document == null || document.getId() == null) {
            log.warn("Skip saving empty product document: {}", document);
            return;
        }
        ensureIndexExists();
        searchRepository.save(document);
    }

    /**
     * 批量保存商品索引文档。
     *
     * @param documents 商品索引文档列表
     */
    @Override
    public void saveAll(List<MallProductDocument> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        ensureIndexExists();
        searchRepository.saveAll(documents);
    }

    /**
     * 删除商品索引文档。
     *
     * @param productId 商品ID
     */
    @Override
    public void deleteById(Long productId) {
        if (productId == null) {
            return;
        }
        if (!indexExists()) {
            return;
        }
        searchRepository.deleteById(productId);
    }

    /**
     * 确保商品索引存在。
     */
    private void ensureIndexExists() {
        synchronized (indexInitializationMonitor) {
            if (indexExists()) {
                return;
            }
            var indexOps = elasticsearchOperations.indexOps(MallProductDocument.class);
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }
    }

    /**
     * 判断商品索引是否存在。
     *
     * @return 商品索引是否存在
     */
    private boolean indexExists() {
        return elasticsearchOperations.indexOps(MallProductDocument.class).exists();
    }

    /**
     * 搜索商品索引。
     *
     * @param request 搜索请求
     * @return 搜索结果与动态筛选结果
     */
    @Override
    public MallProductSearchQueryResult search(MallProductSearchRequest request) {
        validateSearchRequest(request);

        NativeQuery searchQuery = buildSearchQuery(request);
        SearchHits<MallProductDocument> searchHits = elasticsearchOperations.search(searchQuery, MallProductDocument.class);

        Map<String, Long> tagBindingCountMap = extractTagBindingCounts(searchHits);
        for (MallProductTagFilterGroup filterGroup : normalizeFilterGroups(request.getTagFilterGroups())) {
            SearchHits<MallProductDocument> relaxedGroupHits = elasticsearchOperations.search(
                    buildTagAggregationQuery(request, filterGroup.getTypeCode()),
                    MallProductDocument.class
            );
            replaceTypeBindingCounts(
                    tagBindingCountMap,
                    filterGroup.getTypeCode(),
                    extractTagBindingCounts(relaxedGroupHits)
            );
        }

        MallProductSearchQueryResult result = new MallProductSearchQueryResult();
        result.setSearchHits(searchHits);
        result.setTagFilters(mallProductTagService.buildSearchTagFilters(tagBindingCountMap));
        return result;
    }

    /**
     * 商品搜索自动补全。
     *
     * @param keyword 搜索关键字
     * @param size    返回数量
     * @return 自动补全结果
     */
    @Override
    public List<String> suggest(String keyword, int size) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(size, MAX_SUGGEST_SIZE));
        String normalizedKeyword = keyword.trim();

        NativeQuery query = NativeQuery.builder()
                .withMaxResults(0)
                .withSuggester(buildKeywordSuggester(normalizedKeyword, limit))
                .build();
        SearchHits<MallProductDocument> searchHits = elasticsearchOperations.search(query, MallProductDocument.class);
        if (searchHits == null || !searchHits.hasSuggest()) {
            return List.of();
        }
        Suggest.Suggestion<?> suggestion = searchHits.getSuggest().getSuggestion(KEYWORD_SUGGEST_NAME);
        if (!(suggestion instanceof CompletionSuggestion<?> completionSuggestion)) {
            return List.of();
        }
        Set<String> suggestionSet = new LinkedHashSet<>();
        for (CompletionSuggestion.Entry<?> entry : completionSuggestion.getEntries()) {
            for (CompletionSuggestion.Entry.Option<?> option : entry.getOptions()) {
                addSuggestion(option.getText(), suggestionSet, limit);
                if (suggestionSet.size() >= limit) {
                    return suggestionSet.stream().limit(limit).toList();
                }
            }
        }
        return suggestionSet.stream().limit(limit).toList();
    }

    /**
     * 校验搜索请求。
     *
     * @param request 搜索请求
     */
    private void validateSearchRequest(MallProductSearchRequest request) {
        int pageNum = Math.max(request.getPageNum(), 1);
        int pageSize = Math.min(Math.max(request.getPageSize(), 1), MAX_PAGE_SIZE);
        long offset = (long) (pageNum - 1) * pageSize;
        if (offset + pageSize > MAX_RESULT_WINDOW) {
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
     * 构建商品搜索查询。
     *
     * @param request 搜索请求
     * @return Elasticsearch 原生查询
     */
    private NativeQuery buildSearchQuery(MallProductSearchRequest request) {
        int pageNum = Math.max(request.getPageNum(), 1);
        int pageSize = Math.min(Math.max(request.getPageSize(), 1), MAX_PAGE_SIZE);
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildRootQuery(request, null))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withTrackTotalHits(true)
                .withAggregation(TAG_BINDING_AGGREGATION_NAME, buildTagBindingAggregation());
        Sort sort = buildSort(request);
        if (sort.isSorted()) {
            builder.withSort(sort);
        }
        return builder.build();
    }

    /**
     * 构建只用于标签聚合的查询。
     *
     * @param request         搜索请求
     * @param ignoredTypeCode 需要忽略的标签类型编码
     * @return Elasticsearch 原生聚合查询
     */
    private NativeQuery buildTagAggregationQuery(MallProductSearchRequest request, String ignoredTypeCode) {
        return NativeQuery.builder()
                .withQuery(buildRootQuery(request, ignoredTypeCode))
                .withMaxResults(0)
                .withAggregation(TAG_BINDING_AGGREGATION_NAME, buildTagBindingAggregation())
                .build();
    }

    /**
     * 构建搜索根查询。
     *
     * @param request         搜索请求
     * @param ignoredTypeCode 需要忽略的标签类型编码
     * @return Elasticsearch 查询对象
     */
    private Query buildRootQuery(MallProductSearchRequest request, String ignoredTypeCode) {
        List<Query> mustQueries = buildMustQueries(request);
        List<Query> filterQueries = buildFilterQueries(request, ignoredTypeCode);
        if (mustQueries.isEmpty() && filterQueries.isEmpty()) {
            return Query.of(queryBuilder -> queryBuilder.matchAll(matchAllBuilder -> matchAllBuilder));
        }
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
    private List<Query> buildMustQueries(MallProductSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();
        if (StringUtils.hasText(request.getKeyword())) {
            mustQueries.add(buildKeywordQuery(request.getKeyword().trim()));
        }
        if (StringUtils.hasText(request.getEfficacy())) {
            mustQueries.add(buildUsageQuery(request.getEfficacy().trim()));
        }
        return mustQueries;
    }

    /**
     * 构建过滤条件列表。
     *
     * @param request         搜索请求
     * @param ignoredTypeCode 需要忽略的标签类型编码
     * @return 过滤条件列表
     */
    private List<Query> buildFilterQueries(MallProductSearchRequest request, String ignoredTypeCode) {
        List<Query> filterQueries = new ArrayList<>();
        filterQueries.add(Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                termBuilder.field("status").value(request.getStatus() != null ? request.getStatus() : 1)
        )));
        if (StringUtils.hasText(request.getCategoryName())) {
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                    termBuilder.field("categoryNames.keyword").value(request.getCategoryName().trim())
            )));
        }
        if (request.getCategoryId() != null) {
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.term(termBuilder ->
                    termBuilder.field("categoryIds").value(request.getCategoryId())
            )));
        }
        appendPriceFilters(filterQueries, request);
        appendTagFilters(filterQueries, request.getTagFilterGroups(), ignoredTypeCode);
        return filterQueries;
    }

    /**
     * 构建关键字查询。
     *
     * @param keyword 搜索关键字
     * @return 关键字查询
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
     * 构建用途查询。
     *
     * @param usage 用途关键字
     * @return 用途查询
     */
    private Query buildUsageQuery(String usage) {
        return Query.of(queryBuilder -> queryBuilder.multiMatch(multiMatchBuilder ->
                multiMatchBuilder.query(usage)
                        .fields(USAGE_MATCH_FIELDS)
                        .type(TextQueryType.BestFields)
        ));
    }

    /**
     * 追加价格过滤条件。
     *
     * @param filterQueries 过滤条件列表
     * @param request       搜索请求
     */
    private void appendPriceFilters(List<Query> filterQueries, MallProductSearchRequest request) {
        if (request.getPrice() != null) {
            BigDecimal exactPrice = request.getPrice();
            filterQueries.add(Query.of(queryBuilder -> queryBuilder.range(rangeBuilder ->
                    rangeBuilder.number(numberRangeBuilder -> numberRangeBuilder
                            .field("price")
                            .gte(exactPrice.doubleValue())
                            .lte(exactPrice.doubleValue())
                    )
            )));
            return;
        }
        BigDecimal minPrice = request.getMinPrice();
        if (minPrice != null) {
            filterQueries.add(buildNumberRangeQuery(minPrice, true));
        }
        BigDecimal maxPrice = request.getMaxPrice();
        if (maxPrice != null) {
            filterQueries.add(buildNumberRangeQuery(maxPrice, false));
        }
    }

    /**
     * 构建单边价格区间查询。
     *
     * @param price    价格边界
     * @param isMinHit true 表示最低价格；false 表示最高价格
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
     * @param filterQueries   过滤条件列表
     * @param filterGroups    标签分组
     * @param ignoredTypeCode 需要忽略的标签类型编码
     */
    private void appendTagFilters(List<Query> filterQueries,
                                  List<MallProductTagFilterGroup> filterGroups,
                                  String ignoredTypeCode) {
        for (MallProductTagFilterGroup filterGroup : normalizeFilterGroups(filterGroups)) {
            if (Objects.equals(filterGroup.getTypeCode(), ignoredTypeCode)) {
                continue;
            }
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
     * 构建标签绑定聚合。
     *
     * @return 标签绑定聚合定义
     */
    private Aggregation buildTagBindingAggregation() {
        return Aggregation.of(aggregationBuilder -> aggregationBuilder.terms(termsBuilder ->
                termsBuilder.field("tagTypeBindings").size(TAG_BINDING_AGGREGATION_SIZE)
        ));
    }

    /**
     * 提取标签绑定命中数量。
     *
     * @param searchHits 搜索结果
     * @return 标签绑定与命中数量映射
     */
    private Map<String, Long> extractTagBindingCounts(SearchHits<MallProductDocument> searchHits) {
        if (searchHits == null || !searchHits.hasAggregations()) {
            return new LinkedHashMap<>();
        }
        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();
        if (!(aggregationsContainer instanceof ElasticsearchAggregations elasticsearchAggregations)) {
            return new LinkedHashMap<>();
        }
        if (elasticsearchAggregations.get(TAG_BINDING_AGGREGATION_NAME) == null) {
            return new LinkedHashMap<>();
        }
        Aggregate aggregate = elasticsearchAggregations.get(TAG_BINDING_AGGREGATION_NAME).aggregation().getAggregate();
        if (!aggregate.isSterms()) {
            return new LinkedHashMap<>();
        }
        Buckets<StringTermsBucket> buckets = aggregate.sterms().buckets();
        Iterable<StringTermsBucket> bucketIterable = buckets.isArray()
                ? buckets.array()
                : buckets.keyed().values();
        Map<String, Long> tagBindingCountMap = new LinkedHashMap<>();
        for (StringTermsBucket bucket : bucketIterable) {
            if (bucket == null || !bucket.key().isString() || bucket.docCount() <= 0) {
                continue;
            }
            tagBindingCountMap.put(bucket.key().stringValue(), bucket.docCount());
        }
        return tagBindingCountMap;
    }

    /**
     * 替换指定标签类型的聚合结果。
     *
     * @param targetCountMap 目标数量映射
     * @param typeCode       标签类型编码
     * @param replacementMap 替换数量映射
     */
    private void replaceTypeBindingCounts(Map<String, Long> targetCountMap,
                                          String typeCode,
                                          Map<String, Long> replacementMap) {
        if (!StringUtils.hasText(typeCode)) {
            return;
        }
        String bindingPrefix = typeCode + MallProductTagConstants.TYPE_BINDING_SEPARATOR;
        targetCountMap.keySet().removeIf(binding -> binding.startsWith(bindingPrefix));
        for (Map.Entry<String, Long> entry : replacementMap.entrySet()) {
            if (!entry.getKey().startsWith(bindingPrefix)) {
                continue;
            }
            targetCountMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 构建关键字补全配置。
     *
     * @param keyword 搜索关键字
     * @param size    返回数量
     * @return 关键字补全配置
     */
    private Suggester buildKeywordSuggester(String keyword, int size) {
        return Suggester.of(suggesterBuilder -> suggesterBuilder
                .text(keyword)
                .suggesters(KEYWORD_SUGGEST_NAME, fieldSuggesterBuilder -> fieldSuggesterBuilder
                        .prefix(keyword)
                        .completion(completionSuggesterBuilder -> completionSuggesterBuilder
                                .field("keywordSuggest")
                                .size(size)
                                .skipDuplicates(true)
                        )
                )
        );
    }

    /**
     * 追加自动补全候选项。
     *
     * @param value       原始值
     * @param suggestions 候选集合
     * @param limit       返回上限
     */
    private void addSuggestion(String value, Set<String> suggestions, int limit) {
        if (!StringUtils.hasText(value) || suggestions.size() >= limit) {
            return;
        }
        suggestions.add(value.trim());
    }

    /**
     * 判断是否提供了至少一个可用搜索条件。
     *
     * @param request 搜索请求
     * @return 是否提供了有效搜索条件
     */
    private boolean hasSearchCondition(MallProductSearchRequest request) {
        return StringUtils.hasText(request.getKeyword())
                || StringUtils.hasText(request.getCategoryName())
                || request.getCategoryId() != null
                || StringUtils.hasText(request.getEfficacy())
                || !CollectionUtils.isEmpty(request.getTagIds());
    }

    /**
     * 标准化标签筛选分组列表。
     *
     * @param filterGroups 标签筛选分组列表
     * @return 标准化后的标签筛选分组列表
     */
    private List<MallProductTagFilterGroup> normalizeFilterGroups(
            List<MallProductTagFilterGroup> filterGroups
    ) {
        if (CollectionUtils.isEmpty(filterGroups)) {
            return List.of();
        }
        return filterGroups.stream()
                .filter(Objects::nonNull)
                .filter(filterGroup -> StringUtils.hasText(filterGroup.getTypeCode()))
                .filter(filterGroup -> !CollectionUtils.isEmpty(filterGroup.getTagIds()))
                .toList();
    }

    /**
     * 判断关键字是否包含空白字符。
     *
     * @param keyword 搜索关键字
     * @return 是否包含空白字符
     */
    private boolean containsWhitespace(String keyword) {
        return keyword.chars().anyMatch(Character::isWhitespace);
    }

    /**
     * 构建排序条件。
     *
     * @param request 搜索请求
     * @return 排序条件
     */
    private Sort buildSort(MallProductSearchRequest request) {
        Sort.Direction priceDirection = parseDirection(request.getPriceSort());
        Sort.Direction salesDirection = parseDirection(request.getSalesSort());
        if (priceDirection != null && salesDirection != null) {
            return Sort.by(new Sort.Order(priceDirection, "price"), new Sort.Order(salesDirection, "sales"));
        }
        if (priceDirection != null) {
            return Sort.by(new Sort.Order(priceDirection, "price"));
        }
        if (salesDirection != null) {
            return Sort.by(new Sort.Order(salesDirection, "sales"));
        }
        return Sort.unsorted();
    }

    /**
     * 解析排序方向。
     *
     * @param direction 排序方向
     * @return Spring Sort 排序方向
     */
    private Sort.Direction parseDirection(String direction) {
        if (!StringUtils.hasText(direction)) {
            return null;
        }
        String normalizedDirection = direction.trim().toLowerCase();
        if ("asc".equals(normalizedDirection)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equals(normalizedDirection)) {
            return Sort.Direction.DESC;
        }
        return null;
    }
}
