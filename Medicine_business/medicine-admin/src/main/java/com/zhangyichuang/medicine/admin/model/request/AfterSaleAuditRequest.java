package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 管理员审核售后请求
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Schema(description = "审核售后请求")
public class AfterSaleAuditRequest {

    @NotNull(message = "售后申请ID不能为空")
    @Positive(message = "售后申请ID必须为正数")
    @Schema(description = "售后申请ID", example = "1")
    private Long afterSaleId;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果(true-通过, false-拒绝)", example = "true")
    private Boolean approved;

    @Schema(description = "拒绝原因(审核拒绝时必填)", example = "提供的凭证不足")
    private String rejectReason;

    @Schema(description = "管理员备注", example = "已核实，同意退款")
    private String adminRemark;
}

