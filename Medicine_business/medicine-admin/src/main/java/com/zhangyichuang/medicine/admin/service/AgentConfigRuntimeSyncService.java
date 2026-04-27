package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderMapper;
import com.zhangyichuang.medicine.admin.mapper.LlmProviderModelMapper;
import com.zhangyichuang.medicine.admin.publisher.AgentConfigPublisher;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.model.cache.*;
import com.zhangyichuang.medicine.model.constants.LlmModelTypeConstants;
import com.zhangyichuang.medicine.model.constants.LlmProviderTypeConstants;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import com.zhangyichuang.medicine.model.mq.AgentConfigRefreshMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Agent 运行时 Redis 与 MQ 联动同步服务。
 * 负责管理模型提供商、模型槽位、知识库配置在 Redis 中的全量缓存，
 * 并通过 MQ 广播机制实现多实例间的配置刷新。
 */
@Service
@RequiredArgsConstructor
public class AgentConfigRuntimeSyncService {

    /**
     * Redis 存储 Agent 全量配置的 Key
     */
    private static final String REDIS_KEY = RedisConstants.AgentConfig.ALL_CONFIG_KEY;

    /**
     * 提供商启用状态码
     */
    private static final int PROVIDER_STATUS_ENABLED = 1;

    /**
     * 模型启用状态码（0为正常启用）
     */
    private static final int MODEL_STATUS_ENABLED = 0;

    /**
     * 模型能力（如深度思考、图片理解）启用标识
     */
    private static final int CAPABILITY_ENABLED = 1;

    /**
     * 默认操作人名称
     */
    private static final String DEFAULT_OPERATOR = "system";

    /**
     * MQ 刷新消息类型标识
     */
    private static final String AGENT_CONFIG_REFRESH_MESSAGE_TYPE = "agent_config_refresh";

    // 异常与校验提示语常量
    private static final String PROVIDER_TYPE_MISSING_MESSAGE = "当前启用的模型提供商未配置类型，请先在模型提供商中补充类型";
    private static final String PROVIDER_DISABLE_MESSAGE = "当前启用的提供商不允许停用，请先切换到其他提供商";
    private static final String PROVIDER_DELETE_MESSAGE = "当前启用的提供商不允许删除，请先切换到其他提供商";
    private static final String PROVIDER_SWITCH_MODEL_MISSING_MESSAGE = "切换失败，目标提供商下不存在模型：%s";
    private static final String KNOWLEDGE_BASE_DISABLE_MESSAGE = "当前知识库已被知识库Agent配置引用，请先移除后再停用";
    private static final String KNOWLEDGE_BASE_DELETE_MESSAGE = "当前知识库已被知识库Agent配置引用，请先移除后再删除";
    private static final String MODEL_DISABLED_MESSAGE = "模型未启用：%s";
    private static final String REASONING_UNSUPPORTED_MESSAGE = "模型不支持深度思考：%s";
    private static final String VISION_UNSUPPORTED_MESSAGE = "模型不支持图片理解：%s";

    /**
     * 知识库模型绑定关系列表。
     * 定义了知识库中不同类型模型（向量、排序）的获取与设置逻辑。
     */
    private static final List<KnowledgeBaseModelBinding> KNOWLEDGE_BASE_MODEL_BINDINGS = List.of(
            new KnowledgeBaseModelBinding(
                    LlmModelTypeConstants.EMBEDDING,
                    KnowledgeBaseAgentConfig::getEmbeddingModel,
                    KnowledgeBaseAgentConfig::setEmbeddingModel
            ),
            new KnowledgeBaseModelBinding(
                    LlmModelTypeConstants.RERANK,
                    KnowledgeBaseAgentConfig::getRankingModel,
                    AgentConfigRuntimeSyncService::setKnowledgeBaseRankingModel
            )
    );

