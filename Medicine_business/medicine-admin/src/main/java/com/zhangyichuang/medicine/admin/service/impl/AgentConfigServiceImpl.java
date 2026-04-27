package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.cache.*;
import com.zhangyichuang.medicine.model.constants.LlmModelTypeConstants;
import com.zhangyichuang.medicine.model.entity.KbBase;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent 配置服务实现。
 * <p>
 * 负责将管理端编辑态配置解析为运行时缓存结构，并统一读写 Redis 中的 Agent 全量配置。
 */
@Service
@RequiredArgsConstructor
public class AgentConfigServiceImpl implements AgentConfigService, BaseService {

    private static final int PROVIDER_STATUS_ENABLED = 1;
    private static final int KNOWLEDGE_BASE_MAX_COUNT = 5;
    private static final int KNOWLEDGE_BASE_TOP_K_MIN = 1;
    private static final int KNOWLEDGE_BASE_TOP_K_MAX = 100;
    private static final int MODEL_STATUS_ENABLED = 0;
    private static final int CAPABILITY_ENABLED = 1;
    private static final int SPEECH_MAX_TEXT_CHARS_MIN = 1;
    private static final int SPEECH_MAX_TEXT_CHARS_MAX = 3000;
    private static final String DEFAULT_OPERATOR = "system";
    private static final String SPEECH_PROVIDER = "volcengine";
    private static final String VOLCENGINE_STT_RESOURCE_ID = "volc.seedasr.sauc.duration";
    private static final String VOLCENGINE_TTS_RESOURCE_ID = "seed-tts-2.0";
    private static final String ENABLED_PROVIDER_MISSING_MESSAGE = "当前没有启用的模型提供商";
    private static final String MODEL_DISABLED_MESSAGE = "模型未启用：%s";
    private static final String REASONING_UNSUPPORTED_MESSAGE = "模型不支持深度思考：%s";
    /**
     * 客户端统一深度思考开关不满足条件时的提示文案。
     */
    private static final String CLIENT_ASSISTANT_REASONING_UNAVAILABLE_MESSAGE =
            "服务节点模型和诊断节点模型都支持深度思考后，才可开启客户端统一深度思考";
    private static final String VISION_UNSUPPORTED_MESSAGE = "模型不支持图片理解：%s";
    private static final String EMBEDDING_MODEL_MISSING_MESSAGE = "当前启用提供商下不存在向量模型：%s";
    private static final String RANKING_MODEL_MISSING_MESSAGE = "当前启用提供商下不存在排序模型：%s";
    private static final String CHAT_MODEL_MISSING_MESSAGE = "当前启用提供商下不存在聊天模型：%s";
    private static final String VISION_MODEL_MISSING_MESSAGE = "当前启用提供商下不存在图片理解模型：%s";
    private static final String KNOWLEDGE_BASE_NAME_REQUIRED_MESSAGE = "知识库名称不能为空";
    private static final String KNOWLEDGE_BASE_DUPLICATE_MESSAGE = "知识库名称不能重复：%s";
    private static final String KNOWLEDGE_BASE_NOT_FOUND_MESSAGE = "启用中的知识库不存在：%s";
    private static final String KNOWLEDGE_BASE_MODEL_MISMATCH_MESSAGE = "知识库向量模型必须与第一个知识库保持一致：%s";
    private static final String KNOWLEDGE_BASE_DIM_MISMATCH_MESSAGE = "知识库向量维度必须与第一个知识库保持一致：%s";
    private static final String KNOWLEDGE_BASE_CONFIG_MODEL_MISMATCH_MESSAGE = "向量模型必须与第一个知识库保持一致";
    private static final String KNOWLEDGE_BASE_CONFIG_DIM_MISMATCH_MESSAGE = "向量维度必须与第一个知识库保持一致";
    private static final String KNOWLEDGE_BASE_RANKING_REQUIRED_MESSAGE = "启用排序时必须选择排序模型";
    private static final String KNOWLEDGE_BASE_RANKING_DISABLED_MESSAGE = "关闭排序时不允许选择排序模型";
    private static final String KNOWLEDGE_BASE_TOP_K_MIN_MESSAGE = "知识库返回条数不能小于1";
    private static final String KNOWLEDGE_BASE_TOP_K_MAX_MESSAGE = "知识库返回条数不能大于100";
    private static final String SPEECH_APP_ID_REQUIRED_MESSAGE = "豆包语音AppId不能为空";
    private static final String SPEECH_ACCESS_TOKEN_REQUIRED_MESSAGE = "豆包语音AccessToken不能为空";
    private static final String SPEECH_TTS_REQUIRED_MESSAGE = "语音合成配置不能为空";
    private static final String SPEECH_TTS_VOICE_TYPE_REQUIRED_MESSAGE = "语音合成VoiceType不能为空";
    private static final String SPEECH_TTS_MAX_TEXT_CHARS_REQUIRED_MESSAGE = "语音合成最大文本长度不能为空";
    private static final String SPEECH_TTS_MAX_TEXT_CHARS_MIN_MESSAGE = "语音合成最大文本长度不能小于1";
    private static final String SPEECH_TTS_MAX_TEXT_CHARS_MAX_MESSAGE = "语音合成最大文本长度不能大于3000";
    /**
     * 管理端聊天展示模型自定义名称重复提示。
     */
    private static final String ADMIN_ASSISTANT_DISPLAY_MODEL_DUPLICATE_MESSAGE = "前端展示模型名称不能重复：%s";

    private final KbBaseService kbBaseService;
    private final LlmProviderService llmProviderService;
    private final LlmProviderModelService llmProviderModelService;
    private final AgentConfigRuntimeSyncService agentConfigRuntimeSyncService;

    /**
     * 查询知识库 Agent 配置详情。
     *
     * @return 知识库 Agent 配置
     */
    @Override
    public KnowledgeBaseAgentConfigVo getKnowledgeBaseConfig() {
        return toKnowledgeBaseConfigVo(agentConfigRuntimeSyncService.readCache().getKnowledgeBase());
    }

    /**
     * 保存知识库 Agent 配置。
     *
     * @param request 知识库 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveKnowledgeBaseConfig(KnowledgeBaseAgentConfigRequest request) {
        return saveKnowledgeBaseConfig(request, AgentAllConfigCache::getKnowledgeBase, AgentAllConfigCache::setKnowledgeBase);
    }

    /**
     * 查询知识库下拉选项列表。
     *
     * @return 知识库下拉选项
     */
    @Override
    public List<KnowledgeBaseOptionVo> listKnowledgeBaseOptions() {
        return buildKnowledgeBaseOptions();
    }

    /**
     * 查询客户端知识库 Agent 配置详情。
     *
     * @return 客户端知识库 Agent 配置
     */
    @Override
    public KnowledgeBaseAgentConfigVo getClientKnowledgeBaseConfig() {
        return toKnowledgeBaseConfigVo(agentConfigRuntimeSyncService.readCache().getClientKnowledgeBase());
    }

