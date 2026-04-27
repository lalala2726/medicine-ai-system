package com.zhangyichuang.medicine.model.coupon;

import lombok.Data;

import java.util.Date;

/**
 * 激活码批次查询结果。
 */
@Data
public class ActivationBatchRowDto {

    /**
     * 批次ID。
     */
    private Long id;

    /**
     * 批次号。
     */
    private String batchNo;

    /**
     * 模板ID。
     */
    private Long templateId;

    /**
     * 模板名称。
     */
    private String templateName;

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
    private String status;

    /**
     * 生成数量。
     */
    private Integer generateCount;

    /**
     * 成功使用次数。
     */
    private Integer successUseCount;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 创建时间。
     */
    private Date createTime;
}
