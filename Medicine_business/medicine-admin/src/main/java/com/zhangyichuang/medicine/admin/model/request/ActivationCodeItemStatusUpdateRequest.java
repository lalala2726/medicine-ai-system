package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 激活码单码状态更新请求。
 */
@Data
@Schema(description = "激活码单码状态更新请求")
public class ActivationCodeItemStatusUpdateRequest {

    /**
     * 激活码ID。
     */
    @NotNull(message = "激活码ID不能为空")
    @Schema(description = "激活码ID", example = "10001")
    private Long id;

    /**
     * 激活码状态。
     */
    @NotBlank(message = "激活码状态不能为空")
    @Schema(description = "激活码状态（ACTIVE-启用，DISABLED-停用）", example = "DISABLED")
    private String status;
}
