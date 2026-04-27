package com.zhangyichuang.medicine.model.coupon;

import lombok.Data;

import java.util.Date;

/**
 * 激活码兑换日志查询结果。
 */
@Data
public class ActivationRedeemLogRowDto {

    /**
     * 日志ID。
     */
    private Long id;

    /**
     * 兑换请求ID。
     */
    private String requestId;

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
     * 激活码明文。
     */
    private String plainCodeSnapshot;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 结果状态。
     */
    private String resultStatus;

    /**
     * 失败编码。
     */
    private String failCode;

    /**
     * 失败信息。
     */
    private String failMessage;

    /**
     * 客户端IP。
     */
    private String clientIp;

    /**
     * 发券方式。
     */
    private String grantMode;

    /**
     * 发券状态。
     */
    private String grantStatus;

    /**
     * 创建时间。
     */
    private Date createTime;
}
