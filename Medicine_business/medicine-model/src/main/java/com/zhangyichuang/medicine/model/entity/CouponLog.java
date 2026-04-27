package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券日志实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_log")
public class CouponLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券日志ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 关联订单号。
     */
    private String orderNo;

    /**
     * 变更类型。
     */
    private String changeType;

    /**
     * 变更金额。
     */
    private BigDecimal changeAmount;

    /**
     * 订单抵扣金额。
     */
    private BigDecimal deductAmount;

    /**
     * 浪费金额。
     */
    private BigDecimal wasteAmount;

    /**
     * 变更前可用金额。
     */
    private BigDecimal beforeAvailableAmount;

    /**
     * 变更后可用金额。
     */
    private BigDecimal afterAvailableAmount;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源业务号。
     */
    private String sourceBizNo;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 操作人标识。
     */
    private String operatorId;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
