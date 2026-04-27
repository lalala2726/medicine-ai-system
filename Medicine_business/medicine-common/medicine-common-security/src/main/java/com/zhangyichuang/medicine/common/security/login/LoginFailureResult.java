package com.zhangyichuang.medicine.common.security.login;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录失败处理结果。
 */
@Data
@AllArgsConstructor
public class LoginFailureResult {

    /**
     * 当前失败后是否已经进入锁定状态。
     */
    private boolean locked;

    /**
     * 当前连续失败次数。
     */
    private int currentRetryCount;

    /**
     * 剩余锁定秒数。
     */
    private long remainingSeconds;
}
