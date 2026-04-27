package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;

import java.util.List;

/**
 * @author Chuang
 */
public interface MallOrderItemService extends IService<MallOrderItem> {

    /**
     * 根据订单ID查询订单项列表。
     */
    List<MallOrderItem> getOrderItemByOrderId(Long orderId);

    /**
     * 获取指定商品的已完成订单销量汇总。
     */
    java.util.Map<Long, Integer> getCompletedSalesByProductIds(List<Long> productIds);

    /**
     * 获取指定商品的已完成订单销量。
     */
    Integer getCompletedSalesByProductId(Long productId);

}
