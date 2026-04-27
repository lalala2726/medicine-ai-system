package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档切片新增 result 消息体。
 * <p>
 * 字段定义遵循《knowledge-chunk-add-mq-integration.md》协议，
 * 采用 snake_case，避免跨语言对接时字段映射歧义。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkAddResultMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 knowledge_chunk_add_result。
     */
    private String message_type;

    /**
     * 对应 command 的任务 ID。
     */
    private String task_uuid;

    /**
     * 本地占位切片 ID。
     */
    private Long chunk_id;

    /**
     * 当前处理阶段，取值见 com.zhangyichuang.medicine.model.enums.KnowledgeChunkTaskStageEnum。
     */
    private String stage;

    /**
     * 当前阶段说明或失败原因。
     */
    private String message;

    /**
     * 知识库业务名称。
     */
    private String knowledge_name;

    /**
     * 文档主键 ID。
     */
    private Long document_id;

    /**
     * 新增后生成的向量记录主键 ID。
     */
    private Long vector_id;

    /**
     * AI 侧确定的最终切片序号。
     */
    private Integer chunk_index;

    /**
     * 实际执行的向量模型名称。
     */
    private String embedding_model;

    /**
     * 向量维度。
     */
    private Integer embedding_dim;

    /**
     * 结果事件发生时间。
     */
    private String occurred_at;

    /**
     * 从 AI 接收任务到当前结果事件的耗时，单位毫秒。
     */
    private Long duration_ms;
}
