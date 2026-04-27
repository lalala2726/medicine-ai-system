package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.publisher.AgentPromptPublisher;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.model.cache.AgentPromptConfigCache;
import com.zhangyichuang.medicine.model.entity.AgentPromptConfig;
import com.zhangyichuang.medicine.model.mq.AgentPromptRefreshMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 提示词运行时 Redis 与 MQ 联动同步服务。
 * <p>
 * 负责维护 Redis 中的单条提示词缓存，并广播 Python 端刷新通知。
 */
@Service
@RequiredArgsConstructor
public class AgentPromptRuntimeSyncService {

    /**
     * MQ 刷新消息类型标识。
     */
    private static final String AGENT_PROMPT_REFRESH_MESSAGE_TYPE = "agent_prompt_refresh";

    /**
     * 默认操作人名称。
     */
    private static final String DEFAULT_OPERATOR = "system";

    /**
     * Redis 缓存工具。
     */
    private final RedisCache redisCache;

    /**
     * 提示词刷新 MQ 生产者。
     */
    private final AgentPromptPublisher agentPromptPublisher;

    /**
     * 保存单条提示词到 Redis，并发布 MQ 刷新通知。
     *
     * @param promptConfig 当前生效提示词配置
     * @param operator     操作人（用于更新人兜底）
     * @return 无返回值
     */
    public void savePromptAndPublish(AgentPromptConfig promptConfig, String operator) {
        savePromptAndPublishInternal(promptConfig, operator, true);
    }

    /**
     * 删除单条提示词 Redis 缓存，并发布 MQ 刷新通知。
     *
     * @param promptKey     提示词业务键
     * @param promptVersion 当前版本号（删除时可为空）
     * @param operator      操作人
     * @return 无返回值
     */
    public void deletePromptAndPublish(String promptKey, Long promptVersion, String operator) {
        deletePromptAndPublishInternal(promptKey, promptVersion, operator, true);
    }

    /**
     * 按数据库镜像语义同步全部提示词到 Redis，并逐条发布 MQ 刷新通知。
     *
     * @param promptConfigs        当前数据库中的提示词配置集合
     * @param registeredPromptKeys 当前已注册的提示词业务键集合
     * @param operator             操作人（用于更新人兜底）
     * @return 无返回值
     */
    public void syncPromptConfigsAndPublish(Collection<AgentPromptConfig> promptConfigs,
                                            Collection<String> registeredPromptKeys,
                                            String operator) {
        Map<String, AgentPromptConfig> promptConfigMap = indexPromptConfigs(promptConfigs);
        deleteLegacyPromptAllKey();
        for (String registeredPromptKey : normalizePromptKeys(registeredPromptKeys)) {
            syncPromptConfigAndPublishInternal(
                    registeredPromptKey,
                    promptConfigMap.get(registeredPromptKey),
                    operator,
                    false
            );
        }
    }

    /**
     * 按数据库镜像语义同步单条提示词到 Redis，并发布 MQ 刷新通知。
     *
     * @param promptKey    提示词业务键
     * @param promptConfig 当前数据库中的提示词配置，可为空
     * @param operator     操作人（用于更新人兜底）
     * @return 无返回值
     */
    public void syncPromptConfigAndPublish(String promptKey, AgentPromptConfig promptConfig, String operator) {
        syncPromptConfigAndPublishInternal(promptKey, promptConfig, operator, true);
    }

    /**
     * 保存单条提示词到 Redis，并发布 MQ 刷新通知的内部实现。
     *
     * @param promptConfig      当前生效提示词配置
     * @param operator          操作人（用于更新人兜底）
     * @param clearLegacyPrompt 是否先清理历史聚合 key
     * @return 无返回值
     */
    private void savePromptAndPublishInternal(AgentPromptConfig promptConfig,
                                              String operator,
                                              boolean clearLegacyPrompt) {
        AgentPromptConfigCache cache = buildPromptCache(promptConfig, operator);
        String redisKey = buildPromptRedisKey(cache.getPromptKey());
        if (clearLegacyPrompt) {
            deleteLegacyPromptAllKey();
        }
        redisCache.setCacheObject(redisKey, cache);
        agentPromptPublisher.publishRefresh(
                buildRefreshMessage(
                        cache.getPromptKey(),
                        cache.getPromptVersion(),
                        redisKey,
                        cache.getUpdatedAt(),
                        cache.getUpdatedBy()
                )
        );
    }

