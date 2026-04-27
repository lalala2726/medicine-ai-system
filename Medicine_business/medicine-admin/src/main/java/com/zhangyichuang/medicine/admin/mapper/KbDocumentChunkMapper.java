package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkListRequest;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunk;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
public interface KbDocumentChunkMapper extends BaseMapper<KbDocumentChunk> {

    /**
     * 分页查询指定文档下的切片列表。
     *
     * @param page       分页参数
     * @param documentId 文档ID
     * @param request    查询参数
     * @return 切片分页结果
     */
    Page<KbDocumentChunk> listDocumentChunk(Page<KbDocumentChunk> page,
                                            @Param("documentId") Long documentId,
                                            @Param("request") DocumentChunkListRequest request);

    /**
     * 查询指定文档当前最大切片序号。
     *
     * @param documentId 文档ID
     * @return 最大切片序号
     */
    Integer selectMaxChunkIndex(@Param("documentId") Long documentId);
}




