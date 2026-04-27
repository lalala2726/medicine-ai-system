package com.zhangyichuang.medicine.model.coupon;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 激活码批次查询参数。
 */
@Data
@Builder
public class ActivationBatchQueryDto {

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
     * 批次状态。
     */
    private String status;

    /**
     * 创建开始时间。
     */
    private Date startTime;

    /**
     * 创建结束时间。
     */
    private Date endTime;
}
