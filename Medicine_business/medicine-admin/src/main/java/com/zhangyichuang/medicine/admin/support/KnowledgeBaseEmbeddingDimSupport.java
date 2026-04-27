package com.zhangyichuang.medicine.admin.support;

import java.util.Set;

/**
 * 知识库向量维度支持规则。
 */
public final class KnowledgeBaseEmbeddingDimSupport {

    public static final String SCHEMA_DESCRIPTION = "向量维度，仅支持 2048、1536、1024、768、512、256、128、64";
    public static final String SUPPORTED_DIM_MESSAGE =
            "知识库支持的向量维度为2,048、1,536、1,024、768、512、256、128、64，必须在这些数字之中";

    private static final Set<Integer> SUPPORTED_DIMS = Set.of(2048, 1536, 1024, 768, 512, 256, 128, 64);

    private KnowledgeBaseEmbeddingDimSupport() {
    }

    /**
     * 判断当前向量维度是否受支持。
     *
     * @param embeddingDim 向量维度
     * @return true 表示受支持
     */
    public static boolean isSupported(Integer embeddingDim) {
        return embeddingDim != null && SUPPORTED_DIMS.contains(embeddingDim);
    }
}
