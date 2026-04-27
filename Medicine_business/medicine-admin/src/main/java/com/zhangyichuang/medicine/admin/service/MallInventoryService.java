package com.zhangyichuang.medicine.admin.service;

/**
 * 商品库存管理服务
 * <p>
 * 提供商品库存的增减操作，独立于商品管理服务，
 * 避免循环依赖问题。
 *
 * @author Chuang
 * created on 2025/11/25
 */
public interface MallInventoryService {

    /**
     * 恢复商品库存
     * <p>
     * 用于订单取消或未支付超时关闭时恢复商品库存数量
     *
     * @param productId 商品ID
     * @param quantity  恢复数量
     */
    void restoreStock(Long productId, Integer quantity);

    /**
     * 扣减商品库存
     * <p>
     * 用于下单时扣减商品库存数量
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     */
    void reduceStock(Long productId, Integer quantity);
}
