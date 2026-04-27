package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/4
 */
@Data
@Schema(description = "知识库导入请求参数")
public class KnowledgeBaseImportRequest {

    @NotNull(message = "知识库ID不能为空")
    @Min(value = 1, message = "知识库ID必须大于0")
    @Schema(description = "知识库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long knowledgeBaseId;

    @NotEmpty(message = "导入文件不能为空")
    @Schema(description = "导入文件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull(message = "文件详情不能为空") @Valid FileDetail> fileDetails;

    @NotBlank(message = "切片模式不能为空")
    @Schema(description = "切片模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "balancedMode")
    private String chunkMode;

    @Valid
    @Schema(description = "自定义切片模式参数，chunkMode=custom 时使用", requiredMode = Schema.RequiredMode.AUTO)
    private CustomChunkMode customChunkMode;

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "自定义切片模式参数")
    public static class CustomChunkMode {
        @Schema(description = "切片大小，custom 模式必填", requiredMode = Schema.RequiredMode.AUTO, example = "500")
        private Integer chunkSize;

        @Schema(description = "切片重叠大小，custom 模式必填", requiredMode = Schema.RequiredMode.AUTO, example = "50")
        private Integer chunkOverlap;
    }


    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "文件详情")
    public static class FileDetail {

        @Schema(description = "文件类型，优先使用扩展名（如 pdf、docx）", requiredMode = Schema.RequiredMode.AUTO,
                example = "pdf")
        private String fileType;

        /**
         * 文件名
         */
        @NotBlank(message = "文件名字不能为空")
        @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "file.pdf")
        private String fileName;

        /**
         * 文件地址
         */
        @NotBlank(message = "文件地址不能为空")
        @Schema(description = "文件地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/file.pdf")
        private String fileUrl;
    }
}
