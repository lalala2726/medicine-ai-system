package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseDocumentDto;
import com.zhangyichuang.medicine.admin.model.request.DocumentListRequest;
import com.zhangyichuang.medicine.model.entity.KbDocument;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    /**
     * 分页查询知识库文档列表。
     *
     * @param page            分页参数
     * @param knowledgeBaseId 知识库ID
     * @param request         查询条件
     * @return 文档分页结果
     */
    Page<KnowledgeBaseDocumentDto> listDocument(Page<KnowledgeBaseDocumentDto> page,
                                                @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                @Param("request") DocumentListRequest request);

    /**
     * 根据ID查询知识库文档详情。
     *
     * @param id 文档ID
     * @return 文档详情
     */
    KnowledgeBaseDocumentDto getDocumentDetailById(@Param("id") Long id);
}
