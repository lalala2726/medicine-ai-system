package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.KbDocumentChunkHistoryMapper;
import com.zhangyichuang.medicine.admin.service.KbDocumentChunkHistoryService;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunkHistory;
import org.springframework.stereotype.Service;

/**
 * 知识库文档切片历史服务实现。
 *
 * @author Chuang
 * @deprecated 该实现仅服务于已废弃的 kb_document_chunk_history 表，后续会删除。
 */
@Deprecated(since = "1.0-beta", forRemoval = true)
@Service
public class KbDocumentChunkHistoryServiceImpl extends ServiceImpl<KbDocumentChunkHistoryMapper, KbDocumentChunkHistory>
        implements KbDocumentChunkHistoryService {

}




