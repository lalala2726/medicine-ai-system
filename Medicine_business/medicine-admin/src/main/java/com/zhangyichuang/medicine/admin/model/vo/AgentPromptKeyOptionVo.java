package com.zhangyichuang.medicine.admin.model.vo;

import lombok.Data;

/**
 * Agent 提示词键选项视图对象。
 */
@Data
public class AgentPromptKeyOptionVo {

    /**
     * 提示词业务键。
     */
    private String promptKey;

    /**
     * 提示词用途说明。
     */
    private String description;

    /**
     * 是否已完成数据库配置且 Redis 运行时 key 已存在。
     */
    private Boolean configured;

    /**
     * 当前生效版本号。
     */
    private Long promptVersion;
}
