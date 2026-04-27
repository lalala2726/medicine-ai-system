package com.zhangyichuang.medicine.admin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库 AI 服务配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "knowledge-base.ai")
public class KnowledgeBaseAiProperties {

    /**
     * AI 服务地址。
     */
    private String baseUrl = "http://localhost:8000";
}

