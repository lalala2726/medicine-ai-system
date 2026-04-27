package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 切片任务回调阶段枚举。
 * <p>
 * 供切片新增与切片重建 MQ 回调共用。
 * </p>
 */
@Getter
public enum KnowledgeChunkTaskStageEnum {

    /**
     * 任务已接收，准备开始处理。
     */
    STARTED("STARTED", "已开始", "任务已接收，准备开始处理"),

    /**
     * 处理成功。
     */
    COMPLETED("COMPLETED", "已完成", "处理成功"),

    /**
     * 处理失败。
     */
    FAILED("FAILED", "失败", "处理失败");

    /**
     * 阶段编码。
     */
    private final String code;

    /**
     * 中文名称。
     */
    private final String name;

    /**
     * 阶段描述。
     */
    private final String description;

    KnowledgeChunkTaskStageEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据阶段编码查找枚举。
     *
     * @param code 阶段编码
     * @return 对应枚举；不存在时返回 null
     */
    public static KnowledgeChunkTaskStageEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalizedCode = code.trim();
        for (KnowledgeChunkTaskStageEnum stage : values()) {
            if (stage.code.equalsIgnoreCase(normalizedCode)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * 判断当前枚举是否匹配指定阶段编码。
     *
     * @param code 阶段编码
     * @return true 表示匹配
     */
    public boolean matches(String code) {
        return code != null && this.code.equalsIgnoreCase(code.trim());
    }
}
