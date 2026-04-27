package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelCreateRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelItemRequest;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderModelUpdateRequest;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmProviderModelServiceImplTests {

    @Mock
    private LlmProviderModelMapper llmProviderModelMapper;

    @Mock
    private AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    @Spy
    @InjectMocks
    private LlmProviderModelServiceImpl llmProviderModelService;

    @Test
    void createProviderModel_ShouldInsertNormalizedModelWithoutTriggeringSync() {
        when(llmProviderModelMapper.selectCount(any())).thenReturn(0L);
        when(llmProviderModelMapper.insert(any(LlmProviderModel.class))).thenReturn(1);

        LlmProviderModelCreateRequest request = new LlmProviderModelCreateRequest();
        request.setProviderId(1L);
        request.setModelName("gpt-4.1");
        request.setModelType("chat");

        boolean result = llmProviderModelService.createProviderModel(buildProvider(), request);

        assertTrue(result);
        ArgumentCaptor<LlmProviderModel> modelCaptor = ArgumentCaptor.forClass(LlmProviderModel.class);
        verify(llmProviderModelMapper).insert(modelCaptor.capture());
        assertEquals("gpt-4.1", modelCaptor.getValue().getModelName());
        assertEquals("CHAT", modelCaptor.getValue().getModelType());
        verifyNoInteractions(agentConfigRuntimeSyncService);
    }

    @Test
    void updateProviderModel_ShouldUpdateNormalizedModelAndTriggerSync() {
        when(llmProviderModelMapper.selectById(1L)).thenReturn(LlmProviderModel.builder()
                .id(1L)
                .providerId(1L)
                .modelName("old-chat")
                .modelType("CHAT")
                .supportReasoning(0)
                .supportVision(1)
                .enabled(0)
                .sort(10)
                .description("old")
                .build());
        when(llmProviderModelMapper.selectCount(any())).thenReturn(0L);
        when(llmProviderModelMapper.updateById(any(LlmProviderModel.class))).thenReturn(1);

        LlmProviderModelUpdateRequest request = new LlmProviderModelUpdateRequest();
        request.setId(1L);
        request.setProviderId(1L);
        request.setModelName("new-chat");
        request.setModelType("CHAT");
        request.setSupportReasoning(1);

        boolean result = llmProviderModelService.updateProviderModel(buildProvider(), request);

        assertTrue(result);
        verify(agentConfigRuntimeSyncService).syncAfterModelUpdate(any(LlmProviderModel.class), any(LlmProviderModel.class), anyString());
    }

    @Test
    void deleteProviderModel_ShouldDeleteByIdAndTriggerSync() {
        when(llmProviderModelMapper.selectById(1L)).thenReturn(LlmProviderModel.builder()
                .id(1L)
                .providerId(1L)
                .modelName("gpt-4.1")
                .modelType("CHAT")
                .build());
        when(llmProviderModelMapper.deleteById(1L)).thenReturn(1);

        boolean result = llmProviderModelService.deleteProviderModel(1L);

        assertTrue(result);
        verify(agentConfigRuntimeSyncService).syncAfterModelDelete(any(LlmProviderModel.class), anyString());
    }

    @Test
    void saveProviderModels_ShouldBatchInsertAllModels() {
        doReturn(true).when(llmProviderModelService).saveBatch(anyCollection());

        boolean result = llmProviderModelService.saveProviderModels(buildProvider(),
                List.of(buildRequestModel("gpt-4.1", "CHAT"), buildRequestModel("text-embedding-3-small", "EMBEDDING")),
                "tester");

        assertTrue(result);
        ArgumentCaptor<Collection<LlmProviderModel>> modelCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(llmProviderModelService).saveBatch(modelCaptor.capture());
        assertEquals(2, modelCaptor.getValue().size());
        verifyNoInteractions(agentConfigRuntimeSyncService);
    }

    @Test
    void saveProviderModels_WhenDuplicateCodeAndType_ShouldThrowParamException() {
        ParamException exception = assertThrows(ParamException.class,
                () -> llmProviderModelService.saveProviderModels(buildProvider(),
                        List.of(buildRequestModel("dup-model", "CHAT"), buildRequestModel("dup-model", "CHAT")),
                        "tester"));

        assertEquals("模型名称和模型类型不能重复", exception.getMessage());
    }

    private LlmProvider buildProvider() {
        return LlmProvider.builder()
                .id(1L)
                .build();
    }

    private LlmProviderModelItemRequest buildRequestModel(String modelName, String modelType) {
        LlmProviderModelItemRequest request = new LlmProviderModelItemRequest();
        request.setModelName(modelName);
        request.setModelType(modelType);
        return request;
    }
}
