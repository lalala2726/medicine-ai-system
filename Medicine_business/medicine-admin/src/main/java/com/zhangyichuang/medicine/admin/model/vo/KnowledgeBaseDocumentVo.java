package com.zhangyichuang.medicine.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/5
 */
@Data
@Schema(description = "知识库文档")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KnowledgeBaseDocumentVo {

    @Schema(description = "文档ID", example = "1")
    private Long id;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "文件名", example = "1.pdf")
    private String fileName;

    @Schema(description = "文件 URL", example = "https://example.com/file.pdf")
    private String fileUrl;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "文件大小，单位 Bytes，1 表示 1 个字节", example = "1024")
    private Long fileSize;

    @Schema(description = "切片模式", example = "custom")
    private String chunkMode;

    @Schema(description = "切片长度", example = "500")
    private Integer chunkSize;

    @Schema(description = "切片重叠长度", example = "100")
    private Integer chunkOverlap;

    @Schema(description = "切片数量", example = "12")
    private Long chunkCount;

    @Schema(description = "索引阶段，取值见 KbDocumentStageEnum", example = "PENDING")
    private String stage;

    @Schema(description = "最近一次处理失败错误信息", example = "向量化失败")
    private String lastError;

    @Schema(description = "创建人账号", example = "admin")
    private String createBy;

    @Schema(description = "最后更新人账号", example = "admin")
    private String updateBy;

    @Schema(description = "创建时间", example = "2025-12-05 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @Schema(description = "更新时间", example = "2025-12-05 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

}
