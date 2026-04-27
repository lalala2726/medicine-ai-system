package com.zhangyichuang.medicine.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户列表 DTO。
 */
@Data
public class UserListDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

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
     * 角色列表
     */
    private String roles;

    /**
     * 用户状态（0-正常，非0-异常）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;
}
