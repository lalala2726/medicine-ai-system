package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 知识库结构化检索请求对象。
 */
@Data
@Schema(description = "知识库结构化检索请求对象")
public class KnowledgeBaseSearchRequest {

    @Schema(description = "检索问题", example = "感冒药的服用禁忌有哪些")
    @NotBlank(message = "检索问题不能为空")
    private String question;

    @Schema(description = "参与检索的知识库名称列表", example = "[\"common_medicine_kb\", \"otc_guide_kb\"]")
    @NotNull(message = "知识库名称列表不能为空")
    @Size(min = 1, max = 5, message = "知识库数量必须在1到5个之间")
    private List<@NotBlank(message = "知识库名称不能为空") String> knowledgeNames;

    @Schema(description = "本次检索使用的重排模型名称，不传则表示不启用重排", example = "gte-rerank-v2")
    private String rankingModel;
}
