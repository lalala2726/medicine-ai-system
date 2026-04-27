package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent 配置刷新消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigRefreshMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，固定为 agent_config_refresh。
     */
    private String message_type;

    /**
     * Agent 配置 Redis key。
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
