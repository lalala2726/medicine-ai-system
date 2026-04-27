package com.zhangyichuang.medicine.common.core.exception;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import lombok.Getter;

/**
 * 访问限流异常。
 * <p>
 * 当请求命中滑动窗口限流策略时，使用该异常统一向上层返回限流错误。
 * </p>
 *
 * @author Chuang
 */
@Getter
public class AccessLimitException extends RuntimeException {

    /**
     * 业务响应码。
     */
    private final Integer code;

    /**
     * 构造访问限流异常。
     *
     * @param message 异常信息
     * @param code    业务响应码
     */
    public AccessLimitException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 构造访问限流异常。
     *
     * @param message 异常信息
     */
    public AccessLimitException(String message) {
        this(message, ResponseCode.TOO_MANY_REQUESTS.getCode());
    }
}
