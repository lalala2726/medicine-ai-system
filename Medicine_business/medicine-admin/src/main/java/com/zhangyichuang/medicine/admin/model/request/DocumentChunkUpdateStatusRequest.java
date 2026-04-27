package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档切片状态更新请求。
 */
@Data
@Schema(description = "文档切片状态更新请求")
public class DocumentChunkUpdateStatusRequest {

    @NotNull(message = "切片ID不能为空")
    @Schema(description = "切片ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "切片状态不能为空")
    @Min(value = 0, message = "切片状态只允许为0或1")
    @Max(value = 1, message = "切片状态只允许为0或1")
    @Schema(description = "切片状态：0启用，1禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
