package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseDocumentDto;
import com.zhangyichuang.medicine.admin.model.request.DocumentDeleteRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentUpdateFileNameRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseImportRequest;
import com.zhangyichuang.medicine.model.entity.KbDocument;
import com.zhangyichuang.medicine.model.mq.KnowledgeImportResultMessage;

/**
 * @author Chuang
 */
public interface KbDocumentService extends IService<KbDocument> {

    /**
     * 分页查询文档列表。
     *
     * @param knowledgeBaseId 知识库ID
     * @param request         查询参数
     * @return 分页结果
     */
    Page<KnowledgeBaseDocumentDto> listDocument(Long knowledgeBaseId, DocumentListRequest request);

    /**
     * 根据ID查询文档详情（含切片数量）。
     *
     * @param id 文档ID
     * @return 文档详情
     */
    KnowledgeBaseDocumentDto getDocumentDetailById(Long id);

    /**
     * 根据ID查询文档详情。
     *
     * @param id 文档ID
     * @return 文档详情
     */
    KbDocument getDocumentById(Long id);

    /**
     * 修改文档文件名。
     *
     * @param request 修改请求
     * @return 修改结果
     */
    boolean updateDocumentFileName(DocumentUpdateFileNameRequest request);

    /**
     * 删除文档及其关联切片。
     *
     * @param id 文档ID
     * @return 删除结果
     */
    boolean deleteDocument(Long id);

    /**
     * 批量删除文档及其关联切片。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    boolean deleteDocuments(DocumentDeleteRequest request);

    /**
     * 发起文档导入任务。
     *
     * @param request 导入请求
     */
    void importDocument(KnowledgeBaseImportRequest request);

    /**
     * 处理知识库导入结果消息。
     *
     * @param message 结果消息
     */
    void handleImportResult(KnowledgeImportResultMessage message);

    /**
     * 处理切片同步消息。
     *
     * @param message 切片同步消息
     */
    void handleChunkUpdateResult(KnowledgeImportResultMessage message);

}
