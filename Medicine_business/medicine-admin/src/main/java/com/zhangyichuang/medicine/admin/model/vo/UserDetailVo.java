package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * 用户
 */
@Schema(description = "用户信息视图对象")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDetailVo {

    @Schema(description = "头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "昵称", example = "张三")
    private String nickName;

    @Schema(description = "钱包余额", example = "100.00")
    private BigDecimal walletBalance;

    @Schema(description = "总订单数", example = "10")
    private Integer totalOrders;

    @Schema(description = "总消费金额", example = "500.00")
    private BigDecimal totalConsume;

    @Schema(description = "角色ID集合", example = "[1,2]")
    private Set<Long> roles;

    @Schema(description = "基础信息")
    private BasicInfo basicInfo;

    @Schema(description = "安全信息")
    private SecurityInfo securityInfo;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "用户基础信息视图对象")
    public static class BasicInfo {
        @Schema(description = "用户ID", example = "1")
        private Long userId;

        @Schema(description = "用户名", example = "zhangsan")
        private String realName;

        @Schema(description = "手机号", example = "13800000000")
        private String phoneNumber;

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        private String email;

        @Schema(description = "性别", example = "男")
        private Integer gender;

        @Schema(description = "身份证号", example = "110101199001011234")
        private String idCard;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "用户安全信息视图对象")
    public static class SecurityInfo {
        @Schema(description = "注册时间", example = "2025-11-07 14:31:00")
        private Date registerTime;

        @Schema(description = "最后登录时间", example = "2025-11-07 14:31:00")
        private Date lastLoginTime;

        @Schema(description = "最后登录IP", example = "192.168.1.1")
        private String lastLoginIp;

        @Schema(description = "用户状态", example = "正常")
        private Integer status;
    }

}
