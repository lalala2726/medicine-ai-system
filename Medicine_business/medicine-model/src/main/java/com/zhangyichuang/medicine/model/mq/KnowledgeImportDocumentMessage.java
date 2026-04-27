package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库导入文档消息体。
 * <p>
 * 字段定义遵循《knowledge-import-mq-integration.md》协议，
 * 采用 snake_case，避免跨语言对接时字段映射歧义。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeImportDocumentMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，协议值固定为 knowledge_import_command。
     */
    private String message_type;

    /**
     * 单次导入任务唯一标识（一次请求一个 task_uuid）。
     */
    private String task_uuid;

    /**
     * 业务唯一键，建议格式：knowledge_name:document_id。
     */
    private String biz_key;

    /**
     * 同一 biz_key 下的递增版本号，用于新旧判定（只让最新版本生效）。
     */
    private Long version;

    /**
     * 知识库业务名称。
     */
    private String knowledge_name;

    /**
     * 文档主键 ID（来自业务库 kb_document.id）。
     */
    private Long document_id;

    /**
     * 待导入文件的访问地址。
     */
    private String file_url;

    /**
     * 向量模型名称（从知识库配置读取）。
     */
    private String embedding_model;

    /**
     * 切片大小（单位：字符）。
     */
    private Integer chunk_size;

    /**
     * 切片重叠大小（单位：字符）。
     */
    private Integer chunk_overlap;

    /**
     * 消息创建时间（ISO8601，推荐 UTC）。
     */
    private String created_at;
}
