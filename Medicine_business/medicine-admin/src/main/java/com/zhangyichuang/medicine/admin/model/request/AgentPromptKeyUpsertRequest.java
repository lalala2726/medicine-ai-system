package com.zhangyichuang.medicine.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 提示词业务键新增/更新请求。
 */
@Data
public class AgentPromptKeyUpsertRequest {

    /**
     * 提示词业务键。
     */
    @NotBlank(message = "提示词键不能为空")
    private String promptKey;

    /**
     * 提示词用途说明。
     */
    private String description;
}
