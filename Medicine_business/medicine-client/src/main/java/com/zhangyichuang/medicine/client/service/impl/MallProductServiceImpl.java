package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.elasticsearch.service.MallProductSearchService;
import com.zhangyichuang.medicine.client.enums.ProductViewPeriod;
import com.zhangyichuang.medicine.client.mapper.MallProductMapper;
import com.zhangyichuang.medicine.client.model.dto.AssistantProductPurchaseCardDto;
import com.zhangyichuang.medicine.client.model.dto.MallProductSearchQueryResult;
import com.zhangyichuang.medicine.client.model.dto.MallProductSearchResultDto;
import com.zhangyichuang.medicine.client.model.dto.RecommendProductDto;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.AssistantProductPurchaseCardsVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductVo;
import com.zhangyichuang.medicine.client.service.*;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductWithImageDto;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import com.zhangyichuang.medicine.model.vo.RecommendListVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallProductServiceImpl extends ServiceImpl<MallProductMapper, MallProduct>
        implements MallProductService, BaseService {

    /**
     * 推荐商品的最大返回数量。
     */
    private static final int RECOMMEND_LIMIT = 20;
    private final MallProductMapper mallProductMapper;
    private final MallCategoryService mallCategoryService;
    private final MallProductCategoryRelService mallProductCategoryRelService;
    private final MallProductImageService mallProductImageService;
    private final MallProductViewHistoryService mallProductViewHistoryService;
    private final MallOrderItemService mallOrderItemService;
    private final MallProductTagService mallProductTagService;
    private final MallProductSearchService mallProductSearchService;
    private final MallProductDetailCacheService mallProductDetailCacheService;

    @Override
    public List<RecommendListVo> recommend() {

        // 获取候选集（销量/浏览量靠前），随后在代码层加入权重+随机
        List<RecommendProductDto> candidates = mallProductMapper.listRecommendProducts();

        // 如果没有商品，直接返回空列表
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 追加销量信息（单独查询，避免多表关联超过 3 张表）
        Map<Long, Integer> salesMap = mallOrderItemService.getCompletedSalesByProductIds(
                candidates.stream()
                        .map(MallProduct::getId)
                        .toList());
        candidates.forEach(product -> product.setSales(Optional.ofNullable(salesMap.get(product.getId())).orElse(0)));

        // 计算权重，加入适度随机，排序后截取
        List<RecommendProductDto> picked = candidates.stream()
                .sorted((a, b) -> Double.compare(weight(b), weight(a)))
                .limit(RECOMMEND_LIMIT)
                .toList();

        // 提取商品ID列表
        List<Long> productIds = picked.stream()
                .map(MallProduct::getId)
                .toList();

        // 查询商品封面图片
        List<MallProductImage> productCoverImages = mallProductImageService.getProductCoverImage(productIds);

        // 将图片列表转换为Map，方便后续查询
        HashMap<Long, String> imageMap = new HashMap<>();
        productCoverImages.forEach(productImage ->
                imageMap.put(productImage.getProductId(), productImage.getImageUrl()));

        // 转换为推荐列表VO
        return picked.stream().map(product ->
                RecommendListVo.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .cover(imageMap.get(product.getId()))
                        .price(product.getPrice())
                        .sales(product.getSales())
                        .drugCategory(product.getDrugCategory())
                        .build()
        ).toList();
    }

    /**
     * 权重 = 销量/浏览量/排序号权重 * 随机系数，保证热门优先且保留一定随机性。
     */
    private double weight(RecommendProductDto product) {
        double sales = Optional.ofNullable(product.getSales()).orElse(0);
        double views = Optional.ofNullable(product.getViews()).orElse(0L);
        int sort = Optional.ofNullable(product.getSort()).orElse(100);
        double base = sales * 0.6 + views * 0.25 + (100 - Math.min(sort, 100)) * 0.15;
        double randomFactor = 0.8 + Math.random() * 0.4; // 0.8~1.2
        return base * randomFactor;
    }

    @Override
    public MallProduct getMallProductById(Long id) {
        MallProduct mallProduct = getById(id);
        if (mallProduct == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }
        return mallProduct;
    }

    @Override
    public MallProductVo getMallProductDetail(Long id) {
        if (id == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品ID不能为空");
        }

        MallProductVo cachedProductDetail = mallProductDetailCacheService.getCachedMallProductDetail(id);
        MallProduct latestProduct = getById(id);
        if (latestProduct == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }

        MallProductVo productVo = copyProductDetail(cachedProductDetail);
        productVo.setStock(latestProduct.getStock());
        productVo.setSales(mallOrderItemService.getCompletedSalesByProductId(id));
        return productVo;
    }

    @Override
    public MallProductWithImageDto getProductWithImagesById(Long id) {
        if (id == null) {
            return null;
        }
        MallProductWithImageDto product = mallProductMapper.getProductWithImagesById(id);
        if (product == null) {
            return null;
        }
        product.setSales(mallOrderItemService.getCompletedSalesByProductId(id));
        return product;
    }

    @Override
    public AssistantProductPurchaseCardsVo getAssistantProductPurchaseCards(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return emptyAssistantProductPurchaseCards();
        }

        List<Long> normalizedIds = productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return emptyAssistantProductPurchaseCards();
        }

        List<AssistantProductPurchaseCardDto> cardDtos = mallProductMapper.listAssistantProductPurchaseCardsByIds(normalizedIds);
        if (cardDtos == null || cardDtos.isEmpty()) {
            return emptyAssistantProductPurchaseCards();
        }

        Map<Long, AssistantProductPurchaseCardDto> cardDtoMap = cardDtos.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        AssistantProductPurchaseCardDto::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Long productId : normalizedIds) {
            AssistantProductPurchaseCardDto cardDto = cardDtoMap.get(productId);
            if (cardDto == null) {
                continue;
            }
            BigDecimal itemPrice = defaultPrice(cardDto.getPrice());
            totalPrice = totalPrice.add(itemPrice);
            items.add(AssistantProductPurchaseCardsVo.AssistantProductPurchaseItemVo.builder()
                    .id(String.valueOf(cardDto.getId()))
                    .name(cardDto.getName())
                    .image(cardDto.getImage())
                    .price(formatPrice(itemPrice))
                    .spec(cardDto.getSpec())
                    .efficacy(cardDto.getEfficacy())
                    .drugCategory(cardDto.getDrugCategory())
                    .stock(cardDto.getStock())
                    .build());
        }

        return AssistantProductPurchaseCardsVo.builder()
                .totalPrice(formatPrice(totalPrice))
                .items(items)
                .build();
    }

    @Override
    public PageResult<MallProductSearchVo> search(MallProductSearchRequest request) {
        return searchWithTagFilters(request).getPageResult();
    }

    /**
     * 搜索商品并返回动态标签筛选结果。
     *
     * @param request 搜索请求
     * @return 搜索结果与动态标签筛选列表
     */
    @Override
    public MallProductSearchResultDto searchWithTagFilters(MallProductSearchRequest request) {
        int safePageNum = Math.max(request.getPageNum(), 1);
        int safePageSize = Math.max(request.getPageSize(), 1);
        if (!hasSearchCondition(request)) {
            MallProductSearchResultDto emptyResult = new MallProductSearchResultDto();
            emptyResult.setPageResult(new PageResult<>((long) safePageNum, (long) safePageSize, 0L, Collections.emptyList()));
            emptyResult.setTagFilters(List.of());
            return emptyResult;
        }
        if (StringUtils.hasText(request.getKeyword())) {
            request.setKeyword(request.getKeyword().trim());
        }
        if (StringUtils.hasText(request.getCategoryName())) {
            request.setCategoryName(request.getCategoryName().trim());
        }
        mallProductTagService.fillSearchTagGroups(request);
        if (StringUtils.hasText(request.getEfficacy())) {
            request.setEfficacy(request.getEfficacy().trim());
        }
        request.setPageNum(safePageNum);
        request.setPageSize(safePageSize);

        MallProductSearchQueryResult queryResult = mallProductSearchService.search(request);
        SearchHits<MallProductDocument> hits = queryResult.getSearchHits();
        PageResult<MallProductSearchVo> pageResult;
        if (hits == null || hits.isEmpty()) {
            pageResult = new PageResult<>((long) safePageNum, (long) safePageSize, 0L, Collections.emptyList());
        } else {
            List<MallProductSearchVo> rows = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toSearchVo)
                    .filter(vo -> vo.getProductId() != null)
                    .toList();

            pageResult = new PageResult<>(
                    (long) safePageNum,
                    (long) safePageSize,
                    hits.getTotalHits(),
                    rows
            );
        }
        MallProductSearchResultDto result = new MallProductSearchResultDto();
        result.setPageResult(pageResult);
        result.setTagFilters(queryResult.getTagFilters() == null ? List.of() : queryResult.getTagFilters());
        return result;
    }

    /**
     * 查询搜索筛选弹窗使用的全量标签分组。
     *
     * @return 按标签类型分组后的筛选标签列表
     */
    @Override
    public List<MallProductSearchTagFilterVo> listAllEnabledSearchTagFilters() {
        return mallProductTagService.listAllEnabledSearchTagFilters();
    }

    private MallProductSearchVo toSearchVo(MallProductDocument doc) {
        if (doc == null) {
            return MallProductSearchVo.builder().build();
        }

        return MallProductSearchVo.builder()
                .productId(doc.getId())
                .productName(doc.getName())
                .cover(doc.getCoverImage())
                .commonName(doc.getCommonName())
                .categoryNames(doc.getCategoryNames())
                .brand(doc.getBrand())
                .price(doc.getPrice())
                .drugCategory(doc.getDrugCategory())
                .tagNames(doc.getTagNames())
                .efficacy(doc.getEfficacy())
                .taboo(doc.getTaboo())
                .build();
    }

    @Override
    public List<String> suggest(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return mallProductSearchService.suggest(keyword.trim(), 10);
    }

    @Override
    public MallProductDetailDto getProductAndDrugInfoById(Long id) {
        Assert.isPositive(id, "商品ID不能为空");
        MallProductDetailDto mallProductDetailDto = mallProductMapper.getProductAndDrugInfoById(id);
        if (mallProductDetailDto == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }
        mallProductDetailDto.setSales(mallOrderItemService.getCompletedSalesByProductId(id));
        List<String> imageUrls = mallProductImageService.lambdaQuery()
                .eq(MallProductImage::getProductId, id)
                .orderByAsc(MallProductImage::getSort)
                .list()
                .stream()
                .map(MallProductImage::getImageUrl)
                .toList();
        mallProductDetailDto.setImages(imageUrls);
        mallProductDetailDto.setTags(mallProductTagService.listEnabledTagVoMapByProductIds(List.of(id)).getOrDefault(id, List.of()));
        MallProductDetailDto categoryDetail = buildCategoryDetail(id);
        mallProductDetailDto.setCategoryIds(categoryDetail.getCategoryIds());
        mallProductDetailDto.setCategoryNames(categoryDetail.getCategoryNames());
        return mallProductDetailDto;

    }

    @Override
    public void recordView(Long productId) {
        Objects.requireNonNull(productId);
        Long userId = getUserId();
        if (userId == null) {
            return;
        }
        mallProductViewHistoryService.recordViewHistory(userId, productId);
    }

    /**
     * 计算商品浏览量
     *
     * @param views      商品浏览量
     * @param timeMillis 时间戳
     * @return 计算后的商品浏览量
     */
    public Double calculateProductViews(int views, long timeMillis) {
        return views + 1 - timeMillis / 1e13;
    }

    /**
     * 复制商品详情响应对象，避免直接修改缓存对象。
     *
     * @param source 缓存中的商品详情
     * @return 可安全补充动态字段的商品详情副本
     */
    private MallProductVo copyProductDetail(MallProductVo source) {
        MallProductVo target = new MallProductVo();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setCategoryIds(source.getCategoryIds());
        target.setCategoryNames(source.getCategoryNames());
        target.setUnit(source.getUnit());
        target.setPrice(source.getPrice());
        target.setCouponEnabled(source.getCouponEnabled());
        target.setImages(source.getImages());
        target.setDrugDetail(source.getDrugDetail());
        target.setTags(source.getTags());
        return target;
    }

    private AssistantProductPurchaseCardsVo emptyAssistantProductPurchaseCards() {
        return AssistantProductPurchaseCardsVo.builder()
                .totalPrice(formatPrice(BigDecimal.ZERO))
                .items(Collections.emptyList())
                .build();
    }

    private BigDecimal defaultPrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private String formatPrice(BigDecimal price) {
        return defaultPrice(price)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    /**
     * 构建商品的分类详情信息。
     *
     * @param productId 商品ID
     * @return 仅包含分类信息的商品详情对象
     */
    private MallProductDetailDto buildCategoryDetail(Long productId) {
        MallProductDetailDto detailDto = new MallProductDetailDto();
        Map<Long, List<Long>> categoryIdsMap = mallProductCategoryRelService.listCategoryIdsMapByProductIds(List.of(productId));
        Map<Long, MallCategory> categoryMap = listCategoryEntityMap(categoryIdsMap);
        applyCategoryBindings(detailDto, categoryIdsMap.get(productId), categoryMap);
        return detailDto;
    }

    /**
     * 按商品分类关联结果构建分类实体映射。
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
                .collect(Collectors.toMap(MallCategory::getId, category -> category));
    }

    /**
     * 将分类关联结果写入商品详情对象。
     *
     * @param detail      商品详情对象
     * @param categoryIds 商品分类ID列表
     * @param categoryMap 分类实体映射
     */
    private void applyCategoryBindings(MallProductDetailDto detail,
                                       List<Long> categoryIds,
                                       Map<Long, MallCategory> categoryMap) {
        if (detail == null) {
            return;
        }
        List<Long> normalizedCategoryIds = categoryIds == null ? List.of() : categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        detail.setCategoryIds(normalizedCategoryIds);
        if (normalizedCategoryIds.isEmpty()) {
            detail.setCategoryNames(List.of());
            return;
        }
        List<String> categoryNames = normalizedCategoryIds.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .map(MallCategory::getName)
                .filter(Objects::nonNull)
                .toList();
        detail.setCategoryNames(categoryNames);
    }

    @Override
    public long getViewCount(Long productId, ProductViewPeriod period) {
        return 1L;
    }

    /**
     * 判断是否提供了至少一个搜索条件。
     *
     * @param request 搜索请求
     * @return 是否存在有效搜索条件
     */
    private boolean hasSearchCondition(MallProductSearchRequest request) {
        return StringUtils.hasText(request.getKeyword())
                || StringUtils.hasText(request.getCategoryName())
                || request.getCategoryId() != null
                || StringUtils.hasText(request.getEfficacy())
                || (request.getTagIds() != null && !request.getTagIds().isEmpty());
    }

    @Override
    @Transactional
    public void deductStock(Long productId, Integer quantity) {
        Assert.isPositive(quantity, "商品数量不能小于0");
        Assert.isPositive(productId, "商品ID不能小于0");
        // 1. 查询商品信息
        MallProduct product = getById(productId);
        if (product == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品不存在");
        }
        // 2. 校验库存
        Integer stock = product.getStock();
        if (stock == null || stock < quantity) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    String.format("商品库存不足，当前库存：%d", stock));
        }
        // 3. 扣减库存，带乐观锁防止并发超卖
        int updated = baseMapper.updateStockWithVersion(
                productId,
                quantity,
                product.getVersion()
        );

        if (updated == 0) {
            // 更新失败说明库存不足或版本冲突
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "库存更新失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long productId, Integer quantity) {
        Assert.isPositive(quantity, "商品数量不能小于0");
        Assert.isPositive(productId, "商品ID不能小于0");
        MallProduct mallProduct = lambdaQuery()
                .eq(MallProduct::getId, productId)
                .select(MallProduct::getStock, MallProduct::getVersion)
                .one();

        if (mallProduct == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品不存在");
        }
        Integer currentStock = mallProduct.getStock();
        int newStock = (currentStock == null ? 0 : currentStock) + quantity;
        int currentVersion = mallProduct.getVersion() == null ? 0 : mallProduct.getVersion();
        boolean updated = lambdaUpdate()
                .eq(MallProduct::getId, productId)
                .eq(MallProduct::getVersion, currentVersion)
                .set(MallProduct::getStock, newStock)
                .set(MallProduct::getVersion, currentVersion + 1)
                .update();
        if (!updated) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "库存更新失败，请重试");
        }
    }
}
