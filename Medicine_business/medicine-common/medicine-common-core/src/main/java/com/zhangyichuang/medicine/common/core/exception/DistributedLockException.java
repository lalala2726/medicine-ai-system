package com.zhangyichuang.medicine.common.core.exception;

import lombok.Getter;

/**
 * 分布式锁异常。
 * <p>
 * 当业务方法获取分布式锁失败、锁键不合法或线程在加锁过程中被中断时，
 * 使用该异常统一向上层返回明确的业务错误。
 * </p>
 *
 * @author Chuang
 */
@Getter
public class DistributedLockException extends RuntimeException {

    /**
     * 业务响应码。
     */
    private final Integer code;

    /**
     * 构造分布式锁异常。
     *
     * @param message 异常信息
     * @param code    业务响应码
     */
    public DistributedLockException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 构造分布式锁异常。
     *
     * @param message 异常信息
     */
    public DistributedLockException(String message) {
        this(message, com.zhangyichuang.medicine.common.core.enums.ResponseCode.OPERATION_ERROR.getCode());
    }
}
