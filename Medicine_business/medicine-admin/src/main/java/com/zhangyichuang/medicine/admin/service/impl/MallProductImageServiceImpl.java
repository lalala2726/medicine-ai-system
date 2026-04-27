package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallProductImageMapper;
import com.zhangyichuang.medicine.admin.service.MallProductImageService;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 商城商品图片服务实现类
 * <p>
 * 实现商城商品图片的业务逻辑处理，包括图片的增删改查、
 * 图片排序、商品关联等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Service
public class MallProductImageServiceImpl extends ServiceImpl<MallProductImageMapper, MallProductImage>
        implements MallProductImageService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProductImages(List<String> images, Long id) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<MallProductImage> list = IntStream.range(0, images.size())
                .mapToObj(index -> MallProductImage.builder()
                        .imageUrl(images.get(index))
                        .productId(id)
                        .sort(index)
                        .createTime(new Date())
                        .build())
                .toList();
        saveBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductImageById(List<String> images, Long id) {
        LambdaQueryWrapper<MallProductImage> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(MallProductImage::getProductId, id);
        remove(removeWrapper);
        addProductImages(images, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeImagesById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<MallProductImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MallProductImage::getProductId, ids);
        remove(wrapper);
    }

    @Override
    public List<MallProductImage> getFirstImageByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        // 查询出所有相关产品的图片
        List<MallProductImage> images = lambdaQuery()
                .in(MallProductImage::getProductId, productIds)
                .list();

        // 按产品ID分组，并找出每个产品中sort值最小的图片
        return images.stream()
                .collect(java.util.stream.Collectors.groupingBy(MallProductImage::getProductId))
                .values().stream()
                .map(mallProductImages -> mallProductImages.stream()
                        .min(java.util.Comparator.comparing(MallProductImage::getSort))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

}



