package com.zhangyichuang.medicine.admin.task;

import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.publisher.ProductIndexMessagePublisher;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.model.constants.MallProductTagConstants;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import com.zhangyichuang.medicine.model.mq.ProductIndexPayload;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 将后台商品数据异步同步至 Elasticsearch 的任务。
 *
 * @author Chuang
 * @see com.zhangyichuang.medicine.client.elasticsearch.mq.MallProductIndexMessageListener
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallProductSearchIndexer {

    /**
     * 商品 Mapper。
     */
    private final MallProductMapper mallProductMapper;

    /**
     * 订单项服务。
     */
    private final MallOrderItemService mallOrderItemService;

    /**
     * 商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 商品分类关联服务。
     */
    private final MallProductCategoryRelService mallProductCategoryRelService;

    /**
     * 商品图片服务。
     */
    private final MallProductImageService mallProductImageService;

    /**
     * 商品分类服务。
     */
    private final MallCategoryService mallCategoryService;

    /**
     * 商品索引消息发布器。
     */
    private final ProductIndexMessagePublisher productIndexMessagePublisher;

    /**
     * 异步写入或更新商品索引。
     *
     * @param productId 商品ID
     */
    @Async
    public void reindexAsync(Long productId) {
        if (productId == null) {
            return;
        }
        MallProductDetailDto detail = mallProductMapper.getMallProductDetailById(productId);
        if (detail == null) {
            log.warn("跳过重建索引，商品 {} 未找到", productId);
            return;
        }
        reindexBatch(List.of(detail));
    }

    /**
     * 异步删除商品索引。
     *
     * @param productIds 商品ID集合
     */
    @Async
    public void removeAsync(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return;
        }
        productIndexMessagePublisher.publishDelete(productIds);
        log.info("向 RabbitMQ 发布 {} 个商品的删除事件", productIds.size());
    }

    /**
     * 按商品ID列表异步重建索引。
     *
     * @param productIds 商品ID列表
     */
    @Async
    public void reindexByProductIdsAsync(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return;
        }
        List<Long> normalizedProductIds = productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedProductIds.isEmpty()) {
            return;
        }
        List<MallProductDetailDto> products = mallProductMapper.getMallProductDetailByIds(normalizedProductIds);
        if (CollectionUtils.isEmpty(products)) {
            return;
        }
        reindexBatch(products);
    }

    /**
     * 批量发布商品索引事件。
     *
     * @param products 商品详情列表
     */
    public void reindexBatch(Collection<MallProductDetailDto> products) {
        if (CollectionUtils.isEmpty(products)) {
            return;
        }
        products.stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getId() != null)
                .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::enrichProductsForIndex))
                .stream()
                .map(this::toPayload)
                .filter(payload -> payload != null && payload.getId() != null)
                .forEach(productIndexMessagePublisher::publishUpsert);
        log.info("向 RabbitMQ 发布 {} 个商品的索引事件", products.size());
    }

    /**
     * 统一补齐商品索引所需的展示字段。
     *
     * @param products 原始商品详情列表
     * @return 补齐后的商品详情列表
     */
    private List<MallProductDetailDto> enrichProductsForIndex(List<MallProductDetailDto> products) {
        if (CollectionUtils.isEmpty(products)) {
            return List.of();
        }
        List<Long> productIds = products.stream()
                .map(MallProductDetailDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> salesMap = mallOrderItemService.getCompletedSalesByProductIds(productIds);
        Map<Long, String> coverImageMap = mallProductImageService.getFirstImageByProductIds(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        MallProductImage::getProductId,
                        MallProductImage::getImageUrl,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<Long, List<Long>> categoryIdsMap = mallProductCategoryRelService.listCategoryIdsMapByProductIds(productIds);
        Map<Long, MallCategory> categoryMap = listCategoryEntityMap(categoryIdsMap);
        Map<Long, List<MallProductTagVo>> tagMap = mallProductTagService.listEnabledTagVoMapByProductIds(productIds);

        products.forEach(product -> {
            if (product == null || product.getId() == null) {
                return;
            }
            product.setSales(salesMap.getOrDefault(product.getId(), 0));
            String coverImage = coverImageMap.get(product.getId());
            product.setImages(coverImage == null ? List.of() : List.of(coverImage));
            product.setTags(tagMap.getOrDefault(product.getId(), List.of()));
            applyCategoryBindings(product, categoryIdsMap.get(product.getId()), categoryMap);
        });
        return products;
    }

    /**
     * 查询分类实体映射。
     *
     * @param categoryIdsMap 商品到分类ID列表的映射
     * @return 分类ID到分类实体的映射
     */
    private Map<Long, MallCategory> listCategoryEntityMap(Map<Long, List<Long>> categoryIdsMap) {
        if (categoryIdsMap == null || categoryIdsMap.isEmpty()) {
            return Map.of();
        }
        List<Long> categoryIds = categoryIdsMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return mallCategoryService.listByIds(categoryIds).stream()
                .collect(java.util.stream.Collectors.toMap(MallCategory::getId, category -> category));
    }

    /**
     * 将分类关联结果写入商品详情对象。
     *
     * @param product     商品详情对象
     * @param categoryIds 商品分类ID列表
     * @param categoryMap 分类实体映射
     */
    private void applyCategoryBindings(MallProductDetailDto product,
                                       List<Long> categoryIds,
                                       Map<Long, MallCategory> categoryMap) {
        if (product == null) {
            return;
        }
        List<Long> normalizedCategoryIds = categoryIds == null ? List.of() : categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        product.setCategoryIds(normalizedCategoryIds);
        if (normalizedCategoryIds.isEmpty()) {
            product.setCategoryNames(List.of());
            return;
        }
        List<String> categoryNames = normalizedCategoryIds.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .map(MallCategory::getName)
                .filter(Objects::nonNull)
                .toList();
        product.setCategoryNames(categoryNames);
    }

    /**
     * 将商品详情转换为索引载荷。
     *
     * @param detail 商品详情
     * @return 索引载荷
     */
    private ProductIndexPayload toPayload(MallProductDetailDto detail) {
        if (detail == null) {
            return null;
        }
        return ProductIndexPayload.builder()
                .id(detail.getId())
                .name(detail.getName())
                .categoryIds(detail.getCategoryIds())
                .price(detail.getPrice())
                .sales(detail.getSales())
                .status(detail.getStatus())
                .categoryNames(detail.getCategoryNames())
                .drugCategory(detail.getDrugDetail() != null ? detail.getDrugDetail().getDrugCategory() : null)
                .brand(detail.getDrugDetail() != null ? detail.getDrugDetail().getBrand() : null)
                .commonName(detail.getDrugDetail() != null ? detail.getDrugDetail().getCommonName() : null)
                .efficacy(detail.getDrugDetail() != null ? detail.getDrugDetail().getEfficacy() : null)
                .tagIds(extractTagIds(detail.getTags()))
                .tagNames(extractTagNames(detail.getTags()))
                .keywordSuggestInputs(extractKeywordSuggestInputs(detail))
                .tagTypeBindings(extractTagTypeBindings(detail.getTags()))
                .instruction(detail.getDrugDetail() != null ? detail.getDrugDetail().getInstruction() : null)
                .taboo(detail.getDrugDetail() != null ? detail.getDrugDetail().getTaboo() : null)
                .coverImage(detail.getImages() != null && !detail.getImages().isEmpty() ? detail.getImages().getFirst() : null)
                .build();
    }

    /**
     * 提取标签ID列表。
     *
     * @param tags 标签列表
     * @return 标签ID列表
     */
    private List<Long> extractTagIds(List<MallProductTagVo> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(MallProductTagVo::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 提取标签名称列表。
     *
     * @param tags 标签列表
     * @return 标签名称列表
     */
    private List<String> extractTagNames(List<MallProductTagVo> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(MallProductTagVo::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 提取关键字补全输入列表。
     *
     * @param detail 商品详情
     * @return 关键字补全输入列表
     */
    private List<String> extractKeywordSuggestInputs(MallProductDetailDto detail) {
        if (detail == null) {
            return List.of();
        }
        List<String> inputs = new ArrayList<>();
        addKeywordSuggestInput(inputs, detail.getName());
        if (detail.getCategoryNames() != null) {
            detail.getCategoryNames().forEach(categoryName -> addKeywordSuggestInput(inputs, categoryName));
        }
        if (detail.getDrugDetail() != null) {
            addKeywordSuggestInput(inputs, detail.getDrugDetail().getBrand());
            addKeywordSuggestInput(inputs, detail.getDrugDetail().getCommonName());
        }
        extractTagNames(detail.getTags()).forEach(tagName -> addKeywordSuggestInput(inputs, tagName));
        return inputs;
    }

    /**
     * 追加关键字补全输入项。
     *
     * @param inputs 关键字补全输入列表
     * @param value  补全输入项
     */
    private void addKeywordSuggestInput(List<String> inputs, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String normalizedValue = value.trim();
        if (!inputs.contains(normalizedValue)) {
            inputs.add(normalizedValue);
        }
    }

    /**
     * 提取标签类型绑定列表。
     *
     * @param tags 标签列表
     * @return 标签类型绑定列表
     */
    private List<String> extractTagTypeBindings(List<MallProductTagVo> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .filter(tag -> tag.getId() != null && StringUtils.hasText(tag.getTypeCode()))
                .map(tag -> tag.getTypeCode() + MallProductTagConstants.TYPE_BINDING_SEPARATOR + tag.getId())
                .distinct()
                .toList();
    }
}
