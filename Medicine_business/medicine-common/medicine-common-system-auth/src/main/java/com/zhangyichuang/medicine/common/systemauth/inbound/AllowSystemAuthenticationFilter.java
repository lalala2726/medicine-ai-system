package com.zhangyichuang.medicine.common.systemauth.inbound;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.utils.ResponseUtils;
import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthCanonicalBuilder;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthClientRegistry;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthHeaders;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @AllowSystem 接口系统签名认证过滤器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllowSystemAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 默认签名版本。
     */
    private static final String DEFAULT_SIGN_VERSION = "v1";

    /**
     * 默认 nonce Redis key 前缀。
     */
    private static final String DEFAULT_NONCE_PREFIX = "system_auth:nonce";

    private final SystemAuthProperties properties;
    private final AllowSystemEndpointRegistry endpointRegistry;
    private final SystemAuthClientRegistry clientRegistry;
    private final SystemAuthCanonicalBuilder canonicalBuilder;
    private final SystemAuthSigner signer;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        return !endpointRegistry.requiresSystemAuth(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = request instanceof CachedBodyHttpServletRequest cached
                ? cached
                : new CachedBodyHttpServletRequest(request);

        VerifyResult verifyResult = verifyRequest(wrappedRequest);
        if (!verifyResult.passed()) {
            ResponseUtils.writeErrMsg(response, verifyResult.httpStatus(), verifyResult.responseCode(), verifyResult.message());
            return;
        }
        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * 依次执行请求头校验、签名校验与防重放校验。
     *
     * @param request 已缓存请求体的请求对象
     * @return 校验结果
     */
    private VerifyResult verifyRequest(CachedBodyHttpServletRequest request) {
        String agentKey = trimHeader(request.getHeader(SystemAuthHeaders.X_AGENT_KEY));
        String agentTimestamp = trimHeader(request.getHeader(SystemAuthHeaders.X_AGENT_TIMESTAMP));
        String agentNonce = trimHeader(request.getHeader(SystemAuthHeaders.X_AGENT_NONCE));
        String signVersion = trimHeader(request.getHeader(SystemAuthHeaders.X_AGENT_SIGN_VERSION));
        String agentSignature = trimHeader(request.getHeader(SystemAuthHeaders.X_AGENT_SIGNATURE));

        List<String> missingHeaders = collectMissingHeaders(agentKey, agentTimestamp, agentNonce, signVersion, agentSignature);
        if (!missingHeaders.isEmpty()) {
            return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED,
                    "Missing required system-auth headers: " + String.join(",", missingHeaders));
        }

        String expectedSignVersion = StringUtils.defaultIfBlank(properties.getDefaultSignVersion(), DEFAULT_SIGN_VERSION);
        if (!Objects.equals(expectedSignVersion, signVersion)) {
            return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED,
                    "X-Agent-Sign-Version is invalid");
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(agentTimestamp);
        } catch (NumberFormatException ex) {
            return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED, "Invalid timestamp");
        }
        long nowSeconds = Instant.now().getEpochSecond();
        long maxSkewSeconds = Math.max(0L, properties.getMaxSkewSeconds());
        if (Math.abs(nowSeconds - timestampSeconds) > maxSkewSeconds) {
            return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED, "Timestamp out of allowed window");
        }

        SystemAuthClientRegistry.SystemAuthClient client = clientRegistry.findEnabledClient(agentKey).orElse(null);
        if (client == null) {
            return VerifyResult.fail(HttpStatus.FORBIDDEN, ResponseCode.FORBIDDEN, "System client is not registered");
        }

        String canonical = canonicalBuilder.buildCanonical(
                request.getMethod(),
                request.getRequestURI(),
                canonicalBuilder.buildSortedQuery(request.getQueryString()),
                agentTimestamp,
                agentNonce,
                canonicalBuilder.sha256Hex(request.getCachedBody())
        );
        String expectedSignature = signer.sign(client.getSecret(), canonical);
        if (!signer.constantTimeEquals(expectedSignature, agentSignature)) {
            return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED, "Signature mismatch");
        }

        String nonceKey = buildNonceKey(agentKey, agentNonce);
        try {
            Boolean nonceSet = redisTemplate.opsForValue().setIfAbsent(
                    nonceKey,
                    "1",
                    Math.max(1L, properties.getNonceTtlSeconds()),
                    TimeUnit.SECONDS
            );
            if (!Boolean.TRUE.equals(nonceSet)) {
                return VerifyResult.fail(HttpStatus.UNAUTHORIZED, ResponseCode.UNAUTHORIZED, "Nonce replay detected");
            }
        } catch (Exception ex) {
            log.error("System auth nonce storage failed: {}", ex.getMessage(), ex);
            return VerifyResult.fail(HttpStatus.SERVICE_UNAVAILABLE, ResponseCode.SERVICE_UNAVAILABLE,
                    "System auth nonce storage unavailable");
        }

        return VerifyResult.ok();
    }

    /**
     * 收集缺失的系统签名请求头名称。
     */
    private List<String> collectMissingHeaders(String agentKey,
                                               String timestamp,
                                               String nonce,
                                               String signVersion,
                                               String signature) {
        List<String> missing = new ArrayList<>();
        if (StringUtils.isBlank(agentKey)) {
            missing.add(SystemAuthHeaders.X_AGENT_KEY);
        }
        if (StringUtils.isBlank(timestamp)) {
            missing.add(SystemAuthHeaders.X_AGENT_TIMESTAMP);
        }
        if (StringUtils.isBlank(nonce)) {
            missing.add(SystemAuthHeaders.X_AGENT_NONCE);
        }
        if (StringUtils.isBlank(signVersion)) {
            missing.add(SystemAuthHeaders.X_AGENT_SIGN_VERSION);
        }
        if (StringUtils.isBlank(signature)) {
            missing.add(SystemAuthHeaders.X_AGENT_SIGNATURE);
        }
        return missing;
    }

    /**
     * 生成 nonce 防重放 Redis key。
     */
    private String buildNonceKey(String appId, String nonce) {
        String prefix = StringUtils.defaultIfBlank(properties.getNonceKeyPrefix(), DEFAULT_NONCE_PREFIX);
        return prefix + ":" + appId + ":" + nonce;
    }

    /**
     * 对请求头值做 trim，统一空白处理。
     */
    private String trimHeader(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 单次验签结果。
     */
    private record VerifyResult(boolean passed, HttpStatus httpStatus, ResponseCode responseCode, String message) {
        /**
         * 构造成功结果。
         */
        private static VerifyResult ok() {
            return new VerifyResult(true, HttpStatus.OK, ResponseCode.SUCCESS, null);
        }

        /**
         * 构造失败结果。
         */
        private static VerifyResult fail(HttpStatus status, ResponseCode code, String message) {
            return new VerifyResult(false, status, code, message);
        }
    }
}
