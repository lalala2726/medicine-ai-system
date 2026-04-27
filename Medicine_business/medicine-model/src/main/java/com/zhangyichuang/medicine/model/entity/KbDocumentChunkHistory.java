package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 知识库文档切片历史表。
 *
 * @deprecated 该历史表已废弃，后续会随 kb_document_chunk_history 表一起删除。
 */
@Deprecated(since = "1.0-beta", forRemoval = true)
@TableName(value = "kb_document_chunk_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KbDocumentChunkHistory {

    /**
     * 历史记录主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 业务切片ID，对应 knowledge_document_chunk.id
     */
    private Long chunkId;

    /**
     * 知识库名称
     */
    private String knowledgeName;

    /**
     * Milvus 向量主键ID
     */
    private Long vectorId;

    /**
     * 修改前旧内容
     */
    private String oldContent;

    /**
     * 本次编辑对应任务ID
     */
    private String taskId;

    /**
     * 操作人ID，可为空
     */
    private Long operatorId;

    /**
     * 创建时间
     */
    private Date createdAt;
}
