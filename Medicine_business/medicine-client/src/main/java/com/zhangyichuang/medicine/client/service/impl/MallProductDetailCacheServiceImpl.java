package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.client.mapper.MallProductMapper;
import com.zhangyichuang.medicine.client.model.vo.MallProductVo;
import com.zhangyichuang.medicine.client.service.MallCategoryService;
import com.zhangyichuang.medicine.client.service.MallProductCategoryRelService;
import com.zhangyichuang.medicine.client.service.MallProductDetailCacheService;
import com.zhangyichuang.medicine.client.service.MallProductTagService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductWithImageDto;
import com.zhangyichuang.medicine.model.entity.MallCategory;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 客户端商品详情缓存服务实现。
 *
 * <p>缓存商品基础信息、图片、分类、标签和药品说明等静态展示字段。</p>
 *
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallProductDetailCacheServiceImpl implements MallProductDetailCacheService {

    /**
     * 商品 Mapper。
     */
    private final MallProductMapper mallProductMapper;

    /**
     * 商品分类服务。
     */
    private final MallCategoryService mallCategoryService;

    /**
     * 商品分类关联服务。
     */
    private final MallProductCategoryRelService mallProductCategoryRelService;

    /**
     * 商品标签服务。
     */
    private final MallProductTagService mallProductTagService;

    /**
     * 获取缓存中的商品静态详情。
     *
     * @param id 商品ID
     * @return 商品静态详情
     */
    @Override
    @Cacheable(cacheNames = RedisConstants.MallProduct.CACHE_NAME, key = "#id")
    public MallProductVo getCachedMallProductDetail(Long id) {
        if (id == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品ID不能为空");
        }

        MallProductWithImageDto productWithImages = mallProductMapper.getProductWithImagesById(id);
        if (productWithImages == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }

        MallProductVo productVo = new MallProductVo();
        productVo.setId(productWithImages.getId());
        productVo.setName(productWithImages.getName());
        productVo.setUnit(productWithImages.getUnit());
        productVo.setPrice(productWithImages.getPrice());
        productVo.setCouponEnabled(productWithImages.getCouponEnabled());
        productVo.setDrugDetail(productWithImages.getDrugDetail());
        productVo.setTags(mallProductTagService.listEnabledTagVoMapByProductIds(List.of(id)).getOrDefault(id, List.of()));

        MallProductDetailDto categoryDetail = buildCategoryDetail(id);
        productVo.setCategoryIds(categoryDetail.getCategoryIds());
        productVo.setCategoryNames(categoryDetail.getCategoryNames());
        productVo.setImages(extractImageUrls(productWithImages));
        return productVo;
    }

    /**
     * 提取商品图片 URL 列表。
     *
     * @param productWithImages 商品与图片聚合对象
     * @return 商品图片 URL 列表
     */
    private List<String> extractImageUrls(MallProductWithImageDto productWithImages) {
        if (productWithImages.getProductImages() == null || productWithImages.getProductImages().isEmpty()) {
            return List.of();
        }
        return productWithImages.getProductImages().stream()
                .map(MallProductImage::getImageUrl)
                .toList();
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
}
