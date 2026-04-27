package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 文档切片视图对象。
 */
@Data
@Schema(description = "文档切片")
public class DocumentChunkVo {

    @Schema(description = "切片ID", example = "2001")
    private Long id;

    @Schema(description = "文档ID", example = "1001")
    private Long documentId;

    @Schema(description = "切片序号", example = "1")
    private Integer chunkIndex;

    @Schema(description = "切片内容", example = "这是文档切片内容")
    private String content;

    @Schema(description = "向量ID", example = "900001")
    private String vectorId;

    @Schema(description = "字符数", example = "128")
    private Integer charCount;

    @Schema(description = "切片状态：0启用，1禁用", example = "0")
    private Integer status;

    @Schema(description = "切片阶段，取值见 KbDocumentChunkStageEnum", example = "COMPLETED")
    private String stage;

    @Schema(description = "创建时间", example = "2026-03-06 12:00:00")
    private Date createdAt;

    @Schema(description = "更新时间", example = "2026-03-06 12:05:00")
    private Date updatedAt;
}
