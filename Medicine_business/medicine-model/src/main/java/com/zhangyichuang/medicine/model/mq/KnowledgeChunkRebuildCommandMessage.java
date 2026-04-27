package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单切片重建 command 消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkRebuildCommandMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 knowledge_chunk_rebuild_command。
     */
    private String message_type;

    /**
     * 单次切片重建任务唯一标识。
     */
    private String task_uuid;

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
     * 同一 vector_id 下递增的版本号。
     */
    private Long version;

    /**
     * 修改后的切片内容。
     */
    private String content;

    /**
     * 向量模型名称。
     */
    private String embedding_model;

    /**
     * 消息创建时间，推荐使用 UTC ISO-8601。
     */
    private String created_at;
}
