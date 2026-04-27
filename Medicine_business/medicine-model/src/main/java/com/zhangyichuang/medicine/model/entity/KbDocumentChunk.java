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
 * 知识库文档切片表
 */
@TableName(value = "kb_document_chunk")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KbDocumentChunk {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 切片序号（按上游返回值存储，当前接口从1开始）
     */
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 向量库记录ID（Milvus主键）
     */
    private String vectorId;

    /**
     * 切片字符数
     */
    private Integer charCount;

    /**
     * 状态：0启用，1禁用（禁用后不参与向量检索）
     */
    private Integer status;

    /**
     * 切片阶段，取值见 com.zhangyichuang.medicine.model.enums.KbDocumentChunkStageEnum
     */
    private String stage;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
