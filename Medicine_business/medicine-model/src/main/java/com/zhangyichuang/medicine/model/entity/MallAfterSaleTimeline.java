package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 售后时间线表
 *
 * @author Chuang
 * created 2025/11/08
 */
@TableName(value = "mall_after_sale_timeline")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallAfterSaleTimeline {

    /**
     * 时间线ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 售后申请ID
     */
    private Long afterSaleId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件状态
     */
    private String eventStatus;

    /**
     * 操作人类型(USER-用户, ADMIN-管理员, SYSTEM-系统)
     */
    private String operatorType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}

