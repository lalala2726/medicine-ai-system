package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 大模型提供商状态更新请求。
 */
@Data
@Schema(description = "大模型提供商状态更新请求")
public class LlmProviderUpdateStatusRequest {

    @NotNull(message = "提供商ID不能为空")
    @Positive(message = "提供商ID必须大于0")
    @Schema(description = "提供商ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(value = 0L, message = "状态值不合法")
    @Max(value = 1L, message = "状态值不合法")
    @Schema(description = "状态（1启用 0停用）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
