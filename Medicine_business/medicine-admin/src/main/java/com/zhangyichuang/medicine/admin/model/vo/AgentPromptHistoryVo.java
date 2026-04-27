package com.zhangyichuang.medicine.admin.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * Agent 提示词历史版本视图对象。
 */
@Data
public class AgentPromptHistoryVo {

    /**
     * 提示词业务键。
     */
    private String promptKey;

    /**
     * 历史版本号。
     */
    private Long promptVersion;

    /**
     * 历史版本提示词正文。
     */
    private String promptContent;

    /**
     * 历史记录创建时间。
     */
    private Date createdAt;

    /**
     * 历史记录创建人。
     */
    private String createdBy;
}
