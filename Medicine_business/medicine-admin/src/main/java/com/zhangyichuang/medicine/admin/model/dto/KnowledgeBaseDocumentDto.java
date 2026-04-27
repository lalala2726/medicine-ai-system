package com.zhangyichuang.medicine.admin.model.dto;

import com.zhangyichuang.medicine.model.entity.KbDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chuang
 * <p>
 * created on 2026/3/8 06:26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeBaseDocumentDto extends KbDocument {

    @Schema(description = "切片数量")
    private Long chunkCount;
}
