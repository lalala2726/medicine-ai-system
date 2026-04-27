package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 软件协议配置更新请求。
 */
@Data
@Schema(description = "软件协议配置更新请求")
public class AgreementConfigUpdateRequest {

    /**
     * 软件协议 Markdown 内容。
     */
    @NotBlank(message = "软件协议内容不能为空")
    @Schema(description = "软件协议 Markdown 内容", example = "# 服务协议")
    private String softwareAgreementMarkdown;

    /**
     * 隐私协议 Markdown 内容。
     */
    @NotBlank(message = "隐私协议内容不能为空")
    @Schema(description = "隐私协议 Markdown 内容", example = "# 隐私政策")
    private String privacyAgreementMarkdown;
}
