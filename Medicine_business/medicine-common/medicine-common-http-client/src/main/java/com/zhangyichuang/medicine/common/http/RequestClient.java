package com.zhangyichuang.medicine.common.http;

import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.http.exception.HttpClientException;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 轻量级 OkHttp 请求封装，支持自定义请求与响应类型解析。
 *
 * <p>默认以 JSON 解析响应体；当 dataClass/dataType 为 String 时直接返回原始文本。</p>
 *
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
public final class RequestClient {

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static volatile OkHttpClient CLIENT = new OkHttpClient();

    private RequestClient() {
    }

    /**
     * 注入自定义 OkHttpClient（由配置类统一构建）。
     */
    public static void setClient(OkHttpClient client) {
        if (client != null) {
            CLIENT = client;
        }
    }

    /**
     * GET 请求，返回原始文本。
     */
    public static HttpResult<String> get(String url) {
        return get(url, String.class);
    }

    /**
     * GET 请求，按指定类型解析响应体。
     */
    public static <T> HttpResult<T> get(String url, Class<T> dataClass) {
        ClientRequest request = buildRequest(url, HttpMethod.GET, null);
        return get(request, dataClass);
    }

    /**
     * GET 请求，外部传入 ClientRequest，method 会被强制为 GET。
     */
    public static HttpResult<String> get(ClientRequest request) {
        return get(request, String.class);
    }

    /**
     * GET 请求，外部传入 ClientRequest，method 会被强制为 GET。
     */
    public static <T> HttpResult<T> get(ClientRequest request, Class<T> dataClass) {
        return execute(request, dataClass, HttpMethod.GET);
    }

    /**
     * POST 请求，返回原始文本。
     */
    public static HttpResult<String> post(String url, String body) {
        return post(url, body, String.class);
    }

    /**
     * POST 请求，按指定类型解析响应体。
     */
    public static <T> HttpResult<T> post(String url, String body, Class<T> dataClass) {
        ClientRequest request = buildRequest(url, HttpMethod.POST, body);
        return post(request, dataClass);
    }

    /**
     * POST 请求，外部传入 ClientRequest，method 会被强制为 POST。
     */
    public static HttpResult<String> post(ClientRequest request) {
        return post(request, String.class);
    }

    /**
     * POST 请求，外部传入 ClientRequest，method 会被强制为 POST。
     */
    public static <T> HttpResult<T> post(ClientRequest request, Class<T> dataClass) {
        return execute(request, dataClass, HttpMethod.POST);
    }

    /**
     * PUT 请求，返回原始文本。
     */
    public static HttpResult<String> put(String url, String body) {
        return put(url, body, String.class);
    }

    /**
     * PUT 请求，按指定类型解析响应体。
     */
    public static <T> HttpResult<T> put(String url, String body, Class<T> dataClass) {
        ClientRequest request = buildRequest(url, HttpMethod.PUT, body);
        return put(request, dataClass);
    }

    /**
     * PUT 请求，外部传入 ClientRequest，method 会被强制为 PUT。
     */
    public static HttpResult<String> put(ClientRequest request) {
        return put(request, String.class);
    }

    /**
     * PUT 请求，外部传入 ClientRequest，method 会被强制为 PUT。
     */
    public static <T> HttpResult<T> put(ClientRequest request, Class<T> dataClass) {
        return execute(request, dataClass, HttpMethod.PUT);
    }

    /**
     * DELETE 请求，返回原始文本。
     */
    public static HttpResult<String> delete(String url) {
        return delete(url, null, String.class);
    }

    /**
     * DELETE 请求，按指定类型解析响应体。
     */
    public static <T> HttpResult<T> delete(String url, Class<T> dataClass) {
        return delete(url, null, dataClass);
    }

    /**
     * DELETE 请求，返回原始文本。
     */
    public static HttpResult<String> delete(String url, String body) {
        return delete(url, body, String.class);
    }

    /**
     * DELETE 请求，按指定类型解析响应体。
     */
    public static <T> HttpResult<T> delete(String url, String body, Class<T> dataClass) {
        ClientRequest request = buildRequest(url, HttpMethod.DELETE, body);
        return delete(request, dataClass);
    }

    /**
     * DELETE 请求，外部传入 ClientRequest，method 会被强制为 DELETE。
     */
    public static HttpResult<String> delete(ClientRequest request) {
        return delete(request, String.class);
    }

