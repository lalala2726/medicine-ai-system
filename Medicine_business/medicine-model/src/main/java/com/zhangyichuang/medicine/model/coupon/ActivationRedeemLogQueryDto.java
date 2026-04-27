package com.zhangyichuang.medicine.model.coupon;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 激活码兑换日志查询参数。
 */
@Data
@Builder
public class ActivationRedeemLogQueryDto {

    /**
     * 批次ID。
     */
    private Long batchId;

    /**
     * 激活码ID。
     */
    private Long activationCodeId;

    /**
     * 批次号。
     */
    private String batchNo;

    /**
     * 激活码关键字。
     */
    private String plainCode;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 结果状态。
     */
    private String resultStatus;

    /**
     * 创建开始时间。
     */
    private Date startTime;

    /**
     * 创建结束时间。
     */
    private Date endTime;
}
