package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库结构化检索命中结果视图对象。
 */
@Data
@Schema(description = "知识库结构化检索命中结果视图对象")
public class KnowledgeBaseSearchHitVo {

    @Schema(description = "命中结果所在的知识库名称", example = "common_medicine_kb")
    private String knowledgeName;

    @Schema(description = "命中结果所在的知识库展示名称", example = "常见用药知识库")
    private String knowledgeDisplayName;

    @Schema(description = "命中的相似度分数", example = "0.9132")
    private Double score;

    @Schema(description = "命中的业务文档ID", example = "1001")
    private Long documentId;

    @Schema(description = "命中的切片序号", example = "3")
    private Integer chunkIndex;

    @Schema(description = "命中的切片字符数", example = "512")
    private Integer charCount;

    @Schema(description = "命中的知识文本内容", example = "服用前请仔细阅读说明书，并严格遵循医嘱。")
    private String content;
}
