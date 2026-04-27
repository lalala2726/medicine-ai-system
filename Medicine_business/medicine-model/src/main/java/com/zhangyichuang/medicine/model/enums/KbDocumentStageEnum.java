package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 知识库文档阶段枚举。
 */
@Getter
public enum KbDocumentStageEnum {

    /**
     * admin 已创建导入记录，等待 AI 开始处理。
     */
    PENDING("PENDING", "待处理", "admin 刚创建导入记录时", false),

    /**
     * AI 已开始处理导入任务。
     */
    STARTED("STARTED", "已开始", "AI 已开始处理导入任务", true),

    /**
     * AI 正在执行导入流程。
     */
    PROCESSING("PROCESSING", "处理中", "AI 正在执行导入流程", true),

    /**
     * AI 已完成导入，admin 正在主动拉取切片并写入本地库。
     */
    INSERTING("INSERTING", "入库中", "AI 发 COMPLETED 后，admin 正在拉切片并入本地库", false),

    /**
     * 导入与切片同步均已完成。
     */
    COMPLETED("COMPLETED", "已完成", "导入与切片同步均已完成", true),

    /**
     * 导入或切片同步失败。
     */
    FAILED("FAILED", "失败", "导入或切片同步失败", true);

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

    /**
     * 是否允许 AI 导入结果直接回传该阶段。
     */
    private final boolean aiCallbackStage;

    KbDocumentStageEnum(String code, String name, String description, boolean aiCallbackStage) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.aiCallbackStage = aiCallbackStage;
    }

    /**
     * 根据阶段编码查找枚举。
     *
     * @param code 阶段编码
     * @return 对应枚举；不存在时返回 null
     */
    public static KbDocumentStageEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalizedCode = code.trim();
        for (KbDocumentStageEnum stage : values()) {
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