    /**
     * 删除单条提示词 Redis 缓存，并发布 MQ 刷新通知的内部实现。
     *
     * @param promptKey          提示词业务键
     * @param promptVersion      当前版本号（删除时可为空）
     * @param operator           操作人
     * @param clearLegacyPrompt  是否先清理历史聚合 key
     * @return 无返回值
     */
    private void deletePromptAndPublishInternal(String promptKey,
                                                Long promptVersion,
                                                String operator,
                                                boolean clearLegacyPrompt) {
        String normalizedPromptKey = normalizeRequiredText(promptKey);
        String redisKey = buildPromptRedisKey(normalizedPromptKey);
        String normalizedOperator = normalizeOperator(operator);
        String updatedAt = formatUpdatedAt(new Date());
        if (clearLegacyPrompt) {
            deleteLegacyPromptAllKey();
        }
        redisCache.deleteObject(redisKey);
        agentPromptPublisher.publishRefresh(
                buildRefreshMessage(
                        normalizedPromptKey,
                        promptVersion,
                        redisKey,
                        updatedAt,
                        normalizedOperator
                )
        );
    }

    /**
     * 按数据库镜像语义同步单条提示词到 Redis 的内部实现。
     *
     * @param promptKey          提示词业务键
     * @param promptConfig       当前数据库中的提示词配置，可为空
     * @param operator           操作人
     * @param clearLegacyPrompt  是否先清理历史聚合 key
     * @return 无返回值
     */
    private void syncPromptConfigAndPublishInternal(String promptKey,
                                                    AgentPromptConfig promptConfig,
                                                    String operator,
                                                    boolean clearLegacyPrompt) {
        String normalizedPromptKey = normalizeRequiredText(promptKey);
        if (promptConfig == null || !StringUtils.hasText(promptConfig.getPromptContent())) {
            Long promptVersion = promptConfig == null ? null : promptConfig.getPromptVersion();
            deletePromptAndPublishInternal(normalizedPromptKey, promptVersion, operator, clearLegacyPrompt);
            return;
        }
        savePromptAndPublishInternal(promptConfig, operator, clearLegacyPrompt);
    }

    /**
     * 构建单条提示词缓存对象。
     *
     * @param promptConfig 当前生效提示词配置
     * @param operator     操作人（用于更新人兜底）
     * @return 单条提示词缓存对象
     */
    private AgentPromptConfigCache buildPromptCache(AgentPromptConfig promptConfig, String operator) {
        if (promptConfig == null) {
            throw new IllegalArgumentException("promptConfig cannot be null");
        }
        AgentPromptConfigCache cache = new AgentPromptConfigCache();
        cache.setPromptKey(normalizeRequiredText(promptConfig.getPromptKey()));
        cache.setPromptVersion(promptConfig.getPromptVersion());
        cache.setUpdatedAt(resolveUpdatedAt(promptConfig.getUpdatedAt()));
        cache.setUpdatedBy(resolveUpdatedBy(promptConfig.getUpdateBy(), operator));
        cache.setPromptContent(normalizeRequiredText(promptConfig.getPromptContent()));
        return cache;
    }

