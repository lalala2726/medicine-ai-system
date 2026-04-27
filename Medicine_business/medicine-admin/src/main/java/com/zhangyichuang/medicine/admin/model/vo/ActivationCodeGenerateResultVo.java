package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 激活码生成结果视图对象。
 */
@Data
@Builder
@Schema(description = "激活码生成结果")
public class ActivationCodeGenerateResultVo {

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
     * 本次生成数量。
     */
    @Schema(description = "本次生成数量", example = "20")
    private Integer generatedCount;

    /**
     * 激活码列表。
     */
    @Schema(description = "激活码列表")
    private List<ActivationCodeGeneratedItemVo> codes;
}
