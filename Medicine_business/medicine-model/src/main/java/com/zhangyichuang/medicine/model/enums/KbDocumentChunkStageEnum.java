package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 知识库文档切片本地阶段枚举。
 * <p>
 * 本地切片阶段来源有三类：
 * 1. 本地主动提交新增/编辑请求时，先落为 {@link #PENDING}；
 * 2. AI 对新增/编辑切片的 MQ 回调，会推进为 {@link #STARTED}/{@link #COMPLETED}/{@link #FAILED}；
 * 3. 文档导入完成后，管理端主动从 AI 拉取分页切片并落库时，直接视为 {@link #COMPLETED}。
 * </p>
 */
@Getter
public enum KbDocumentChunkStageEnum {

    /**
     * 本地已创建占位切片，等待 AI 处理。
     */
    PENDING("PENDING", "待处理", "本地已创建占位切片，等待 AI 处理"),

    /**
     * AI 已开始处理切片任务。
     */
    STARTED("STARTED", "已开始", "AI 已开始处理切片任务"),

    /**
     * 切片任务已完成。
     */
    COMPLETED("COMPLETED", "已完成", "切片任务已完成"),

    /**
     * 切片任务处理失败。
     */
    FAILED("FAILED", "失败", "切片任务处理失败");

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

    KbDocumentChunkStageEnum(String code, String name, String description) {
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
    public static KbDocumentChunkStageEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalizedCode = code.trim();
        for (KbDocumentChunkStageEnum stage : values()) {
            if (stage.code.equalsIgnoreCase(normalizedCode)) {
                return stage;
            }
        }
        return null;
    }

    /**
     * 将切片任务回调阶段映射为本地切片阶段。
     *
     * @param taskStage AI 回调阶段
     * @return 对应本地切片阶段；无法映射时返回 null
     */
    public static KbDocumentChunkStageEnum fromTaskStage(KnowledgeChunkTaskStageEnum taskStage) {
        if (taskStage == null) {
            return null;
        }
        return switch (taskStage) {
            case STARTED -> STARTED;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
        };
    }

    /**
     * 根据 AI 回调阶段编码映射为本地切片阶段。
     *
     * @param code AI 回调阶段编码
     * @return 对应本地切片阶段；无法映射时返回 null
     */
    public static KbDocumentChunkStageEnum fromTaskStageCode(String code) {
        return fromTaskStage(KnowledgeChunkTaskStageEnum.fromCode(code));
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

    /**
     * 是否为处理中阶段。
     *
     * @return true 表示待处理或已开始
     */
    public boolean isProcessing() {
        return this == PENDING || this == STARTED;
    }

    /**
     * 是否为终态。
     *
     * @return true 表示已完成或失败
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
