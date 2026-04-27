package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档切片内容更新请求。
 */
@Data
@Schema(description = "文档切片内容更新请求")
public class DocumentChunkUpdateContentRequest {

    @NotNull(message = "切片ID不能为空")
    @Schema(description = "切片ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "切片内容不能为空")
    @Schema(description = "修改后的切片内容", example = "这是更新后的切片内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
