package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.MallOrderTimelineMapper;
import com.zhangyichuang.medicine.admin.service.MallOrderTimelineService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.dto.OrderTimelineDto;
import com.zhangyichuang.medicine.model.entity.MallOrderTimeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 订单时间线服务实现类
 *
 * @author Chuang
 */
@Slf4j
@Service
public class MallOrderTimelineServiceImpl extends ServiceImpl<MallOrderTimelineMapper, MallOrderTimeline>
        implements MallOrderTimelineService {

    /**
     * 添加订单时间线记录
     * <p>
     * 插入前会检查该订单是否已存在相同的事件类型，如果存在则抛出异常
     * </p>
     *
     * @param dto 时间线数据传输对象
     * @return 是否添加成功
     */
    @Override
    public boolean addTimeline(OrderTimelineDto dto) {
        // 参数校验
        Assert.notNull(dto, "时间线数据不能为空");
        Assert.notNull(dto.getOrderId(), "订单ID不能为空");
        Assert.hasText(dto.getEventType(), "事件类型不能为空");
        Assert.hasText(dto.getOperatorType(), "操作方不能为空");

        // 检查是否已存在相同的事件类型
        long count = lambdaQuery()
                .eq(MallOrderTimeline::getOrderId, dto.getOrderId())
                .eq(MallOrderTimeline::getEventType, dto.getEventType())
                .count();

        if (count > 0) {
            log.warn("订单时间线记录已存在，orderId={}, eventType={}", dto.getOrderId(), dto.getEventType());
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "该订单已存在相同的事件记录，不能重复添加");
        }

        // 构建时间线实体
        MallOrderTimeline timeline = new MallOrderTimeline();
        timeline.setOrderId(dto.getOrderId());
        timeline.setEventType(dto.getEventType());
        timeline.setEventStatus(dto.getEventStatus());
        timeline.setOperatorType(dto.getOperatorType());
        timeline.setDescription(dto.getDescription());
        timeline.setCreatedTime(new Date());

        // 保存时间线记录
        boolean saved = save(timeline);
        if (saved) {
            log.info("订单时间线记录添加成功，orderId={}, eventType={}", dto.getOrderId(), dto.getEventType());
        }
        return saved;
    }

    /**
     * 根据订单ID查询时间线列表
     * <p>
     * 按创建时间倒序排列，最新的事件在前
     * </p>
     *
     * @param orderId 订单ID
     * @return 时间线列表
     */
    @Override
    public List<MallOrderTimeline> getTimelineByOrderId(Long orderId) {
        Assert.notNull(orderId, "订单ID不能为空");

        return lambdaQuery()
                .eq(MallOrderTimeline::getOrderId, orderId)
                .orderByDesc(MallOrderTimeline::getCreatedTime)
                .list();
    }

    /**
     * 添加时间线记录（如果不存在）
     * <p>
     * 用于自动创建时间线，如果该订单已存在相同的事件类型则跳过，不抛出异常
     * </p>
     *
     * @param dto 时间线数据传输对象
     */
    public void addTimelineIfNotExists(OrderTimelineDto dto) {
        // 参数校验
        if (dto == null || dto.getOrderId() == null || dto.getEventType() == null) {
            log.warn("时间线数据不完整，跳过添加");
            return;
        }

        // 检查是否已存在相同的事件类型
        long count = lambdaQuery()
                .eq(MallOrderTimeline::getOrderId, dto.getOrderId())
                .eq(MallOrderTimeline::getEventType, dto.getEventType())
                .count();

        if (count > 0) {
            log.debug("订单时间线记录已存在，跳过添加，orderId={}, eventType={}", dto.getOrderId(), dto.getEventType());
            return;
        }

        // 构建时间线实体
        MallOrderTimeline timeline = new MallOrderTimeline();
        timeline.setOrderId(dto.getOrderId());
        timeline.setEventType(dto.getEventType());
        timeline.setEventStatus(dto.getEventStatus());
        timeline.setOperatorType(dto.getOperatorType());
        timeline.setDescription(dto.getDescription());
        timeline.setCreatedTime(new Date());

        // 保存时间线记录
        boolean saved = save(timeline);
        if (saved) {
            log.info("订单时间线记录自动添加成功，orderId={}, eventType={}", dto.getOrderId(), dto.getEventType());
        }
    }
}




