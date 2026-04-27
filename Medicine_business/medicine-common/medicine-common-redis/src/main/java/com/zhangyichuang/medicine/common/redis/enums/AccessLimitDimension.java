package com.zhangyichuang.medicine.common.redis.enums;

/**
 * 限流主体维度枚举。
 *
 * @author Chuang
 */
public enum AccessLimitDimension {

    /**
     * 按用户 ID 限流。
     */
    USER,

    /**
     * 按客户端 IP 限流。
     */
    IP,

    /**
     * 优先按用户 ID 限流，用户不存在时按 IP 限流。
     */
    USER_OR_IP,

    /**
     * 同时按用户 ID 与 IP 限流，任一维度命中即拒绝。
     */
    USER_AND_IP
}
