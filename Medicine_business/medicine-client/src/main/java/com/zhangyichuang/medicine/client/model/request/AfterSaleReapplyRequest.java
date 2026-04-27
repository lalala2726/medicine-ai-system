package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.model.enums.AfterSaleReasonEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 售后重新申请请求
 */
@Data
@Schema(description = "售后重新申请请求")
public class AfterSaleReapplyRequest {

    @NotBlank(message = "售后单号不能为空")
    @Schema(description = "售后单号", example = "AS202501011200000001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String afterSaleNo;

    @NotNull(message = "申请原因不能为空")
    @Schema(description = "申请原因", example = "DAMAGED", requiredMode = Schema.RequiredMode.REQUIRED)
    private AfterSaleReasonEnum applyReason;

    @Schema(description = "详细说明", example = "商品仍存在问题，需要重新售后")
    private String applyDescription;

    @NotEmpty(message = "凭证图片不能为空")
    @Schema(description = "凭证图片URL列表", example = "[\"https://img.example.com/a.png\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> evidenceImages;
}
