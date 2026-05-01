package com.zhangyichuang.medicine.admin.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 激活码兑换日志视图对象。
 */
@Data
@Builder
@Schema(description = "激活码兑换日志")
public class ActivationLogVo {

    /**
     * 日志ID。
     */
    @Schema(description = "日志ID", example = "10001")
    private Long id;

    /**
     * 兑换请求ID。
     */
    @Schema(description = "兑换请求ID", example = "6fdd12a72a6d4e95b6f3b9fb51a9937d")
    private String requestId;

    /**
     * 批次ID。
     */
    @Schema(description = "批次ID", example = "10001")
    private Long batchId;

    /**
     * 激活码ID。
     */
    @Schema(description = "激活码ID", example = "10001")
    private Long activationCodeId;

    /**
     * 批次号。
     */
    @Schema(description = "批次号", example = "ACT202604091200000001")
    private String batchNo;

    /**
     * 优惠券模板ID。
     */
    @Schema(description = "优惠券模板ID", example = "1")
    private Long templateId;

    /**
     * 优惠券模板名称。
     */
    @Schema(description = "优惠券模板名称", example = "新人100元券")
    private String templateName;

    /**
     * 兑换规则类型。
     */
    @Schema(description = "兑换规则类型", example = "SHARED_PER_USER_ONCE")
    private String redeemRuleType;

    /**
     * 激活码列表展示值。
     */
    @DataMasking(type = MaskingType.CUSTOM, prefixKeep = 4, suffixKeep = 4, preserveLength = false, maskLength = 4)
    @Schema(description = "激活码（日志返回时自动脱敏）", example = "ABCD****5678")
    private String plainCodeSnapshot;

    /**
     * 结果状态。
     */
    @Schema(description = "结果状态", example = "SUCCESS")
    private String resultStatus;

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 用户名。
     */
    @Schema(description = "用户名", example = "zhangsan")
    private String userName;

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "20001")
    private Long couponId;

    /**
     * 失败编码。
     */
    @Schema(description = "失败编码", example = "ACTIVATION_CODE_EXPIRED")
    private String failCode;

    /**
     * 失败原因。
     */
    @Schema(description = "失败原因", example = "激活码已过期")
    private String failMessage;

    /**
     * 客户端IP。
     */
    @Schema(description = "客户端IP", example = "127.0.0.1")
    private String clientIp;

    /**
     * 发券方式。
     */
    @Schema(description = "发券方式", example = "COUPON_GRANT_CORE")
    private String grantMode;

    /**
     * 发券状态。
     */
    @Schema(description = "发券状态", example = "SUCCESS")
    private String grantStatus;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
