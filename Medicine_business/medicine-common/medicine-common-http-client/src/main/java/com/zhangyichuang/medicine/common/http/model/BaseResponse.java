package com.zhangyichuang.medicine.common.http.model;

import com.google.gson.reflect.TypeToken;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.http.exception.HttpClientException;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * 通用业务响应对象。
 *
 * @param <T> 业务数据类型
 * @author Chuang
 * created on 2026/2/1
 */
@Data
public class BaseResponse<T> {

    /**
     * 业务成功状态码。
     */
    private static final int SUCCESS_CODE = 200;

    /**
     * 业务状态码。
     */
    private int code;

    /**
     * 业务消息。
     */
    private String message;

    /**
     * 业务数据。
     */
    private T data;

    /**
     * 根据 dataClass 解析 JSON 为 BaseResponse。
     *
     * @param json      原始 JSON
     * @param dataClass 业务数据类型
     * @param <T>       业务数据泛型
     * @return 解析后的 BaseResponse
     */
    public static <T> BaseResponse<T> fromJson(String json, Class<T> dataClass) {
        Type type = TypeToken.getParameterized(BaseResponse.class, dataClass).getType();
        return JSONUtils.fromJson(json, type);
    }

    /**
     * 根据 dataType 解析 JSON 为 BaseResponse（支持泛型）。
     *
     * @param json     原始 JSON
     * @param dataType 业务数据类型
     * @param <T>      业务数据泛型
     * @return 解析后的 BaseResponse
     */
    public static <T> BaseResponse<T> fromJson(String json, Type dataType) {
        Type type = TypeToken.getParameterized(BaseResponse.class, dataType).getType();
        return JSONUtils.fromJson(json, type);
    }

    /**
     * 从 HttpResult 中提取 data 字符串，并校验 HTTP 与业务状态。
     *
     * @param httpResult HTTP 调用结果
     * @return data 字符串；data 为空时返回 null
     */
    public static String extractData(HttpResult<String> httpResult) {
        BaseResponse<Object> response = parseAndValidate(httpResult);
        Object data = response.getData();
        if (data == null) {
            return null;
        }
        if (data instanceof String || data instanceof Number || data instanceof Boolean || data instanceof Character) {
            return String.valueOf(data);
        }
        return JSONUtils.toJson(data);
    }

    /**
     * 从 HttpResult 中提取 data，并按 Class 反序列化。
     *
     * @param httpResult HTTP 调用结果
     * @param dataClass  data 目标类型
     * @param <T>        data 目标泛型
     * @return 反序列化后的 data；data 为空时返回 null
     */
    public static <T> T extractData(HttpResult<String> httpResult, Class<T> dataClass) {
        if (dataClass == null) {
            throw new HttpClientException("dataClass 不能为空");
        }
        if (dataClass == String.class) {
            return dataClass.cast(extractData(httpResult));
        }
        String dataJson = toDataJson(httpResult);
        if (dataJson == null) {
            return null;
        }
        try {
            return JSONUtils.fromJson(dataJson, dataClass);
        } catch (RuntimeException ex) {
            throw new HttpClientException("响应 data 解析失败", ex);
        }
    }

    /**
     * 从 HttpResult 中提取 data，并按 Type 反序列化（支持泛型）。
     *
     * @param httpResult HTTP 调用结果
     * @param dataType   data 目标类型
     * @param <T>        data 目标泛型
     * @return 反序列化后的 data；data 为空时返回 null
     */
    public static <T> T extractData(HttpResult<String> httpResult, Type dataType) {
        if (dataType == null) {
            throw new HttpClientException("dataType 不能为空");
        }
        String dataJson = toDataJson(httpResult);
        if (dataJson == null) {
            return null;
        }
        try {
            return JSONUtils.fromJson(dataJson, dataType);
        } catch (RuntimeException ex) {
            throw new HttpClientException("响应 data 解析失败", ex);
        }
    }

    /**
     * 将 data 字段转为 JSON。
     *
     * @param httpResult HTTP 调用结果
     * @return data JSON 字符串
     */
    private static String toDataJson(HttpResult<String> httpResult) {
        BaseResponse<Object> response = parseAndValidate(httpResult);
        Object data = response.getData();
        if (data == null) {
            return null;
        }
        return JSONUtils.toJson(data);
    }

    /**
     * 解析并校验业务响应。
     *
     * @param httpResult HTTP 调用结果
     * @return 解析后的业务响应
     */
    private static BaseResponse<Object> parseAndValidate(HttpResult<String> httpResult) {
        if (httpResult == null) {
            throw new HttpClientException("HttpResult 不能为空");
        }
        if (!httpResult.isSuccessful()) {
            throw new HttpClientException("请求失败，HTTP 状态码: " + httpResult.getStatusCode());
        }
        String body = httpResult.getBody();
        if (body == null || body.isBlank()) {
            throw new HttpClientException("响应体不能为空");
        }
        BaseResponse<Object> response;
        try {
            response = BaseResponse.fromJson(body, Object.class);
        } catch (RuntimeException ex) {
            throw new HttpClientException("响应体解析失败", ex);
        }
        if (response == null) {
            throw new HttpClientException("响应体解析失败");
        }
        if (response.getCode() != SUCCESS_CODE) {
            throw new HttpClientException("业务失败，code=" + response.getCode() + ", message=" + response.getMessage());
        }
        return response;
    }
}
