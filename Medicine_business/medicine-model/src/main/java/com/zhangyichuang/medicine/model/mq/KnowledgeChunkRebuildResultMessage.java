package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单切片重建 result 消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkRebuildResultMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 knowledge_chunk_rebuild_result。
     */
    private String message_type;

    /**
     * 对应 command 的任务 ID。
     */
    private String task_uuid;

    /**
     * 对应 command 的版本号。
     */
    private Long version;

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
     * 向量记录主键 ID。
     */
    private Long vector_id;

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
