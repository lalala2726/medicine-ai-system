package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 商城商品图片服务接口
 * <p>
 * 提供商城商品图片的业务逻辑处理，包括图片的增删改查、
 * 图片排序、商品关联等功能。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Validated
public interface MallProductImageService extends IService<MallProductImage> {


    /**
     * 添加商品图片
     *
     * @param images 图片列表
     * @param id     商品ID
     */
    void addProductImages(@NotEmpty(message = "图片列表不能为空") List<String> images,
                          @NotNull(message = "商品ID不能为空") Long id);

    /**
     * 更新商品图片
     *
     * @param images 图片列表
     * @param id     商品ID
     */
    void updateProductImageById(@NotEmpty(message = "商品图片列表不能为空") List<String> images,
                                @NotNull(message = "商品ID不能为空") Long id);

    /**
     * 删除商品图片
     *
     * @param ids 商品ID列表
     */
    void removeImagesById(@NotEmpty(message = "商品ID列表不能为空") List<Long> ids);

    /**
     * 根据商品ID获取商品的首张图片
     *
     * @param productIds 商品ID列表
     * @return 商品首张图片列表
     */
    List<MallProductImage> getFirstImageByProductIds(@NotEmpty(message = "商品ID列表不能为空") List<Long> productIds);
}
