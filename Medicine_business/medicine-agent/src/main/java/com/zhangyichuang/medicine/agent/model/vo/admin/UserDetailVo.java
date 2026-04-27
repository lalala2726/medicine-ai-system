package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端用户详情。
 */
@Data
@Schema(description = "管理端用户详情")
@FieldDescription(description = "管理端用户详情")
public class UserDetailVo {

    @Schema(description = "头像")
    @FieldDescription(description = "头像")
    private String avatar;

    @Schema(description = "昵称")
    @FieldDescription(description = "昵称")
    private String nickName;

    @Schema(description = "钱包余额")
    @FieldDescription(description = "钱包余额")
    private BigDecimal walletBalance;

    @Schema(description = "总订单数")
    @FieldDescription(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "总消费金额")
    @FieldDescription(description = "总消费金额")
    private BigDecimal totalConsume;

    @Schema(description = "基础信息")
    @FieldDescription(description = "基础信息")
    private BasicInfo basicInfo;

    @Schema(description = "安全信息")
    @FieldDescription(description = "安全信息")
    private SecurityInfo securityInfo;

    @Data
    @Schema(description = "用户基础信息")
    @FieldDescription(description = "用户基础信息")
    public static class BasicInfo {

        @Schema(description = "用户ID")
        @FieldDescription(description = "用户ID")
        private Long userId;

        @Schema(description = "真实姓名")
        @FieldDescription(description = "真实姓名")
        private String realName;

        @Schema(description = "手机号")
        @FieldDescription(description = "手机号")
        private String phoneNumber;

        @Schema(description = "邮箱")
        @FieldDescription(description = "邮箱")
        private String email;

        @Schema(description = "性别")
        @FieldDescription(description = "性别")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_USER_GENDER)
        private Integer gender;

        @Schema(description = "身份证号")
        @FieldDescription(description = "身份证号")
        private String idCard;
    }

    @Data
    @Schema(description = "用户安全信息")
    @FieldDescription(description = "用户安全信息")
    public static class SecurityInfo {

        @Schema(description = "注册时间")
        @FieldDescription(description = "注册时间")
        private Date registerTime;

        @Schema(description = "最后登录时间")
        @FieldDescription(description = "最后登录时间")
        private Date lastLoginTime;

        @Schema(description = "最后登录IP")
        @FieldDescription(description = "最后登录IP")
        private String lastLoginIp;

        @Schema(description = "用户状态")
        @FieldDescription(description = "用户状态")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_USER_STATUS)
        private Integer status;
    }
}
