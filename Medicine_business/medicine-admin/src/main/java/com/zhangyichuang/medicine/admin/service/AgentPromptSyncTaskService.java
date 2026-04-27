package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptConfigMapper;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptKeyMapper;
import com.zhangyichuang.medicine.admin.publisher.AgentPromptSyncPublisher;
import com.zhangyichuang.medicine.admin.support.AgentPromptKeyCatalog;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.AgentPromptConfig;
import com.zhangyichuang.medicine.model.entity.AgentPromptKey;
import com.zhangyichuang.medicine.model.mq.AgentPromptSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 提示词手动同步任务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPromptSyncTaskService implements BaseService {

    /**
     * 全量同步范围标识。
     */
    private static final String SYNC_SCOPE_ALL = "all";

    /**
     * 单条同步范围标识。
     */
    private static final String SYNC_SCOPE_SINGLE = "single";

    /**
     * 默认操作人。
     */
    private static final String DEFAULT_OPERATOR = "system";

    /**
     * 不支持的提示词键提示语模板。
     */
    private static final String UNSUPPORTED_PROMPT_KEY_MESSAGE = "不支持的提示词键：%s";

    /**
     * 提示词同步任务 MQ 生产者。
     */
    private final AgentPromptSyncPublisher agentPromptSyncPublisher;

    /**
     * 提示词当前配置 Mapper。
     */
    private final AgentPromptConfigMapper agentPromptConfigMapper;

    /**
     * 提示词业务键配置 Mapper。
     */
    private final AgentPromptKeyMapper agentPromptKeyMapper;

    /**
     * 提示词运行态同步服务。
     */
    private final AgentPromptRuntimeSyncService agentPromptRuntimeSyncService;

    /**
     * 提交全量提示词同步任务。
     *
     * @return true 表示任务提交成功
     */
    public boolean submitSyncAllTask() {
        agentPromptSyncPublisher.publishSync(buildSyncMessage(SYNC_SCOPE_ALL, null, currentOperator()));
        return true;
    }

    /**
     * 提交单条提示词同步任务。
     *
     * @param promptKey 提示词业务键
     * @return true 表示任务提交成功
     */
    public boolean submitSyncSingleTask(String promptKey) {
        String normalizedPromptKey = normalizeRegisteredPromptKey(promptKey);
        agentPromptSyncPublisher.publishSync(
                buildSyncMessage(SYNC_SCOPE_SINGLE, normalizedPromptKey, currentOperator())
        );
        return true;
    }

    /**
     * 执行提示词同步任务。
     *
     * @param message 同步任务消息
     * @return 无返回值
     */
    public void executeSyncTask(AgentPromptSyncMessage message) {
        if (message == null) {
            log.warn("跳过Agent提示词同步任务: message is null");
            return;
        }
        String syncScope = normalizeRequiredText(message.getSync_scope(), "同步范围不能为空");
        String operator = normalizeOperator(message.getOperator());
        if (SYNC_SCOPE_ALL.equals(syncScope)) {
            syncAllPromptConfigs(operator);
            return;
        }
        if (SYNC_SCOPE_SINGLE.equals(syncScope)) {
            syncSinglePromptConfig(
                    normalizeRequiredText(message.getPrompt_key(), "单条同步的提示词键不能为空"),
                    operator
            );
            return;
        }
        throw new ServiceException(ResponseCode.PARAM_ERROR, "不支持的提示词同步范围：" + syncScope);
    }

    /**
     * 同步全部已注册提示词到 Redis。
     *
     * @param operator 操作人
     * @return 无返回值
     */
    private void syncAllPromptConfigs(String operator) {
        agentPromptRuntimeSyncService.syncPromptConfigsAndPublish(
                listCurrentPromptConfigs(),
                loadRegisteredPromptKeyDescriptionMap().keySet(),
                operator
        );
    }

    /**
     * 同步单条提示词到 Redis。
     *
     * @param promptKey 提示词业务键
     * @param operator  操作人
     * @return 无返回值
     */
    private void syncSinglePromptConfig(String promptKey, String operator) {
        String normalizedPromptKey = normalizeRegisteredPromptKey(promptKey);
        agentPromptRuntimeSyncService.syncPromptConfigAndPublish(
                normalizedPromptKey,
                getPromptConfigEntityOrNull(normalizedPromptKey),
                operator
        );
    }

    /**
     * 构建同步任务消息。
     *
     * @param syncScope 同步范围
     * @param promptKey 提示词业务键
     * @param operator  操作人
     * @return 同步任务消息
     */
    private AgentPromptSyncMessage buildSyncMessage(String syncScope, String promptKey, String operator) {
        return AgentPromptSyncMessage.builder()
                .sync_scope(syncScope)
                .prompt_key(promptKey)
                .operator(operator)
                .created_at(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    /**
     * 加载当前所有提示词配置实体。
     *
     * @return 当前提示词配置实体列表
     */
    private List<AgentPromptConfig> listCurrentPromptConfigs() {
        return agentPromptConfigMapper.selectList(
                Wrappers.<AgentPromptConfig>lambdaQuery()
                        .orderByAsc(AgentPromptConfig::getPromptKey)
        );
    }

    /**
     * 加载当前已注册提示词业务键与说明。
     *
     * @return 提示词键与说明映射
     */
    private Map<String, String> loadRegisteredPromptKeyDescriptionMap() {
        List<AgentPromptKey> promptKeyEntities = agentPromptKeyMapper.selectList(
                Wrappers.<AgentPromptKey>lambdaQuery()
                        .orderByAsc(AgentPromptKey::getPromptKey)
        );
        Map<String, String> promptKeyDescriptionMap = new LinkedHashMap<>();
        for (AgentPromptKey promptKeyEntity : promptKeyEntities) {
            String normalizedPromptKey = normalizeNullableText(promptKeyEntity.getPromptKey());
            if (normalizedPromptKey == null) {
                continue;
            }
            promptKeyDescriptionMap.put(normalizedPromptKey, normalizeNullableText(promptKeyEntity.getDescription()));
        }
        if (!promptKeyDescriptionMap.isEmpty()) {
            return promptKeyDescriptionMap;
        }
        for (AgentPromptKeyCatalog.PromptKeyMeta defaultKeyMeta : AgentPromptKeyCatalog.listDefaultKeys()) {
            promptKeyDescriptionMap.put(defaultKeyMeta.promptKey(), defaultKeyMeta.description());
        }
        return promptKeyDescriptionMap;
    }

    /**
     * 查询当前提示词配置，不存在时返回 null。
     *
     * @param promptKey 提示词业务键
     * @return 当前提示词配置
     */
    private AgentPromptConfig getPromptConfigEntityOrNull(String promptKey) {
        return agentPromptConfigMapper.selectOne(
                Wrappers.<AgentPromptConfig>lambdaQuery()
                        .eq(AgentPromptConfig::getPromptKey, promptKey)
                        .last("limit 1")
        );
    }

    /**
     * 归一化并返回已注册的提示词键。
     *
     * @param promptKey 原始提示词键
     * @return 归一化后的提示词键
     */
    private String normalizeRegisteredPromptKey(String promptKey) {
        String normalizedPromptKey = normalizeRequiredText(promptKey, "提示词键不能为空");
        Assert.isParamTrue(
                loadRegisteredPromptKeyDescriptionMap().containsKey(normalizedPromptKey),
                UNSUPPORTED_PROMPT_KEY_MESSAGE.formatted(normalizedPromptKey)
        );
        return normalizedPromptKey;
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
     * 归一化操作人。
     *
     * @param operator 原始操作人
     * @return 归一化后的操作人
     */
    private String normalizeOperator(String operator) {
        String normalizedOperator = normalizeNullableText(operator);
        return normalizedOperator != null ? normalizedOperator : DEFAULT_OPERATOR;
    }

    /**
     * 归一化并返回必填文本。
     *
     * @param value        原始文本
     * @param errorMessage 校验失败提示语
     * @return 归一化后的文本
     */
    private String normalizeRequiredText(String value, String errorMessage) {
        String normalizedValue = normalizeNullableText(value);
        Assert.notEmpty(normalizedValue, errorMessage);
        return normalizedValue;
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
