package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.facade.LlmProviderFacade;
import com.zhangyichuang.medicine.admin.model.dto.LlmPresetProviderTemplateDto;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderDetailDto;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderListDto;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.LlmProviderConnectivityTestVo;
import com.zhangyichuang.medicine.admin.service.LlmProviderModelService;
import com.zhangyichuang.medicine.admin.service.LlmProviderService;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMmProviderControllerTests {

    @Mock
    private LlmProviderService llmProviderService;

    @Mock
    private LlmProviderModelService llmProviderModelService;

    @Mock
    private LlmProviderFacade llmProviderFacade;

    @InjectMocks
    private LLMmProviderController LLMmProviderController;

    @Test
    void listPresetProviders_ShouldReturnOpenAiPreset() {
        when(llmProviderService.listPresetProviders()).thenReturn(List.of(buildPresetTemplate()));

        var result = LLMmProviderController.listPresetProviders();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("openai", result.getData().get(0).getProviderKey());
        verify(llmProviderService).listPresetProviders();
    }

    @Test
    void getPresetProvider_ShouldReturnPresetDetail() {
        when(llmProviderService.getPresetProvider("openai")).thenReturn(buildPresetTemplate());

        var result = LLMmProviderController.getPresetProvider("openai");

        assertEquals(200, result.getCode());
        assertEquals("https://api.openai.com/v1", result.getData().getBaseUrl());
        assertEquals(2, result.getData().getModels().size());
        assertEquals("gpt-4.1", result.getData().getModels().get(0).getModelName());
        assertFalse(hasDeclaredField(result.getData().getModels().get(0).getClass(), "id"));
        assertFalse(hasDeclaredField(result.getData().getModels().get(0).getClass(), "providerId"));
        assertFalse(hasDeclaredField(result.getData().getModels().get(0).getClass(), "createdAt"));
        assertFalse(hasDeclaredField(result.getData().getModels().get(0).getClass(), "updatedAt"));
        verify(llmProviderService).getPresetProvider("openai");
    }

    @Test
    void listProviders_ShouldReturnPagedResult() {
        LlmProviderListRequest request = new LlmProviderListRequest();
        Page<LlmProviderListDto> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(LlmProviderListDto.builder()
                .id(1L)
                .providerName("OpenAI")
                .providerType("openai")
                .modelCount(5)
                .build()));
        when(llmProviderService.listProviders(request)).thenReturn(page);

        var result = LLMmProviderController.listProviders(request);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getRows().size());
        verify(llmProviderService).listProviders(request);
    }

    @Test
    void getProviderDetail_ShouldDelegateToFacade() {
        LlmProviderDetailDto detailDto = LlmProviderDetailDto.builder()
                .id(1L)
                .providerName("OpenAI")
                .providerType("openai")
                .baseUrl("https://api.openai.com/v1")
                .apiKey("sk-detail")
                .models(List.of(buildProviderModel(1L, "gpt-4.1", "CHAT")))
                .build();
        when(llmProviderFacade.getProviderDetail(1L)).thenReturn(detailDto);

        var result = LLMmProviderController.getProviderDetail(1L);

        assertEquals(200, result.getCode());
        assertEquals("OpenAI", result.getData().getProviderName());
        assertEquals("openai", result.getData().getProviderType());
        assertEquals(1, result.getData().getModels().size());
        verify(llmProviderFacade).getProviderDetail(1L);
    }

    @Test
    void createProvider_ShouldDelegateToFacade() {
        LlmProviderCreateRequest request = new LlmProviderCreateRequest();
        request.setProviderKey("openai");
        request.setProviderType("openai");
        request.setApiKey("sk-test");
        when(llmProviderFacade.createProvider(request)).thenReturn(true);

        var result = LLMmProviderController.createProvider(request);

        assertEquals(200, result.getCode());
        verify(llmProviderFacade).createProvider(request);
    }

    @Test
    void testConnectivity_ShouldDelegateToService() {
        LlmProviderConnectivityTestRequest request = new LlmProviderConnectivityTestRequest();
        request.setBaseUrl("https://api.openai.com/v1");
        request.setApiKey("sk-test");
        LlmProviderConnectivityTestVo response = LlmProviderConnectivityTestVo.builder()
                .success(true)
                .httpStatus(200)
                .endpoint("https://api.openai.com/v1/models")
                .latencyMs(123L)
                .message("连通成功")
                .build();
        when(llmProviderService.testConnectivity(request)).thenReturn(response);

        var result = LLMmProviderController.testConnectivity(request);

        assertEquals(200, result.getCode());
        assertEquals("https://api.openai.com/v1/models", result.getData().getEndpoint());
        verify(llmProviderService).testConnectivity(request);
    }

    @Test
    void updateProvider_ShouldDelegateToFacade() {
        LlmProviderUpdateRequest request = new LlmProviderUpdateRequest();
        request.setId(1L);
        when(llmProviderFacade.updateProvider(request)).thenReturn(true);

        var result = LLMmProviderController.updateProvider(request);

        assertEquals(200, result.getCode());
        verify(llmProviderFacade).updateProvider(request);
    }

    @Test
    void updateProviderStatus_ShouldDelegateToService() {
        LlmProviderUpdateStatusRequest request = new LlmProviderUpdateStatusRequest();
        request.setId(1L);
        request.setStatus(1);
        when(llmProviderService.updateProviderStatus(request)).thenReturn(true);

        var result = LLMmProviderController.updateProviderStatus(request);

        assertEquals(200, result.getCode());
        verify(llmProviderService).updateProviderStatus(request);
    }

    @Test
    void updateProviderApiKey_ShouldDelegateToService() {
        LlmProviderApiKeyUpdateRequest request = new LlmProviderApiKeyUpdateRequest();
        request.setId(1L);
        request.setApiKey("sk-new");
        when(llmProviderService.updateProviderApiKey(request)).thenReturn(true);

        var result = LLMmProviderController.updateProviderApiKey(request);

        assertEquals(200, result.getCode());
        verify(llmProviderService).updateProviderApiKey(request);
    }

    @Test
    void deleteProvider_ShouldDelegateToFacade() {
        when(llmProviderFacade.deleteProvider(1L)).thenReturn(true);

        var result = LLMmProviderController.deleteProvider(1L);

        assertEquals(200, result.getCode());
        verify(llmProviderFacade).deleteProvider(1L);
    }

    @Test
    void listProviderModels_ShouldDelegateToFacade() {
        when(llmProviderFacade.listProviderModels(1L))
                .thenReturn(List.of(buildProviderModel(1L, "gpt-4.1", "CHAT")));

        var result = LLMmProviderController.listProviderModels(1L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(llmProviderFacade).listProviderModels(1L);
    }

    @Test
    void createProviderModel_ShouldDelegateToFacade() {
        LlmProviderModelCreateRequest request = new LlmProviderModelCreateRequest();
        request.setProviderId(1L);
        request.setModelName("gpt-4.1");
        request.setModelType("CHAT");
        when(llmProviderFacade.createProviderModel(request)).thenReturn(true);

        var result = LLMmProviderController.createProviderModel(request);

        assertEquals(200, result.getCode());
        verify(llmProviderFacade).createProviderModel(request);
    }

    @Test
    void updateProviderModel_ShouldDelegateToFacade() {
        LlmProviderModelUpdateRequest request = new LlmProviderModelUpdateRequest();
        request.setId(1L);
        request.setProviderId(1L);
        request.setModelName("gpt-4.1");
        request.setModelType("CHAT");
        when(llmProviderFacade.updateProviderModel(request)).thenReturn(true);

        var result = LLMmProviderController.updateProviderModel(request);

        assertEquals(200, result.getCode());
        verify(llmProviderFacade).updateProviderModel(request);
    }

    @Test
    void deleteProviderModel_ShouldDelegateToModelService() {
        when(llmProviderModelService.deleteProviderModel(1L)).thenReturn(true);

        var result = LLMmProviderController.deleteProviderModel(1L);

        assertEquals(200, result.getCode());
        verify(llmProviderModelService).deleteProviderModel(1L);
    }

    private LlmPresetProviderTemplateDto buildPresetTemplate() {
        return LlmPresetProviderTemplateDto.builder()
                .providerKey("openai")
                .providerName("OpenAI")
                .providerType("openai")
                .baseUrl("https://api.openai.com/v1")
                .description("OpenAI 官方接口")
                .models(List.of(
                        LlmPresetProviderTemplateDto.Model.builder()
                                .modelName("gpt-4.1")
                                .modelType("CHAT")
                                .supportReasoning(0)
                                .supportVision(0)
                                .build(),
                        LlmPresetProviderTemplateDto.Model.builder()
                                .modelName("text-embedding-3-small")
                                .modelType("EMBEDDING")
                                .supportReasoning(0)
                                .supportVision(0)
                                .build()
                ))
                .build();
    }

    private LlmProviderModel buildProviderModel(Long providerId, String modelName, String modelType) {
        return LlmProviderModel.builder()
                .id(1L)
                .providerId(providerId)
                .modelName(modelName)
                .modelType(modelType)
                .supportReasoning(0)
                .supportVision(0)
                .build();
    }

    private boolean hasDeclaredField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals(fieldName));
    }
}
