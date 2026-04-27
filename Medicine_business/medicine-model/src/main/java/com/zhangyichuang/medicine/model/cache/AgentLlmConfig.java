package com.zhangyichuang.medicine.model.cache;

import java.io.Serializable;

/**
 * 当前启用 LLM 提供商运行时配置。
 */
public class AgentLlmConfig implements Serializable {

    /**
     * 提供商类型：aliyun。
     */
    private String providerType;

    /**
     * 模型调用基础地址。
     */
    private String baseUrl;

    /**
     * 提供商 API Key。
     */
    private String apiKey;

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
