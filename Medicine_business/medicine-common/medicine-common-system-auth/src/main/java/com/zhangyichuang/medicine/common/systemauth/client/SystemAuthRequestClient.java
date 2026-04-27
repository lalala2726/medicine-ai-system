package com.zhangyichuang.medicine.common.systemauth.client;

import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.http.RequestClient;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthCanonicalBuilder;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthHeaders;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthSigner;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * 系统签名请求客户端。
 * <p>
 * 仅在原有 {@link ClientRequest} 基础上补齐签名头，不改动调用方请求组装方式。
 */
@Component
@RequiredArgsConstructor
public class SystemAuthRequestClient {

    /**
     * 默认签名版本。
     */
    private static final String DEFAULT_SIGN_VERSION = "v1";

    private final SystemAuthProperties properties;
    private final SystemAuthCanonicalBuilder canonicalBuilder;
    private final SystemAuthSigner signer;

    /**
     * 发送带系统签名头的 GET 请求。
     */
    public HttpResult<String> get(ClientRequest request) {
        return get(request, String.class);
    }

    /**
     * 发送带系统签名头的 GET 请求并按指定类型解析响应。
     */
    public <T> HttpResult<T> get(ClientRequest request, Class<T> dataClass) {
        return RequestClient.get(signRequest(request, HttpMethod.GET), dataClass);
    }

    /**
     * 发送带系统签名头的 POST 请求。
     */
    public HttpResult<String> post(ClientRequest request) {
        return post(request, String.class);
    }

    /**
     * 发送带系统签名头的 POST 请求并按指定类型解析响应。
     */
    public <T> HttpResult<T> post(ClientRequest request, Class<T> dataClass) {
        return RequestClient.post(signRequest(request, HttpMethod.POST), dataClass);
    }

    /**
     * 发送带系统签名头的请求并按指定 Class 解析响应。
     */
    public <T> HttpResult<T> execute(ClientRequest request, Class<T> dataClass) {
        return RequestClient.execute(signRequest(request, null), dataClass);
    }

    /**
     * 发送带系统签名头的请求并按指定 Type 解析响应。
     */
    public <T> HttpResult<T> execute(ClientRequest request, Type dataType) {
        return RequestClient.execute(signRequest(request, null), dataType);
    }

    /**
     * 复制原请求并补齐 X-Agent 系统签名请求头。
     */
    ClientRequest signRequest(ClientRequest request, HttpMethod methodOverride) {
        Assert.notNull(request, "ClientRequest must not be null");
        HttpUrl url = request.getUrl();
        Assert.notNull(url, "ClientRequest.url must not be null");

        String appId = StringUtils.trimToEmpty(properties.getXAgentKey());
        String localSecret = StringUtils.trimToEmpty(properties.getLocalSecret());
        Assert.notEmpty(appId, "system-auth.x-agent-key 未配置");
        Assert.notEmpty(localSecret, "system-auth.local-secret 未配置");

        HttpMethod method = methodOverride != null ? methodOverride : request.getMethod();
        if (method == null) {
            method = HttpMethod.GET;
        }
        long timestampSeconds = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();
        String body = request.getBody();
        String bodySha256 = canonicalBuilder.sha256Hex(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        String canonical = canonicalBuilder.buildCanonical(
                method.name(),
                url.encodedPath(),
                canonicalBuilder.buildSortedQuery(url),
                String.valueOf(timestampSeconds),
                nonce,
                bodySha256
        );
        String signature = signer.sign(localSecret, canonical);

        ClientRequest.Builder builder = ClientRequest.builder()
                .method(method)
                .url(url)
                .body(body);

        Headers headers = request.getHeaders();
        if (headers != null) {
            for (String name : headers.names()) {
                if (isSystemAuthHeader(name)) {
                    continue;
                }
                for (String value : headers.values(name)) {
                    builder.addHeader(name, value);
                }
            }
        }

        builder.addHeader(SystemAuthHeaders.X_AGENT_KEY, appId);
        builder.addHeader(SystemAuthHeaders.X_AGENT_TIMESTAMP, String.valueOf(timestampSeconds));
        builder.addHeader(SystemAuthHeaders.X_AGENT_NONCE, nonce);
        builder.addHeader(SystemAuthHeaders.X_AGENT_SIGN_VERSION,
                StringUtils.defaultIfBlank(properties.getDefaultSignVersion(), DEFAULT_SIGN_VERSION));
        builder.addHeader(SystemAuthHeaders.X_AGENT_SIGNATURE, signature);
        return builder.build();
    }

    /**
     * 判断给定请求头是否属于系统签名请求头。
     */
    private boolean isSystemAuthHeader(String headerName) {
        return equalsIgnoreCase(headerName, SystemAuthHeaders.X_AGENT_KEY)
                || equalsIgnoreCase(headerName, SystemAuthHeaders.X_AGENT_TIMESTAMP)
                || equalsIgnoreCase(headerName, SystemAuthHeaders.X_AGENT_NONCE)
                || equalsIgnoreCase(headerName, SystemAuthHeaders.X_AGENT_SIGN_VERSION)
                || equalsIgnoreCase(headerName, SystemAuthHeaders.X_AGENT_SIGNATURE);
    }

    /**
     * 使用 JDK String API 进行忽略大小写比较，避免依赖已弃用的 CharSequence 重载。
     */
    private boolean equalsIgnoreCase(String source, String target) {
        return source != null && source.equalsIgnoreCase(target);
    }
}
