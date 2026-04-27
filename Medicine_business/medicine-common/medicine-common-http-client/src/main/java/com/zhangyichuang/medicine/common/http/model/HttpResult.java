package com.zhangyichuang.medicine.common.http.model;

import lombok.Builder;
import lombok.Getter;
import okhttp3.Headers;

/**
 * 统一 HTTP 结果封装，包含状态码、响应头、原始内容与解析后的数据。
 *
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
@Getter
@Builder
public class HttpResult<T> {

    /**
     * HTTP 状态码。
     */
    private final int statusCode;

    /**
     * HTTP 响应头。
     */
    private final Headers headers;

    /**
     * HTTP 原始响应体。
     */
    private final String body;

    /**
     * 解析后的业务数据。
     */
    private final T data;

    /**
     * 判断 HTTP 请求是否成功。
     *
     * @return true-成功，false-失败
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
}
