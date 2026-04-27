package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 订单时间线记录表
 */
@TableName(value = "mall_order_timeline")
@Data
public class MallOrderTimeline {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件状态
     */
    private String eventStatus;

    /**
     * 操作方(USER/ADMIN/SYSTEM)
     */
    private String operatorType;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 事件时间
     */
    private Date createdTime;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
