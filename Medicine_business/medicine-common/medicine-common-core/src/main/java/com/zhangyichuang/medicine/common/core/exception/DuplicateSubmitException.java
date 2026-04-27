package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * 重复提交异常。
 * <p>
 * 当同一用户在短时间内对同一业务请求重复提交时，
 * 使用该异常统一向上层返回明确的错误提示。
 * </p>
 */
@Getter
public class DuplicateSubmitException extends RuntimeException {

    /**
     * 业务响应码。
     */
    private final Integer code;

    /**
     * 构造重复提交异常。
     *
     * @param message 异常信息
     * @param code    业务响应码
     */
    public DuplicateSubmitException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 构造重复提交异常。
     *
     * @param message 异常信息
     */
    public DuplicateSubmitException(String message) {
        this(message, ResponseCode.TOO_MANY_REQUESTS.getCode());
    }
}
