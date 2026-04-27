package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * @author Chuang
 * <p>
 * created on 2025/1/11
 */
@Getter
public final class ServiceException extends RuntimeException {

    /**
     * 状态码
     */
    private final Integer code;


    public ServiceException(ResponseCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public ServiceException(ResponseCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public ServiceException(String message) {
        super(message);
        this.code = ResponseCode.ERROR.getCode();
    }

}
