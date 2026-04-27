package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * 认证失败异常
 *
 * @author Chuang
 * <p>
 * created on 2025/1/11
 */
@Getter
public final class AuthorizationException extends RuntimeException {

    /**
     * 状态码
     */
    private final Integer code;

    public AuthorizationException() {
        super(ResponseCode.AUTHORIZED.getMessage());
        this.code = ResponseCode.AUTHORIZED.getCode();
    }

    public AuthorizationException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }

    public AuthorizationException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public AuthorizationException(String message) {
        super(message);
        this.code = ResponseCode.AUTHORIZED.getCode();
    }

}