    /**
     * 构建提示词刷新消息。
     *
     * @param promptKey     本次变更的提示词键
     * @param promptVersion 本次变更后的版本号
     * @param redisKey      提示词运行时 Redis key
     * @param updatedAt     本次配置更新时间
     * @param updatedBy     本次配置更新人
     * @return 刷新消息对象
     */
    private AgentPromptRefreshMessage buildRefreshMessage(String promptKey,
                                                          Long promptVersion,
                                                          String redisKey,
                                                          String updatedAt,
                                                          String updatedBy) {
        return AgentPromptRefreshMessage.builder()
                .message_type(AGENT_PROMPT_REFRESH_MESSAGE_TYPE)
                .prompt_key(promptKey)
                .prompt_version(promptVersion)
                .redis_key(redisKey)
                .updated_at(updatedAt)
                .updated_by(updatedBy)
                .created_at(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    /**
     * 将提示词配置集合转换为按业务键索引的字典。
     *
     * @param promptConfigs 原始提示词配置集合
     * @return 按业务键索引的提示词配置字典
     */
    private Map<String, AgentPromptConfig> indexPromptConfigs(Collection<AgentPromptConfig> promptConfigs) {
        Map<String, AgentPromptConfig> promptConfigMap = new LinkedHashMap<>();
        if (promptConfigs == null || promptConfigs.isEmpty()) {
            return promptConfigMap;
        }
        for (AgentPromptConfig promptConfig : promptConfigs) {
            if (promptConfig == null) {
                continue;
            }
            String promptKey = normalizeText(promptConfig.getPromptKey());
            if (promptKey == null) {
                continue;
            }
            promptConfigMap.put(promptKey, promptConfig);
        }
        return promptConfigMap;
    }

    /**
     * 归一化提示词业务键集合。
     *
     * @param registeredPromptKeys 原始提示词业务键集合
     * @return 去空白并过滤空值后的业务键集合
     */
    private Collection<String> normalizePromptKeys(Collection<String> registeredPromptKeys) {
        Collection<String> normalizedPromptKeys = new java.util.ArrayList<>();
        if (registeredPromptKeys == null || registeredPromptKeys.isEmpty()) {
            return normalizedPromptKeys;
        }
        for (String registeredPromptKey : registeredPromptKeys) {
            String normalizedPromptKey = normalizeText(registeredPromptKey);
            if (normalizedPromptKey == null) {
                continue;
            }
            normalizedPromptKeys.add(normalizedPromptKey);
        }
        return normalizedPromptKeys;
    }

    /**
     * 构建单条提示词 Redis key。
     *
     * @param promptKey 提示词业务键
     * @return Redis key
     */
    private String buildPromptRedisKey(String promptKey) {
        return RedisConstants.AgentConfig.PROMPT_CONFIG_KEY_TEMPLATE.formatted(normalizeRequiredText(promptKey));
    }

    /**
     * 删除历史聚合提示词缓存 key。
     *
     * @return 无返回值
     */
    private void deleteLegacyPromptAllKey() {
        redisCache.deleteObject(RedisConstants.AgentConfig.LEGACY_PROMPT_ALL_CONFIG_KEY);
    }

    /**
     * 解析运行时缓存中的更新时间。
     *
     * @param updatedAt 原始更新时间
     * @return ISO-8601 格式时间字符串
     */
    private String resolveUpdatedAt(Date updatedAt) {
        Date resolvedUpdatedAt = updatedAt == null ? new Date() : updatedAt;
        return formatUpdatedAt(resolvedUpdatedAt);
    }

    /**
     * 解析运行时缓存中的更新人。
     *
     * @param configUpdatedBy 配置中的更新人
     * @param operator        当前操作人
     * @return 最终写入缓存的更新人
     */
    private String resolveUpdatedBy(String configUpdatedBy, String operator) {
        String normalizedConfigUpdatedBy = normalizeText(configUpdatedBy);
        if (normalizedConfigUpdatedBy != null) {
            return normalizedConfigUpdatedBy;
        }
        return normalizeOperator(operator);
    }

    /**
     * 将时间格式化为 ISO-8601 字符串。
     *
     * @param updatedAt 原始时间
     * @return ISO-8601 格式时间字符串
     */
    private String formatUpdatedAt(Date updatedAt) {
        return OffsetDateTime.ofInstant(updatedAt.toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 归一化必填文本。
     *
     * @param value 原始文本
     * @return 去首尾空白后的文本
     */
    private String normalizeRequiredText(String value) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("required text cannot be empty");
        }
        return normalizedValue;
    }

    /**
     * 归一化操作人。
     *
     * @param operator 原始操作人
     * @return 归一化后的操作人
     */
    private String normalizeOperator(String operator) {
        String normalizedOperator = normalizeText(operator);
        return normalizedOperator == null ? DEFAULT_OPERATOR : normalizedOperator;
    }

    /**
     * 归一化文本。
     *
     * @param value 原始文本
     * @return 去首尾空白后的文本；为空时返回 null
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
