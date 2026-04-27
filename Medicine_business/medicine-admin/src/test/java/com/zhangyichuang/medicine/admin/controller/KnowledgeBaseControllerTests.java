package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseListDto;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseAddRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseListRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.KnowledgeBaseListVo;
import com.zhangyichuang.medicine.admin.service.KbBaseService;
import com.zhangyichuang.medicine.model.entity.KbBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerTests {

    @Mock
    private KbBaseService kbBaseService;

    @InjectMocks
    private KnowledgeBaseController knowledgeBaseController;

    @Test
    void listKnowledgeBase_ShouldReturnPagedResult() {
        KnowledgeBaseListRequest request = new KnowledgeBaseListRequest();
        Page<KnowledgeBaseListDto> page = new Page<>(1, 10, 1);
        KnowledgeBaseListDto rowDto = new KnowledgeBaseListDto();
        rowDto.setId(1L);
        rowDto.setKnowledgeName("kb_abc1234");
        rowDto.setCover("https://example.com/kb-cover.png");
        KnowledgeBaseListDto.Detail detail = new KnowledgeBaseListDto.Detail();
        detail.setChunkCount(10L);
        detail.setFileCount(5L);
        rowDto.setDetail(detail);
        page.setRecords(java.util.List.of(rowDto));
        when(kbBaseService.listKnowledgeBase(request)).thenReturn(page);

        var result = knowledgeBaseController.listKnowledgeBase(request);

        assertEquals(200, result.getCode());
        KnowledgeBaseListVo row = (KnowledgeBaseListVo) result.getData().getRows().get(0);
        assertEquals("https://example.com/kb-cover.png", row.getCover());
        assertEquals(10L, row.getDetail().getChunkCount());
        assertEquals(5L, row.getDetail().getFileCount());
        verify(kbBaseService).listKnowledgeBase(request);
    }

    @Test
    void getKnowledgeBaseById_ShouldReturnDetail() {
        KbBase kbBase = new KbBase();
        kbBase.setId(1L);
        kbBase.setKnowledgeName("kb_abc1234");
        kbBase.setCover("https://example.com/kb-cover.png");
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(kbBase);

        var result = knowledgeBaseController.getKnowledgeBaseById(1L);

        assertEquals(200, result.getCode());
        assertEquals("kb_abc1234", result.getData().getKnowledgeName());
        assertEquals("https://example.com/kb-cover.png", result.getData().getCover());
        verify(kbBaseService).getKnowledgeBaseById(1L);
    }

    @Test
    void addKnowledgeBase_ShouldDelegateToService() {
        KnowledgeBaseAddRequest request = new KnowledgeBaseAddRequest();
        request.setKnowledgeName("drug_faq");
        request.setDisplayName("常见用药知识库");
        request.setCover("https://example.com/kb-cover.png");
        request.setEmbeddingModel("text-embedding-3-large");
        request.setEmbeddingDim(1024);
        when(kbBaseService.addKnowledgeBase(request)).thenReturn(true);

        var result = knowledgeBaseController.addKnowledgeBase(request);

        assertEquals(200, result.getCode());
        verify(kbBaseService).addKnowledgeBase(request);
    }

    @Test
    void updateKnowledgeBase_ShouldDelegateToService() {
        KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
        request.setId(1L);
        request.setDisplayName("更新后的知识库");
        request.setCover("https://example.com/kb-cover.png");
        request.setStatus(1);
        when(kbBaseService.updateKnowledgeBase(request)).thenReturn(true);

        var result = knowledgeBaseController.updateKnowledgeBase(request);

        assertEquals(200, result.getCode());
        verify(kbBaseService).updateKnowledgeBase(request);
    }

    @Test
    void deleteKnowledgeBase_ShouldDelegateToService() {
        when(kbBaseService.deleteKnowledgeBase(1L)).thenReturn(true);

        var result = knowledgeBaseController.deleteKnowledgeBase(1L);

        assertEquals(200, result.getCode());
        verify(kbBaseService).deleteKnowledgeBase(1L);
    }
}
