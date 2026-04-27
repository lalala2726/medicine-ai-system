package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;

import java.util.List;

/**
 * 订单时间线服务接口
 *
 * @author Chuang
 */
public interface MallOrderTimelineService extends IService<MallOrderTimeline> {

    /**
     * 添加订单时间线记录
     * <p>
     * 插入前会检查该订单是否已存在相同的事件类型，如果存在则抛出异常
     * </p>
     *
     * @param dto 时间线数据传输对象
     */
    void addTimeline(OrderTimelineDto dto);

    /**
     * 根据订单ID查询时间线列表。
     *
     * @param orderId 订单ID
     * @return 时间线列表
     */
    List<MallOrderTimeline> getTimelineByOrderId(Long orderId);

    /**
     * 添加时间线记录（如果不存在）
     * <p>
     * 用于自动创建时间线，如果该订单已存在相同的事件类型则跳过，不抛出异常
     * </p>
     *
     * @param dto 时间线数据传输对象
     */
    void addTimelineIfNotExists(OrderTimelineDto dto);
}
