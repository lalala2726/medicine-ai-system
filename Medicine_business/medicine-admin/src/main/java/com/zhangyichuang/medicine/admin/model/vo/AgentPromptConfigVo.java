package com.zhangyichuang.medicine.admin.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * Agent 提示词详情视图对象。
 */
@Data
public class AgentPromptConfigVo {

    /**
     * 提示词业务键。
     */
    private String promptKey;

    /**
     * 当前生效提示词正文。
     */
    private String promptContent;

    /**
     * 当前生效版本号。
     */
    private Long promptVersion;

    /**
     * 最后更新时间。
     */
    private Date updatedAt;

    /**
     * 最后更新人。
     */
    private String updatedBy;
}
