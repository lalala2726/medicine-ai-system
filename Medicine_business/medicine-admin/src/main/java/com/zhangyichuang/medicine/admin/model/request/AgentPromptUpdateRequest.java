package com.zhangyichuang.medicine.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 提示词保存请求。
 */
@Data
public class AgentPromptUpdateRequest {

    /**
     * 提示词业务键。
     */
    @NotBlank(message = "提示词键不能为空")
    private String promptKey;

    /**
     * 提示词正文。
     */
    @NotBlank(message = "提示词内容不能为空")
    private String promptContent;
}
