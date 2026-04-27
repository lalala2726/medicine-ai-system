package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档切片新增请求。
 */
@Data
@Schema(description = "文档切片新增请求")
public class DocumentChunkAddRequest {

    @NotNull(message = "文档ID不能为空")
    @Schema(description = "文档ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long documentId;

    @NotBlank(message = "切片内容不能为空")
    @Schema(description = "切片内容", example = "这是需要手工补充的新知识切片", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