    /**
     * DELETE 请求，外部传入 ClientRequest，method 会被强制为 DELETE。
     */
    public static <T> HttpResult<T> delete(ClientRequest request, Class<T> dataClass) {
        return execute(request, dataClass, HttpMethod.DELETE);
    }

    /**
     * 执行请求并返回原始文本。
     */
    public static HttpResult<String> execute(ClientRequest request) {
        return execute(request, String.class, null);
    }

    /**
     * 执行请求并按 Class 解析响应体。
     */
    public static <T> HttpResult<T> execute(ClientRequest request, Class<T> dataClass) {
        return execute(request, dataClass, null);
    }

    /**
     * 执行请求并按 Type 解析响应体（支持泛型）。
     */
    public static <T> HttpResult<T> execute(ClientRequest request, Type dataType) {
        return executeInternal(request, null, body -> parseBody(body, dataType));
    }

    private static <T> HttpResult<T> execute(ClientRequest request, Class<T> dataClass, HttpMethod methodOverride) {
        return executeInternal(request, methodOverride, body -> parseBody(body, (Type) dataClass));
    }

    /**
     * 核心执行逻辑：统一发起请求并封装响应。
     */
    private static <T> HttpResult<T> executeInternal(
            ClientRequest request,
            HttpMethod methodOverride,
            BodyParser<T> parser
    ) {
        if (request == null) {
            throw new HttpClientException("ClientRequest must not be null");
        }
        Request okRequest = buildRequest(request, methodOverride);
        try (Response response = CLIENT.newCall(okRequest).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            T data = parser.parse(body);
            return HttpResult.<T>builder()
                    .statusCode(response.code())
                    .headers(response.headers())
                    .body(body)
                    .data(data)
                    .build();
        } catch (IOException ex) {
            throw new HttpClientException("Request failed", ex);
        } catch (RuntimeException ex) {
            throw new HttpClientException("Response parse failed", ex);
        }
    }

    /**
     * 构建 OkHttp Request：优先使用 methodOverride，若为空则使用 ClientRequest.method。
     */
    private static Request buildRequest(ClientRequest request, HttpMethod methodOverride) {
        HttpUrl url = request.getUrl();
        if (url == null) {
            throw new HttpClientException("ClientRequest.url must not be null");
        }
        HttpMethod method = methodOverride != null ? methodOverride : request.getMethod();
        if (method == null) {
            method = HttpMethod.GET;
        }
        Request.Builder builder = new Request.Builder().url(url);
        Headers headers = request.getHeaders();
        if (headers != null) {
            builder.headers(headers);
        }
        if (method == HttpMethod.GET) {
            builder.get();
        } else {
            String body = request.getBody();
            MediaType mediaType = resolveMediaType(headers);
            RequestBody requestBody = RequestBody.create(body == null ? "" : body, mediaType);
            builder.method(method.name(), requestBody);
        }
        return builder.build();
    }

    private static ClientRequest buildRequest(String url, HttpMethod method, String body) {
        try {
            return ClientRequest.builder()
                    .method(method)
                    .url(url)
                    .body(body)
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new HttpClientException("Invalid url: " + url, ex);
        }
    }


    /**
     * 以 headers 中的 Content-Type 为准，否则默认 JSON。
     */
    private static MediaType resolveMediaType(Headers headers) {
        if (headers == null) {
            return DEFAULT_MEDIA_TYPE;
        }
        String contentType = headers.get("Content-Type");
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_MEDIA_TYPE;
        }
        MediaType parsed = MediaType.parse(contentType);
        return parsed == null ? DEFAULT_MEDIA_TYPE : parsed;
    }

    /**
     * Type 解析：String 直接返回文本，Void 返回 null，其余按 JSON 解析。
     */
    @SuppressWarnings("unchecked")
    private static <T> T parseBody(String body, Type dataType) {
        if (body == null || body.isBlank()) {
            return dataType == String.class ? (T) (body == null ? "" : body) : null;
        }
        if (dataType == null || dataType == String.class) {
            return (T) body;
        }
        if (dataType == Void.class) {
            return null;
        }
        return JSONUtils.fromJson(body, dataType);
    }

    private interface BodyParser<T> {
        T parse(String body);
    }
}
