package com.zhangyichuang.medicine.common.systemauth.client;

import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.systemauth.config.SystemAuthProperties;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthCanonicalBuilder;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthHeaders;
import com.zhangyichuang.medicine.common.systemauth.core.SystemAuthSigner;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SystemAuthRequestClientTests {

    private final SystemAuthCanonicalBuilder canonicalBuilder = new SystemAuthCanonicalBuilder();
    private final SystemAuthSigner signer = new SystemAuthSigner();

    @Test
    void signRequest_ShouldAddSystemHeadersAndKeepCustomHeaders() {
        SystemAuthRequestClient client = newClient("medicine-agent", "local-secret");
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.GET)
                .url("https://example.com/knowledge_base/document/chunks/list?b=2&a=1")
                .addHeader("X-Custom-Header", "value1")
                .build();

        ClientRequest signed = client.signRequest(request, HttpMethod.GET);

        assertEquals("medicine-agent", signed.getHeaders().get(SystemAuthHeaders.X_AGENT_KEY));
        assertEquals("v1", signed.getHeaders().get(SystemAuthHeaders.X_AGENT_SIGN_VERSION));
        assertNotNull(signed.getHeaders().get(SystemAuthHeaders.X_AGENT_TIMESTAMP));
        assertNotNull(signed.getHeaders().get(SystemAuthHeaders.X_AGENT_NONCE));
        assertNotNull(signed.getHeaders().get(SystemAuthHeaders.X_AGENT_SIGNATURE));
        assertEquals("value1", signed.getHeaders().get("X-Custom-Header"));
    }

    @Test
    void signRequest_ShouldUseBodyInSignature() {
        SystemAuthRequestClient client = newClient("medicine-agent", "local-secret");
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.POST)
                .url("https://example.com/knowledge_base")
                .body("{\"a\":1}")
                .build();

        ClientRequest signed = client.signRequest(request, HttpMethod.POST);
        String timestamp = signed.getHeaders().get(SystemAuthHeaders.X_AGENT_TIMESTAMP);
        String nonce = signed.getHeaders().get(SystemAuthHeaders.X_AGENT_NONCE);
        String actualSignature = signed.getHeaders().get(SystemAuthHeaders.X_AGENT_SIGNATURE);

        String canonicalForBodyA = canonicalBuilder.buildCanonical(
                HttpMethod.POST.name(),
                request.getUrl().encodedPath(),
                canonicalBuilder.buildSortedQuery(request.getUrl()),
                timestamp,
                nonce,
                canonicalBuilder.sha256Hex("{\"a\":1}".getBytes(StandardCharsets.UTF_8))
        );
        String signatureForBodyA = signer.sign("local-secret", canonicalForBodyA);
        assertEquals(signatureForBodyA, actualSignature);

        String canonicalForBodyB = canonicalBuilder.buildCanonical(
                HttpMethod.POST.name(),
                request.getUrl().encodedPath(),
                canonicalBuilder.buildSortedQuery(request.getUrl()),
                timestamp,
                nonce,
                canonicalBuilder.sha256Hex("{\"a\":2}".getBytes(StandardCharsets.UTF_8))
        );
        String signatureForBodyB = signer.sign("local-secret", canonicalForBodyB);
        assertNotEquals(signatureForBodyB, actualSignature);
    }

    @Test
    void signRequest_WhenSystemConfigMissing_ShouldThrowParamException() {
        SystemAuthRequestClient client = newClient("medicine-agent", null);
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.GET)
                .url("https://example.com/test")
                .build();

        assertThrows(ParamException.class, () -> client.signRequest(request, HttpMethod.GET));
    }

    @Test
    void signRequest_WhenRequestAlreadyContainsSystemHeaders_ShouldOverwriteByGeneratedValues() {
        SystemAuthRequestClient client = newClient("medicine-agent", "local-secret");
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.GET)
                .url("https://example.com/test")
                .addHeader(SystemAuthHeaders.X_AGENT_KEY, "old-key")
                .addHeader(SystemAuthHeaders.X_AGENT_SIGNATURE, "old-signature")
                .build();

        ClientRequest signed = client.signRequest(request, HttpMethod.GET);

        assertEquals(1, signed.getHeaders().values(SystemAuthHeaders.X_AGENT_KEY).size());
        assertEquals("medicine-agent", signed.getHeaders().get(SystemAuthHeaders.X_AGENT_KEY));
        assertNotEquals("old-signature", signed.getHeaders().get(SystemAuthHeaders.X_AGENT_SIGNATURE));
    }

    private SystemAuthRequestClient newClient(String appId, String secret) {
        SystemAuthProperties properties = new SystemAuthProperties();
        properties.setXAgentKey(appId);
        properties.setLocalSecret(secret);
        properties.setDefaultSignVersion("v1");
        return new SystemAuthRequestClient(properties, canonicalBuilder, signer);
    }
}
