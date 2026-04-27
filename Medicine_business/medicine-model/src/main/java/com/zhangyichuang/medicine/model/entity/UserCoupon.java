package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户优惠券实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户优惠券ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 优惠券名称快照。
     */
    private String couponNameSnapshot;

    /**
     * 使用门槛金额快照。
     */
    private BigDecimal thresholdAmount;

    /**
     * 优惠券初始总金额。
     */
    private BigDecimal totalAmount;

    /**
     * 当前可用金额。
     */
    private BigDecimal availableAmount;

    /**
     * 当前锁定消耗金额。
     */
    private BigDecimal lockedConsumeAmount;

    /**
     * 是否允许继续使用：1-允许，0-不允许。
     */
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加：1-允许，0-不允许。
     */
    private Integer stackableEnabled;

    /**
     * 生效时间。
     */
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    private Date expireTime;

    /**
     * 用户优惠券状态。
     */
    private String couponStatus;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源业务号。
     */
    private String sourceBizNo;

    /**
     * 锁定订单号。
     */
    private String lockOrderNo;

    /**
     * 锁定时间。
     */
    private Date lockTime;

    /**
     * 乐观锁版本号。
     */
    @Version
    private Integer version;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 更新人。
     */
    private String updateBy;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
