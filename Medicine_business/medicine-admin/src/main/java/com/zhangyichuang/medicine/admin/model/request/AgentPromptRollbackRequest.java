package com.zhangyichuang.medicine.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 提示词版本回滚请求。
 */
@Data
public class AgentPromptRollbackRequest {

    /**
     * 提示词业务键。
     */
    @NotBlank(message = "提示词键不能为空")
    private String promptKey;

    /**
     * 目标回滚版本号。
     */
    @NotNull(message = "目标版本号不能为空")
    private Long targetVersion;
}
