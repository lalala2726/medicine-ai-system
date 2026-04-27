package com.zhangyichuang.medicine.common.elasticsearch.config;

import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Java Client 兼容性配置。
 * <p>
 * Spring Boot 4 默认集成的 Elasticsearch Java Client 9.x 会按 9.x 协议发送媒体类型头。
 * 当前项目连接的 Elasticsearch 服务端为 8.x，因此需要显式声明 8.x 兼容媒体类型，
 * 避免服务端拒绝 Accept 与 Content-Type 请求头。
 */
@Configuration
public class ElasticsearchCompatibilityConfig {

    /**
     * Elasticsearch API 兼容版本号。
     */
    private static final String ELASTICSEARCH_API_COMPATIBILITY_VERSION = "8";

    /**
     * Elasticsearch 兼容媒体类型。
     */
    private static final String ELASTICSEARCH_COMPATIBLE_MEDIA_TYPE =
            "application/vnd.elasticsearch+json; compatible-with=" + ELASTICSEARCH_API_COMPATIBILITY_VERSION;

    /**
     * 自定义 Elasticsearch Rest5 客户端请求头兼容策略。
     * <p>
     * Elasticsearch Java Client 9.x 会默认发送 compatible-with=9，ES 8.x 服务端会拒绝；
     * 这里通过请求拦截器统一重写为 compatible-with=8，保证请求头只有单个有效值。
     *
     * @return Elasticsearch Rest5 客户端构建自定义器
     */
    @Bean
    public Rest5ClientBuilderCustomizer elasticsearchCompatibilityHeadersCustomizer() {
        return new Rest5ClientBuilderCustomizer() {
            @Override
            public void customize(@NonNull Rest5ClientBuilder builder) {
                // 仅通过 HttpClient 拦截器重写请求头，不在此处注入默认头，避免重复头值。
            }

            @Override
            public void customize(@NonNull HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder.addRequestInterceptorLast((request, entityDetails, context) -> {
                    rewriteCompatibilityHeader(request, HttpHeaders.ACCEPT, true);
                    rewriteCompatibilityHeader(request, HttpHeaders.CONTENT_TYPE, false);
                });
            }
        };
    }

    /**
     * 重写 Elasticsearch 兼容请求头。
     *
     * @param request           HTTP 请求
     * @param headerName        请求头名称
     * @param setWhenHeaderMiss 请求头不存在时是否补写
     */
    private void rewriteCompatibilityHeader(HttpRequest request, String headerName, boolean setWhenHeaderMiss) {
        if (!request.containsHeader(headerName) && !setWhenHeaderMiss) {
            return;
        }
        request.removeHeaders(headerName);
        request.addHeader(headerName, ELASTICSEARCH_COMPATIBLE_MEDIA_TYPE);
    }
}
