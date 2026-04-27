package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文档文件名更新请求。
 */
@Data
@Schema(description = "文档文件名更新请求")
public class DocumentUpdateFileNameRequest {

    @NotNull(message = "文档ID不能为空")
    @Schema(description = "文档ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "修改后的文件名", example = "更新后的文件名.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
}
