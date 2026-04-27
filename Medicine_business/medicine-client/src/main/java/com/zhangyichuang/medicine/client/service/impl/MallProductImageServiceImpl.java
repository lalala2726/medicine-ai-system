package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallProductImageMapper;
import com.zhangyichuang.medicine.client.service.MallProductImageService;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * @author Chuang
 */
@Service
public class MallProductImageServiceImpl extends ServiceImpl<MallProductImageMapper, MallProductImage>
        implements MallProductImageService {

    private final MallProductImageMapper mallProductImageMapper;

    public MallProductImageServiceImpl(MallProductImageMapper mallProductImageMapper) {
        this.mallProductImageMapper = mallProductImageMapper;
    }

    /**
     * 获取商品封面图片
     *
     * @param productId 商品ID
     * @return 商品封面图片
     */
    @Override
    public String getProductCoverImage(Long productId) {
        List<MallProductImage> list = lambdaQuery()
                .eq(MallProductImage::getProductId, productId)
                .list();
        return list.stream()
                .min(Comparator.comparingInt(MallProductImage::getSort))
                .map(MallProductImage::getImageUrl)
                .orElse("");
    }

    @Override
    public List<MallProductImage> getProductCoverImage(List<Long> productIds) {
        return mallProductImageMapper.getProductCoverImage(productIds);
    }
}




