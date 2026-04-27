package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 激活码兑换日志列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "激活码兑换日志列表查询请求")
public class ActivationLogListRequest extends PageRequest {

    /**
     * 批次ID。
     */
    @Schema(description = "批次ID", example = "10001")
    private Long batchId;

    /**
     * 激活码ID。
     */
    @Schema(description = "激活码ID", example = "10002")
    private Long activationCodeId;

    /**
     * 批次号。
     */
    @Schema(description = "批次号", example = "ACT202604091200000001")
    private String batchNo;

    /**
     * 激活码关键字。
     */
    @Schema(description = "激活码关键字", example = "ABCD1234")
    private String plainCode;

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
