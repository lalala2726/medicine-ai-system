package com.zhangyichuang.medicine.model.cache;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 知识库 Agent 配置。
 */
@Data
public class KnowledgeBaseAgentConfig implements Serializable {

    /**
     * 是否启用知识库能力。
     */
    private Boolean enabled;

    /**
     * Agent 可访问的知识库名称列表。
     */
    private List<String> knowledgeNames;

    /**
     * 向量维度
     */
    private Integer embeddingDim;

    /**
     * 默认检索返回条数。
     */
    private Integer topK;

    /**
     * 向量模型名称。
     */
    private String embeddingModel;

    /**
     * 是否启用排序。
     */
    @JsonAlias("rerankEnabled")
    private Boolean rankingEnabled;

    /**
     * 排序模型名称。
     */
    @JsonAlias("rerankModel")
    private String rankingModel;
}
