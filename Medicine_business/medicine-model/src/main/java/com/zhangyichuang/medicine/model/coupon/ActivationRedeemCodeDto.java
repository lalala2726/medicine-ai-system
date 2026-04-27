package com.zhangyichuang.medicine.model.coupon;

import lombok.Data;

import java.util.Date;

/**
 * 激活码兑换查询结果。
 */
@Data
public class ActivationRedeemCodeDto {

    /**
     * 批次ID。
     */
    private Long batchId;

    /**
     * 激活码ID。
     */
    private Long codeId;

    /**
     * 批次号。
     */
    private String batchNo;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 兑换规则类型。
     */
    private String redeemRuleType;

    /**
     * 有效期类型。
     */
    private String validityType;

    /**
     * 固定生效时间。
     */
    private Date fixedEffectiveTime;

    /**
     * 固定失效时间。
     */
    private Date fixedExpireTime;

    /**
     * 激活后有效天数。
     */
    private Integer relativeValidDays;

    /**
     * 批次状态。
     */
    private String batchStatus;

    /**
     * 激活码状态。
     */
    private String codeStatus;

    /**
     * 激活码明文。
     */
    private String plainCode;
}
