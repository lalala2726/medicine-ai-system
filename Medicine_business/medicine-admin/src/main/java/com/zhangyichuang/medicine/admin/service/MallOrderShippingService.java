package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallOrderShipping;

/**
 * 订单物流服务
 *
 * @author Chuang
 * created 2025/11/08
 */
public interface MallOrderShippingService extends IService<MallOrderShipping> {

    /**
     * 根据订单ID查询物流信息
     *
     * @param orderId 订单ID
     * @return 物流信息
     */
    MallOrderShipping getByOrderId(Long orderId);

    /**
     * 创建物流记录
     *
     * @param shipping 物流信息
     * @return 是否成功
     */
    boolean createShipping(MallOrderShipping shipping);
}

