package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户端软件协议配置详情。
 */
@Data
@Schema(description = "客户端软件协议配置详情")
public class AgreementConfigVo {

    /**
     * 软件协议 Markdown 内容。
     */
    @Schema(description = "软件协议 Markdown 内容", example = "# 服务协议")
    private String softwareAgreementMarkdown;

    /**
     * 隐私协议 Markdown 内容。
     */
    @Schema(description = "隐私协议 Markdown 内容", example = "# 隐私政策")
    private String privacyAgreementMarkdown;

    /**
     * 最后更新时间。
     */
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedTime;
}
