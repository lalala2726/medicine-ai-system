package com.zhangyichuang.medicine.common.http.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * HTTP 客户端配置。
 *
 * @author Chuang
 * created on 2026/1/31
 */
@Data
@ConfigurationProperties(prefix = "http")
public class HttpClientProperties {

    /**
     * 连接超时。
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * 读超时。
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * 写超时。
     */
    private Duration writeTimeout = Duration.ofSeconds(30);

    /**
     * 请求整体超时。
     */
    private Duration callTimeout = Duration.ofSeconds(60);

    /**
     * 是否在连接失败时重试。
     */
    private boolean retryOnConnectionFailure = true;
}
