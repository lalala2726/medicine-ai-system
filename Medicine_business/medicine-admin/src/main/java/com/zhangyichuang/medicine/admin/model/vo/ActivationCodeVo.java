package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 激活码批次视图对象。
 */
@Data
@Builder
@Schema(description = "激活码批次")
public class ActivationCodeVo {

    /**
     * 激活码批次ID。
     */
    @Schema(description = "激活码批次ID", example = "10001")
    private Long id;

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
     * 有效期类型。
     */
    @Schema(description = "有效期类型", example = "ONCE")
    private String validityType;

    /**
     * 固定生效时间。
     */
    @Schema(description = "固定生效时间")
    private Date fixedEffectiveTime;

    /**
     * 固定失效时间。
     */
    @Schema(description = "固定失效时间")
    private Date fixedExpireTime;

    /**
     * 激活后有效天数。
     */
    @Schema(description = "激活后有效天数", example = "30")
    private Integer relativeValidDays;

    /**
     * 批次状态。
     */
    @Schema(description = "批次状态", example = "ACTIVE")
    private String status;

    /**
     * 生成数量。
     */
    @Schema(description = "生成数量", example = "20")
    private Integer generateCount;

    /**
     * 成功使用次数。
     */
    @Schema(description = "成功使用次数", example = "5")
    private Integer successUseCount;

    /**
     * 备注。
     */
    @Schema(description = "备注", example = "四月活动激活码")
    private String remark;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 创建人。
     */
    @Schema(description = "创建人", example = "admin")
    private String createBy;
}
