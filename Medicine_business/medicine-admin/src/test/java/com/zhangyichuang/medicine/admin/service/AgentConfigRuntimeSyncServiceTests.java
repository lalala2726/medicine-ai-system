package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.mapper.LlmProviderMapper;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.publisher.AgentConfigPublisher;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.model.cache.*;
import com.zhangyichuang.medicine.model.constants.LlmModelTypeConstants;
import com.zhangyichuang.medicine.model.constants.LlmProviderTypeConstants;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import com.zhangyichuang.medicine.model.mq.AgentConfigRefreshMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConfigRuntimeSyncServiceTests {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    @Mock
    private AgentConfigPublisher agentConfigPublisher;

    @Mock
    private LlmProviderMapper llmProviderMapper;

    @Mock
    private LlmProviderModelMapper llmProviderModelMapper;

    private AgentConfigRuntimeSyncService syncService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        syncService = new AgentConfigRuntimeSyncService(
                new RedisCache(redisTemplate),
                agentConfigPublisher,
                llmProviderMapper,
                llmProviderModelMapper
        );
    }

    @Test
    void syncEnabledProviderChange_ShouldWriteLlmNodeAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        cache.setSpeech(buildSpeech());
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);

        syncService.syncEnabledProviderChange(buildEnabledProvider(), "admin");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        AgentAllConfigCache saved = cacheCaptor.getValue();
        assertEquals(LlmProviderTypeConstants.ALIYUN, saved.getLlm().getProviderType());
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", saved.getLlm().getBaseUrl());
        assertEquals("sk-aliyun", saved.getLlm().getApiKey());
        assertNotNull(saved.getSpeech());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void clearAgentConfigsForProviderSwitch_ShouldClearModelConfigsAndKeepSpeech() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        KnowledgeBaseAgentConfig knowledgeBase = new KnowledgeBaseAgentConfig();
        knowledgeBase.setEmbeddingModel("text-embedding-v4");
        cache.setKnowledgeBase(knowledgeBase);
        AdminAssistantAgentConfig adminAssistant = new AdminAssistantAgentConfig();
        adminAssistant.setChatDisplayModels(List.of(buildAdminAssistantChatDisplayModel("管理端模型", "gpt-4.1")));
        cache.setAdminAssistant(adminAssistant);
        ClientAssistantAgentConfig clientAssistant = new ClientAssistantAgentConfig();
        clientAssistant.setServiceNodeModel(buildSlot("gpt-4.1-mini", false));
        clientAssistant.setDiagnosisNodeModel(buildSlot("gpt-4.1", true));
        cache.setClientAssistant(clientAssistant);
        cache.setCommonCapability(buildCommonCapability(
                buildSlot("qwen-vl-max", true),
                buildSlot("gpt-4.1-mini", false),
                buildSlot("gpt-4.1-mini", false)
        ));
        SpeechAgentConfig speech = buildSpeech();
        cache.setSpeech(speech);

        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        syncService.clearAgentConfigsForProviderSwitch("tester");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        AgentAllConfigCache saved = cacheCaptor.getValue();
        assertEquals(LlmProviderTypeConstants.ALIYUN, saved.getLlm().getProviderType());
        assertNull(saved.getKnowledgeBase());
        assertNull(saved.getAdminAssistant());
        assertNull(saved.getClientAssistant());
        assertNull(saved.getCommonCapability());
        assertNotNull(saved.getSpeech());
        assertEquals("speech-app-id", saved.getSpeech().getAppId());
        assertEquals("speech-token", saved.getSpeech().getAccessToken());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void assertKnowledgeBaseCanDisable_WhenKnowledgeBaseConfigDisabled_ShouldNotThrow() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        KnowledgeBaseAgentConfig knowledgeBase = new KnowledgeBaseAgentConfig();
        knowledgeBase.setEnabled(false);
        knowledgeBase.setKnowledgeNames(List.of("drug_faq"));
        cache.setKnowledgeBase(knowledgeBase);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);

        assertDoesNotThrow(() -> syncService.assertKnowledgeBaseCanDisable("drug_faq"));
    }

    @Test
    void validateProviderSwitchCompatibility_WhenTargetModelMissing_ShouldThrowServiceException() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        KnowledgeBaseAgentConfig knowledgeBase = new KnowledgeBaseAgentConfig();
        knowledgeBase.setEmbeddingModel("text-embedding-3-large");
        cache.setKnowledgeBase(knowledgeBase);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderModelMapper.selectList(any())).thenReturn(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> syncService.validateProviderSwitchCompatibility(buildTargetProvider()));

        assertEquals("切换失败，目标提供商下不存在模型：text-embedding-3-large", exception.getMessage());
    }

    @Test
    void syncAfterModelUpdate_ShouldRenameKnowledgeBaseReferencedModelAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        KnowledgeBaseAgentConfig knowledgeBase = new KnowledgeBaseAgentConfig();
        knowledgeBase.setEmbeddingModel("text-embedding-3-large");
        cache.setKnowledgeBase(knowledgeBase);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        LlmProviderModel existing = buildModel(1L, "text-embedding-3-large", LlmModelTypeConstants.EMBEDDING,
                0, 0, 0);
        LlmProviderModel updated = buildModel(1L, "text-embedding-3-large-v2", LlmModelTypeConstants.EMBEDDING,
                0, 0, 0);

        syncService.syncAfterModelUpdate(existing, updated, "tester");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertEquals("text-embedding-3-large-v2", cacheCaptor.getValue().getKnowledgeBase().getEmbeddingModel());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncAfterModelUpdate_ShouldRenameReferencedSlotAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        cache.setCommonCapability(buildCommonCapability(
                null,
                buildSlot("gpt-4.1-mini", false),
                null
        ));
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        LlmProviderModel existing = buildModel(1L, "gpt-4.1-mini", LlmModelTypeConstants.CHAT, 0, 1, 0);
        LlmProviderModel updated = buildModel(1L, "gpt-4.1-mini-renamed", LlmModelTypeConstants.CHAT, 0, 1, 0);

        syncService.syncAfterModelUpdate(existing, updated, "tester");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertEquals("gpt-4.1-mini-renamed",
                cacheCaptor.getValue().getCommonCapability().getChatHistorySummaryModel().getModelName());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncAfterModelUpdate_ShouldRenameClientAssistantReferencedSlotAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        ClientAssistantAgentConfig clientAssistant = new ClientAssistantAgentConfig();
        clientAssistant.setDiagnosisNodeModel(buildSlot("gpt-4.1-mini", false));
        cache.setClientAssistant(clientAssistant);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        LlmProviderModel existing = buildModel(1L, "gpt-4.1-mini", LlmModelTypeConstants.CHAT, 0, 1, 0);
        LlmProviderModel updated = buildModel(1L, "gpt-4.1-mini-renamed", LlmModelTypeConstants.CHAT, 0, 1, 0);

        syncService.syncAfterModelUpdate(existing, updated, "tester");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertEquals("gpt-4.1-mini-renamed",
                cacheCaptor.getValue().getClientAssistant().getDiagnosisNodeModel().getModelName());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncAfterModelDelete_ShouldClearKnowledgeBaseRankingModelAndDisableRanking() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        KnowledgeBaseAgentConfig knowledgeBase = new KnowledgeBaseAgentConfig();
        knowledgeBase.setRankingEnabled(true);
        knowledgeBase.setRankingModel("gpt-4.1-mini");
        cache.setKnowledgeBase(knowledgeBase);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        syncService.syncAfterModelDelete(
                buildModel(1L, "gpt-4.1-mini", LlmModelTypeConstants.RERANK, 0, 0, 0),
                "tester"
        );

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertNull(cacheCaptor.getValue().getKnowledgeBase().getRankingModel());
        assertEquals(Boolean.FALSE, cacheCaptor.getValue().getKnowledgeBase().getRankingEnabled());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncAfterModelDelete_ShouldClearReferencedSlotAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        cache.setCommonCapability(buildCommonCapability(
                buildSlot("qwen-vl-max", true),
                null,
                null
        ));
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        syncService.syncAfterModelDelete(
                buildModel(1L, "qwen-vl-max", LlmModelTypeConstants.CHAT, 0, 1, 1),
                "tester"
        );

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertNull(cacheCaptor.getValue().getCommonCapability().getImageRecognitionModel());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncAfterModelDelete_ShouldClearClientAssistantReferencedSlotAndPublish() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        ClientAssistantAgentConfig clientAssistant = new ClientAssistantAgentConfig();
        clientAssistant.setDiagnosisNodeModel(buildSlot("gpt-4.1-mini", false));
        cache.setClientAssistant(clientAssistant);
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderMapper.selectList(any())).thenReturn(List.of(buildEnabledProvider()));

        syncService.syncAfterModelDelete(
                buildModel(1L, "gpt-4.1-mini", LlmModelTypeConstants.CHAT, 0, 1, 0),
                "tester"
        );

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        assertNull(cacheCaptor.getValue().getClientAssistant().getDiagnosisNodeModel());
        verify(agentConfigPublisher).publishRefresh(any(AgentConfigRefreshMessage.class));
    }

    @Test
    void syncActiveProviderSnapshot_ShouldClearIncompatibleSlotsAndRefreshLlm() {
        AgentAllConfigCache cache = new AgentAllConfigCache();
        AdminAssistantAgentConfig adminAssistant = new AdminAssistantAgentConfig();
        adminAssistant.setChatDisplayModels(List.of(buildAdminAssistantChatDisplayModel("管理端模型", "gpt-4.1-mini")));
        cache.setAdminAssistant(adminAssistant);
        cache.setCommonCapability(buildCommonCapability(
                buildSlot("qwen-vl-max", true),
                null,
                null
        ));
        when(valueOperations.get(RedisConstants.AgentConfig.ALL_CONFIG_KEY)).thenReturn(cache);
        when(llmProviderModelMapper.selectList(any())).thenReturn(List.of(
                buildModel(1L, "gpt-4.1-mini", LlmModelTypeConstants.CHAT, 0, 1, 0),
                buildModel(1L, "qwen-vl-max", LlmModelTypeConstants.CHAT, 0, 1, 0)
        ));

        syncService.syncActiveProviderSnapshot(buildEnabledProvider(), "tester");

        ArgumentCaptor<AgentAllConfigCache> cacheCaptor = ArgumentCaptor.forClass(AgentAllConfigCache.class);
        verify(valueOperations).set(eq(RedisConstants.AgentConfig.ALL_CONFIG_KEY), cacheCaptor.capture());
        AgentAllConfigCache saved = cacheCaptor.getValue();
        assertEquals(LlmProviderTypeConstants.ALIYUN, saved.getLlm().getProviderType());
        assertNotNull(saved.getAdminAssistant());
        assertEquals(1, saved.getAdminAssistant().getChatDisplayModels().size());
        assertEquals("gpt-4.1-mini", saved.getAdminAssistant().getChatDisplayModels().getFirst().getActualModelName());
        assertEquals(Boolean.TRUE, saved.getAdminAssistant().getChatDisplayModels().getFirst().getSupportReasoning());
        assertEquals(Boolean.FALSE, saved.getAdminAssistant().getChatDisplayModels().getFirst().getSupportVision());
        assertNull(saved.getCommonCapability().getImageRecognitionModel());
    }

    /**
     * 构建通用能力配置测试数据。
     *
     * @param imageRecognitionModel   图片识别模型槽位配置
     * @param chatHistorySummaryModel 聊天历史总结模型槽位配置
     * @param chatTitleModel          聊天标题模型槽位配置
     * @return 通用能力配置测试数据
     */
    private CommonCapabilityAgentConfig buildCommonCapability(AgentModelSlotConfig imageRecognitionModel,
                                                              AgentModelSlotConfig chatHistorySummaryModel,
                                                              AgentModelSlotConfig chatTitleModel) {
        CommonCapabilityAgentConfig commonCapability = new CommonCapabilityAgentConfig();
        commonCapability.setImageRecognitionModel(imageRecognitionModel);
        commonCapability.setChatHistorySummaryModel(chatHistorySummaryModel);
        commonCapability.setChatTitleModel(chatTitleModel);
        return commonCapability;
    }

    private LlmProvider buildEnabledProvider() {
        return LlmProvider.builder()
                .id(1L)
                .providerType(LlmProviderTypeConstants.ALIYUN)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-aliyun")
                .status(1)
                .build();
    }

    private LlmProvider buildTargetProvider() {
        return LlmProvider.builder()
                .id(2L)
                .providerType("aliyun")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-aliyun")
                .status(1)
                .build();
    }

    private LlmProviderModel buildModel(Long providerId,
                                        String modelName,
                                        String modelType,
                                        Integer enabled,
                                        Integer supportReasoning,
                                        Integer supportVision) {
        return LlmProviderModel.builder()
                .providerId(providerId)
                .modelName(modelName)
                .modelType(modelType)
                .enabled(enabled)
                .supportReasoning(supportReasoning)
                .supportVision(supportVision)
                .build();
    }

    private AgentModelSlotConfig buildSlot(String modelName,
                                           boolean reasoningEnabled) {
        AgentModelSlotConfig slotConfig = new AgentModelSlotConfig();
        slotConfig.setModelName(modelName);
        slotConfig.setReasoningEnabled(reasoningEnabled);
        slotConfig.setSupportReasoning(true);
        slotConfig.setSupportVision(false);
        return slotConfig;
    }

    /**
     * 构建管理端聊天展示模型测试数据。
     *
     * @param customModelName 前端展示模型名称
     * @param actualModelName 实际调用模型名称
     * @return 管理端聊天展示模型测试数据
     */
    private AdminAssistantChatDisplayModelConfig buildAdminAssistantChatDisplayModel(String customModelName,
                                                                                     String actualModelName) {
        AdminAssistantChatDisplayModelConfig config = new AdminAssistantChatDisplayModelConfig();
        config.setCustomModelName(customModelName);
        config.setActualModelName(actualModelName);
        return config;
    }

    private SpeechAgentConfig buildSpeech() {
        SpeechAgentConfig config = new SpeechAgentConfig();
        config.setProvider("volcengine");
        config.setAppId("speech-app-id");
        config.setAccessToken("speech-token");
        return config;
    }
}
