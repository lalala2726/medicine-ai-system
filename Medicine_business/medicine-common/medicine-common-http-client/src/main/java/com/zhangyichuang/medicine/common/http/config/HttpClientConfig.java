package com.zhangyichuang.medicine.common.http.config;

import com.zhangyichuang.medicine.common.http.RequestClient;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OkHttp 客户端配置入口，统一基于 HttpClientProperties 构建。
 *
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
@Configuration
@EnableConfigurationProperties(HttpClientProperties.class)
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient(HttpClientProperties properties) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .writeTimeout(properties.getWriteTimeout())
                .callTimeout(properties.getCallTimeout())
                .retryOnConnectionFailure(properties.isRetryOnConnectionFailure())
                .build();
        RequestClient.setClient(client);
        return client;
    }
}
