package com.zhangyichuang.medicine.admin.integration;

import com.zhangyichuang.medicine.common.http.RequestClient;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import org.springframework.stereotype.Component;

/**
 * 大模型提供商连通性 HTTP 客户端。
 */
@Component
public class LlmProviderConnectivityClient {

    /**
     * 请求 OpenAI 兼容 models 端点。
     *
     * @param endpoint 实际请求地址
     * @param apiKey   API Key
     * @return HTTP 响应结果
     */
    public HttpResult<String> getModels(String endpoint, String apiKey) {
        ClientRequest request = ClientRequest.builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();
        return RequestClient.get(request);
    }
}
