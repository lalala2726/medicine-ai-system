package com.zhangyichuang.medicine.common.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * 通用认证用户模型，屏蔽不同端的用户实体差异。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -904867432593234359L;


    /**
     * 角色编码集合
     */
    @Builder.Default
    private Set<String> roles = Collections.emptySet();

    /**
     * 权限编码集合
     */
    @Builder.Default
    private Set<String> permissions = Collections.emptySet();

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 加密后的密码（仅运行期使用，不参与 Redis/Gson 序列化）
     */
    private transient String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 上次登陆时间
     */
    private Date lastLoginTime;

    /**
     * 上次登陆IP
     */
    private String lastLoginIp;

    /**
     * 账号状态，0-正常，非0表示不可用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 删除时间
     */
    private Date deleteTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}
