package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptConfigMapper;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptHistoryMapper;
import com.zhangyichuang.medicine.admin.mapper.AgentPromptKeyMapper;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptKeyUpsertRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptRollbackRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptHistoryVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptKeyOptionVo;
import com.zhangyichuang.medicine.admin.service.AgentPromptConfigService;
import com.zhangyichuang.medicine.admin.service.AgentPromptRuntimeSyncService;
import com.zhangyichuang.medicine.admin.support.AgentPromptKeyCatalog;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.AgentPromptConfig;
import com.zhangyichuang.medicine.model.entity.AgentPromptHistory;
import com.zhangyichuang.medicine.model.entity.AgentPromptKey;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Agent 提示词配置服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentPromptConfigServiceImpl implements AgentPromptConfigService, BaseService {

    /**
     * 历史查询最小条数。
     */
    private static final int HISTORY_LIMIT_MIN = 1;

    /**
     * 新增版本初始值。
     */
    private static final long INITIAL_PROMPT_VERSION = 1L;

    /**
     * 版本冲突自动重试最大次数。
     */
    private static final int PROMPT_VERSION_RETRY_MAX = 5;

    /**
     * 默认操作人。
     */
    private static final String DEFAULT_OPERATOR = "system";

    /**
     * 提示词键不存在提示语模板。
     */
    private static final String PROMPT_NOT_FOUND_MESSAGE = "提示词不存在：%s";

    /**
     * 提示词版本不存在提示语模板。
     */
    private static final String PROMPT_HISTORY_NOT_FOUND_MESSAGE = "提示词历史版本不存在：%s@v%s";

    /**
     * 不支持的提示词键提示语模板。
     */
    private static final String UNSUPPORTED_PROMPT_KEY_MESSAGE = "不支持的提示词键：%s";

    /**
     * 提示词版本冲突提示语模板。
     */
    private static final String PROMPT_VERSION_CONFLICT_MESSAGE = "提示词版本冲突，请重试：%s";

    /**
     * 提示词当前配置 Mapper。
     */
    private final AgentPromptConfigMapper agentPromptConfigMapper;

    /**
     * 提示词历史配置 Mapper。
     */
    private final AgentPromptHistoryMapper agentPromptHistoryMapper;

    /**
     * 提示词业务键配置 Mapper。
     */
    private final AgentPromptKeyMapper agentPromptKeyMapper;

    /**
     * 提示词运行态同步服务。
     */
    private final AgentPromptRuntimeSyncService agentPromptRuntimeSyncService;

    /**
     * 验证码服务。
     */
    private final CaptchaService captchaService;

    /**
     * Redis 缓存工具。
     */
    private final RedisCache redisCache;

    /**
     * 查询指定提示词当前配置。
     *
     * @param promptKey 提示词业务键
     * @return 提示词当前配置
     */
    @Override
    public AgentPromptConfigVo getPromptConfig(String promptKey) {
        String normalizedPromptKey = normalizeRegisteredPromptKey(promptKey);
        AgentPromptConfig config = getPromptConfigEntityOrNull(normalizedPromptKey);
        if (config == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, PROMPT_NOT_FOUND_MESSAGE.formatted(normalizedPromptKey));
        }
        return toPromptConfigVo(config);
    }

    /**
     * 保存提示词配置并生成新版本。
     *
     * @param request 保存请求
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePromptConfig(AgentPromptUpdateRequest request) {
        Assert.notNull(request, "提示词保存请求不能为空");
        String normalizedPromptKey = normalizeRegisteredPromptKey(request.getPromptKey());
        String normalizedPromptContent = normalizeRequiredPromptContent(request.getPromptContent());
        savePromptContentInternal(
                normalizedPromptKey,
                normalizedPromptContent,
                currentOperator()
        );
        return true;
    }

    /**
     * 查询指定提示词历史版本列表。
     *
     * @param promptKey 提示词业务键
     * @param limit     返回条数上限
     * @return 历史版本列表（按版本倒序）
     */
    @Override
    public List<AgentPromptHistoryVo> listPromptHistory(String promptKey, Integer limit) {
        String normalizedPromptKey = normalizeRegisteredPromptKey(promptKey);
        Integer resolvedLimit = resolveHistoryLimit(limit);
        var historyQuery = Wrappers.<AgentPromptHistory>lambdaQuery()
                .eq(AgentPromptHistory::getPromptKey, normalizedPromptKey)
                .orderByDesc(AgentPromptHistory::getPromptVersion);
        if (resolvedLimit != null) {
            historyQuery.last("limit " + resolvedLimit);
        }
        List<AgentPromptHistory> historyList = agentPromptHistoryMapper.selectList(historyQuery);
        List<AgentPromptHistoryVo> result = new ArrayList<>(historyList.size());
        for (AgentPromptHistory history : historyList) {
            result.add(toPromptHistoryVo(history));
        }
        return result;
    }

    /**
     * 将提示词回滚到指定历史版本（回滚本身会生成新版本）。
     *
     * @param request 回滚请求
     * @return 是否回滚成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackPromptConfig(AgentPromptRollbackRequest request) {
        Assert.notNull(request, "提示词回滚请求不能为空");
        String normalizedPromptKey = normalizeRegisteredPromptKey(request.getPromptKey());
        Long targetVersion = request.getTargetVersion();
        Assert.isPositive(targetVersion, "目标版本号必须大于0");

        AgentPromptHistory targetHistory = agentPromptHistoryMapper.selectOne(
                Wrappers.<AgentPromptHistory>lambdaQuery()
                        .eq(AgentPromptHistory::getPromptKey, normalizedPromptKey)
                        .eq(AgentPromptHistory::getPromptVersion, targetVersion)
                        .last("limit 1")
        );
        if (targetHistory == null) {
            throw new ServiceException(
                    ResponseCode.RESULT_IS_NULL,
                    PROMPT_HISTORY_NOT_FOUND_MESSAGE.formatted(normalizedPromptKey, targetVersion)
            );
        }
        savePromptContentInternal(
                normalizedPromptKey,
                targetHistory.getPromptContent(),
                currentOperator()
        );
        return true;
    }

    /**
     * 删除指定提示词当前配置与历史版本，并同步运行时快照。
     *
     * @param promptKey             提示词业务键
     * @param captchaVerificationId 验证码校验凭证
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePromptConfig(String promptKey, String captchaVerificationId) {
        captchaService.validateLoginCaptcha(captchaVerificationId);
        String normalizedPromptKey = normalizeRegisteredPromptKey(promptKey);
        String operator = currentOperator();
        agentPromptConfigMapper.delete(
                Wrappers.<AgentPromptConfig>lambdaQuery()
                        .eq(AgentPromptConfig::getPromptKey, normalizedPromptKey)
        );
        agentPromptHistoryMapper.delete(
                Wrappers.<AgentPromptHistory>lambdaQuery()
                        .eq(AgentPromptHistory::getPromptKey, normalizedPromptKey)
        );
        runAfterCommit(() -> agentPromptRuntimeSyncService.deletePromptAndPublish(normalizedPromptKey, null, operator));
        return true;
    }

    /**
     * 查询提示词键选项列表。
     *
     * @return 提示词键选项列表
     */
    @Override
    public List<AgentPromptKeyOptionVo> listPromptKeys() {
        Map<String, String> promptKeyDescriptionMap = loadRegisteredPromptKeyDescriptionMap();
        List<AgentPromptConfig> currentConfigs = agentPromptConfigMapper.selectList(
                Wrappers.<AgentPromptConfig>lambdaQuery()
                        .orderByAsc(AgentPromptConfig::getPromptKey)
        );
        Map<String, AgentPromptConfig> currentConfigMap = new LinkedHashMap<>();
        for (AgentPromptConfig currentConfig : currentConfigs) {
            currentConfigMap.put(currentConfig.getPromptKey(), currentConfig);
        }

        Map<String, AgentPromptKeyOptionVo> optionMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> promptKeyEntry : promptKeyDescriptionMap.entrySet()) {
            AgentPromptKeyOptionVo option = new AgentPromptKeyOptionVo();
            option.setPromptKey(promptKeyEntry.getKey());
            option.setDescription(promptKeyEntry.getValue());
            AgentPromptConfig configured = currentConfigMap.get(promptKeyEntry.getKey());
            boolean configuredState = isPromptConfigured(promptKeyEntry.getKey(), configured);
            option.setConfigured(configuredState);
            option.setPromptVersion(configuredState ? configured.getPromptVersion() : null);
            optionMap.put(option.getPromptKey(), option);
        }
        return new ArrayList<>(optionMap.values());
    }

    /**
     * 判断提示词是否已配置。
     *
     * @param promptKey 提示词业务键
     * @param config    当前提示词配置实体（可空）
     * @return true 表示数据库正文非空且 Redis 运行时 key 已存在
     */
    private boolean isPromptConfigured(String promptKey, AgentPromptConfig config) {
        return config != null
                && StringUtils.hasText(config.getPromptContent())
                && hasRuntimePromptConfig(promptKey);
    }

    /**
     * 判断提示词运行时 Redis key 是否已存在。
     *
     * @param promptKey 提示词业务键
     * @return true 表示 Redis 中已存在对应运行时 key
     */
    private boolean hasRuntimePromptConfig(String promptKey) {
        return redisCache.hasKey(buildPromptRedisKey(promptKey));
    }

    /**
     * 构建提示词运行时 Redis key。
     *
     * @param promptKey 提示词业务键
     * @return 提示词运行时 Redis key
     */
    private String buildPromptRedisKey(String promptKey) {
        return RedisConstants.AgentConfig.PROMPT_CONFIG_KEY_TEMPLATE.formatted(normalizeRequiredPromptKeyText(promptKey));
    }

    /**
     * 新增或更新提示词业务键。
     *
     * @param request 业务键新增/更新请求
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePromptKey(AgentPromptKeyUpsertRequest request) {
        Assert.notNull(request, "提示词键保存请求不能为空");
        String normalizedPromptKey = normalizeRequiredPromptKeyText(request.getPromptKey());
        String normalizedDescription = normalizeNullableText(request.getDescription());
        String operator = currentOperator();
        Date now = new Date();
        AgentPromptKey existing = getPromptKeyEntityOrNull(normalizedPromptKey);
        if (existing == null) {
            AgentPromptKey newPromptKey = AgentPromptKey.builder()
                    .promptKey(normalizedPromptKey)
                    .description(normalizedDescription)
                    .createBy(operator)
                    .updateBy(operator)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            agentPromptKeyMapper.insert(newPromptKey);
            return true;
        }
        existing.setDescription(normalizedDescription);
        existing.setUpdateBy(operator);
        existing.setUpdatedAt(now);
        agentPromptKeyMapper.updateById(existing);
        return true;
    }

    /**
     * 保存提示词正文到当前配置与历史配置，并触发运行态同步。
     *
     * @param promptKey     提示词业务键
     * @param promptContent 提示词正文
     * @param operator      操作人
     */
    private void savePromptContentInternal(String promptKey, String promptContent, String operator) {
        Date now = new Date();
        AgentPromptConfig savedPromptConfig = savePromptContentWithVersionRetry(promptKey, promptContent, operator, now);
        runAfterCommit(() -> agentPromptRuntimeSyncService.savePromptAndPublish(savedPromptConfig, operator));
    }

    /**
     * 在数据库事务提交后执行外部副作用。
     *
     * @param task 需要在提交后执行的任务
     * @return 无返回值
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    /**
     * 保存提示词正文（带版本冲突自动重试）。
     *
     * @param promptKey     提示词业务键
     * @param promptContent 提示词正文
     * @param operator      操作人
     * @param now           当前时间
     * @return 最终保存成功的当前提示词配置实体
     */
    private AgentPromptConfig savePromptContentWithVersionRetry(String promptKey,
                                                               String promptContent,
                                                               String operator,
                                                               Date now) {
        for (int retryIndex = 0; retryIndex < PROMPT_VERSION_RETRY_MAX; retryIndex++) {
            AgentPromptConfig existing = getPromptConfigEntityOrNull(promptKey);
            long nextVersion = resolveNextPromptVersion(promptKey, existing);
            try {
                insertPromptHistory(promptKey, nextVersion, promptContent, operator, now);
            } catch (DuplicateKeyException ex) {
                if (retryIndex + 1 >= PROMPT_VERSION_RETRY_MAX) {
                    throw new ServiceException(
                            ResponseCode.OPERATION_ERROR,
                            PROMPT_VERSION_CONFLICT_MESSAGE.formatted(promptKey)
                    );
                }
                continue;
            }
            return upsertPromptConfig(existing, promptKey, promptContent, nextVersion, operator, now);
        }
        throw new ServiceException(ResponseCode.OPERATION_ERROR, PROMPT_VERSION_CONFLICT_MESSAGE.formatted(promptKey));
    }

    /**
     * 解析下一个可用提示词版本号。
     *
     * @param promptKey     提示词业务键
     * @param currentConfig 当前配置实体（可空）
     * @return 下一个可用版本号
     */
    private long resolveNextPromptVersion(String promptKey, AgentPromptConfig currentConfig) {
        long currentConfigVersion = currentConfig == null || currentConfig.getPromptVersion() == null
                ? 0L
                : currentConfig.getPromptVersion();
        AgentPromptHistory latestHistory = getLatestPromptHistoryEntityOrNull(promptKey);
        long latestHistoryVersion = latestHistory == null || latestHistory.getPromptVersion() == null
                ? 0L
                : latestHistory.getPromptVersion();
        long maxVersion = Math.max(currentConfigVersion, latestHistoryVersion);
        return Math.max(maxVersion + 1L, INITIAL_PROMPT_VERSION);
    }

    /**
     * 写入提示词历史版本记录。
     *
     * @param promptKey     提示词业务键
     * @param promptVersion 本次版本号
     * @param promptContent 提示词正文
     * @param operator      操作人
     * @param now           当前时间
     */
    private void insertPromptHistory(String promptKey,
                                     long promptVersion,
                                     String promptContent,
                                     String operator,
                                     Date now) {
        AgentPromptHistory history = AgentPromptHistory.builder()
                .promptKey(promptKey)
                .promptVersion(promptVersion)
                .promptContent(promptContent)
                .createBy(operator)
                .createdAt(now)
                .build();
        agentPromptHistoryMapper.insert(history);
    }

    /**
     * 新增或更新当前提示词配置。
     *
     * @param existing      当前配置实体（可空）
     * @param promptKey     提示词业务键
     * @param promptContent 提示词正文
     * @param promptVersion 本次版本号
     * @param operator      操作人
     * @param now           当前时间
     * @return 已保存的当前提示词配置实体
     */
    private AgentPromptConfig upsertPromptConfig(AgentPromptConfig existing,
                                                 String promptKey,
                                                 String promptContent,
                                                 long promptVersion,
                                                 String operator,
                                                 Date now) {
        if (existing == null) {
            AgentPromptConfig newConfig = AgentPromptConfig.builder()
                    .promptKey(promptKey)
                    .promptContent(promptContent)
                    .promptVersion(promptVersion)
                    .createBy(operator)
                    .updateBy(operator)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            agentPromptConfigMapper.insert(newConfig);
            return newConfig;
        }
        existing.setPromptContent(promptContent);
        existing.setPromptVersion(promptVersion);
        existing.setUpdateBy(operator);
        existing.setUpdatedAt(now);
        agentPromptConfigMapper.updateById(existing);
        return existing;
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
     * 查询最新提示词历史配置，不存在时返回 null。
     *
     * @param promptKey 提示词业务键
     * @return 最新提示词历史配置
     */
    private AgentPromptHistory getLatestPromptHistoryEntityOrNull(String promptKey) {
        return agentPromptHistoryMapper.selectOne(
                Wrappers.<AgentPromptHistory>lambdaQuery()
                        .eq(AgentPromptHistory::getPromptKey, promptKey)
                        .orderByDesc(AgentPromptHistory::getPromptVersion)
                        .last("limit 1")
        );
    }

    /**
     * 查询提示词业务键配置，不存在时返回 null。
     *
     * @param promptKey 提示词业务键
     * @return 提示词业务键配置
     */
    private AgentPromptKey getPromptKeyEntityOrNull(String promptKey) {
        return agentPromptKeyMapper.selectOne(
                Wrappers.<AgentPromptKey>lambdaQuery()
                        .eq(AgentPromptKey::getPromptKey, promptKey)
                        .last("limit 1")
        );
    }

    /**
     * 解析历史条数上限。
     *
     * @param limit 原始条数上限
     * @return 生效条数上限
     */
    private Integer resolveHistoryLimit(Integer limit) {
        if (limit == null) {
            return null;
        }
        Assert.isParamTrue(limit >= HISTORY_LIMIT_MIN, "历史查询条数不能小于1");
        return limit;
    }

    /**
     * 转换提示词详情视图对象。
     *
     * @param config 当前配置实体
     * @return 提示词详情视图对象
     */
    private AgentPromptConfigVo toPromptConfigVo(AgentPromptConfig config) {
        AgentPromptConfigVo vo = new AgentPromptConfigVo();
        vo.setPromptKey(config.getPromptKey());
        vo.setPromptContent(config.getPromptContent());
        vo.setPromptVersion(config.getPromptVersion());
        vo.setUpdatedAt(config.getUpdatedAt());
        vo.setUpdatedBy(config.getUpdateBy());
        return vo;
    }

    /**
     * 转换提示词历史视图对象。
     *
     * @param history 历史配置实体
     * @return 提示词历史视图对象
     */
    private AgentPromptHistoryVo toPromptHistoryVo(AgentPromptHistory history) {
        AgentPromptHistoryVo vo = new AgentPromptHistoryVo();
        vo.setPromptKey(history.getPromptKey());
        vo.setPromptVersion(history.getPromptVersion());
        vo.setPromptContent(history.getPromptContent());
        vo.setCreatedAt(history.getCreatedAt());
        vo.setCreatedBy(history.getCreateBy());
        return vo;
    }

    /**
     * 归一化并返回必填提示词键。
     *
     * @param promptKey 原始提示词键
     * @return 归一化后的提示词键
     */
    private String normalizeRequiredPromptKeyText(String promptKey) {
        String normalizedPromptKey = normalizeNullableText(promptKey);
        Assert.notEmpty(normalizedPromptKey, "提示词键不能为空");
        return normalizedPromptKey;
    }

    /**
     * 归一化并返回已注册的提示词键。
     *
     * @param promptKey 原始提示词键
     * @return 归一化后的提示词键
     */
    private String normalizeRegisteredPromptKey(String promptKey) {
        String normalizedPromptKey = normalizeRequiredPromptKeyText(promptKey);
        Assert.isParamTrue(
                loadRegisteredPromptKeyDescriptionMap().containsKey(normalizedPromptKey),
                UNSUPPORTED_PROMPT_KEY_MESSAGE.formatted(normalizedPromptKey)
        );
        return normalizedPromptKey;
    }

    /**
     * 归一化并返回必填提示词正文。
     *
     * @param promptContent 原始提示词正文
     * @return 归一化后的提示词正文
     */
    private String normalizeRequiredPromptContent(String promptContent) {
        Assert.isParamTrue(StringUtils.hasText(promptContent), "提示词内容不能为空");
        return promptContent;
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
