package com.zhangyichuang.medicine.common.security.agreement;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 软件协议配置。
 */
@Data
public class AgreementConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 软件协议 Markdown 内容。
     */
    private String softwareAgreementMarkdown;

    /**
     * 隐私协议 Markdown 内容。
     */
    private String privacyAgreementMarkdown;

    /**
     * 最后更新时间。
     */
    private LocalDateTime updatedTime;
}
