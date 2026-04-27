package com.zhangyichuang.medicine.common.http;

import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestClientTests {

    private MockWebServer server;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getOverridesMethodAndReturnsString() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("OK"));
        HttpUrl url = server.url("/ping");
        Headers headers = new Headers.Builder()
                .add("Content-Type", "text/plain")
                .build();
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.POST)
                .url(url)
                .headers(headers)
                .body("ignored")
                .build();

        HttpResult<String> result = RequestClient.get(request);

        assertEquals(200, result.getStatusCode());
        assertEquals("OK", result.getData());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals(0, recorded.getBody().size());
    }

    @Test
    void executeParsesJsonToClass() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"Alice\",\"age\":30}"));
        HttpUrl url = server.url("/user");
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .build();

        HttpResult<TestPayload> result = RequestClient.execute(request, TestPayload.class);

        assertEquals(200, result.getStatusCode());
        assertNotNull(result.getData());
        assertEquals("Alice", result.getData().name);
        assertEquals(30, result.getData().age);
    }

    @Test
    void builderAddsQueryParameters() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("OK"));
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.GET)
                .url(server.url("/search").toString())
                .addQueryParameter("q", "medicine")
                .addQueryParameter("page", "1")
                .build();

        RequestClient.get(request);

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/search?q=medicine&page=1", recorded.getPath());
    }

    static class TestPayload {
        String name;
        int age;
    }
}
