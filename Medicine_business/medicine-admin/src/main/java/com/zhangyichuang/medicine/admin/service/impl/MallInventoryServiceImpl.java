package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.service.MallInventoryService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商品库存管理服务实现类
 * <p>
 * 实现商品库存的增减操作，直接操作Mapper层避免循环依赖。
 *
 * @author Chuang
 * created on 2025/11/25
 */
@Service
@RequiredArgsConstructor
public class MallInventoryServiceImpl implements MallInventoryService {

    private final MallProductMapper mallProductMapper;

    @Override
    public void restoreStock(Long productId, Integer quantity) {
        updateStock(productId, quantity, true);
    }

    @Override
    public void reduceStock(Long productId, Integer quantity) {
        updateStock(productId, quantity, false);
    }

    /**
     * 更新商品库存
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @param isRestore 是否为恢复库存（true为恢复，false为减少）
     */
    private void updateStock(Long productId, Integer quantity, boolean isRestore) {
        Assert.isPositive(quantity, "商品数量不能小于0");
        Assert.isPositive(productId, "商品ID不能小于0");

        MallProduct mallProduct = mallProductMapper.selectOne(
                new LambdaQueryWrapper<MallProduct>()
                        .eq(MallProduct::getId, productId)
                        .select(MallProduct::getStock, MallProduct::getVersion)
        );

        int newStock = getNewStock(quantity, isRestore, mallProduct);

        int currentVersion = mallProduct.getVersion() == null ? 0 : mallProduct.getVersion();

        int updatedRows = mallProductMapper.update(null,
                new LambdaUpdateWrapper<MallProduct>()
                        .eq(MallProduct::getId, productId)
                        .eq(MallProduct::getVersion, currentVersion)
                        .set(MallProduct::getStock, newStock)
                        .set(MallProduct::getVersion, currentVersion + 1)
        );

        if (updatedRows == 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "库存更新失败，请重试");
        }
    }

    private int getNewStock(Integer quantity, boolean isRestore, MallProduct mallProduct) {
        if (mallProduct == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "商品不存在");
        }

        Integer currentStock = mallProduct.getStock();
        int newStock;
        if (isRestore) {
            // 恢复库存
            newStock = (currentStock == null ? 0 : currentStock) + quantity;
        } else {
            // 减少库存
            if (currentStock == null || currentStock < quantity) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "库存不足");
            }
            newStock = currentStock - quantity;
        }
        return newStock;
    }
}
