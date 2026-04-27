package com.zhangyichuang.medicine.common.http.model;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.http.exception.HttpClientException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseResponseTests {

    @Test
    void extractData_WhenDataIsString_ShouldReturnString() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":\"hello\"}");

        String data = BaseResponse.extractData(result);

        assertEquals("hello", data);
    }

    @Test
    void extractData_WhenDataIsObject_ShouldReturnJsonString() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":{\"name\":\"kb\",\"dim\":1024}}");

        String data = BaseResponse.extractData(result);

        JsonObject dataObj = JSONUtils.parseObject(data);
        assertEquals("kb", dataObj.get("name").getAsString());
        assertEquals(1024, dataObj.get("dim").getAsInt());
    }

    @Test
    void extractData_WhenDataIsNull_ShouldReturnNull() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":null}");

        String data = BaseResponse.extractData(result);

        assertNull(data);
    }

    @Test
    void extractData_WithClass_ShouldDeserializeEntity() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":{\"name\":\"kb\",\"dim\":1024}}");

        TestPayload payload = BaseResponse.extractData(result, TestPayload.class);

        assertNotNull(payload);
        assertEquals("kb", payload.getName());
        assertEquals(1024, payload.getDim());
    }

    @Test
    void extractData_WithType_ShouldDeserializeGeneric() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":[{\"name\":\"a\",\"dim\":1},{\"name\":\"b\",\"dim\":2}]}");
        Type listType = new TypeToken<List<TestPayload>>() {
        }.getType();

        List<TestPayload> payloads = BaseResponse.extractData(result, listType);

        assertNotNull(payloads);
        assertEquals(2, payloads.size());
        assertEquals("a", payloads.get(0).getName());
        assertEquals(2, payloads.get(1).getDim());
    }

    @Test
    void extractData_WhenHttpFailed_ShouldThrowException() {
        HttpResult<String> result = httpResult(500, "{\"code\":200,\"message\":\"ok\",\"data\":\"hello\"}");

        assertThrows(HttpClientException.class, () -> BaseResponse.extractData(result));
    }

    @Test
    void extractData_WhenBusinessCodeFailed_ShouldThrowException() {
        HttpResult<String> result = httpResult(200, "{\"code\":500,\"message\":\"failed\",\"data\":\"hello\"}");

        assertThrows(HttpClientException.class, () -> BaseResponse.extractData(result));
    }

    @Test
    void extractData_WhenBodyInvalid_ShouldThrowException() {
        HttpResult<String> result = httpResult(200, "not-json");

        assertThrows(HttpClientException.class, () -> BaseResponse.extractData(result));
    }

    @Test
    void extractData_WhenHttpResultNull_ShouldThrowException() {
        assertThrows(HttpClientException.class, () -> BaseResponse.extractData((HttpResult<String>) null));
    }

    @Test
    void extractData_WhenDataClassNull_ShouldThrowException() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":\"hello\"}");

        assertThrows(HttpClientException.class, () -> BaseResponse.extractData(result, (Class<Object>) null));
    }

    @Test
    void extractData_WhenDataTypeNull_ShouldThrowException() {
        HttpResult<String> result = httpResult(200, "{\"code\":200,\"message\":\"ok\",\"data\":\"hello\"}");

        assertThrows(HttpClientException.class, () -> BaseResponse.extractData(result, (Type) null));
    }

    private HttpResult<String> httpResult(int statusCode, String body) {
        return HttpResult.<String>builder()
                .statusCode(statusCode)
                .body(body)
                .build();
    }

    static class TestPayload {
        private String name;
        private Integer dim;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getDim() {
            return dim;
        }

        public void setDim(Integer dim) {
            this.dim = dim;
        }
    }
}
