package com.zhangyichuang.medicine.common.systemauth.inbound;

import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthCanonicalBuilder;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthClientRegistry;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthHeaders;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllowSystemAuthenticationFilterTests {

    private static final String APP_ID = "medicine-admin";
    private static final String SECRET = "secret-123";
    private final SystemAuthCanonicalBuilder canonicalBuilder = new SystemAuthCanonicalBuilder();
    private final SystemAuthSigner signer = new SystemAuthSigner();
    @Mock
    private AllowSystemEndpointRegistry endpointRegistry;
    @Mock
    private SystemAuthClientRegistry clientRegistry;
    @Mock
    private RedisTemplate<Object, Object> redisTemplate;
    @Mock
    private ValueOperations<Object, Object> valueOperations;
    private AllowSystemAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SystemAuthProperties properties = new SystemAuthProperties();
        properties.setEnabled(true);
        properties.setDefaultSignVersion("v1");
        properties.setMaxSkewSeconds(300);
        properties.setNonceTtlSeconds(600);
        properties.setNonceKeyPrefix("system_auth:nonce");
        filter = new AllowSystemAuthenticationFilter(
                properties,
                endpointRegistry,
                clientRegistry,
                canonicalBuilder,
                signer,
                redisTemplate
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(clientRegistry.findEnabledClient(APP_ID)).thenReturn(Optional.of(enabledClient(APP_ID, SECRET)));
    }

    @Test
    void doFilter_WhenEndpointDoesNotRequireSystemAuth_ShouldSkipValidation() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/open/path");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        verify(clientRegistry, never()).findEnabledClient(anyString());
    }

    @Test
    void doFilter_WhenMissingHeaders_ShouldReturn401() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/path");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void doFilter_WhenAuthorizationHeaderExistsAndRequestValid_ShouldPass() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        request.addHeader(SystemAuthHeaders.AUTHORIZATION, "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        verify(valueOperations).setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void doFilter_WhenAuthorizationHeaderExistsAndMissingSystemHeaders_ShouldReturn401() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/path");
        request.addHeader(SystemAuthHeaders.AUTHORIZATION, "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing required system-auth headers"));
    }

    @Test
    void doFilter_WhenTimestampOutOfWindow_ShouldReturn401() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        MockHttpServletRequest request = buildSignedRequest(Instant.now().minusSeconds(1000).getEpochSecond());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void doFilter_WhenClientNotRegistered_ShouldReturn403() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(clientRegistry.findEnabledClient(APP_ID)).thenReturn(Optional.empty());
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void doFilter_WhenAuthorizationHeaderExistsAndSignatureMismatch_ShouldReturn401() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(clientRegistry.findEnabledClient(APP_ID)).thenReturn(Optional.of(enabledClient(APP_ID, "another-secret")));
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        request.addHeader(SystemAuthHeaders.AUTHORIZATION, "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Signature mismatch"));
    }

    @Test
    void doFilter_WhenNonceReplay_ShouldReturn401() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void doFilter_WhenNonceStorageFails_ShouldReturn503() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS)))
                .thenThrow(new RuntimeException("redis down"));
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(503, response.getStatus());
    }

    @Test
    void doFilter_WhenRequestValid_ShouldPass() throws Exception {
        when(endpointRegistry.requiresSystemAuth(any())).thenReturn(true);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        MockHttpServletRequest request = buildSignedRequest(Instant.now().getEpochSecond());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest buildSignedRequest(long timestampSeconds) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/knowledge_base/document/chunks/list");
        request.setQueryString("document_id=1&knowledge_name=kb_a");
        String nonce = UUID.randomUUID().toString();
        String canonical = canonicalBuilder.buildCanonical(
                request.getMethod(),
                request.getRequestURI(),
                canonicalBuilder.buildSortedQuery(request.getQueryString()),
                String.valueOf(timestampSeconds),
                nonce,
                canonicalBuilder.sha256Hex(new byte[0])
        );
        String signature = signer.sign(SECRET, canonical);
        request.addHeader(SystemAuthHeaders.X_AGENT_KEY, APP_ID);
        request.addHeader(SystemAuthHeaders.X_AGENT_TIMESTAMP, String.valueOf(timestampSeconds));
        request.addHeader(SystemAuthHeaders.X_AGENT_NONCE, nonce);
        request.addHeader(SystemAuthHeaders.X_AGENT_SIGN_VERSION, "v1");
        request.addHeader(SystemAuthHeaders.X_AGENT_SIGNATURE, signature);
        return request;
    }

    private SystemAuthClientRegistry.SystemAuthClient enabledClient(String appId, String secret) {
        SystemAuthClientRegistry.SystemAuthClient client = new SystemAuthClientRegistry.SystemAuthClient();
        client.setAppId(appId);
        client.setSecret(secret);
        client.setEnabled(true);
        return client;
    }
}
