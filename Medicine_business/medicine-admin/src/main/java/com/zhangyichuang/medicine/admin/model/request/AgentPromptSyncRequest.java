package com.zhangyichuang.medicine.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 提示词单条同步请求。
 */
@Data
public class AgentPromptSyncRequest {

    /**
     * 需要同步的提示词业务键。
     */
    @NotBlank(message = "提示词键不能为空")
    private String promptKey;
}
