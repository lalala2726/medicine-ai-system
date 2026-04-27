package com.zhangyichuang.medicine.model.constants;

import java.util.Set;

/**
 * 大模型类型常量。
 *
 * @author Chuang
 */
public final class LlmModelTypeConstants {

    /**
     * 对话模型类型。
     */
    public static final String CHAT = "CHAT";

    /**
     * 向量模型类型。
     */
    public static final String EMBEDDING = "EMBEDDING";

    /**
     * 重排模型类型。
     */
    public static final String RERANK = "RERANK";

    /**
     * 支持的全部模型类型集合。
     */
    public static final Set<String> ALL = Set.of(CHAT, EMBEDDING, RERANK);

    private LlmModelTypeConstants() {
    }
}
