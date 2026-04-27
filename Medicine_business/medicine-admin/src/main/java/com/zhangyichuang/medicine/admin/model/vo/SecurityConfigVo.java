package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 安全配置详情。
 */
@Data
@Schema(description = "安全配置详情")
public class SecurityConfigVo {

    /**
     * 管理端安全策略。
     */
    @Schema(description = "管理端安全策略")
    private SecurityPolicyVo admin;

    /**
     * 客户端安全策略。
     */
    @Schema(description = "客户端安全策略")
    private SecurityPolicyVo client;

    /**
     * 管理端水印配置。
     */
    @Schema(description = "管理端水印配置")
    private AdminWatermarkConfigVo adminWatermark;

    /**
     * 单端安全策略详情。
     */
    @Data
    @Schema(description = "单端安全策略详情")
    public static class SecurityPolicyVo {

        /**
         * 连续失败阈值。
         */
        @Schema(description = "连续失败阈值", example = "3")
        private Integer maxRetryCount;

        /**
         * 锁定时长（分钟）。
         */
        @Schema(description = "锁定时长（分钟）", example = "10")
        private Integer lockMinutes;
    }

    /**
     * 管理端水印配置详情。
     */
    @Data
    @Schema(description = "管理端水印配置详情")
    public static class AdminWatermarkConfigVo {

        /**
         * 是否启用管理端水印。
         */
        @Schema(description = "是否启用管理端水印", example = "false")
        private Boolean enabled;

        /**
         * 是否展示用户名。
         */
        @Schema(description = "是否展示用户名", example = "true")
        private Boolean showUsername;

        /**
         * 是否展示用户ID。
         */
        @Schema(description = "是否展示用户ID", example = "true")
        private Boolean showUserId;
    }
}
