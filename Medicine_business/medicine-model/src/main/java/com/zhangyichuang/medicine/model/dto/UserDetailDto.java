package com.zhangyichuang.medicine.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * 用户详情 DTO。
 */
@Data
public class UserDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 钱包余额
     */
    private BigDecimal walletBalance;

    /**
     * 总订单数
     */
    private Integer totalOrders;

    /**
     * 总消费金额
     */
    private BigDecimal totalConsume;

    /**
     * 角色ID集合
     */
    private Set<Long> roles;

    /**
     * 基础信息
     */
    private BasicInfo basicInfo;

    /**
     * 安全信息
     */
    private SecurityInfo securityInfo;

    /**
     * 用户基础信息
     */
    @Data
    public static class BasicInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 真实姓名
         */
        private String realName;

        /**
         * 手机号
         */
        private String phoneNumber;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 性别（0-未知，1-男，2-女）
         */
        private Integer gender;

        /**
         * 身份证号
         */
        private String idCard;
    }

    /**
     * 用户安全信息
     */
    @Data
    public static class SecurityInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 注册时间
         */
        private Date registerTime;

        /**
         * 最后登录时间
         */
        private Date lastLoginTime;

        /**
         * 最后登录IP
         */
        private String lastLoginIp;

        /**
         * 用户状态（0-正常，非0-异常）
         */
        private Integer status;
    }
}
