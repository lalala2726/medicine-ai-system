package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 安全配置更新请求。
 */
@Data
@Schema(description = "安全配置更新请求")
public class SecurityConfigUpdateRequest {

    /**
     * 管理端安全策略。
     */
    @Valid
    @NotNull(message = "管理端安全策略不能为空")
    @Schema(description = "管理端安全策略")
    private SecurityPolicyUpdateRequest admin;

    /**
     * 客户端安全策略。
     */
    @Valid
    @NotNull(message = "客户端安全策略不能为空")
    @Schema(description = "客户端安全策略")
    private SecurityPolicyUpdateRequest client;

    /**
     * 管理端水印配置。
     */
    @Valid
    @NotNull(message = "管理端水印配置不能为空")
    @Schema(description = "管理端水印配置")
    private AdminWatermarkConfigUpdateRequest adminWatermark;

    /**
     * 单端安全策略更新请求。
     */
    @Data
    @Schema(description = "单端安全策略更新请求")
    public static class SecurityPolicyUpdateRequest {

        /**
         * 连续失败阈值。
         */
        @NotNull(message = "连续失败阈值不能为空")
        @Min(value = 1, message = "连续失败阈值最小为1")
        @Max(value = 20, message = "连续失败阈值最大为20")
        @Schema(description = "连续失败阈值", example = "3")
        private Integer maxRetryCount;

        /**
         * 锁定时长（分钟）。
         */
        @NotNull(message = "锁定时长不能为空")
        @Min(value = 1, message = "锁定时长最小为1分钟")
        @Max(value = 1440, message = "锁定时长最大为1440分钟")
        @Schema(description = "锁定时长（分钟）", example = "10")
        private Integer lockMinutes;
    }

    /**
     * 管理端水印配置更新请求。
     */
    @Data
    @Schema(description = "管理端水印配置更新请求")
    public static class AdminWatermarkConfigUpdateRequest {

        /**
         * 是否启用管理端水印。
         */
        @NotNull(message = "管理端水印启用状态不能为空")
        @Schema(description = "是否启用管理端水印", example = "false")
        private Boolean enabled;

        /**
         * 是否展示用户名。
         */
        @NotNull(message = "管理端水印用户名展示状态不能为空")
        @Schema(description = "是否展示用户名", example = "true")
        private Boolean showUsername;

        /**
         * 是否展示用户ID。
         */
        @NotNull(message = "管理端水印用户ID展示状态不能为空")
        @Schema(description = "是否展示用户ID", example = "true")
        private Boolean showUserId;
    }
}
