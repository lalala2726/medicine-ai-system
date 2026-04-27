package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.ViewHistoryRequest;
import com.zhangyichuang.medicine.client.model.vo.ViewHistoryVo;
import com.zhangyichuang.medicine.model.entity.MallProductViewHistory;

/**
 * @author Chuang
 */
public interface MallProductViewHistoryService extends IService<MallProductViewHistory> {


    /**
     * 获取浏览列表
     *
     * @param userId  用户ID
     * @param request 列表查询参数
     * @return 浏览列表
     */
    Page<ViewHistoryVo> listViewHistory(Long userId, ViewHistoryRequest request);

    /**
     * 记录浏览记录,如果存在则更新时间
     *
     * @param userId    用户ID
     * @param productId 商品ID
     */
    void recordViewHistory(Long userId, Long productId);


    /**
     * 删除浏览记录
     *
     * @param userId    用户ID
     * @param productId 商品ID
     */
    void deleteViewHistory(Long userId, Long productId);

    /**
     * 删除所有浏览记录
     *
     * @param userId 用户ID
     */
    void deleteAllViewHistory(Long userId);


    /**
     * 获取浏览记录
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 浏览记录
     */
    MallProductViewHistory getViewHistory(Long userId, Long productId);


}
