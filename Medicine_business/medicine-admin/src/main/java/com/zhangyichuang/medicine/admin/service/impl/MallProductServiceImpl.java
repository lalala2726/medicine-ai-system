package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.elasticsearch.model.dto.AdminMallProductSearchResult;
import com.zhangyichuang.medicine.admin.elasticsearch.service.AdminMallProductSearchService;
import com.zhangyichuang.medicine.admin.mapper.MallOrderItemMapper;
import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.model.dto.ProductSalesDto;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.admin.task.MallProductSearchIndexer;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.DeliveryTypeEnum;
import com.zhangyichuang.medicine.model.enums.DrugCategoryEnum;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import com.zhangyichuang.medicine.model.request.MallProductAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商城商品服务实现类
 * <p>
 * 实现商城商品的业务逻辑处理，包括商品的增删改查、
 * 商品列表查询、商品详情获取等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Service
@RequiredArgsConstructor
public class MallProductServiceImpl extends ServiceImpl<MallProductMapper, MallProduct>
        implements MallProductService, BaseService {

    private final MallProductMapper mallProductMapper;
    private final MallCategoryService mallCategoryService;
    private final MallProductCategoryRelService mallProductCategoryRelService;
    private final MallProductImageService mallProductImageService;
    private final MallMedicineDetailService medicineDetailService;
    private final MallProductStatsService mallProductStatsService;
    private final MallProductTagService mallProductTagService;
    private final MallProductTagRelService mallProductTagRelService;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final AdminMallProductSearchService adminMallProductSearchService;
    private final MallProductSearchIndexer mallProductSearchIndexer;

    @Override
    public Page<MallProduct> listMallProduct(MallProductListQueryRequest request) {
        mallProductTagService.fillTagFilterGroups(request);
        Page<MallProduct> page = page(new Page<>(request.getPageNum(), request.getPageSize()));
        return mallProductMapper.listMallProduct(page, request);
    }

    @Override
    public Page<MallProductDetailDto> listMallProductWithCategory(MallProductListQueryRequest request) {
        mallProductTagService.fillTagFilterGroups(request);
        // 先查询商品列表
        Page<MallProductDetailDto> page = mallProductMapper.listMallProductWithCategory(request.toPage(), request);

        if (page.getRecords().isEmpty()) {
            return page;
        }

        // 查询商品的销量
        List<ProductSalesDto> productSales = mallProductStatsService.getProductSales();

        // 将销量数据转换为Map，方便快速查找
        HashMap<Long, Integer> salesMap = new HashMap<>();
        if (productSales != null && !productSales.isEmpty()) {
            productSales.forEach(sales -> salesMap.put(sales.getProductId(), sales.getSales()));
        }

        List<Long> productIds = page.getRecords().stream().map(MallProduct::getId).toList();
        Map<Long, String> coverImageMap = mallProductImageService.getFirstImageByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(MallProductImage::getProductId, MallProductImage::getImageUrl));
        Map<Long, List<com.zhangyichuang.medicine.model.vo.MallProductTagVo>> tagMap =
                mallProductTagService.listTagVoMapByProductIds(productIds);
        Map<Long, List<Long>> categoryIdsMap = mallProductCategoryRelService.listCategoryIdsMapByProductIds(productIds);
        Map<Long, MallCategory> categoryMap = listCategoryEntityMap(categoryIdsMap);

        // 为每个商品设置销量
        page.getRecords().forEach(product -> {
            Long productId = product.getId();
            Integer sales = salesMap.get(productId);
            product.setSales(sales != null ? sales : 0);
            String cover = coverImageMap.get(productId);
            product.setImages(cover == null ? List.of() : List.of(cover));
            product.setTags(tagMap.getOrDefault(productId, List.of()));
            applyCategoryBindings(product, categoryIdsMap.get(productId), categoryMap);
        });
        return page;
    }

    /**
     * 使用 Elasticsearch 搜索商品并按命中顺序补齐展示字段。
     *
     * @param request 查询参数
     * @return 分页的商城商品列表（基于 Elasticsearch 命中结果）
     */
    @Override
    public Page<MallProductDetailDto> searchMallProductWithCategory(MallProductListQueryRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        AdminMallProductSearchResult searchResult = adminMallProductSearchService.searchProducts(request);
        Page<MallProductDetailDto> page = new Page<>(searchResult.getPageNum(), searchResult.getPageSize(), searchResult.getTotal());

        List<Long> orderedProductIds = searchResult.getDocuments() == null ? List.of() : searchResult.getDocuments().stream()
                .map(MallProductDocument::getId)
                .filter(Objects::nonNull)
                .toList();
        if (orderedProductIds.isEmpty()) {
            page.setRecords(List.of());
            return page;
        }

        Map<Long, Integer> salesMap = searchResult.getDocuments().stream()
                .filter(Objects::nonNull)
                .filter(document -> document.getId() != null)
                .collect(Collectors.toMap(
                        MallProductDocument::getId,
                        document -> document.getSales() == null ? 0 : document.getSales(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<Long, MallProductDetailDto> productMap = getMallProductByIds(orderedProductIds).stream()
                .collect(Collectors.toMap(
                        MallProduct::getId,
                        product -> product,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<MallProductDetailDto> orderedRecords = orderedProductIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .peek(product -> product.setSales(salesMap.getOrDefault(product.getId(), 0)))
                .toList();
        page.setRecords(orderedRecords);
        return page;
    }

    @Override
    public MallProductDetailDto getMallProductById(Long id) {
        if (id == null) {
            throw new ServiceException("商品ID不能为空");
        }

        MallProductDetailDto product = mallProductMapper.getMallProductDetailById(id);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }
        List<String> images = mallProductImageService.lambdaQuery()
                .eq(MallProductImage::getProductId, id)
                .orderByAsc(MallProductImage::getSort)
                .list()
                .stream()
                .map(MallProductImage::getImageUrl)
                .toList();
        product.setImages(images);
        product.setTags(mallProductTagService.listTagVoMapByProductIds(List.of(id)).getOrDefault(id, List.of()));
        Map<Long, List<Long>> categoryIdsMap = mallProductCategoryRelService.listCategoryIdsMapByProductIds(List.of(id));
        Map<Long, MallCategory> categoryMap = listCategoryEntityMap(categoryIdsMap);
        applyCategoryBindings(product, categoryIdsMap.get(id), categoryMap);
        return product;
    }

    @Override
    public List<MallProductDetailDto> getMallProductByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // 批量查询商品基础信息
        List<MallProductDetailDto> products = mallProductMapper.getMallProductDetailByIds(ids);
        if (products.isEmpty()) {
            return List.of();
        }

        // 批量查询图片并按 productId 分组
        List<Long> productIds = products.stream().map(MallProduct::getId).toList();
        Map<Long, List<String>> imageMap = mallProductImageService.lambdaQuery()
                .in(MallProductImage::getProductId, productIds)
                .orderByAsc(MallProductImage::getSort)
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        MallProductImage::getProductId,
                        Collectors.mapping(MallProductImage::getImageUrl, Collectors.toList())
                ));
        Map<Long, List<com.zhangyichuang.medicine.model.vo.MallProductTagVo>> tagMap =
                mallProductTagService.listTagVoMapByProductIds(productIds);
        Map<Long, List<Long>> categoryIdsMap = mallProductCategoryRelService.listCategoryIdsMapByProductIds(productIds);
        Map<Long, MallCategory> categoryMap = listCategoryEntityMap(categoryIdsMap);

        // 设置图片到每个商品
        products.forEach(product -> {
            List<String> images = imageMap.getOrDefault(product.getId(), List.of());
            product.setImages(images);
            product.setTags(tagMap.getOrDefault(product.getId(), List.of()));
            applyCategoryBindings(product, categoryIdsMap.get(product.getId()), categoryMap);
        });

        return products;
    }

    @Override
    public List<AgentDrugDetailDto> getDrugDetailByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        // 批量查询商品信息（获取商品名称）
        List<MallProduct> products = listByIds(productIds);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(MallProduct::getId, MallProduct::getName, (existing, ignore) -> existing));

        // 批量查询药品详情
        List<DrugDetail> drugDetails = medicineDetailService.lambdaQuery()
                .in(DrugDetail::getProductId, productIds)
                .list();
        if (drugDetails.isEmpty()) {
            return List.of();
        }

        // 组装结果
        return drugDetails.stream()
                .map(drug -> {
                    DrugDetailDto drugDetailDto = copyProperties(drug, DrugDetailDto.class);
                    AgentDrugDetailDto dto = new AgentDrugDetailDto();
                    dto.setProductId(drug.getProductId());
                    dto.setProductName(productNameMap.get(drug.getProductId()));
                    dto.setDrugDetail(drugDetailDto);
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisConstants.MallProduct.CACHE_NAME, allEntries = true)
    public boolean addMallProduct(MallProductAddRequest request) {
        List<Long> normalizedTagIds = mallProductTagService.normalizeEnabledTagIds(request.getTagIds());
        List<Long> normalizedCategoryIds = validateAndNormalizeCategoryIds(request.getCategoryIds());

        // 检查商品名称是否已存在
        LambdaQueryWrapper<MallProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallProduct::getName, request.getName());
        if (count(queryWrapper) > 0) {
            throw new ServiceException("商品名称已存在");
        }

        // 检查价格是否为负数
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("商品价格不能为负数");
        }

        // 检查库存是否为负数
        if (request.getStock() < 0) {
            throw new ServiceException("商品库存不能为负数");
        }

        // 检查配送方式是否存在
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromLegacyCode(request.getDeliveryType());
        Assert.isTrue(deliveryTypeEnum != null, "配送方式不存在");

        MallProduct product = new MallProduct();
        BeanUtils.copyProperties(request, product);
        product.setCategoryId(resolvePrimaryCategoryId(normalizedCategoryIds));
        product.setCreateTime(new Date());
        product.setCreateBy(SecurityUtils.getUsername());

        boolean save = save(product);

        // 仅聚焦商品与图片：确保至少一张图片后批量写入图片表
        Assert.isTrue(request.getImages() != null && !request.getImages().isEmpty(), "商品图片至少需要上传一张图片");
        mallProductImageService.addProductImages(request.getImages(), product.getId());
        mallProductCategoryRelService.replaceProductCategories(product.getId(), normalizedCategoryIds);
        mallProductTagRelService.replaceProductTags(product.getId(), normalizedTagIds);
        // 上述是通用的商城属性,下面是药品特有的属性
        validateDrugCategory(request.getDrugDetail());
        DrugDetail drugDetail = copyProperties(request.getDrugDetail(), DrugDetail.class);
        drugDetail.setProductId(product.getId());
        boolean result = medicineDetailService.addMedicineDetail(drugDetail);

        boolean success = save && result;
        if (success) {
            runAfterCommit(() -> mallProductSearchIndexer.reindexAsync(product.getId()));
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisConstants.MallProduct.CACHE_NAME, key = "#request.id", condition = "#request.id != null")
    public boolean updateMallProduct(MallProductUpdateRequest request) {
        List<Long> existingTagIds = mallProductTagRelService.listByProductIds(List.of(request.getId())).stream()
                .map(MallProductTagRel::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> normalizedTagIds = mallProductTagService.normalizeProductTagIds(request.getTagIds(), existingTagIds);
        List<Long> normalizedCategoryIds = validateAndNormalizeCategoryIds(request.getCategoryIds());

        // 检查商品是否存在
        MallProduct existingProduct = getById(request.getId());
        if (existingProduct == null) {
            throw new ServiceException("商品不存在");
        }

        // 检查商品名称是否已存在（排除自己）
        LambdaQueryWrapper<MallProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallProduct::getName, request.getName())
                .ne(MallProduct::getId, request.getId());
        if (count(queryWrapper) > 0) {
            throw new ServiceException("商品名称已存在");
        }

        // 检查价格是否为负数
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("商品价格不能为负数");
        }

        // 检查库存是否为负数
        if (request.getStock() != null && request.getStock() < 0) {
            throw new ServiceException("商品库存不能为负数");
        }

        // 检查配送方式是否存在
        DeliveryTypeEnum deliveryTypeEnum = DeliveryTypeEnum.fromLegacyCode(request.getDeliveryType());
        Assert.isTrue(deliveryTypeEnum != null, "配送方式不存在");

        BeanUtils.copyProperties(request, existingProduct);
        existingProduct.setCategoryId(resolvePrimaryCategoryId(normalizedCategoryIds));
        existingProduct.setUpdateTime(new Date());
        existingProduct.setUpdateBy(SecurityUtils.getUsername());

        // 更新商品主图集合，同样保障后台始终有可展示的图片
        Assert.isTrue(request.getImages() != null && !request.getImages().isEmpty(), "商品图片至少需要上传一张图片");
        mallProductImageService.updateProductImageById(request.getImages(), existingProduct.getId());
        mallProductCategoryRelService.replaceProductCategories(existingProduct.getId(), normalizedCategoryIds);
        mallProductTagRelService.replaceProductTags(existingProduct.getId(), normalizedTagIds);

        // 更新药品详情
        if (request.getDrugDetail() != null) {
            validateDrugCategory(request.getDrugDetail());
            DrugDetail drugDetail = copyProperties(request.getDrugDetail(), DrugDetail.class);
            drugDetail.setProductId(existingProduct.getId());
            medicineDetailService.updateMedicineDetail(drugDetail);
        }

        boolean updated = updateById(existingProduct);
        if (updated) {
            runAfterCommit(() -> mallProductSearchIndexer.reindexAsync(existingProduct.getId()));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisConstants.MallProduct.CACHE_NAME, allEntries = true)
    public boolean deleteMallProduct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择要删除的商品");
        }

        // 批量检查商品是否存在，避免循环查询数据库
        List<MallProduct> products = listByIds(ids);
        if (products.size() != ids.size()) {
            // 找出不存在的商品ID
            List<Long> existIds = products.stream()
                    .map(MallProduct::getId)
                    .toList();
            List<Long> notExistIds = ids.stream()
                    .filter(id -> !existIds.contains(id))
                    .toList();
            throw new ServiceException("商品不存在: " + notExistIds);
        }

        List<String> limitedOrderStatuses = List.of(
                OrderStatusEnum.PENDING_SHIPMENT.getType(),
                OrderStatusEnum.PENDING_RECEIPT.getType(),
                OrderStatusEnum.AFTER_SALE.getType()
        );
        List<Long> blockedProductIds = mallOrderItemMapper.findProductIdsWithOrderStatuses(ids, limitedOrderStatuses);
        if (!blockedProductIds.isEmpty()) {
            throw new ServiceException("商品存在待发货/待收货/售后中的订单，无法删除: " + blockedProductIds);
        }

        // 删除关联的图片
        mallProductImageService.removeImagesById(ids);

        // 删除关联的商品分类
        mallProductCategoryRelService.removeByProductIds(ids);

        // 删除关联的商品标签
        mallProductTagRelService.removeByProductIds(ids);

        // 删除关联的药品详情
        medicineDetailService.deleteMedicineDetailByProductIds(ids);

        boolean removed = removeByIds(ids);
        if (removed) {
            runAfterCommit(() -> mallProductSearchIndexer.removeAsync(ids));
        }
        return removed;
    }

    /**
     * 校验并归一化商品分类ID列表。
     *
     * @param categoryIds 原始商品分类ID列表
     * @return 去重后的商品分类ID列表
     */
    private List<Long> validateAndNormalizeCategoryIds(List<Long> categoryIds) {
        Assert.notEmpty(categoryIds, "商品分类不能为空");
        List<Long> normalizedCategoryIds = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Assert.notEmpty(normalizedCategoryIds, "商品分类不能为空");
        List<MallCategory> categories = mallCategoryService.listByIds(normalizedCategoryIds);
        Assert.isTrue(categories.size() == normalizedCategoryIds.size(), "商品分类不存在");
        return normalizedCategoryIds;
    }

    /**
     * 解析用于保留表结构的主分类ID。
     * 分类事实来源统一以关联表为准。
     *
     * @param categoryIds 商品分类ID列表
     * @return 首个商品分类ID
     */
    private Long resolvePrimaryCategoryId(List<Long> categoryIds) {
        Assert.notEmpty(categoryIds, "商品分类不能为空");
        return categoryIds.getFirst();
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
     * 在事务提交后触发异步同步，避免读取到未提交数据。
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 校验药品分类编码是否有效。
     *
     * @param drugDetail 药品详情
     */
    private void validateDrugCategory(DrugDetailDto drugDetail) {
        Assert.notNull(drugDetail, "药品说明信息不能为空");
        Integer drugCategory = drugDetail.getDrugCategory();
        Assert.notNull(drugCategory, "药品分类不能为空");
        Assert.isTrue(DrugCategoryEnum.fromCode(drugCategory) != null, "药品分类不合法");
    }
}