    /**
     * 保存客户端知识库 Agent 配置。
     *
     * @param request 客户端知识库 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveClientKnowledgeBaseConfig(KnowledgeBaseAgentConfigRequest request) {
        return saveKnowledgeBaseConfig(request, AgentAllConfigCache::getClientKnowledgeBase,
                AgentAllConfigCache::setClientKnowledgeBase);
    }

    /**
     * 查询客户端知识库下拉选项列表。
     *
     * @return 客户端知识库下拉选项
     */
    @Override
    public List<KnowledgeBaseOptionVo> listClientKnowledgeBaseOptions() {
        return buildKnowledgeBaseOptions();
    }

    /**
     * 查询管理端助手 Agent 配置详情。
     *
     * @return 管理端助手 Agent 配置
     */
    @Override
    public AdminAssistantAgentConfigVo getAdminAssistantConfig() {
        AdminAssistantAgentConfigVo vo = new AdminAssistantAgentConfigVo();
        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        AdminAssistantAgentConfig config = ensureAdminAssistantConfig(cache);
        if (config == null) {
            return vo;
        }
        LlmProvider provider = getEnabledProviderOrNull();
        vo.setChatDisplayModels(toAdminAssistantChatDisplayModelVoList(provider, config.getChatDisplayModels()));
        return vo;
    }

    /**
     * 查询客户端助手 Agent 配置详情。
     *
     * @return 客户端助手 Agent 配置
     */
    @Override
    public ClientAssistantAgentConfigVo getClientAssistantConfig() {
        ClientAssistantAgentConfigVo vo = new ClientAssistantAgentConfigVo();
        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        ClientAssistantAgentConfig config = cache.getClientAssistant();
        if (config == null) {
            return vo;
        }
        LlmProvider provider = getEnabledProviderOrNull();
        vo.setRouteModel(toAgentModelSelectionVo(provider, config.getRouteModel(), LlmModelTypeConstants.CHAT));
        vo.setServiceNodeModel(toAgentModelSelectionVo(provider, config.getServiceNodeModel(),
                LlmModelTypeConstants.CHAT));
        vo.setDiagnosisNodeModel(toAgentModelSelectionVo(provider, config.getDiagnosisNodeModel(),
                LlmModelTypeConstants.CHAT));
        vo.setReasoningEnabled(config.getReasoningEnabled());
        return vo;
    }

    /**
     * 保存管理端助手 Agent 配置。
     *
     * @param request 管理端助手 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveAdminAssistantConfig(AdminAssistantAgentConfigRequest request) {
        Assert.notNull(request, "管理端助手Agent配置不能为空");

        LlmProvider provider = getRequiredEnabledProvider();
        AdminAssistantAgentConfig config = new AdminAssistantAgentConfig();
        config.setChatDisplayModels(resolveAdminAssistantChatDisplayModels(provider, request.getChatDisplayModels()));

        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        cache.setAdminAssistant(config);
        agentConfigRuntimeSyncService.saveCache(cache, provider, currentOperator());
        return true;
    }

    /**
     * 保存客户端助手 Agent 配置。
     *
     * @param request 客户端助手 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveClientAssistantConfig(ClientAssistantAgentConfigRequest request) {
        Assert.notNull(request, "客户端助手Agent配置不能为空");

        LlmProvider provider = getRequiredEnabledProvider();
        AgentModelSlotConfig routeModel = resolveRequiredClientAssistantSlotConfig(provider, request.getRouteModel());
        AgentModelSlotConfig serviceNodeModel = resolveRequiredClientAssistantSlotConfig(provider,
                request.getServiceNodeModel());
        AgentModelSlotConfig diagnosisNodeModel = resolveRequiredClientAssistantSlotConfig(provider,
                request.getDiagnosisNodeModel());
        validateClientAssistantReasoningToggle(request.getReasoningEnabled(), serviceNodeModel, diagnosisNodeModel);

        ClientAssistantAgentConfig config = new ClientAssistantAgentConfig();
        config.setRouteModel(routeModel);
        config.setServiceNodeModel(serviceNodeModel);
        config.setDiagnosisNodeModel(diagnosisNodeModel);
        config.setReasoningEnabled(request.getReasoningEnabled());

        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        cache.setClientAssistant(config);
        agentConfigRuntimeSyncService.saveCache(cache, provider, currentOperator());
        return true;
    }

    /**
     * 查询通用能力 Agent 配置详情。
     *
     * @return 通用能力 Agent 配置
     */
    @Override
    public CommonCapabilityAgentConfigVo getCommonCapabilityConfig() {
        CommonCapabilityAgentConfigVo vo = new CommonCapabilityAgentConfigVo();
        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        CommonCapabilityAgentConfig config = cache.getCommonCapability();
        if (config == null) {
            return vo;
        }
        LlmProvider provider = getEnabledProviderOrNull();
        vo.setImageRecognitionModel(toAgentModelSelectionVo(provider, config.getImageRecognitionModel(),
                LlmModelTypeConstants.CHAT));
        vo.setChatHistorySummaryModel(toAgentModelSelectionVo(provider, config.getChatHistorySummaryModel(),
                LlmModelTypeConstants.CHAT));
        vo.setChatTitleModel(toAgentModelSelectionVo(provider, config.getChatTitleModel(),
                LlmModelTypeConstants.CHAT));
        return vo;
    }

    /**
     * 查询豆包语音 Agent 配置详情。
     *
     * @return 豆包语音 Agent 配置
     */
    @Override
    public SpeechAgentConfigVo getSpeechConfig() {
        return toSpeechConfigVo(agentConfigRuntimeSyncService.readCache().getSpeech());
    }

    /**
     * 保存通用能力 Agent 配置。
     *
     * @param request 通用能力 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveCommonCapabilityConfig(CommonCapabilityAgentConfigRequest request) {
        Assert.notNull(request, "通用能力Agent配置不能为空");

        LlmProvider provider = getRequiredEnabledProvider();
        CommonCapabilityAgentConfig config = new CommonCapabilityAgentConfig();
        config.setImageRecognitionModel(resolveRequiredSlotConfig(provider, request.getImageRecognitionModel(),
                LlmModelTypeConstants.CHAT, true, VISION_MODEL_MISSING_MESSAGE));
        config.setChatHistorySummaryModel(resolveRequiredSlotConfig(provider, request.getChatHistorySummaryModel(),
                LlmModelTypeConstants.CHAT, false, CHAT_MODEL_MISSING_MESSAGE));
        config.setChatTitleModel(resolveRequiredSlotConfig(provider, request.getChatTitleModel(),
                LlmModelTypeConstants.CHAT, false, CHAT_MODEL_MISSING_MESSAGE));

        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        cache.setCommonCapability(config);
        agentConfigRuntimeSyncService.saveCache(cache, provider, currentOperator());
        return true;
    }

    /**
     * 保存豆包语音 Agent 配置。
     *
     * @param request 豆包语音 Agent 配置请求
     * @return 是否保存成功
     */
    @Override
    public boolean saveSpeechConfig(SpeechAgentConfigRequest request) {
        Assert.notNull(request, "豆包语音Agent配置不能为空");

        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        SpeechAgentConfig existingConfig = cache.getSpeech();
        validateSpeechRequest(request, existingConfig);

        cache.setSpeech(buildSpeechConfig(request, existingConfig));
        agentConfigRuntimeSyncService.saveCache(cache, getEnabledProviderOrNull(), currentOperator());
        return true;
    }

