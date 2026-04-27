package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent 提示词手动同步任务消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPromptSyncMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 同步范围，固定为 all 或 single。
     */
    private String sync_scope;

    /**
     * 单条同步时的提示词业务键；全量同步时为空。
     */
    private String prompt_key;

    /**
     * 本次同步任务提交人。
     */
    private String operator;

    /**
     * 消息创建时间，推荐使用 ISO-8601。
     */
    private String created_at;
}
