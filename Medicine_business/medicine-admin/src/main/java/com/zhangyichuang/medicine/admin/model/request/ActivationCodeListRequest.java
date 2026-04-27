package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 激活码批次列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "激活码批次列表查询请求")
public class ActivationCodeListRequest extends PageRequest {

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
     * 激活码状态。
     */
    @Schema(description = "激活码状态", example = "ACTIVE")
    private String status;

    /**
     * 创建开始时间。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "创建开始时间")
    private Date startTime;

    /**
     * 创建结束时间。
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "创建结束时间")
    private Date endTime;
}
