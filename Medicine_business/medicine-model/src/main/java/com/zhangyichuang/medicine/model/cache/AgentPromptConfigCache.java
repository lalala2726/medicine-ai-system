package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent 单条提示词运行时缓存对象。
 * <p>
 * 该对象作为 Redis 中 `agent:prompt:{promptKey}` 的最终保存结构，
 * 供管理端写入、Python Agent 端读取。
 */
@Data
public class AgentPromptConfigCache implements Serializable {

    /**
     * 当前缓存结构版本。
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Redis JSON 结构版本。
     */
    private Integer schemaVersion = CURRENT_SCHEMA_VERSION;

    /**
     * 提示词业务键。
     */
    private String promptKey;

    /**
     * 当前生效提示词版本号。
     */
    private Long promptVersion;

    /**
     * 配置更新时间（ISO-8601 字符串）。
     */
    private String updatedAt;

    /**
     * 配置更新人。
     */
    private String updatedBy;

    /**
     * 当前生效提示词正文。
     */
    private String promptContent;
}
