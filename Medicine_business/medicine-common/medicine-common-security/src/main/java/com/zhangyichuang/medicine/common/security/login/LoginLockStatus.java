package com.zhangyichuang.medicine.common.security.login;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录锁定状态。
 */
@Data
@AllArgsConstructor
public class LoginLockStatus {

    /**
     * 是否处于锁定状态。
     */
    private boolean locked;

    /**
     * 剩余锁定秒数。
     */
    private long remainingSeconds;
}
