package com.zhangyichuang.medicine.common.security.agreement;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 软件协议配置服务，负责 Redis 中的软件协议配置读写。
 */
@Service
@RequiredArgsConstructor
public class AgreementConfigService {

    /**
     * 未配置协议内容时用于页面展示和编辑初始化的默认 Markdown 文案。
     */
    private static final String DEFAULT_AGREEMENT_MARKDOWN = "暂未配置";

    private final RedisCache redisCache;

    /**
     * 读取软件协议配置。
     *
     * @return Redis 中的软件协议配置；未配置时返回默认展示内容
     */
    public AgreementConfig getAgreementConfig() {
        AgreementConfig config = redisCache.getCacheObject(RedisConstants.SystemConfig.AGREEMENT_CONFIG_KEY);
        return normalizeAgreementConfig(config);
    }

    /**
     * 更新软件协议配置。
     *
     * @param config 软件协议配置
     * @return 无返回值
     */
    public void updateAgreementConfig(AgreementConfig config) {
        Assert.notNull(config, "软件协议配置不能为空");
        Assert.isParamTrue(StringUtils.hasText(config.getSoftwareAgreementMarkdown()), "软件协议内容不能为空");
        Assert.isParamTrue(StringUtils.hasText(config.getPrivacyAgreementMarkdown()), "隐私协议内容不能为空");
        config.setUpdatedTime(LocalDateTime.now());
        redisCache.setCacheObject(RedisConstants.SystemConfig.AGREEMENT_CONFIG_KEY, config);
    }

    /**
     * 归一化软件协议配置，保证前端始终可以展示与进入编辑。
     *
     * @param config Redis 中读取到的软件协议配置
     * @return 已补齐默认文案的软件协议配置
     */
    private AgreementConfig normalizeAgreementConfig(AgreementConfig config) {
        AgreementConfig normalizedConfig = new AgreementConfig();
        if (config != null) {
            normalizedConfig.setSoftwareAgreementMarkdown(resolveAgreementMarkdown(config.getSoftwareAgreementMarkdown()));
            normalizedConfig.setPrivacyAgreementMarkdown(resolveAgreementMarkdown(config.getPrivacyAgreementMarkdown()));
            normalizedConfig.setUpdatedTime(config.getUpdatedTime());
            return normalizedConfig;
        }
        normalizedConfig.setSoftwareAgreementMarkdown(DEFAULT_AGREEMENT_MARKDOWN);
        normalizedConfig.setPrivacyAgreementMarkdown(DEFAULT_AGREEMENT_MARKDOWN);
        return normalizedConfig;
    }

    /**
     * 解析单项协议 Markdown 内容。
     *
     * @param markdownContent Redis 中读取到的协议 Markdown 内容
     * @return 有效内容或默认未配置文案
     */
    private String resolveAgreementMarkdown(String markdownContent) {
        if (StringUtils.hasText(markdownContent)) {
            return markdownContent;
        }
        return DEFAULT_AGREEMENT_MARKDOWN;
    }
}
