package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端智能体用户聚合上下文 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户聚合上下文")
public class UserContextDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    @Schema(description = "用户 ID")
    private Long userId;

    /**
     * 用户基础摘要。
     */
    @Schema(description = "用户基础摘要")
    private BasicSummary basicSummary;

    /**
     * 用户订单摘要。
     */
    @Schema(description = "用户订单摘要")
    private OrderSummary orderSummary;

    /**
     * 用户钱包摘要。
     */
    @Schema(description = "用户钱包摘要")
    private WalletSummary walletSummary;

    /**
     * 用户风险摘要。
     */
    @Schema(description = "用户风险摘要")
    private RiskSummary riskSummary;

    /**
     * 智能体决策提示。
     */
    @Schema(description = "智能体决策提示")
    private AiHints aiHints;

    /**
     * 用户基础摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户基础摘要")
    public static class BasicSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 用户昵称。
         */
        @Schema(description = "用户昵称")
        private String nickName;

        /**
         * 脱敏后的手机号。
         */
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        @Schema(description = "脱敏后的手机号")
        private String phoneNumber;

        /**
         * 账号状态编码。
         */
        @Schema(description = "账号状态编码")
        private Integer status;

        /**
         * 注册时间。
         */
        @Schema(description = "注册时间")
        private Date registerTime;

        /**
         * 最近登录时间。
         */
        @Schema(description = "最近登录时间")
        private Date lastLoginTime;
    }

    /**
     * 用户订单摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户订单摘要")
    public static class OrderSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 总订单数。
         */
        @Schema(description = "总订单数")
        private Integer totalOrders;

        /**
         * 总消费金额。
         */
        @Schema(description = "总消费金额")
        private BigDecimal totalConsume;

        /**
         * 最近订单概览。
         */
        @Schema(description = "最近订单概览")
        private String recentOrderOverview;
    }

    /**
     * 用户钱包摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户钱包摘要")
    public static class WalletSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 钱包余额。
         */
        @Schema(description = "钱包余额")
        private BigDecimal balance;

        /**
         * 钱包状态编码。
         */
        @Schema(description = "钱包状态编码")
        private Integer status;

        /**
         * 冻结原因。
         */
        @Schema(description = "冻结原因")
        private String freezeReason;

        /**
         * 冻结时间。
         */
        @Schema(description = "冻结时间")
        private Date freezeTime;

        /**
         * 累计收入。
         */
        @Schema(description = "累计收入")
        private BigDecimal totalIncome;

        /**
         * 累计支出。
         */
        @Schema(description = "累计支出")
        private BigDecimal totalExpend;
    }

    /**
     * 用户风险摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户风险摘要")
    public static class RiskSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 账号是否禁用。
         */
        @Schema(description = "账号是否禁用")
        private Boolean accountDisabled;

        /**
         * 钱包是否冻结。
         */
        @Schema(description = "钱包是否冻结")
        private Boolean walletFrozen;

        /**
         * 是否缺少关键资料。
         */
        @Schema(description = "是否缺少关键资料")
        private Boolean missingBasicProfile;
    }

    /**
     * 用户智能体决策提示。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户智能体决策提示")
    public static class AiHints implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否可继续查询订单。
         */
        @Schema(description = "是否可继续查询订单")
        private Boolean canQueryOrders;

        /**
         * 是否可操作钱包。
         */
        @Schema(description = "是否可操作钱包")
        private Boolean canOperateWallet;

        /**
         * 钱包是否冻结。
         */
        @Schema(description = "钱包是否冻结")
        private Boolean walletFrozen;

        /**
         * 账号是否禁用。
         */
        @Schema(description = "账号是否禁用")
        private Boolean accountDisabled;
    }
}
