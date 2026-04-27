package com.zhangyichuang.medicine.admin.integration;

import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.admin.config.KnowledgeBaseAiProperties;
import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.http.exception.HttpClientException;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import com.zhangyichuang.medicine.common.systemauth.client.SystemAuthRequestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MedicineAgentClientTests {

    @Test
    void createKnowledgeBase_WhenEmbeddingDimNotInSupportedSet_ShouldThrowParamException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);

        ParamException exception = assertThrows(ParamException.class,
                () -> client.createKnowledgeBase("kb_123", 1000, "desc"));

        assertEquals(KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE, exception.getMessage());
        verify(requestClient, never()).post(any(ClientRequest.class));
    }

    @Test
    void createKnowledgeBase_ShouldSendCorrectRequestWithoutAuthorizationHeader() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.post(requestCaptor.capture())).thenReturn(httpOk("{\"code\":200,\"message\":\"ok\"}"));

        client.createKnowledgeBase("kb_123", 1024, "desc");

        ClientRequest request = requestCaptor.getValue();
        assertEquals("http://localhost:8000/knowledge_base", request.getUrl().toString());
        assertNull(request.getHeaders() == null ? null : request.getHeaders().get("Authorization"));

        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals("kb_123", bodyJson.get("knowledge_name").getAsString());
        assertEquals(1024, bodyJson.get("embedding_dim").getAsInt());
        assertEquals("desc", bodyJson.get("description").getAsString());
    }

    @Test
    void createKnowledgeBase_WhenHttpStatusFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenReturn(httpError("{\"code\":500,\"message\":\"知识库已存在\"}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.createKnowledgeBase("kb_123", 1024, "desc"));
        assertEquals("调用Agent服务创建知识库失败: 知识库已存在", exception.getMessage());
    }

    @Test
    void createKnowledgeBase_WhenBodyCodeFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenReturn(httpOk("{\"code\":500,\"message\":\"向量维度不合法\"}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.createKnowledgeBase("kb_123", 1024, "desc"));
        assertEquals("调用Agent服务创建知识库失败: 向量维度不合法", exception.getMessage());
    }

    @Test
    void createKnowledgeBase_WhenHttpStatusFailedAndBodyMessageBlank_ShouldFallbackToStatusCode() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenReturn(httpError("{\"code\":500,\"message\":\"\"}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.createKnowledgeBase("kb_123", 1024, "desc"));
        assertEquals("调用Agent服务创建知识库失败: 请求失败，HTTP 状态码: 500", exception.getMessage());
    }

    @Test
    void createKnowledgeBase_WhenBodyInvalid_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenReturn(httpOk("not-json"));

        assertThrows(ServiceException.class, () -> client.createKnowledgeBase("kb_123", 1024, "desc"));
    }

    @Test
    void createKnowledgeBase_WhenNetworkError_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenThrow(new HttpClientException("network error"));

        assertThrows(ServiceException.class, () -> client.createKnowledgeBase("kb_123", 1024, "desc"));
    }

    @Test
    void loadKnowledgeBase_ShouldSendCorrectRequest() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.post(requestCaptor.capture())).thenReturn(httpOk("{\"code\":200,\"message\":\"ok\"}"));

        client.loadKnowledgeBase("kb_123");

        ClientRequest request = requestCaptor.getValue();
        assertEquals("http://localhost:8000/knowledge_base/load", request.getUrl().toString());
        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals("kb_123", bodyJson.get("collection_name").getAsString());
    }

    @Test
    void releaseKnowledgeBase_ShouldSendCorrectRequest() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.post(requestCaptor.capture())).thenReturn(httpOk("{\"code\":200,\"message\":\"ok\"}"));

        client.releaseKnowledgeBase("kb_123");

        ClientRequest request = requestCaptor.getValue();
        assertEquals("http://localhost:8000/knowledge_base/release", request.getUrl().toString());
        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals("kb_123", bodyJson.get("collection_name").getAsString());
    }

    @Test
    void loadKnowledgeBase_WhenHttpStatusFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenReturn(httpError("{\"code\":200,\"message\":\"ok\"}"));

        assertThrows(ServiceException.class, () -> client.loadKnowledgeBase("kb_123"));
    }

    @Test
    void releaseKnowledgeBase_WhenNetworkError_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.post(any(ClientRequest.class))).thenThrow(new HttpClientException("network error"));

        assertThrows(ServiceException.class, () -> client.releaseKnowledgeBase("kb_123"));
    }

    @Test
    void deleteKnowledgeBase_ShouldSendCorrectDeleteRequest() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.execute(requestCaptor.capture(), eq(String.class)))
                .thenReturn(httpOk("{\"code\":200,\"message\":\"删除成功\",\"data\":{\"knowledge_name\":\"kb_123\"}}"));

        client.deleteKnowledgeBase("kb_123");

        ClientRequest request = requestCaptor.getValue();
        assertEquals(HttpMethod.DELETE, request.getMethod());
        assertEquals("http://localhost:8000/knowledge_base", request.getUrl().toString());
        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals("kb_123", bodyJson.get("knowledge_name").getAsString());
        assertNull(request.getHeaders() == null ? null : request.getHeaders().get("Authorization"));
    }

    @Test
    void deleteKnowledgeBase_WhenBodyCodeFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.execute(any(ClientRequest.class), eq(String.class)))
                .thenReturn(httpOk("{\"code\":404,\"message\":\"knowledge 不存在\"}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.deleteKnowledgeBase("kb_123"));
        assertEquals("调用Agent服务删除知识库失败: knowledge 不存在", exception.getMessage());
    }

    @Test
    void deleteDocuments_ShouldSendCorrectRequest() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.execute(requestCaptor.capture(), eq(String.class)))
                .thenReturn(httpOk("{\"code\":200,\"message\":\"ok\"}"));

        client.deleteDocuments("kb_123", List.of(1001L, 1002L, 1001L));

        ClientRequest request = requestCaptor.getValue();
        assertEquals(HttpMethod.DELETE, request.getMethod());
        assertEquals("http://localhost:8000/knowledge_base/document", request.getUrl().toString());
        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals("kb_123", bodyJson.get("knowledge_name").getAsString());
        assertEquals(2, bodyJson.getAsJsonArray("document_ids").size());
        assertEquals(1001L, bodyJson.getAsJsonArray("document_ids").get(0).getAsLong());
        assertEquals(1002L, bodyJson.getAsJsonArray("document_ids").get(1).getAsLong());
        assertNull(request.getHeaders() == null ? null : request.getHeaders().get("Authorization"));
    }

    @Test
    void updateDocumentChunkStatus_ShouldSendCorrectPutRequest() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        when(requestClient.execute(requestCaptor.capture(), eq(String.class)))
                .thenReturn(httpOk("{\"code\":200,\"message\":\"更新成功\",\"data\":{\"knowledge_name\":\"demo_kb\",\"vector_id\":101,\"status\":1}}"));

        client.updateDocumentChunkStatus(101L, 1);

        ClientRequest request = requestCaptor.getValue();
        assertEquals(HttpMethod.PUT, request.getMethod());
        assertEquals("http://localhost:8000/knowledge_base/document/chunk/status", request.getUrl().toString());
        JsonObject bodyJson = JSONUtils.parseObject(request.getBody());
        assertEquals(101L, bodyJson.get("vector_id").getAsLong());
        assertEquals(1, bodyJson.get("status").getAsInt());
        assertNull(request.getHeaders() == null ? null : request.getHeaders().get("Authorization"));
    }

    @Test
    void updateDocumentChunkStatus_WhenBodyCodeFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.execute(any(ClientRequest.class), eq(String.class)))
                .thenReturn(httpOk("{\"code\":404,\"message\":\"向量记录不存在\",\"data\":null}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.updateDocumentChunkStatus(101L, 1));
        assertEquals("调用Agent服务修改切片状态失败: 向量记录不存在", exception.getMessage());
    }

    @Test
    void listDocumentChunks_ShouldPaginateAndAggregateRows() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.get(any(ClientRequest.class)))
                .thenReturn(httpOk("{\"code\":200,\"message\":\"ok\",\"data\":{\"rows\":[{\"id\":900001,\"document_id\":1001,\"chunk_index\":1,\"content\":\"A\",\"char_count\":1}],\"total\":2,\"page_num\":1,\"page_size\":50,\"has_next\":true}}"))
                .thenReturn(httpOk("{\"code\":200,\"message\":\"ok\",\"data\":{\"rows\":[{\"id\":900002,\"document_id\":1001,\"chunk_index\":2,\"content\":\"B\",\"char_count\":1}],\"total\":2,\"page_num\":2,\"page_size\":50,\"has_next\":false}}"));

        List<MedicineAgentClient.DocumentChunkRow> rows = client.listDocumentChunks("kb_123", 1001L);

        assertEquals(2, rows.size());
        assertEquals(900001L, rows.get(0).getId());
        assertEquals(900002L, rows.get(1).getId());

        ArgumentCaptor<ClientRequest> requestCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        verify(requestClient, times(2)).get(requestCaptor.capture());
        List<ClientRequest> requests = requestCaptor.getAllValues();
        assertEquals("kb_123", requests.get(0).getUrl().queryParameter("knowledge_name"));
        assertEquals("1001", requests.get(0).getUrl().queryParameter("document_id"));
        assertEquals("1", requests.get(0).getUrl().queryParameter("page"));
        assertEquals("50", requests.get(0).getUrl().queryParameter("page_size"));
        assertEquals("2", requests.get(1).getUrl().queryParameter("page"));
        assertNull(requests.get(0).getHeaders() == null ? null : requests.get(0).getHeaders().get("Authorization"));
    }

    @Test
    void listDocumentChunks_WhenBodyCodeFailed_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.get(any(ClientRequest.class))).thenReturn(httpOk("{\"code\":500,\"message\":\"文档不存在\"}"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> client.listDocumentChunks("kb_123", 1001L));
        assertEquals("调用Agent服务拉取文档切片失败: 文档不存在", exception.getMessage());
    }

    @Test
    void listDocumentChunks_WhenBodyInvalid_ShouldThrowException() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);
        when(requestClient.get(any(ClientRequest.class))).thenReturn(httpOk("not-json"));

        assertThrows(ServiceException.class, () -> client.listDocumentChunks("kb_123", 1001L));
    }

    @Test
    void buildChunkListRequest_ShouldContainExpectedQueryParameters() {
        SystemAuthRequestClient requestClient = mock(SystemAuthRequestClient.class);
        MedicineAgentClient client = newClient("http://localhost:8000", requestClient);

        ClientRequest request = client.buildChunkListRequest(
                "http://localhost:8000/knowledge_base/document/chunks/list", "kb_123", 1001L, 3, 50);

        assertEquals("kb_123", request.getUrl().queryParameter("knowledge_name"));
        assertEquals("1001", request.getUrl().queryParameter("document_id"));
        assertEquals("3", request.getUrl().queryParameter("page"));
        assertEquals("50", request.getUrl().queryParameter("page_size"));
    }

    private HttpResult<String> httpOk(String body) {
        return HttpResult.<String>builder()
                .statusCode(200)
                .body(body)
                .build();
    }

    private HttpResult<String> httpError(String body) {
        return HttpResult.<String>builder()
                .statusCode(500)
                .body(body)
                .build();
    }

    private MedicineAgentClient newClient(String baseUrl, SystemAuthRequestClient requestClient) {
        KnowledgeBaseAiProperties properties = new KnowledgeBaseAiProperties();
        properties.setBaseUrl(baseUrl);
        return new MedicineAgentClient(properties, requestClient);
    }
}
