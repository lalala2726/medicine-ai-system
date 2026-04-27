package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 知识库文档切片模式枚举。
 */
@Getter
public enum KnowledgeChunkModeEnum {

    /**
     * 平衡模式。
     */
    BALANCED_MODE("balancedMode", "平衡模式", 1000, 200, false),

    /**
     * 精准模式。
     */
    PRECISION_MODE("precisionMode", "精准模式", 512, 100, false),

    /**
     * 上下文模式。
     */
    CONTEXT_MODE("contextMode", "上下文模式", 2048, 400, false),

    /**
     * 自定义模式。
     */
    CUSTOM("custom", "自定义模式", null, null, true);

    /**
     * 自定义模式允许的最小切片长度。
     */
    public static final int CUSTOM_CHUNK_SIZE_MIN = 100;

    /**
     * 自定义模式允许的最大切片长度。
     */
    public static final int CUSTOM_CHUNK_SIZE_MAX = 6000;

    /**
     * 自定义模式允许的最小切片重叠长度。
     */
    public static final int CUSTOM_CHUNK_OVERLAP_MIN = 0;

    /**
     * 自定义模式允许的最大切片重叠长度。
     */
    public static final int CUSTOM_CHUNK_OVERLAP_MAX = 1000;

    /**
     * 模式编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    /**
     * 预设切片长度，自定义模式为空。
     */
    private final Integer chunkSize;

    /**
     * 预设切片重叠长度，自定义模式为空。
     */
    private final Integer chunkOverlap;

    /**
     * 是否为自定义模式。
     */
    private final boolean custom;

    KnowledgeChunkModeEnum(String code, String name, Integer chunkSize, Integer chunkOverlap, boolean custom) {
        this.code = code;
        this.name = name;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.custom = custom;
    }

    /**
     * 根据模式编码查找枚举。
     *
     * @param code 模式编码
     * @return 对应枚举；不存在时返回 null
     */
    public static KnowledgeChunkModeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalizedCode = code.trim();
        for (KnowledgeChunkModeEnum mode : values()) {
            if (mode.code.equalsIgnoreCase(normalizedCode)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 判断当前枚举是否匹配指定模式编码。
     *
     * @param code 模式编码
     * @return true 表示匹配
     */
    public boolean matches(String code) {
        return code != null && this.code.equalsIgnoreCase(code.trim());
    }
}
