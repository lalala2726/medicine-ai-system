package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;

import java.util.List;

/**
 * 商品标签关联服务（客户端）。
 *
 * @author Chuang
 */
public interface MallProductTagRelService extends IService<MallProductTagRel> {

    /**
     * 按商品ID列表查询标签关联。
     *
     * @param productIds 商品ID列表
     * @return 标签关联列表
     */
    List<MallProductTagRel> listByProductIds(List<Long> productIds);
}
