package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkAddRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateContentRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateStatusRequest;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunk;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkAddResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkRebuildResultMessage;

import java.util.List;

/**
 * @author Chuang
 */
public interface KbDocumentChunkService extends IService<KbDocumentChunk> {

    /**
     * 分页查询指定文档下的切片。
     *
     * @param documentId 文档ID
     * @param request    查询参数
     * @return 分页结果
     */
    Page<KbDocumentChunk> listDocumentChunk(Long documentId, DocumentChunkListRequest request);

    /**
     * 根据切片ID查询详情。
     *
     * @param id 切片ID
     * @return 切片详情
     */
    KbDocumentChunk getDocumentChunkById(Long id);

    /**
     * 新增手工补充切片并发起异步向量化。
     *
     * @param request 新增请求
     * @return 处理结果
     */
    boolean addDocumentChunk(DocumentChunkAddRequest request);

    /**
     * 修改切片内容并发起异步重建。
     *
     * @param request 更新请求
     * @return 处理结果
     */
    boolean updateDocumentChunkContent(DocumentChunkUpdateContentRequest request);

    /**
     * 修改切片状态。
     *
     * @param request 更新请求
     * @return 处理结果
     */
    boolean updateDocumentChunkStatus(DocumentChunkUpdateStatusRequest request);

    /**
     * 删除切片。
     *
     * @param id 切片ID
     * @return 处理结果
     */
    boolean deleteDocumentChunk(Long id);

    /**
     * 按文档ID替换切片数据（先删后插）。
     *
     * @param documentId 文档ID
     * @param chunks     新切片列表
     */
    void replaceByDocumentId(Long documentId, List<KbDocumentChunk> chunks);

    /**
     * 按文档ID集合批量删除切片。
     *
     * @param documentIds 文档ID集合
     * @return 删除结果
     */
    boolean removeByDocumentIds(List<Long> documentIds);

    /**
     * 处理 AI 回传的单切片重建结果。
     *
     * @param message 结果消息
     */
    void handleChunkRebuildResult(KnowledgeChunkRebuildResultMessage message);

    /**
     * 处理 AI 回传的切片新增结果。
     *
     * @param message 结果消息
     */
    void handleChunkAddResult(KnowledgeChunkAddResultMessage message);

}
