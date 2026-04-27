package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.integration.LlmProviderConnectivityClient;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderMapper;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.service.AgentConfigRuntimeSyncService;
import com.zhangyichuang.medicine.common.core.config.JacksonConfig;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import com.zhangyichuang.medicine.model.constants.LlmProviderTypeConstants;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmProviderServiceImplTests {

    @Mock
    private LlmProviderMapper llmProviderMapper;

    @Mock
    private LlmProviderConnectivityClient llmProviderConnectivityClient;

    @Mock
    private AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LlmProviderServiceImpl llmProviderService;

    @Test
    void createProvider_ShouldDefaultStatusToEnabled_WhenNoEnabledProviderExists() {
        LlmProviderCreateRequest request = new LlmProviderCreateRequest();
        request.setProviderName("OpenAI Custom");
        request.setProviderType(LlmProviderTypeConstants.ALIYUN);
        request.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        request.setApiKey("sk-test");

        when(llmProviderMapper.selectCount(any())).thenReturn(0L, 0L);
        when(llmProviderMapper.insert(any(LlmProvider.class))).thenAnswer(invocation -> {
            LlmProvider provider = invocation.getArgument(0);
            provider.setId(1L);
            return 1;
        });

        LlmProvider provider = llmProviderService.createProvider(request);

        assertEquals(1L, provider.getId());
        assertEquals(1, provider.getStatus());
    }

    @Test
    void updateProvider_ShouldPreserveExistingStatusAndApiKey() {
        LlmProvider existing = LlmProvider.builder()
                .id(1L)
                .providerName("Old Provider")
                .providerType(LlmProviderTypeConstants.ALIYUN)
                .baseUrl("https://old.example.com/v1")
                .status(0)
                .sort(10)
                .apiKey("sk-old")
                .build();
        LlmProviderUpdateRequest request = new LlmProviderUpdateRequest();
        request.setId(1L);
        request.setProviderName("New Provider");
        request.setProviderType("aliyun");
        request.setBaseUrl("https://new.example.com/v1");

        when(llmProviderMapper.selectById(1L)).thenReturn(existing);
        when(llmProviderMapper.selectCount(any())).thenReturn(0L);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        LlmProvider provider = llmProviderService.updateProvider(request);

        assertEquals("New Provider", provider.getProviderName());
        assertEquals("aliyun", provider.getProviderType());
        assertEquals("https://new.example.com/v1", provider.getBaseUrl());
        assertEquals("sk-old", provider.getApiKey());
        verifyNoInteractions(agentConfigRuntimeSyncService);
    }

    @Test
    void updateProviderApiKey_ShouldRefreshRuntimeWhenProviderEnabled() {
        LlmProvider existing = LlmProvider.builder()
                .id(1L)
                .providerType(LlmProviderTypeConstants.ALIYUN)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-old")
                .status(1)
                .build();
        LlmProviderApiKeyUpdateRequest request = new LlmProviderApiKeyUpdateRequest();
        request.setId(1L);
        request.setApiKey("sk-new");

        when(llmProviderMapper.selectById(1L)).thenReturn(existing);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        boolean result = llmProviderService.updateProviderApiKey(request);

        assertTrue(result);
        verify(agentConfigRuntimeSyncService).syncEnabledProviderChange(any(LlmProvider.class), anyString());
    }

    @Test
    void updateProviderApiKey_WhenProviderDisabled_ShouldStillDelegateToSyncService() {
        LlmProvider existing = LlmProvider.builder()
                .id(1L)
                .providerType(LlmProviderTypeConstants.ALIYUN)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-old")
                .status(0)
                .build();
        LlmProviderApiKeyUpdateRequest request = new LlmProviderApiKeyUpdateRequest();
        request.setId(1L);
        request.setApiKey("sk-new");

        when(llmProviderMapper.selectById(1L)).thenReturn(existing);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        llmProviderService.updateProviderApiKey(request);

        verify(agentConfigRuntimeSyncService).syncEnabledProviderChange(any(LlmProvider.class), anyString());
    }

    @Test
    void updateProviderStatus_ShouldRefreshWithoutValidatingCompatibilityWhenEnabling() {
        LlmProvider existing = LlmProvider.builder()
                .id(2L)
                .providerName("Backup Provider")
                .status(0)
                .build();
        LlmProviderUpdateStatusRequest request = new LlmProviderUpdateStatusRequest();
        request.setId(2L);
        request.setStatus(1);

        when(llmProviderMapper.selectById(2L)).thenReturn(existing);
        when(llmProviderMapper.update(any(LlmProvider.class), any())).thenReturn(1);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        boolean result = llmProviderService.updateProviderStatus(request);

        assertTrue(result);
        verify(agentConfigRuntimeSyncService, never()).validateProviderSwitchCompatibility(any());
        verify(agentConfigRuntimeSyncService).clearAgentConfigsForProviderSwitch(anyString());
    }

    @Test
    void updateProviderStatus_WhenCompatibilityWouldFail_ShouldStillEnableProvider() {
        LlmProvider existing = LlmProvider.builder()
                .id(3L)
                .providerName("New Provider")
                .status(0)
                .build();
        LlmProviderUpdateStatusRequest request = new LlmProviderUpdateStatusRequest();
        request.setId(3L);
        request.setStatus(1);

        when(llmProviderMapper.selectById(3L)).thenReturn(existing);
        when(llmProviderMapper.update(any(LlmProvider.class), any())).thenReturn(1);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        boolean result = llmProviderService.updateProviderStatus(request);

        assertTrue(result);
        verify(agentConfigRuntimeSyncService, never()).validateProviderSwitchCompatibility(any());
        verify(agentConfigRuntimeSyncService).clearAgentConfigsForProviderSwitch(anyString());
    }

    @Test
    void updateProviderStatus_WhenDisablingEnabledProvider_ShouldThrowServiceException() {
        LlmProvider existing = LlmProvider.builder()
                .id(1L)
                .providerName("OpenAI")
                .status(1)
                .build();
        LlmProviderUpdateStatusRequest request = new LlmProviderUpdateStatusRequest();
        request.setId(1L);
        request.setStatus(0);

        when(llmProviderMapper.selectById(1L)).thenReturn(existing);
        doThrow(new ServiceException("当前启用的提供商不允许停用，请先切换到其他提供商"))
                .when(agentConfigRuntimeSyncService).assertProviderCanDisable(existing);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> llmProviderService.updateProviderStatus(request));

        assertEquals("当前启用的提供商不允许停用，请先切换到其他提供商", exception.getMessage());
        verify(llmProviderMapper, never()).updateById(any(LlmProvider.class));
    }

    @Test
    void deleteProvider_WhenEnabled_ShouldThrowServiceException() {
        LlmProvider existing = LlmProvider.builder().id(1L).status(1).build();
        when(llmProviderMapper.selectById(1L)).thenReturn(existing);
        doThrow(new ServiceException("当前启用的提供商不允许删除，请先切换到其他提供商"))
                .when(agentConfigRuntimeSyncService).assertProviderCanDelete(existing);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> llmProviderService.deleteProvider(1L));

        assertEquals("当前启用的提供商不允许删除，请先切换到其他提供商", exception.getMessage());
        verify(llmProviderMapper, never()).deleteById(1L);
    }

    @Test
    void updateProviderApiKey_WhenApiKeyBlank_ShouldThrowParamException() {
        LlmProvider existing = LlmProvider.builder()
                .id(1L)
                .providerName("OpenAI")
                .providerType(LlmProviderTypeConstants.ALIYUN)
                .apiKey("sk-old")
                .build();
        LlmProviderApiKeyUpdateRequest request = new LlmProviderApiKeyUpdateRequest();
        request.setId(1L);
        request.setApiKey("   ");
        when(llmProviderMapper.selectById(1L)).thenReturn(existing);

        ParamException exception = assertThrows(ParamException.class,
                () -> llmProviderService.updateProviderApiKey(request));

        assertEquals("API Key不能为空", exception.getMessage());
    }

    @Test
    void updateProviderRequest_WhenJsonContainsApiKey_ShouldIgnoreUnknownProperty() throws Exception {
        String json = """
                {
                  "id": 1,
                  "providerName": "OpenAI",
                  "providerType": "openai",
                  "baseUrl": "https://api.openai.com/v1",
                  "apiKey": "sk-ignored",
                  "status": 1,
                  "models": []
                }
                """;

        LlmProviderUpdateRequest request = new JacksonConfig().jsonMapper()
                .readValue(json, LlmProviderUpdateRequest.class);

        assertEquals("https://api.openai.com/v1", request.getBaseUrl());
    }

    @Test
    void testConnectivity_ShouldAppendModelsAndReturnSuccess() {
        LlmProviderConnectivityTestRequest request = new LlmProviderConnectivityTestRequest();
        request.setBaseUrl("https://api.openai.com/v1");
        request.setApiKey("sk-test");
        when(llmProviderConnectivityClient.getModels("https://api.openai.com/v1/models", "sk-test"))
                .thenReturn(HttpResult.<String>builder()
                        .statusCode(200)
                        .body("{\"data\":[]}")
                        .build());

        var result = llmProviderService.testConnectivity(request);

        assertTrue(result.getSuccess());
        assertEquals("https://api.openai.com/v1/models", result.getEndpoint());
    }
}