    /**
     * Agent 模型槽位 (Slot) 绑定关系列表。
     * 维护了客户端助手与通用能力等业务 Agent 对特定能力模型（聊天、路由、识别等）的引用规则。
     */
    private static final List<SlotBinding> SLOT_BINDINGS = List.of(
            // 客户端助手相关槽位
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    false,
                    cache -> cache.getClientAssistant() == null ? null : cache.getClientAssistant().getRouteModel(),
                    (cache, slot) -> ensureClientAssistant(cache).setRouteModel(slot)
            ),
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    false,
                    cache -> cache.getClientAssistant() == null ? null : cache.getClientAssistant().getServiceNodeModel(),
                    (cache, slot) -> ensureClientAssistant(cache).setServiceNodeModel(slot)
            ),
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    false,
                    cache -> cache.getClientAssistant() == null ? null : cache.getClientAssistant().getDiagnosisNodeModel(),
                    (cache, slot) -> ensureClientAssistant(cache).setDiagnosisNodeModel(slot)
            ),
            // 多模态与通用能力槽位
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    true, // 必须支持图片理解
                    cache -> cache.getCommonCapability() == null ? null : cache.getCommonCapability().getImageRecognitionModel(),
                    (cache, slot) -> ensureCommonCapability(cache).setImageRecognitionModel(slot)
            ),
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    false,
                    cache -> cache.getCommonCapability() == null ? null : cache.getCommonCapability().getChatHistorySummaryModel(),
                    (cache, slot) -> ensureCommonCapability(cache).setChatHistorySummaryModel(slot)
            ),
            new SlotBinding(
                    LlmModelTypeConstants.CHAT,
                    false,
                    cache -> cache.getCommonCapability() == null ? null : cache.getCommonCapability().getChatTitleModel(),
                    (cache, slot) -> ensureCommonCapability(cache).setChatTitleModel(slot)
            )
    );

    private final RedisCache redisCache;
    private final AgentConfigPublisher agentConfigPublisher;
    private final LlmProviderMapper llmProviderMapper;
    private final LlmProviderModelMapper llmProviderModelMapper;

    /**
     * 确保缓存中存在知识库配置节点。
     */
    private static KnowledgeBaseAgentConfig ensureKnowledgeBase(AgentAllConfigCache cache) {
        KnowledgeBaseAgentConfig config = cache.getKnowledgeBase();
        if (config == null) {
            config = new KnowledgeBaseAgentConfig();
            cache.setKnowledgeBase(config);
        }
        return config;
    }

    /**
     * 返回当前缓存中的所有知识库配置节点。
     * <p>
     * 当前包括管理端知识库与客户端聊天知识库两套独立配置。
     *
     * @param cache Agent 全量缓存
     * @return 已存在的知识库配置节点列表
     */
    private static List<KnowledgeBaseAgentConfig> listKnowledgeBaseConfigs(AgentAllConfigCache cache) {
        if (cache == null) {
            return List.of();
        }
        return Stream.of(cache.getKnowledgeBase(), cache.getClientKnowledgeBase())
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 设置知识库配置的排序模型。
     * 如果模型名称为空或 null，此方法还会自动禁用排序功能 (rankingEnabled = false)。
     *
     * @param config    知识库 Agent 配置
     * @param modelName 要设置的排序模型名称，若为 null 则清除它
     */
    private static void setKnowledgeBaseRankingModel(KnowledgeBaseAgentConfig config, String modelName) {
        config.setRankingModel(modelName);
        if (!StringUtils.hasText(modelName)) {
            config.setRankingEnabled(false);
        }
    }

    /**
     * 确保缓存中存在管理端助手配置节点。
     */
    private static AdminAssistantAgentConfig ensureAdminAssistant(AgentAllConfigCache cache) {
        AdminAssistantAgentConfig config = cache.getAdminAssistant();
        if (config == null) {
            config = new AdminAssistantAgentConfig();
            cache.setAdminAssistant(config);
        }
        return config;
    }

    /**
     * 确保缓存中存在客户端助手配置节点。
     *
     * @param cache Agent 全量缓存
     * @return 客户端助手配置节点
     */
    private static ClientAssistantAgentConfig ensureClientAssistant(AgentAllConfigCache cache) {
        ClientAssistantAgentConfig config = cache.getClientAssistant();
        if (config == null) {
            config = new ClientAssistantAgentConfig();
            cache.setClientAssistant(config);
        }
        return config;
    }

    /**
     * 确保缓存中存在通用能力配置节点。
     */
    private static CommonCapabilityAgentConfig ensureCommonCapability(AgentAllConfigCache cache) {
        CommonCapabilityAgentConfig config = cache.getCommonCapability();
        if (config == null) {
            config = new CommonCapabilityAgentConfig();
            cache.setCommonCapability(config);
        }
        return config;
    }

    /**
     * 服务启动后立刻回写一次当前缓存结构，清理已废弃字段。
     * <p>
     * 当前主要用于将历史 Redis 快照中的 `adminNodeModel` 从序列化结果中移除，
     * 避免 Python 运行时升级到新快照结构后读取旧字段产生结构不一致。
     */
    @PostConstruct
    public void rewriteLegacyCacheStructure() {
        AgentAllConfigCache cache = readCache();
        redisCache.setCacheObject(REDIS_KEY, cache);
    }

    /**
     * 读取并规范化 Redis 中的 Agent 全量配置缓存。
     * 若缓存不存在，则初始化一个新的配置对象。
     *
     * @return Agent 全量配置缓存对象
     */
    public AgentAllConfigCache readCache() {
        AgentAllConfigCache cache = redisCache.getCacheObject(REDIS_KEY);
        return normalizeCache(cache == null ? new AgentAllConfigCache() : cache);
    }

    /**
     * 保存 Agent 配置到 Redis 缓存并广播刷新消息。
     * 该方法会同步更新配置的更新时间、更新人，并重新构建 LLM 提供商的基本连接信息。
     *
     * @param cache           要保存的最新缓存对象
     * @param enabledProvider 当前系统启用的 LLM 提供商
     * @param operator        执行操作的人员名称
     */
    public void saveCache(AgentAllConfigCache cache, LlmProvider enabledProvider, String operator) {
        AgentAllConfigCache normalizedCache = normalizeCache(cache == null ? new AgentAllConfigCache() : cache);
        normalizedCache.setLlm(buildLlmConfig(enabledProvider));
        normalizedCache.setUpdatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        normalizedCache.setUpdatedBy(normalizeOperator(operator));
        redisCache.setCacheObject(REDIS_KEY, normalizedCache);
        agentConfigPublisher.publishRefresh(buildRefreshMessage(normalizedCache));
    }

    /**
     * 当提供商启用状态发生变更时，同步刷新 Redis 中的 LLM 连接配置。
     * 仅当传入的提供商确实是启用状态时才执行同步。
     *
     * @param provider 变更后的提供商实体
     * @param operator 操作人
     */
    public void syncEnabledProviderChange(LlmProvider provider, String operator) {
        if (!isProviderEnabled(provider)) {
            return;
        }
        saveCache(readCache(), provider, operator);
    }

    /**
     * 强制同步数据库中当前启用提供商的最新快照到 Redis 缓存中。
     *
     * @param operator 操作人
     */
    public void syncCurrentEnabledProviderSnapshot(String operator) {
        saveCache(readCache(), getEnabledProviderOrNull(), operator);
    }

    /**
     * 在切换启用提供商后，重置除独立语音/多模态配置外的业务 Agent 模型配置。
     * 主要是为了防止旧提供商的模型配置在新提供商下失效。
     *
     * @param operator 操作人
     */
    public void clearAgentConfigsForProviderSwitch(String operator) {
        AgentAllConfigCache cache = readCache();
        cache.setAgentConfigs(new AgentConfigsCache());
        saveCache(cache, getEnabledProviderOrNull(), operator);
    }

    /**
     * 当当前启用的提供商下的模型列表（快照）发生整体变化时，调和 Redis 中的槽位引用。
     * 若槽位引用的模型在新的提供商模型列表中不存在或已禁用，则清空该槽位。
     *
     * @param provider 当前启用的提供商
     * @param operator 操作人
     */
    public void syncActiveProviderSnapshot(LlmProvider provider, String operator) {
        if (!isProviderEnabled(provider)) {
            return;
        }
        AgentAllConfigCache cache = readCache();
        List<LlmProviderModel> models = listProviderModels(provider.getId());
        reconcileSlotsWithModels(cache, models);
        saveCache(cache, provider, operator);
    }

    /**
     * 校验切换到目标提供商后的配置兼容性。
     * 确保目标提供商拥有当前 Redis 槽位中配置的所有同名同类型模型，且模型能力（如图片理解）满足槽位要求。
     *
     * @param provider 目标提供商
     * @throws ServiceException 若存在模型缺失或能力不匹配
     */
    public void validateProviderSwitchCompatibility(LlmProvider provider) {
        if (provider == null) {
            return;
        }
        AgentAllConfigCache cache = readCache();
        List<LlmProviderModel> models = listProviderModels(provider.getId());
        for (KnowledgeBaseAgentConfig knowledgeBase : listKnowledgeBaseConfigs(cache)) {
            validateKnowledgeBaseModelCompatibility(knowledgeBase, models);
        }
        for (SlotBinding binding : SLOT_BINDINGS) {
            AgentModelSlotConfig slot = binding.getter().apply(cache);
            if (!hasSelectedModel(slot)) {
                continue;
            }
            LlmProviderModel model = findMatchingModel(models, binding, slot.getModelName());
            if (model == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        PROVIDER_SWITCH_MODEL_MISSING_MESSAGE.formatted(slot.getModelName()));
            }
            validateModelUsableForSlot(slot, binding, model);
        }
    }

    /**
     * 校验指定提供商是否可以被停用。
     * 规则：如果该提供商是当前全局启用的提供商，则禁止停用。
     *
     * @param provider 提供商实体
     */
    public void assertProviderCanDisable(LlmProvider provider) {
        if (isProviderEnabled(provider)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PROVIDER_DISABLE_MESSAGE);
        }
    }

    /**
     * 校验指定提供商是否可以被删除。
     * 规则：如果该提供商是当前全局启用的提供商，则禁止删除。
     *
     * @param provider 提供商实体
     */
    public void assertProviderCanDelete(LlmProvider provider) {
        if (isProviderEnabled(provider)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PROVIDER_DELETE_MESSAGE);
        }
    }

    /**
     * 校验知识库是否可以被停用。
     * 规则：如果该知识库正在被知识库 Agent 配置引用，则禁止停用。
     *
     * @param knowledgeName 知识库业务名称
     */
    public void assertKnowledgeBaseCanDisable(String knowledgeName) {
        if (isKnowledgeBaseReferenced(knowledgeName)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, KNOWLEDGE_BASE_DISABLE_MESSAGE);
        }
    }

    /**
     * 校验知识库是否可以被删除。
     * 规则：如果该知识库正在被知识库 Agent 配置引用，则禁止删除。
     *
     * @param knowledgeName 知识库业务名称
     */
    public void assertKnowledgeBaseCanDelete(String knowledgeName) {
        if (isKnowledgeBaseReferenced(knowledgeName)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, KNOWLEDGE_BASE_DELETE_MESSAGE);
        }
    }

    /**
     * 在单个模型信息更新后，同步刷新 Redis 缓存中引用该模型的槽位。
     * 若模型名称变更，槽位名称会同步更新；若模型禁用或能力缺失，槽位引用会被清空。
     *
     * @param existing 更新前的模型快照
     * @param updated  更新后的模型快照
     * @param operator 操作人
     */
    public void syncAfterModelUpdate(LlmProviderModel existing, LlmProviderModel updated, String operator) {
        LlmProvider enabledProvider = getEnabledProviderOrNull();
        if (enabledProvider == null || existing == null || !Objects.equals(existing.getProviderId(), enabledProvider.getId())) {
            return;
        }

        AgentAllConfigCache cache = readCache();
        boolean changed = false;
        for (KnowledgeBaseAgentConfig knowledgeBase : listKnowledgeBaseConfigs(cache)) {
            changed |= syncKnowledgeBaseModelUpdate(knowledgeBase, existing, updated);
        }
        for (SlotBinding binding : SLOT_BINDINGS) {
            AgentModelSlotConfig slot = binding.getter().apply(cache);
            if (!matchesSlot(slot, binding, existing.getModelName(), existing.getModelType())) {
                continue;
            }
            if (shouldClearSlot(slot, binding, existing, updated)) {
                binding.setter().accept(cache, null);
                changed = true;
                continue;
            }
            if (!Objects.equals(slot.getModelName(), updated.getModelName())) {
                slot.setModelName(updated.getModelName());
                changed = true;
            }
            Boolean nextSupportReasoning = supportsReasoning(updated);
            if (!Objects.equals(slot.getSupportReasoning(), nextSupportReasoning)) {
                slot.setSupportReasoning(nextSupportReasoning);
                changed = true;
            }
            Boolean nextSupportVision = supportsVision(updated);
            if (!Objects.equals(slot.getSupportVision(), nextSupportVision)) {
                slot.setSupportVision(nextSupportVision);
                changed = true;
            }
            binding.setter().accept(cache, slot);
        }
        changed |= disableUnavailableClientAssistantReasoning(cache.getClientAssistant());
        changed |= syncAdminAssistantChatDisplayModelsAfterModelUpdate(cache, existing, updated);
        if (changed) {
            saveCache(cache, enabledProvider, operator);
        }
    }

    /**
     * 在单个模型被删除后，清理 Redis 缓存中所有引用该模型的槽位。
     *
     * @param existing 被删除的模型实体
     * @param operator 操作人
     */
    public void syncAfterModelDelete(LlmProviderModel existing, String operator) {
        LlmProvider enabledProvider = getEnabledProviderOrNull();
        if (enabledProvider == null || existing == null || !Objects.equals(existing.getProviderId(), enabledProvider.getId())) {
            return;
        }
        AgentAllConfigCache cache = readCache();
        boolean changed = false;
        for (KnowledgeBaseAgentConfig knowledgeBase : listKnowledgeBaseConfigs(cache)) {
            changed |= clearKnowledgeBaseModelReferences(knowledgeBase, existing.getModelName(), existing.getModelType());
        }
        changed |= clearSlotsReferencing(cache, existing.getModelName(), existing.getModelType());
        changed |= disableUnavailableClientAssistantReasoning(cache.getClientAssistant());
        changed |= clearAdminAssistantChatDisplayModels(cache, existing.getModelName(), existing.getModelType());
        if (changed) {
            saveCache(cache, enabledProvider, operator);
        }
    }

    /**
     * 规范化 Agent 缓存对象，确保业务配置节点不为空。
     */
    private AgentAllConfigCache normalizeCache(AgentAllConfigCache cache) {
        if (cache.getAgentConfigs() == null) {
            cache.setAgentConfigs(new AgentConfigsCache());
        }
        return cache;
    }

    /**
     * 根据数据库提供商实体构建 Redis 中的 LLM 连接配置。
     * 会校验提供商类型是否合法。
     */
    private AgentLlmConfig buildLlmConfig(LlmProvider provider) {
        if (provider == null) {
            return null;
        }
        String providerType = normalizeNullableText(provider.getProviderType());
        if (!StringUtils.hasText(providerType) || !LlmProviderTypeConstants.ALL.contains(providerType)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PROVIDER_TYPE_MISSING_MESSAGE);
        }
        AgentLlmConfig config = new AgentLlmConfig();
        config.setProviderType(providerType);
        config.setBaseUrl(normalizeNullableText(provider.getBaseUrl()));
        config.setApiKey(normalizeNullableText(provider.getApiKey()));
        return config;
    }

    /**
     * 构建用于广播的 Agent 配置刷新消息。
     */
    private AgentConfigRefreshMessage buildRefreshMessage(AgentAllConfigCache cache) {
        return AgentConfigRefreshMessage.builder()
                .message_type(AGENT_CONFIG_REFRESH_MESSAGE_TYPE)
                .redis_key(REDIS_KEY)
                .updated_at(cache.getUpdatedAt())
                .updated_by(cache.getUpdatedBy())
                .created_at(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    /**
     * 调和全量槽位配置与当前可用的模型列表。
     */
    private void reconcileSlotsWithModels(AgentAllConfigCache cache, List<LlmProviderModel> models) {
        for (KnowledgeBaseAgentConfig knowledgeBase : listKnowledgeBaseConfigs(cache)) {
            reconcileKnowledgeBaseModelReferences(knowledgeBase, models);
        }
        for (SlotBinding binding : SLOT_BINDINGS) {
            AgentModelSlotConfig slot = binding.getter().apply(cache);
            if (!hasSelectedModel(slot)) {
                continue;
            }
            LlmProviderModel model = findMatchingModel(models, binding, slot.getModelName());
            if (!isModelUsableForSlot(slot, binding, model)) {
                binding.setter().accept(cache, null);
                continue;
            }
            Boolean nextSupportReasoning = supportsReasoning(model);
            if (!Objects.equals(slot.getSupportReasoning(), nextSupportReasoning)) {
                slot.setSupportReasoning(nextSupportReasoning);
                binding.setter().accept(cache, slot);
            }
            Boolean nextSupportVision = supportsVision(model);
            if (!Objects.equals(slot.getSupportVision(), nextSupportVision)) {
                slot.setSupportVision(nextSupportVision);
                binding.setter().accept(cache, slot);
            }
        }
        disableUnavailableClientAssistantReasoning(cache.getClientAssistant());
        reconcileAdminAssistantChatDisplayModels(cache, models);
    }

    /**
     * 当客户端统一深度思考开关已开启，但当前模型能力不再满足条件时自动关闭。
     *
     * @param clientAssistant 客户端助手配置
     * @return 是否发生变更
     */
    private boolean disableUnavailableClientAssistantReasoning(ClientAssistantAgentConfig clientAssistant) {
        if (clientAssistant == null || !Boolean.TRUE.equals(clientAssistant.getReasoningEnabled())) {
            return false;
        }
        AgentModelSlotConfig serviceNodeModel = clientAssistant.getServiceNodeModel();
        AgentModelSlotConfig diagnosisNodeModel = clientAssistant.getDiagnosisNodeModel();
        boolean supportsReasoning = serviceNodeModel != null
                && diagnosisNodeModel != null
                && Boolean.TRUE.equals(serviceNodeModel.getSupportReasoning())
                && Boolean.TRUE.equals(diagnosisNodeModel.getSupportReasoning());
        if (supportsReasoning) {
            return false;
        }
        clientAssistant.setReasoningEnabled(false);
        return true;
    }

    /**
     * 调和管理端聊天展示模型映射，移除失效项并刷新模型能力信息。
     *
     * @param cache  Agent 全量配置缓存
     * @param models 当前启用提供商下的全部模型列表
     */
    private void reconcileAdminAssistantChatDisplayModels(AgentAllConfigCache cache, List<LlmProviderModel> models) {
        AdminAssistantAgentConfig adminAssistant = cache.getAdminAssistant();
        if (adminAssistant == null || adminAssistant.getChatDisplayModels() == null) {
            return;
        }

        List<AdminAssistantChatDisplayModelConfig> nextConfigs = new ArrayList<>();
        for (AdminAssistantChatDisplayModelConfig config : adminAssistant.getChatDisplayModels()) {
            if (config == null || !StringUtils.hasText(config.getActualModelName())) {
                continue;
            }
            LlmProviderModel model = findMatchingModel(models, LlmModelTypeConstants.CHAT, config.getActualModelName());
            if (model == null || !isModelEnabled(model)) {
                continue;
            }
            config.setSupportReasoning(supportsReasoning(model));
            config.setSupportVision(supportsVision(model));
            nextConfigs.add(config);
        }
        adminAssistant.setChatDisplayModels(nextConfigs.isEmpty() ? List.of() : List.copyOf(nextConfigs));
    }

    /**
     * 清理所有引用了特定模型名称和类型的槽位配置。
     */
    private boolean clearSlotsReferencing(AgentAllConfigCache cache, String modelName, String modelType) {
        boolean changed = false;
        for (SlotBinding binding : SLOT_BINDINGS) {
            AgentModelSlotConfig slot = binding.getter().apply(cache);
            if (!matchesSlot(slot, binding, modelName, modelType)) {
                continue;
            }
            binding.setter().accept(cache, null);
            changed = true;
        }
        return changed;
    }

    /**
     * 在模型更新后同步刷新管理端聊天展示模型映射。
     *
     * @param cache    Agent 全量配置缓存
     * @param existing 更新前的模型快照
     * @param updated  更新后的模型快照
     * @return 是否发生变化
     */
    private boolean syncAdminAssistantChatDisplayModelsAfterModelUpdate(AgentAllConfigCache cache,
                                                                        LlmProviderModel existing,
                                                                        LlmProviderModel updated) {
        AdminAssistantAgentConfig adminAssistant = cache.getAdminAssistant();
        if (adminAssistant == null || adminAssistant.getChatDisplayModels() == null
                || adminAssistant.getChatDisplayModels().isEmpty()) {
            return false;
        }

        boolean changed = false;
        List<AdminAssistantChatDisplayModelConfig> nextConfigs = new ArrayList<>();
        for (AdminAssistantChatDisplayModelConfig config : adminAssistant.getChatDisplayModels()) {
            if (config == null) {
                continue;
            }
            if (!Objects.equals(config.getActualModelName(), existing.getModelName())
                    || !Objects.equals(existing.getModelType(), LlmModelTypeConstants.CHAT)) {
                nextConfigs.add(config);
                continue;
            }
            if (updated == null || !Objects.equals(updated.getProviderId(), existing.getProviderId())
                    || !Objects.equals(updated.getModelType(), existing.getModelType())
                    || !isModelEnabled(updated)) {
                changed = true;
                continue;
            }
            if (!Objects.equals(config.getActualModelName(), updated.getModelName())) {
                config.setActualModelName(updated.getModelName());
                changed = true;
            }
            Boolean nextSupportReasoning = supportsReasoning(updated);
            if (!Objects.equals(config.getSupportReasoning(), nextSupportReasoning)) {
                config.setSupportReasoning(nextSupportReasoning);
                changed = true;
            }
            Boolean nextSupportVision = supportsVision(updated);
            if (!Objects.equals(config.getSupportVision(), nextSupportVision)) {
                config.setSupportVision(nextSupportVision);
                changed = true;
            }
            nextConfigs.add(config);
        }
        if (changed) {
            adminAssistant.setChatDisplayModels(nextConfigs.isEmpty() ? List.of() : List.copyOf(nextConfigs));
        }
        return changed;
    }

    /**
     * 清理引用了特定真实模型名称的管理端聊天展示模型映射。
     *
     * @param cache     Agent 全量配置缓存
     * @param modelName 真实模型名称
     * @param modelType 模型类型
     * @return 是否发生变化
     */
    private boolean clearAdminAssistantChatDisplayModels(AgentAllConfigCache cache, String modelName, String modelType) {
        if (!Objects.equals(modelType, LlmModelTypeConstants.CHAT)) {
            return false;
        }
        AdminAssistantAgentConfig adminAssistant = cache.getAdminAssistant();
        if (adminAssistant == null || adminAssistant.getChatDisplayModels() == null
                || adminAssistant.getChatDisplayModels().isEmpty()) {
            return false;
        }
        List<AdminAssistantChatDisplayModelConfig> nextConfigs = adminAssistant.getChatDisplayModels().stream()
                .filter(Objects::nonNull)
                .filter(config -> !Objects.equals(config.getActualModelName(), modelName))
                .toList();
        if (nextConfigs.size() == adminAssistant.getChatDisplayModels().size()) {
            return false;
        }
        adminAssistant.setChatDisplayModels(nextConfigs.isEmpty() ? List.of() : List.copyOf(nextConfigs));
        return true;
    }

    /**
     * 校验知识库引用的模型是否与当前模型列表兼容。
     */
    private void validateKnowledgeBaseModelCompatibility(KnowledgeBaseAgentConfig knowledgeBase,
                                                         List<LlmProviderModel> models) {
        if (knowledgeBase == null) {
            return;
        }
        for (KnowledgeBaseModelBinding binding : KNOWLEDGE_BASE_MODEL_BINDINGS) {
            String modelName = binding.getter().apply(knowledgeBase);
            if (!StringUtils.hasText(modelName)) {
                continue;
            }
            LlmProviderModel model = findMatchingModel(models, binding.modelType(), modelName);
            if (model == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        PROVIDER_SWITCH_MODEL_MISSING_MESSAGE.formatted(modelName));
            }
            if (!isModelEnabled(model)) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR,
                        MODEL_DISABLED_MESSAGE.formatted(model.getModelName()));
            }
        }
    }

    /**
     * 调和知识库引用的模型。
     */
    private void reconcileKnowledgeBaseModelReferences(KnowledgeBaseAgentConfig knowledgeBase,
                                                       List<LlmProviderModel> models) {
        if (knowledgeBase == null) {
            return;
        }
        for (KnowledgeBaseModelBinding binding : KNOWLEDGE_BASE_MODEL_BINDINGS) {
            String modelName = binding.getter().apply(knowledgeBase);
            if (!StringUtils.hasText(modelName)) {
                continue;
            }
            LlmProviderModel model = findMatchingModel(models, binding.modelType(), modelName);
            if (model == null || !isModelEnabled(model)) {
                binding.setter().accept(knowledgeBase, null);
            }
        }
    }

    /**
     * 当单个模型更新时，同步更新知识库对该模型的引用。
     */
    private boolean syncKnowledgeBaseModelUpdate(KnowledgeBaseAgentConfig knowledgeBase,
                                                 LlmProviderModel existing,
                                                 LlmProviderModel updated) {
        if (knowledgeBase == null) {
            return false;
        }
        boolean changed = false;
        for (KnowledgeBaseModelBinding binding : KNOWLEDGE_BASE_MODEL_BINDINGS) {
            String modelName = binding.getter().apply(knowledgeBase);
            if (!matchesKnowledgeBaseModel(binding, modelName, existing.getModelName(), existing.getModelType())) {
                continue;
            }
            if (shouldClearKnowledgeBaseModel(existing, updated)) {
                binding.setter().accept(knowledgeBase, null);
                changed = true;
                continue;
            }
            if (!Objects.equals(modelName, updated.getModelName())) {
                binding.setter().accept(knowledgeBase, updated.getModelName());
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 清理知识库对特定模型的引用。
     */
    private boolean clearKnowledgeBaseModelReferences(KnowledgeBaseAgentConfig knowledgeBase,
                                                      String modelName,
                                                      String modelType) {
        if (knowledgeBase == null) {
            return false;
        }
        boolean changed = false;
        for (KnowledgeBaseModelBinding binding : KNOWLEDGE_BASE_MODEL_BINDINGS) {
            String selectedModelName = binding.getter().apply(knowledgeBase);
            if (!matchesKnowledgeBaseModel(binding, selectedModelName, modelName, modelType)) {
                continue;
            }
            binding.setter().accept(knowledgeBase, null);
            changed = true;
        }
        return changed;
    }

    /**
     * 判断模型更新后，是否应当清空引用该模型的槽位。
     * 若模型跨提供商、跨类型变更，或不再满足能力要求，则清空。
     */
    private boolean shouldClearSlot(AgentModelSlotConfig slot,
                                    SlotBinding binding,
                                    LlmProviderModel existing,
                                    LlmProviderModel updated) {
        if (updated == null) {
            return true;
        }
        if (!Objects.equals(updated.getProviderId(), existing.getProviderId())) {
            return true;
        }
        if (!Objects.equals(updated.getModelType(), existing.getModelType())) {
            return true;
        }
        return !isModelUsableForSlot(slot, binding, updated);
    }

    /**
     * 判断模型更新后，是否应当清空知识库对该模型的引用。
     */
    private boolean shouldClearKnowledgeBaseModel(LlmProviderModel existing, LlmProviderModel updated) {
        if (updated == null) {
            return true;
        }
        if (!Objects.equals(updated.getProviderId(), existing.getProviderId())) {
            return true;
        }
        if (!Objects.equals(updated.getModelType(), existing.getModelType())) {
            return true;
        }
        return !isModelEnabled(updated);
    }

    /**
     * 检查模型是否满足特定槽位的使用要求（启用状态、深度思考支持、多模态支持等）。
     */
    private boolean isModelUsableForSlot(AgentModelSlotConfig slot, SlotBinding binding, LlmProviderModel model) {
        if (slot == null || model == null || !isModelEnabled(model)) {
            return false;
        }
        if (Boolean.TRUE.equals(slot.getReasoningEnabled()) && !supportsReasoning(model)) {
            return false;
        }
        return !binding.visionRequired() || supportsVision(model);
    }

    /**
     * 校验模型是否满足槽位要求，若不满足则抛出业务异常（用于切换检查）。
     */
    private void validateModelUsableForSlot(AgentModelSlotConfig slot, SlotBinding binding, LlmProviderModel model) {
        if (!isModelEnabled(model)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    MODEL_DISABLED_MESSAGE.formatted(model.getModelName()));
        }
        if (Boolean.TRUE.equals(slot.getReasoningEnabled()) && !supportsReasoning(model)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    REASONING_UNSUPPORTED_MESSAGE.formatted(model.getModelName()));
        }
        if (binding.visionRequired() && !supportsVision(model)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    VISION_UNSUPPORTED_MESSAGE.formatted(model.getModelName()));
        }
    }

    /**
     * 获取数据库中当前排在首位的启用提供商。
     */
    private LlmProvider getEnabledProviderOrNull() {
        List<LlmProvider> providers = llmProviderMapper.selectList(Wrappers.<LlmProvider>lambdaQuery()
                .eq(LlmProvider::getStatus, PROVIDER_STATUS_ENABLED)
                .orderByAsc(LlmProvider::getSort, LlmProvider::getId));
        return providers.isEmpty() ? null : providers.getFirst();
    }

    /**
     * 获取指定提供商下的所有模型列表。
     */
    private List<LlmProviderModel> listProviderModels(Long providerId) {
        return llmProviderModelMapper.selectList(Wrappers.<LlmProviderModel>lambdaQuery()
                .eq(LlmProviderModel::getProviderId, providerId)
                .orderByAsc(LlmProviderModel::getSort, LlmProviderModel::getId));
    }

    /**
     * 在模型列表中查找匹配特定槽位绑定要求的模型实体。
     */
    private LlmProviderModel findMatchingModel(List<LlmProviderModel> models, SlotBinding binding, String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return null;
        }
        return models.stream()
                .filter(model -> Objects.equals(binding.modelType(), model.getModelType()))
                .filter(model -> Objects.equals(modelName, model.getModelName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 在模型列表中根据名称和类型查找模型实体。
     */
    private LlmProviderModel findMatchingModel(List<LlmProviderModel> models, String modelType, String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return null;
        }
        return models.stream()
                .filter(model -> Objects.equals(modelType, model.getModelType()))
                .filter(model -> Objects.equals(modelName, model.getModelName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断槽位当前引用的模型是否与给定的模型名称和类型匹配。
     */
    private boolean matchesSlot(AgentModelSlotConfig slot, SlotBinding binding, String modelName, String modelType) {
        return hasSelectedModel(slot)
                && Objects.equals(binding.modelType(), modelType)
                && Objects.equals(slot.getModelName(), modelName);
    }

    /**
     * 判断知识库当前引用的模型是否匹配。
     */
    private boolean matchesKnowledgeBaseModel(KnowledgeBaseModelBinding binding,
                                              String selectedModelName,
                                              String modelName,
                                              String modelType) {
        return StringUtils.hasText(selectedModelName)
                && Objects.equals(binding.modelType(), modelType)
                && Objects.equals(selectedModelName, modelName);
    }

    /**
     * 检查槽位是否已经选择了具体的模型名称。
     */
    private boolean hasSelectedModel(AgentModelSlotConfig slot) {
        return slot != null && StringUtils.hasText(slot.getModelName());
    }

    /**
     * 检查特定名称的知识库是否正在被 Agent 配置引用。
     */
    private boolean isKnowledgeBaseReferenced(String knowledgeName) {
        if (!StringUtils.hasText(knowledgeName)) {
            return false;
        }
        AgentAllConfigCache cache = readCache();
        return listKnowledgeBaseConfigs(cache).stream()
                .filter(this::isKnowledgeBaseEnabled)
                .map(KnowledgeBaseAgentConfig::getKnowledgeNames)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(StringUtils::hasText)
                .anyMatch(knowledgeName::equals);
    }

    /**
     * 检查知识库 Agent 功能是否已启用。
     */
    private boolean isKnowledgeBaseEnabled(KnowledgeBaseAgentConfig knowledgeBase) {
        if (knowledgeBase == null) {
            return false;
        }
        if (knowledgeBase.getEnabled() != null) {
            return knowledgeBase.getEnabled();
        }
        return knowledgeBase.getKnowledgeNames() != null && !knowledgeBase.getKnowledgeNames().isEmpty();
    }

    /**
     * 判断提供商是否处于启用状态。
     */
    private boolean isProviderEnabled(LlmProvider provider) {
        return provider != null && provider.getStatus() != null && provider.getStatus() == PROVIDER_STATUS_ENABLED;
    }

    /**
     * 判断模型是否处于启用状态。
     */
    private boolean isModelEnabled(LlmProviderModel model) {
        return model.getEnabled() != null && model.getEnabled() == MODEL_STATUS_ENABLED;
    }

    /**
     * 检查模型是否支持深度思考能力。
     */
    private boolean supportsReasoning(LlmProviderModel model) {
        return model.getSupportReasoning() != null && model.getSupportReasoning() == CAPABILITY_ENABLED;
    }

    /**
     * 检查模型是否支持多模态图片理解能力。
     */
    private boolean supportsVision(LlmProviderModel model) {
        return model.getSupportVision() != null && model.getSupportVision() == CAPABILITY_ENABLED;
    }

    /**
     * 规范化操作人名称，若为空则返回默认操作人。
     */
    private String normalizeOperator(String operator) {
        String normalized = normalizeNullableText(operator);
        return normalized != null ? normalized : DEFAULT_OPERATOR;
    }

    /**
     * 规范化可为空的文本内容。
     */
    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * Agent 模型槽位 (Slot) 与缓存结构的绑定 Record。
     *
     * @param modelType      要求的模型类型（如 CHAT）
     * @param visionRequired 是否强制要求图片理解能力
     * @param getter         从全量缓存中获取该槽位配置的函数
     * @param setter         向全量缓存中写入该槽位配置的函数
     */
    private record SlotBinding(String modelType,
                               boolean visionRequired,
                               Function<AgentAllConfigCache, AgentModelSlotConfig> getter,
                               BiConsumer<AgentAllConfigCache, AgentModelSlotConfig> setter) {
    }

    /**
     * 知识库模型字段与配置结构的绑定 Record。
     *
     * @param modelType 要求的模型类型（EMBEDDING 或 RERANK）
     * @param getter    从知识库配置中获取模型名称的函数
     * @param setter    设置知识库配置中模型名称的函数
     */
    private record KnowledgeBaseModelBinding(String modelType,
                                             Function<KnowledgeBaseAgentConfig, String> getter,
                                             BiConsumer<KnowledgeBaseAgentConfig, String> setter) {
    }
}
