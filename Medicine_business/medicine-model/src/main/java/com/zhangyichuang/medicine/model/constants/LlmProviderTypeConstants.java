package com.zhangyichuang.medicine.model.constants;

import java.util.Set;

/**
 * 大模型提供商类型常量。
 */
public final class LlmProviderTypeConstants {

    /**
     * 阿里云百联提供商类型标识。
     */
    public static final String ALIYUN = "aliyun";

    /**
     * 当前系统允许的大模型提供商类型集合。
     */
    public static final Set<String> ALL = Set.of(ALIYUN);

    private LlmProviderTypeConstants() {
    }
}
