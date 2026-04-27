package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改知识库请求对象
 */
@Data
@Schema(description = "修改知识库请求对象")
public class KnowledgeBaseUpdateRequest {

    @Schema(description = "主键ID", example = "1")
    @NotNull(message = "主键ID不能为空")
    @Min(value = 1L, message = "主键ID必须大于0")
    @Max(value = Long.MAX_VALUE, message = "主键ID过大")
    private Long id;

    @Schema(description = "知识库展示名称", example = "常见用药知识库")
    private String displayName;

    @Schema(description = "知识库封面，可为空", example = "https://example.com/kb-cover.png")
    private String cover;

    @Schema(description = "知识库描述", example = "覆盖常见用药相关问答内容")
    private String description;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    @Min(value = 0L, message = "状态值不合法")
    @Max(value = 1L, message = "状态值不合法")
    private Integer status;

}
