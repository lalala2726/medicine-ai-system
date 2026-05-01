package com.zhangyichuang.medicine.client.service;

import com.zhangyichuang.medicine.client.model.vo.MallProductVo;

/**
 * 客户端商品详情缓存服务。
 *
 * <p>只缓存商品静态展示信息，库存与销量由调用方实时补齐。</p>
 *
 * @author Chuang
 */
public interface MallProductDetailCacheService {

    /**
     * 获取缓存中的商品静态详情。
     *
     * @param id 商品ID
     * @return 商品静态详情
     */
    MallProductVo getCachedMallProductDetail(Long id);
}
