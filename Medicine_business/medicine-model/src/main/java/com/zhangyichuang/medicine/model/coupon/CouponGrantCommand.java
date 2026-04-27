package com.zhangyichuang.medicine.model.coupon;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 优惠券发放命令。
 */
@Data
@Builder
public class CouponGrantCommand {

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 生效时间。
     */
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    private Date expireTime;

    /**
     * 券来源类型。
     */
    private String sourceType;

    /**
     * 来源业务号。
     */
    private String sourceBizNo;

    /**
     * 发券备注。
     */
    private String remark;

    /**
     * 操作人标识。
     */
    private String operatorId;
}
