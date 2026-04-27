package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallAfterSaleTimeline;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;

import java.util.List;

/**
 * 售后时间线Service
 *
 * @author Chuang
 * created 2025/11/08
 */
public interface MallAfterSaleTimelineService extends IService<MallAfterSaleTimeline> {

    /**
     * 添加售后时间线记录
     *
     * @param afterSaleId  售后申请ID
     * @param eventType    事件类型
     * @param eventStatus  事件状态
     * @param operatorType 操作人类型
     * @param operatorId   操作人ID
     * @param description  事件描述
     */
    void addTimeline(Long afterSaleId, String eventType, String eventStatus,
                     String operatorType, Long operatorId, String description);

    /**
     * 获取售后时间线列表
     *
     * @param afterSaleId 售后申请ID
     * @return 时间线列表
     */
    List<AfterSaleTimelineVo> getTimelineList(Long afterSaleId);
}

