package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新增单个提供商模型请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "新增单个提供商模型请求")
public class LlmProviderModelCreateRequest extends LlmProviderModelItemRequest {

    @Schema(description = "提供商ID", example = "1")
    @NotNull(message = "提供商ID不能为空")
    @Positive(message = "提供商ID必须大于0")
    private Long providerId;
}
