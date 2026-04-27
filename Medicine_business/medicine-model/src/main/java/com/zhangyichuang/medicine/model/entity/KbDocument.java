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
 * 知识库文档元数据表
 */
@TableName(value = "kb_document")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KbDocument {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小，单位 Bytes，1 表示 1 个字节
     */
    private Long fileSize;

    /**
     * 切片模式
     */
    private String chunkMode;

    /**
     * 切片长度
     */
    private Integer chunkSize;

    /**
     * 切片重叠长度
     */
    private Integer chunkOverlap;

    /**
     * 索引阶段，取值见 @see com.zhangyichuang.medicine.model.enums.KbDocumentStageEnum
     */
    private String stage;

    /**
     * 最近一次处理失败错误信息
     */
    private String lastError;

    /**
     * 创建人账号
     */
    private String createBy;

    /**
     * 最后更新人账号
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;
}
