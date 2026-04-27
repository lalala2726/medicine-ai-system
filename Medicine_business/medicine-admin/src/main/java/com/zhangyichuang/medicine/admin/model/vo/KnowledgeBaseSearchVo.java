package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 知识库结构化检索响应视图对象。
 */
@Data
@Schema(description = "知识库结构化检索响应视图对象")
public class KnowledgeBaseSearchVo {

    @Schema(description = "命中结果列表")
    private List<KnowledgeBaseSearchHitVo> hits;
}
