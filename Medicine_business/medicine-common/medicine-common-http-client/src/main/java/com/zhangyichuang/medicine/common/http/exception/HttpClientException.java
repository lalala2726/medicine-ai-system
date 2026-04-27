package com.zhangyichuang.medicine.common.http.exception;

import lombok.Getter;

/**
 * HTTP 调用异常，便于统一捕获与定位。
 *
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
@Getter
public class HttpClientException extends RuntimeException {

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