    /**
     * 查询向量模型选项列表。
     *
     * @return 向量模型选项
     */
    @Override
    public List<AgentModelOptionVo> listEmbeddingModelOptions() {
        return listModelOptions(LlmModelTypeConstants.EMBEDDING, false);
    }

    /**
     * 查询聊天模型选项列表。
     *
     * @return 聊天模型选项
     */
    @Override
    public List<AgentModelOptionVo> listChatModelOptions() {
        return listModelOptions(LlmModelTypeConstants.CHAT, false);
    }

    /**
     * 查询重排模型选项列表。
     *
     * @return 重排模型选项
     */
    @Override
    public List<AgentModelOptionVo> listRerankModelOptions() {
        return listModelOptions(LlmModelTypeConstants.RERANK, false);
    }

    /**
     * 查询图片理解模型选项列表。
     *
     * @return 图片理解模型选项
     */
    @Override
    public List<AgentModelOptionVo> listVisionModelOptions() {
        return listModelOptions(LlmModelTypeConstants.CHAT, true);
    }

    /**
     * 将运行时槽位配置转换为编辑态视图对象。
     *
     * @param provider   当前启用的模型提供商
     * @param slotConfig 运行时槽位配置
     * @param modelType  模型类型
     * @return 编辑态视图对象
     */
    private AgentModelSelectionVo toAgentModelSelectionVo(LlmProvider provider,
                                                          AgentModelSlotConfig slotConfig,
                                                          String modelType) {
        if (slotConfig == null) {
            return null;
        }
        AgentModelSelectionVo vo = new AgentModelSelectionVo();
        vo.setModelName(slotConfig.getModelName());
        vo.setReasoningEnabled(slotConfig.getReasoningEnabled());
        fillModelCapabilities(vo, provider, slotConfig.getModelName(), modelType);
        return vo;
    }

    /**
     * 将知识库中仅保存模型名称的配置转换为编辑态视图对象。
     *
     * @param provider  当前启用的模型提供商
     * @param modelName 模型名称
     * @param modelType 模型类型
     * @return 编辑态视图对象；模型名称为空时返回 null
     */
    private AgentModelSelectionVo toKnowledgeBaseModelSelectionVo(LlmProvider provider,
                                                                  String modelName,
                                                                  String modelType) {
        if (!StringUtils.hasText(modelName)) {
            return null;
        }
        AgentModelSelectionVo vo = new AgentModelSelectionVo();
        vo.setModelName(modelName);
        vo.setReasoningEnabled(false);
        fillModelCapabilities(vo, provider, modelName, modelType);
        return vo;
    }

