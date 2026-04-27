package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent 提示词刷新消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPromptRefreshMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 agent_prompt_refresh。
     */
    private String message_type;

    /**
     * 本次更新的提示词业务键。
     */
    private String prompt_key;

    /**
     * 本次更新后的提示词版本号。
     */
    private Long prompt_version;

    /**
     * 提示词运行时缓存 Redis key。
     */
    private String redis_key;

    /**
     * 本次配置更新时间。
     */
    private String updated_at;

    /**
     * 本次配置更新人。
     */
    private String updated_by;

    /**
     * 消息创建时间，推荐使用 ISO-8601。
     */
    private String created_at;
}
