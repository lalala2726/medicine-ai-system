package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * @author Chuang
 * <p>
 * created on 2025/1/12
 */
@Getter
public class ParamException extends RuntimeException {

    private final Integer code;

    public ParamException(String message) {
        super(message);
        this.code = ResponseCode.PARAM_ERROR.getCode();
    }

    public ParamException(ResponseCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public ParamException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ParamException(ResponseCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }


}
