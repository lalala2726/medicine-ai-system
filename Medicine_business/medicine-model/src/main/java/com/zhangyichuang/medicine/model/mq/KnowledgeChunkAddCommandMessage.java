package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档切片新增 command 消息体。
 * <p>
 * 字段定义遵循《knowledge-chunk-add-mq-integration.md》协议，
 * 采用 snake_case，避免跨语言对接时字段映射歧义。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkAddCommandMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 knowledge_chunk_add_command。
     */
    private String message_type;

    /**
     * 单次切片新增任务唯一标识。
     */
    private String task_uuid;

    /**
     * 本地占位切片 ID。
     */
    private Long chunk_id;

    /**
     * 知识库业务名称。
     */
    private String knowledge_name;

    /**
     * 文档主键 ID。
     */
    private Long document_id;

    /**
     * 切片内容。
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
