package com.zhangyichuang.medicine.model.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库导入 result 消息体。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeImportResultMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型（例如：knowledge_import_result）。
     */
    private String message_type;

    /**
     * 单次导入任务唯一标识。
     */
    private String task_uuid;

    /**
     * 业务唯一键，建议格式：knowledge_name:document_id。
     */
    private String biz_key;

    /**
     * 同一 biz_key 下的递增版本号，用于新旧判定。
     */
    private Long version;

    /**
     * 当前阶段，AI 回传通常为 STARTED/PROCESSING/COMPLETED/FAILED。
     */
    private String stage;

    /**
     * 阶段消息或错误信息。
     */
    private String message;

    /**
     * 知识库业务名称。
     */
    private String knowledge_name;

    /**
     * 文档主键 ID（来自业务库 kb_document.id）。
     */
    private Long document_id;

    /**
     * 文档文件访问地址。
     */
    private String file_url;

    /**
     * 文件类型。
     */
    private String file_type;

    /**
     * 文件大小，单位 Bytes，1 表示 1 个字节
     */
    private Long file_size;

    /**
     * 切片数量统计（可选）。
     */
    private Integer chunk_count;

    /**
     * 向量数量统计（可选）。
     */
    private Integer vector_count;

    /**
     * 向量模型名称。
     */
    private String embedding_model;

    /**
     * 向量维度。
     */
    private Integer embedding_dim;

    /**
     * 事件发生时间（ISO8601）。
     */
    private String occurred_at;

    /**
     * 处理耗时（毫秒）。
     */
    private Long duration_ms;
}
