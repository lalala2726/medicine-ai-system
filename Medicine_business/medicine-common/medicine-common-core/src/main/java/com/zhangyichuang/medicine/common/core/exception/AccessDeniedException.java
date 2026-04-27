package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * 授权失败异常
 *
 * @author Chuang
 * <p>
 * created on 2025/7/27
 */
@Getter
public final class AccessDeniedException extends RuntimeException {

    /**
     * 状态码
     */
    private final Integer code;

    public AccessDeniedException() {
        super(ResponseCode.FORBIDDEN.getMessage());
        this.code = ResponseCode.FORBIDDEN.getCode();
    }

    public AccessDeniedException(String message) {
        super(message);
        this.code = ResponseCode.FORBIDDEN.getCode();
    }

    public AccessDeniedException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = ResponseCode.FORBIDDEN.getCode();
    }


}