    /**
     * 为模型选择视图对象补充能力信息。
     * <p>
     * 根据提供商、模型名称和模型类型查询模型元数据，并回填深度思考与图片理解能力标记。
     *
     * @param vo        待回填能力信息的视图对象
     * @param provider  当前启用的模型提供商
     * @param modelName 模型名称
     * @param modelType 模型类型
     */
    private void fillModelCapabilities(AgentModelSelectionVo vo, LlmProvider provider, String modelName, String modelType) {
        if (provider == null || !StringUtils.hasText(modelName)) {
            return;
        }
        List<LlmProviderModel> models = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId())
                .eq(LlmProviderModel::getModelType, modelType)
                .eq(LlmProviderModel::getModelName, modelName)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId)
                .list();
        if (models.isEmpty()) {
            return;
        }
        LlmProviderModel model = models.getFirst();
        vo.setSupportReasoning(isCapabilityEnabled(model.getSupportReasoning()));
        vo.setSupportVision(isCapabilityEnabled(model.getSupportVision()));
    }

    /**
     * 将豆包语音运行时配置转换为详情视图对象。
     *
     * @param config 豆包语音运行时配置
     * @return 豆包语音详情视图对象
     */
    private SpeechAgentConfigVo toSpeechConfigVo(SpeechAgentConfig config) {
        SpeechAgentConfigVo vo = new SpeechAgentConfigVo();
        if (config == null) {
            return vo;
        }
        vo.setAppId(config.getAppId());
        vo.setAccessToken(null);
        vo.setTextToSpeech(toTextToSpeechConfigVo(config.getTextToSpeech()));
        return vo;
    }

    /**
     * 将语音合成运行时配置转换为详情视图对象。
     *
     * @param config 语音合成运行时配置
     * @return 语音合成详情视图对象
     */
    private TextToSpeechConfigVo toTextToSpeechConfigVo(TextToSpeechAgentConfig config) {
        if (config == null) {
            return null;
        }
        TextToSpeechConfigVo vo = new TextToSpeechConfigVo();
        vo.setVoiceType(config.getVoiceType());
        vo.setMaxTextChars(config.getMaxTextChars());
        return vo;
    }

    /**
     * 查询指定模型类型的下拉选项。
     *
     * @param modelType      模型类型
     * @param visionRequired 是否要求支持图片理解
     * @return 模型选项列表
     */
    private List<AgentModelOptionVo> listModelOptions(String modelType, boolean visionRequired) {
        LlmProvider provider = getEnabledProviderOrNull();
        if (provider == null) {
            return List.of();
        }
        return listEnabledProviderModels(provider.getId(), modelType, visionRequired)
                .stream()
                .map(this::toAgentModelOptionVo)
                .toList();
    }

    /**
     * 将提供商模型实体转换为带能力信息的下拉选项视图对象。
     *
     * @param providerModel 提供商模型实体
     * @return 模型下拉选项视图对象
     */
    private AgentModelOptionVo toAgentModelOptionVo(LlmProviderModel providerModel) {
        AgentModelOptionVo vo = new AgentModelOptionVo();
        vo.setLabel(providerModel.getModelName());
        vo.setValue(providerModel.getModelName());
        vo.setSupportReasoning(isCapabilityEnabled(providerModel.getSupportReasoning()));
        vo.setSupportVision(isCapabilityEnabled(providerModel.getSupportVision()));
        vo.setDescription(providerModel.getDescription());
        return vo;
    }

    /**
     * 将管理端聊天展示模型缓存列表转换为视图对象列表。
     *
     * @param configs 管理端聊天展示模型缓存列表
     * @return 视图对象列表
     */
    private List<AdminAssistantChatDisplayModelVo> toAdminAssistantChatDisplayModelVoList(
            LlmProvider provider,
            List<AdminAssistantChatDisplayModelConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        return configs.stream()
                .filter(Objects::nonNull)
                .map(config -> toAdminAssistantChatDisplayModelVo(provider, config))
                .toList();
    }

    /**
     * 将单个管理端聊天展示模型缓存对象转换为视图对象。
     *
     * @param config 管理端聊天展示模型缓存对象
     * @return 视图对象
     */
    private AdminAssistantChatDisplayModelVo toAdminAssistantChatDisplayModelVo(
            LlmProvider provider,
            AdminAssistantChatDisplayModelConfig config) {
        AdminAssistantChatDisplayModelVo vo = new AdminAssistantChatDisplayModelVo();
        vo.setCustomModelName(config.getCustomModelName());
        vo.setActualModelName(config.getActualModelName());
        vo.setDescription(config.getDescription());
        vo.setSupportReasoning(config.getSupportReasoning());
        vo.setSupportVision(config.getSupportVision());
        fillAdminAssistantDisplayModelCapabilities(vo, provider, config.getActualModelName());
        return vo;
    }

    /**
     * 当管理端展示模型配置为空时，按当前启用的聊天模型自动生成一份默认映射。
     *
     * @param cache Agent 全量配置缓存
     * @return 最终可用于读取的管理端展示模型配置
     */
    private AdminAssistantAgentConfig ensureAdminAssistantConfig(AgentAllConfigCache cache) {
        AdminAssistantAgentConfig existingConfig = cache.getAdminAssistant();
        if (existingConfig != null && existingConfig.getChatDisplayModels() != null
                && !existingConfig.getChatDisplayModels().isEmpty()) {
            return existingConfig;
        }

        LlmProvider provider = getEnabledProviderOrNull();
        if (provider == null) {
            return existingConfig;
        }

        List<AdminAssistantChatDisplayModelConfig> defaultDisplayModels =
                buildDefaultAdminAssistantChatDisplayModels(provider.getId());
        if (defaultDisplayModels.isEmpty()) {
            return existingConfig;
        }

        AdminAssistantAgentConfig nextConfig = existingConfig == null ? new AdminAssistantAgentConfig() : existingConfig;
        nextConfig.setChatDisplayModels(defaultDisplayModels);
        cache.setAdminAssistant(nextConfig);
        agentConfigRuntimeSyncService.saveCache(cache, provider, currentOperator());
        return nextConfig;
    }

    /**
     * 使用当前启用的聊天模型构建默认展示模型映射。
     *
     * @param providerId 提供商 ID
     * @return 默认展示模型映射列表
     */
    private List<AdminAssistantChatDisplayModelConfig> buildDefaultAdminAssistantChatDisplayModels(Long providerId) {
        List<LlmProviderModel> chatModels = listEnabledProviderModels(providerId, LlmModelTypeConstants.CHAT, false);
        if (chatModels.isEmpty()) {
            return List.of();
        }

        List<AdminAssistantChatDisplayModelConfig> configs = new ArrayList<>(chatModels.size());
        for (LlmProviderModel chatModel : chatModels) {
            AdminAssistantChatDisplayModelConfig config = new AdminAssistantChatDisplayModelConfig();
            config.setCustomModelName(chatModel.getModelName());
            config.setActualModelName(chatModel.getModelName());
            config.setDescription(chatModel.getDescription());
            config.setSupportReasoning(isCapabilityEnabled(chatModel.getSupportReasoning()));
            config.setSupportVision(isCapabilityEnabled(chatModel.getSupportVision()));
            configs.add(config);
        }
        return List.copyOf(configs);
    }

    /**
     * 按当前启用提供商回填展示模型能力信息。
     *
     * @param vo              展示模型视图对象
     * @param provider        当前启用提供商
     * @param actualModelName 真实模型名称
     */
    private void fillAdminAssistantDisplayModelCapabilities(AdminAssistantChatDisplayModelVo vo,
                                                            LlmProvider provider,
                                                            String actualModelName) {
        if (provider == null || !StringUtils.hasText(actualModelName)) {
            return;
        }
        List<LlmProviderModel> models = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId())
                .eq(LlmProviderModel::getModelType, LlmModelTypeConstants.CHAT)
                .eq(LlmProviderModel::getModelName, actualModelName)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId)
                .list();
        if (models.isEmpty()) {
            return;
        }
        LlmProviderModel model = models.getFirst();
        if (vo.getSupportReasoning() == null) {
            vo.setSupportReasoning(isCapabilityEnabled(model.getSupportReasoning()));
        }
        if (vo.getSupportVision() == null) {
            vo.setSupportVision(isCapabilityEnabled(model.getSupportVision()));
        }
    }

    /**
     * 解析并校验管理端聊天展示模型映射列表。
     *
     * @param provider 当前启用的模型提供商
     * @param requests 前端提交的展示模型映射列表
     * @return 可写入 Redis 的展示模型映射列表
     */
    private List<AdminAssistantChatDisplayModelConfig> resolveAdminAssistantChatDisplayModels(
            LlmProvider provider,
            List<AdminAssistantChatDisplayModelRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<AdminAssistantChatDisplayModelConfig> configs = new ArrayList<>(requests.size());
        Set<String> customModelNames = new LinkedHashSet<>();
        for (AdminAssistantChatDisplayModelRequest request : requests) {
            Assert.notNull(request, "聊天展示模型配置不能为空");

            String customModelName = normalizeNullableText(request.getCustomModelName());
            Assert.notEmpty(customModelName, "前端展示模型名称不能为空");
            Assert.isParamTrue(customModelNames.add(customModelName),
                    ADMIN_ASSISTANT_DISPLAY_MODEL_DUPLICATE_MESSAGE.formatted(customModelName));

            String actualModelName = normalizeNullableText(request.getActualModelName());
            Assert.notEmpty(actualModelName, "真实模型名称不能为空");

            LlmProviderModel providerModel = getProviderModel(provider, actualModelName, LlmModelTypeConstants.CHAT,
                    CHAT_MODEL_MISSING_MESSAGE);
            validateProviderModelEnabled(providerModel);

            AdminAssistantChatDisplayModelConfig config = new AdminAssistantChatDisplayModelConfig();
            config.setCustomModelName(customModelName);
            config.setActualModelName(providerModel.getModelName());
            config.setDescription(normalizeNullableText(request.getDescription()));
            config.setSupportReasoning(isCapabilityEnabled(providerModel.getSupportReasoning()));
            config.setSupportVision(isCapabilityEnabled(providerModel.getSupportVision()));
            configs.add(config);
        }
        return List.copyOf(configs);
    }

    /**
     * 将知识库实体转换为下拉选项视图对象。
     *
     * @param kbBase 知识库实体
     * @return 知识库下拉选项视图对象
     */
    private KnowledgeBaseOptionVo toKnowledgeBaseOptionVo(KbBase kbBase) {
        KnowledgeBaseOptionVo vo = new KnowledgeBaseOptionVo();
        vo.setKnowledgeName(kbBase.getKnowledgeName());
        vo.setDisplayName(kbBase.getDisplayName());
        vo.setEmbeddingModel(kbBase.getEmbeddingModel());
        vo.setEmbeddingDim(kbBase.getEmbeddingDim());
        return vo;
    }

    /**
     * 解析必填模型槽位配置。
     *
     * @param provider                启用提供商
     * @param request                 槽位请求
     * @param modelType               期望模型类型
     * @param visionRequired          是否要求支持图片理解
     * @param modelMissingMessageForm 模型不存在提示模板
     * @return 运行时槽位配置
     */
    private AgentModelSlotConfig resolveRequiredSlotConfig(LlmProvider provider,
                                                           AgentModelSelectionRequest request,
                                                           String modelType,
                                                           boolean visionRequired,
                                                           String modelMissingMessageForm) {
        Assert.notNull(request, "模型配置不能为空");
        return resolveSlotConfig(provider, request, modelType, visionRequired, modelMissingMessageForm);
    }

    /**
     * 解析客户端助手必填模型槽位配置。
     * <p>
     * 客户端助手系统配置阶段仅保存模型绑定，不在该链路中持久化深度思考开关。
     *
     * @param provider 启用提供商
     * @param request  客户端助手槽位请求
     * @return 运行时槽位配置
     */
    private AgentModelSlotConfig resolveRequiredClientAssistantSlotConfig(LlmProvider provider,
                                                                          ClientAssistantModelSelectionRequest request) {
        Assert.notNull(request, "模型配置不能为空");
        String modelName = normalizeNullableText(request.getModelName());
        Assert.notEmpty(modelName, "模型名称不能为空");

        LlmProviderModel providerModel = getProviderModel(provider, modelName, LlmModelTypeConstants.CHAT,
                CHAT_MODEL_MISSING_MESSAGE);
        validateProviderModelEnabled(providerModel);

        AgentModelSlotConfig slotConfig = new AgentModelSlotConfig();
        slotConfig.setModelName(providerModel.getModelName());
        slotConfig.setReasoningEnabled(false);
        slotConfig.setSupportReasoning(isCapabilityEnabled(providerModel.getSupportReasoning()));
        slotConfig.setSupportVision(isCapabilityEnabled(providerModel.getSupportVision()));
        return slotConfig;
    }

    /**
     * 校验客户端统一深度思考开关是否满足开启条件。
     *
     * @param reasoningEnabled   统一深度思考开关状态
     * @param serviceNodeModel   服务节点模型槽位配置
     * @param diagnosisNodeModel 诊断节点模型槽位配置
     */
    private void validateClientAssistantReasoningToggle(Boolean reasoningEnabled,
                                                        AgentModelSlotConfig serviceNodeModel,
                                                        AgentModelSlotConfig diagnosisNodeModel) {
        if (!Boolean.TRUE.equals(reasoningEnabled)) {
            return;
        }
        boolean supportsReasoning = Boolean.TRUE.equals(serviceNodeModel.getSupportReasoning())
                && Boolean.TRUE.equals(diagnosisNodeModel.getSupportReasoning());
        Assert.isParamTrue(supportsReasoning, CLIENT_ASSISTANT_REASONING_UNAVAILABLE_MESSAGE);
    }

    /**
     * 解析知识库排序模型名称。
     * <p>
     * 当排序关闭时校验不能传入排序模型；当排序开启时校验排序模型必填且必须是可用重排模型。
     *
     * @param provider 启用的模型提供商
     * @param request  知识库配置请求
     * @return 排序模型名称；关闭排序时返回 null
     */
    private String resolveKnowledgeBaseRankingModel(LlmProvider provider,
                                                    KnowledgeBaseAgentConfigRequest request) {
        boolean rankingEnabled = Boolean.TRUE.equals(request.getRankingEnabled());
        AgentModelSelectionRequest rankingRequest = request.getRankingModel();
        if (!rankingEnabled) {
            Assert.isParamTrue(!hasSelectedModel(rankingRequest), KNOWLEDGE_BASE_RANKING_DISABLED_MESSAGE);
            return null;
        }
        Assert.isParamTrue(hasSelectedModel(rankingRequest), KNOWLEDGE_BASE_RANKING_REQUIRED_MESSAGE);
        return resolveSlotConfig(provider, rankingRequest, LlmModelTypeConstants.RERANK, false,
                RANKING_MODEL_MISSING_MESSAGE).getModelName();
    }

    /**
     * 解析单个模型槽位的最终运行时配置。
     *
     * @param provider                启用提供商
     * @param request                 槽位请求
     * @param modelType               期望模型类型
     * @param visionRequired          是否要求支持图片理解
     * @param modelMissingMessageForm 模型不存在提示模板
     * @return 运行时槽位配置
     */
    private AgentModelSlotConfig resolveSlotConfig(LlmProvider provider,
                                                   AgentModelSelectionRequest request,
                                                   String modelType,
                                                   boolean visionRequired,
                                                   String modelMissingMessageForm) {
        String modelName = normalizeRequiredModelName(request);
        LlmProviderModel providerModel = getProviderModel(provider, modelName, modelType, modelMissingMessageForm);
        validateProviderModelEnabled(providerModel);
        validateReasoningCapability(request, providerModel);
        validateVisionCapability(modelName, providerModel, visionRequired);
        return buildSlotConfig(providerModel, request);
    }

    /**
     * 构建知识库运行时配置。
     * <p>
     * 启用状态下会完成知识库、向量模型、向量维度和排序模型的完整校验；禁用状态下转为保留历史配置的轻量更新。
     *
     * @param existingConfig 现有知识库运行时配置
     * @param request        知识库编辑态请求
     * @param enabled        是否启用知识库能力
     * @return 最终写入缓存的知识库运行时配置
     */
    private KnowledgeBaseAgentConfig buildKnowledgeBaseConfig(KnowledgeBaseAgentConfig existingConfig,
                                                              KnowledgeBaseAgentConfigRequest request,
                                                              boolean enabled) {
        if (!enabled) {
            return buildDisabledKnowledgeBaseConfig(existingConfig, request);
        }

        validateEmbeddingDim(request.getEmbeddingDim());

        List<String> knowledgeNames = normalizeKnowledgeNames(request.getKnowledgeNames());
        Integer topK = normalizeKnowledgeBaseTopK(request.getTopK());
        LlmProvider provider = getRequiredEnabledProvider();
        List<KbBase> knowledgeBases = loadEnabledKnowledgeBases(knowledgeNames);
        KbBase baseline = knowledgeBases.getFirst();
        AgentModelSlotConfig embeddingModel = resolveRequiredSlotConfig(provider, request.getEmbeddingModel(),
                LlmModelTypeConstants.EMBEDDING, false, EMBEDDING_MODEL_MISSING_MESSAGE);
        validateKnowledgeBasesAgainstBaseline(knowledgeBases, baseline);
        validateKnowledgeBaseCommonConfig(embeddingModel, request.getEmbeddingDim(), baseline);
        String rankingModel = resolveKnowledgeBaseRankingModel(provider, request);

        KnowledgeBaseAgentConfig config = new KnowledgeBaseAgentConfig();
        config.setEnabled(Boolean.TRUE);
        config.setKnowledgeNames(knowledgeNames);
        config.setEmbeddingDim(request.getEmbeddingDim());
        config.setTopK(topK);
        config.setEmbeddingModel(embeddingModel.getModelName());
        config.setRankingEnabled(Boolean.TRUE.equals(request.getRankingEnabled()));
        config.setRankingModel(rankingModel);
        return config;
    }

    /**
     * 构建禁用状态下的知识库运行时配置。
     * <p>
     * 该方法以现有配置为基准复制出新对象，并按请求覆盖允许更新的字段，同时强制将 enabled 置为 false。
     *
     * @param existingConfig 现有知识库运行时配置
     * @param request        知识库编辑态请求
     * @return 禁用状态下的知识库运行时配置
     */
    private KnowledgeBaseAgentConfig buildDisabledKnowledgeBaseConfig(KnowledgeBaseAgentConfig existingConfig,
                                                                      KnowledgeBaseAgentConfigRequest request) {
        KnowledgeBaseAgentConfig config = copyKnowledgeBaseConfig(existingConfig);
        config.setEnabled(Boolean.FALSE);
        if (request.getKnowledgeNames() != null) {
            config.setKnowledgeNames(copyKnowledgeNames(request.getKnowledgeNames()));
        }
        if (request.getEmbeddingDim() != null || request.getEmbeddingModel() != null || request.getTopK() != null
                || request.getRankingEnabled() != null || request.getRankingModel() != null) {
            config.setEmbeddingDim(request.getEmbeddingDim());
            config.setTopK(normalizeKnowledgeBaseTopK(request.getTopK()));
            config.setEmbeddingModel(normalizeOptionalModelName(request.getEmbeddingModel()));
            config.setRankingEnabled(request.getRankingEnabled());
            config.setRankingModel(normalizeOptionalModelName(request.getRankingModel()));
        }
        return config;
    }

    /**
     * 拷贝知识库运行时配置，避免直接修改原对象。
     *
     * @param existingConfig 原始知识库运行时配置
     * @return 拷贝后的知识库运行时配置；原配置为空时返回空对象
     */
    private KnowledgeBaseAgentConfig copyKnowledgeBaseConfig(KnowledgeBaseAgentConfig existingConfig) {
        KnowledgeBaseAgentConfig config = new KnowledgeBaseAgentConfig();
        if (existingConfig == null) {
            return config;
        }
        config.setEnabled(existingConfig.getEnabled());
        config.setKnowledgeNames(copyKnowledgeNames(existingConfig.getKnowledgeNames()));
        config.setEmbeddingDim(existingConfig.getEmbeddingDim());
        config.setTopK(existingConfig.getTopK());
        config.setEmbeddingModel(existingConfig.getEmbeddingModel());
        config.setRankingEnabled(existingConfig.getRankingEnabled());
        config.setRankingModel(existingConfig.getRankingModel());
        return config;
    }

    /**
     * 将知识库运行时配置转换为前端视图对象。
     *
     * @param config 知识库运行时配置
     * @return 知识库前端视图对象
     */
    private KnowledgeBaseAgentConfigVo toKnowledgeBaseConfigVo(KnowledgeBaseAgentConfig config) {
        KnowledgeBaseAgentConfigVo vo = new KnowledgeBaseAgentConfigVo();
        if (config == null) {
            vo.setEnabled(Boolean.FALSE);
            return vo;
        }
        LlmProvider provider = getEnabledProviderOrNull();
        vo.setEnabled(resolveKnowledgeBaseEnabled(config));
        vo.setKnowledgeNames(copyKnowledgeNames(config.getKnowledgeNames()));
        vo.setEmbeddingDim(config.getEmbeddingDim());
        vo.setTopK(config.getTopK());
        vo.setEmbeddingModel(toKnowledgeBaseModelSelectionVo(provider, config.getEmbeddingModel(),
                LlmModelTypeConstants.EMBEDDING));
        boolean rankingEnabled = resolveKnowledgeBaseRankingEnabled(config);
        vo.setRankingEnabled(rankingEnabled);
        vo.setRankingModel(rankingEnabled
                ? toKnowledgeBaseModelSelectionVo(provider, config.getRankingModel(), LlmModelTypeConstants.RERANK)
                : null);
        return vo;
    }

    /**
     * 保存指定槽位上的知识库配置。
     *
     * @param request            知识库编辑态请求
     * @param existingConfigFunc 读取当前知识库配置的函数
     * @param configSetter       写入知识库配置的函数
     * @return 是否保存成功
     */
    private boolean saveKnowledgeBaseConfig(KnowledgeBaseAgentConfigRequest request,
                                            Function<AgentAllConfigCache, KnowledgeBaseAgentConfig> existingConfigFunc,
                                            BiConsumer<AgentAllConfigCache, KnowledgeBaseAgentConfig> configSetter) {
        Assert.notNull(request, "知识库Agent配置不能为空");
        AgentAllConfigCache cache = agentConfigRuntimeSyncService.readCache();
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        KnowledgeBaseAgentConfig config = buildKnowledgeBaseConfig(existingConfigFunc.apply(cache), request, enabled);
        configSetter.accept(cache, config);
        agentConfigRuntimeSyncService.saveCache(cache, enabled ? getRequiredEnabledProvider() : getEnabledProviderOrNull(),
                currentOperator());
        return true;
    }

    /**
     * 构建知识库下拉选项列表。
     *
     * @return 知识库下拉选项列表
     */
    private List<KnowledgeBaseOptionVo> buildKnowledgeBaseOptions() {
        return kbBaseService.listEnabledKnowledgeBases().stream()
                .map(this::toKnowledgeBaseOptionVo)
                .toList();
    }

    /**
     * 按提供商、名称和类型查询模型。
     *
     * @param provider                启用提供商
     * @param modelName               模型名称
     * @param modelType               模型类型
     * @param modelMissingMessageForm 模型不存在提示模板
     * @return 提供商模型实体
     */
    private LlmProviderModel getProviderModel(LlmProvider provider,
                                              String modelName,
                                              String modelType,
                                              String modelMissingMessageForm) {
        List<LlmProviderModel> models = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, provider.getId())
                .eq(LlmProviderModel::getModelType, modelType)
                .eq(LlmProviderModel::getModelName, modelName)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId)
                .list();
        if (models.isEmpty()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, modelMissingMessageForm.formatted(modelName));
        }
        return models.getFirst();
    }

    /**
     * 校验模型是否启用。
     *
     * @param providerModel 提供商模型实体
     */
    private void validateProviderModelEnabled(LlmProviderModel providerModel) {
        if (providerModel.getEnabled() == null || providerModel.getEnabled() != MODEL_STATUS_ENABLED) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    MODEL_DISABLED_MESSAGE.formatted(providerModel.getModelName()));
        }
    }

    /**
     * 校验模型是否支持深度思考。
     *
     * @param request       模型选择请求
     * @param providerModel 提供商模型实体
     */
    private void validateReasoningCapability(AgentModelSelectionRequest request, LlmProviderModel providerModel) {
        if (Boolean.TRUE.equals(request.getReasoningEnabled())
                && !isCapabilityEnabled(providerModel.getSupportReasoning())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    REASONING_UNSUPPORTED_MESSAGE.formatted(providerModel.getModelName()));
        }
    }

    /**
     * 校验模型是否支持图片理解。
     *
     * @param modelName      模型名称
     * @param providerModel  提供商模型实体
     * @param visionRequired 是否要求图片理解能力
     */
    private void validateVisionCapability(String modelName, LlmProviderModel providerModel, boolean visionRequired) {
        if (visionRequired && !isCapabilityEnabled(providerModel.getSupportVision())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, VISION_UNSUPPORTED_MESSAGE.formatted(modelName));
        }
    }

    /**
     * 构建运行时槽位配置。
     *
     * @param providerModel 提供商模型实体
     * @param request       编辑态请求
     * @return 运行时槽位配置
     */
    private AgentModelSlotConfig buildSlotConfig(LlmProviderModel providerModel,
                                                 AgentModelSelectionRequest request) {
        AgentModelSlotConfig slotConfig = new AgentModelSlotConfig();
        slotConfig.setModelName(providerModel.getModelName());
        slotConfig.setReasoningEnabled(request.getReasoningEnabled());
        slotConfig.setSupportReasoning(isCapabilityEnabled(providerModel.getSupportReasoning()));
        slotConfig.setSupportVision(isCapabilityEnabled(providerModel.getSupportVision()));
        return slotConfig;
    }

    /**
     * 归一化知识库名称列表，并校验非空、数量上限与重复项。
     *
     * @param knowledgeNames 原始知识库名称列表
     * @return 归一化后的知识库名称列表
     */
    private List<String> normalizeKnowledgeNames(List<String> knowledgeNames) {
        Assert.notEmpty(knowledgeNames, "知识库名称列表不能为空");
        Assert.isParamTrue(knowledgeNames.size() <= KNOWLEDGE_BASE_MAX_COUNT, "知识库最多支持5个");

        List<String> normalizedNames = new ArrayList<>(knowledgeNames.size());
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (String knowledgeName : knowledgeNames) {
            String normalizedName = normalizeNullableText(knowledgeName);
            Assert.notEmpty(normalizedName, KNOWLEDGE_BASE_NAME_REQUIRED_MESSAGE);
            Assert.isParamTrue(uniqueNames.add(normalizedName),
                    KNOWLEDGE_BASE_DUPLICATE_MESSAGE.formatted(normalizedName));
            normalizedNames.add(normalizedName);
        }
        return normalizedNames;
    }

    /**
     * 按名称加载启用中的知识库，并保持与请求一致的顺序。
     *
     * @param knowledgeNames 知识库名称列表
     * @return 启用中的知识库列表
     */
    private List<KbBase> loadEnabledKnowledgeBases(List<String> knowledgeNames) {
        List<KbBase> knowledgeBases = kbBaseService.listEnabledKnowledgeBasesByNames(knowledgeNames);
        Map<String, KbBase> knowledgeBaseMap = knowledgeBases.stream()
                .collect(Collectors.toMap(KbBase::getKnowledgeName, Function.identity()));
        List<KbBase> orderedKnowledgeBases = new ArrayList<>(knowledgeNames.size());
        for (String knowledgeName : knowledgeNames) {
            KbBase kbBase = knowledgeBaseMap.get(knowledgeName);
            if (kbBase == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_BASE_NOT_FOUND_MESSAGE.formatted(knowledgeName));
            }
            orderedKnowledgeBases.add(kbBase);
        }
        return orderedKnowledgeBases;
    }

    /**
     * 校验多个知识库的公共向量配置是否与基准知识库一致。
     *
     * @param knowledgeBases 待校验的知识库列表
     * @param baseline       作为基准的第一个知识库
     */
    private void validateKnowledgeBasesAgainstBaseline(List<KbBase> knowledgeBases, KbBase baseline) {
        for (int index = 1; index < knowledgeBases.size(); index++) {
            KbBase kbBase = knowledgeBases.get(index);
            if (!Objects.equals(normalizeNullableText(kbBase.getEmbeddingModel()),
                    normalizeNullableText(baseline.getEmbeddingModel()))) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_BASE_MODEL_MISMATCH_MESSAGE.formatted(kbBase.getKnowledgeName()));
            }
            if (!Objects.equals(kbBase.getEmbeddingDim(), baseline.getEmbeddingDim())) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        KNOWLEDGE_BASE_DIM_MISMATCH_MESSAGE.formatted(kbBase.getKnowledgeName()));
            }
        }
    }

    /**
     * 校验请求中的知识库公共配置是否与基准知识库一致。
     *
     * @param embeddingModel 请求中的向量模型配置
     * @param embeddingDim   请求中的向量维度
     * @param baseline       作为基准的知识库
     */
    private void validateKnowledgeBaseCommonConfig(AgentModelSlotConfig embeddingModel, Integer embeddingDim, KbBase baseline) {
        if (!Objects.equals(normalizeNullableText(baseline.getEmbeddingModel()), embeddingModel.getModelName())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, KNOWLEDGE_BASE_CONFIG_MODEL_MISMATCH_MESSAGE);
        }
        if (!Objects.equals(baseline.getEmbeddingDim(), embeddingDim)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, KNOWLEDGE_BASE_CONFIG_DIM_MISMATCH_MESSAGE);
        }
    }

    /**
     * 校验知识库向量维度是否属于支持集合。
     *
     * @param embeddingDim 向量维度
     */
    private void validateEmbeddingDim(Integer embeddingDim) {
        Assert.notNull(embeddingDim, "向量维度不能为空");
        Assert.isParamTrue(KnowledgeBaseEmbeddingDimSupport.isSupported(embeddingDim),
                KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE);
    }

    /**
     * 归一化知识库召回条数。
     * <p>
     * 当值为 null 或 0 时视为未配置；其余值会校验范围后原样返回。
     *
     * @param topK 原始召回条数
     * @return 归一化后的召回条数；未配置时返回 null
     */
    private Integer normalizeKnowledgeBaseTopK(Integer topK) {
        if (topK == null || topK == 0) {
            return null;
        }
        Assert.isParamTrue(topK >= KNOWLEDGE_BASE_TOP_K_MIN, KNOWLEDGE_BASE_TOP_K_MIN_MESSAGE);
        Assert.isParamTrue(topK <= KNOWLEDGE_BASE_TOP_K_MAX, KNOWLEDGE_BASE_TOP_K_MAX_MESSAGE);
        return topK;
    }

    /**
     * 校验豆包语音请求字段与文本长度范围。
     *
     * @param request        豆包语音配置请求
     * @param existingConfig 现有豆包语音配置
     */
    private void validateSpeechRequest(SpeechAgentConfigRequest request, SpeechAgentConfig existingConfig) {
        Assert.notEmpty(normalizeNullableText(request.getAppId()), SPEECH_APP_ID_REQUIRED_MESSAGE);

        TextToSpeechConfigRequest textToSpeech = request.getTextToSpeech();
        Assert.notNull(textToSpeech, SPEECH_TTS_REQUIRED_MESSAGE);
        Assert.notEmpty(normalizeNullableText(textToSpeech.getVoiceType()), SPEECH_TTS_VOICE_TYPE_REQUIRED_MESSAGE);

        Integer maxTextChars = textToSpeech.getMaxTextChars();
        Assert.notNull(maxTextChars, SPEECH_TTS_MAX_TEXT_CHARS_REQUIRED_MESSAGE);
        Assert.isParamTrue(maxTextChars >= SPEECH_MAX_TEXT_CHARS_MIN, SPEECH_TTS_MAX_TEXT_CHARS_MIN_MESSAGE);
        Assert.isParamTrue(maxTextChars <= SPEECH_MAX_TEXT_CHARS_MAX, SPEECH_TTS_MAX_TEXT_CHARS_MAX_MESSAGE);

        resolveSpeechAccessToken(existingConfig, request.getAccessToken());
    }

    /**
     * 解析知识库配置的启用状态。
     * <p>
     * 优先使用显式 enabled 值；未显式设置时，根据是否存在任意知识库配置项推断是否视为启用。
     *
     * @param config 知识库运行时配置
     * @return 是否启用知识库能力
     */
    private boolean resolveKnowledgeBaseEnabled(KnowledgeBaseAgentConfig config) {
        if (config.getEnabled() != null) {
            return config.getEnabled();
        }
        return !copyKnowledgeNames(config.getKnowledgeNames()).isEmpty()
                || config.getEmbeddingDim() != null
                || config.getTopK() != null
                || StringUtils.hasText(config.getEmbeddingModel())
                || config.getRankingEnabled() != null
                || StringUtils.hasText(config.getRankingModel());
    }

    /**
     * 解析知识库排序能力的启用状态。
     * <p>
     * 优先使用显式 rankingEnabled 值；未显式设置时，根据是否配置了排序模型推断。
     *
     * @param config 知识库运行时配置
     * @return 是否启用排序能力
     */
    private boolean resolveKnowledgeBaseRankingEnabled(KnowledgeBaseAgentConfig config) {
        if (config.getRankingEnabled() != null) {
            return config.getRankingEnabled();
        }
        return StringUtils.hasText(config.getRankingModel());
    }

    /**
     * 复制知识库名称列表，避免外部修改内部集合。
     *
     * @param knowledgeNames 原始知识库名称列表
     * @return 不可变的知识库名称列表；为空时返回空列表
     */
    private List<String> copyKnowledgeNames(List<String> knowledgeNames) {
        if (knowledgeNames == null || knowledgeNames.isEmpty()) {
            return List.of();
        }
        return List.copyOf(knowledgeNames);
    }

    /**
     * 构建豆包语音运行时配置。
     *
     * @param request        豆包语音编辑态请求
     * @param existingConfig 现有豆包语音运行时配置
     * @return 豆包语音运行时配置
     */
    private SpeechAgentConfig buildSpeechConfig(SpeechAgentConfigRequest request, SpeechAgentConfig existingConfig) {
        SpeechAgentConfig config = new SpeechAgentConfig();
        config.setProvider(SPEECH_PROVIDER);
        config.setAppId(normalizeNullableText(request.getAppId()));
        config.setAccessToken(resolveSpeechAccessToken(existingConfig, request.getAccessToken()));
        config.setSpeechRecognition(buildSpeechRecognitionConfig());
        config.setTextToSpeech(buildTextToSpeechConfig(request.getTextToSpeech()));
        return config;
    }

    /**
     * 构建语音识别运行时配置。
     * <p>
     * STT ResourceId 固定写入 `volc.seedasr.sauc.duration`。
     *
     * @return 语音识别运行时配置
     */
    private SpeechRecognitionAgentConfig buildSpeechRecognitionConfig() {
        SpeechRecognitionAgentConfig config = new SpeechRecognitionAgentConfig();
        config.setResourceId(VOLCENGINE_STT_RESOURCE_ID);
        return config;
    }

    /**
     * 构建语音合成运行时配置。
     * <p>
     * TTS ResourceId 固定写入 `seed-tts-2.0`。
     *
     * @param request 语音合成编辑态请求
     * @return 语音合成运行时配置
     */
    private TextToSpeechAgentConfig buildTextToSpeechConfig(TextToSpeechConfigRequest request) {
        TextToSpeechAgentConfig config = new TextToSpeechAgentConfig();
        config.setResourceId(VOLCENGINE_TTS_RESOURCE_ID);
        config.setVoiceType(normalizeNullableText(request.getVoiceType()));
        config.setMaxTextChars(request.getMaxTextChars());
        return config;
    }

    /**
     * 解析本次应写入 Redis 的语音访问令牌。
     *
     * @param existingConfig 现有豆包语音配置
     * @param accessToken    本次请求中的访问令牌
     * @return 最终写入的访问令牌
     */
    private String resolveSpeechAccessToken(SpeechAgentConfig existingConfig, String accessToken) {
        String normalizedToken = normalizeNullableText(accessToken);
        if (normalizedToken != null) {
            return normalizedToken;
        }
        String existingToken = existingConfig == null ? null : normalizeNullableText(existingConfig.getAccessToken());
        Assert.notEmpty(existingToken, SPEECH_ACCESS_TOKEN_REQUIRED_MESSAGE);
        return existingToken;
    }

    /**
     * 查询当前启用提供商，不存在时返回 null。
     *
     * @return 启用提供商；不存在时返回 null
     */
    private LlmProvider getEnabledProviderOrNull() {
        List<LlmProvider> providers = llmProviderService.lambdaQuery()
                .eq(LlmProvider::getStatus, PROVIDER_STATUS_ENABLED)
                .orderByAsc(LlmProvider::getSort, LlmProvider::getId)
                .list();
        return providers.isEmpty() ? null : providers.getFirst();
    }

    /**
     * 查询当前启用提供商，不存在时抛出异常。
     *
     * @return 启用提供商
     */
    private LlmProvider getRequiredEnabledProvider() {
        LlmProvider provider = getEnabledProviderOrNull();
        if (provider == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, ENABLED_PROVIDER_MISSING_MESSAGE);
        }
        return provider;
    }

    /**
     * 判断请求中是否已经选择了模型。
     *
     * @param request 模型选择请求
     * @return true 表示已选择模型名称
     */
    private boolean hasSelectedModel(AgentModelSelectionRequest request) {
        return request != null && StringUtils.hasText(normalizeNullableText(request.getModelName()));
    }

    /**
     * 归一化可选模型名称。
     *
     * @param request 模型选择请求
     * @return 归一化后的模型名称；未选择时返回 null
     */
    private String normalizeOptionalModelName(AgentModelSelectionRequest request) {
        if (request == null) {
            return null;
        }
        return normalizeNullableText(request.getModelName());
    }

    /**
     * 查询当前启用提供商下的启用模型列表。
     *
     * @param providerId     提供商ID
     * @param modelType      模型类型
     * @param visionRequired 是否要求支持图片理解
     * @return 启用模型列表
     */
    private List<LlmProviderModel> listEnabledProviderModels(Long providerId, String modelType, boolean visionRequired) {
        LambdaQueryChainWrapper<LlmProviderModel> wrapper = llmProviderModelService.lambdaQuery()
                .eq(LlmProviderModel::getProviderId, providerId)
                .eq(LlmProviderModel::getModelType, modelType)
                .eq(LlmProviderModel::getEnabled, MODEL_STATUS_ENABLED)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId);
        if (visionRequired) {
            wrapper.eq(LlmProviderModel::getSupportVision, CAPABILITY_ENABLED);
        }
        return wrapper.list();
    }

    /**
     * 归一化并返回必填模型名称。
     *
     * @param request 模型选择请求
     * @return 模型名称
     */
    private String normalizeRequiredModelName(AgentModelSelectionRequest request) {
        String modelName = normalizeNullableText(request.getModelName());
        Assert.notEmpty(modelName, "模型名称不能为空");
        return modelName;
    }

    /**
     * 判断能力开关是否已启用。
     *
     * @param capability 能力值
     * @return 是否启用
     */
    private boolean isCapabilityEnabled(Integer capability) {
        return capability != null && capability == CAPABILITY_ENABLED;
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String currentOperator() {
        try {
            String username = normalizeNullableText(getUsername());
            return username != null ? username : DEFAULT_OPERATOR;
        } catch (RuntimeException ex) {
            return DEFAULT_OPERATOR;
        }
    }

    /**
     * 归一化可空文本。
     *
     * @param value 原始文本
     * @return 去首尾空白后的文本；为空时返回 null
     */
    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
