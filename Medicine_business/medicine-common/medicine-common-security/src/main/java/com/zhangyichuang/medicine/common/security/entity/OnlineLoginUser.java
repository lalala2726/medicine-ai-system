package com.zhangyichuang.medicine.common.security.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 在线用户信息对象
 *
 * @author Chuang
 * <p>
 * created on 2025/2/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineLoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -3193268316173909251L;

    /**
     * 访问令牌ID
     */
    private String accessTokenId;

    /**
     * 刷新令牌ID
     */
    private String refreshTokenId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 完整用户实体（用于减少二次查询）
     */
    private AuthUser user;

    /**
     * 角色集合
     */
    private Set<String> roles;

    /**
     * 权限集合
     */
    private Set<String> permissions;

    /**
     * 登录IP地址
     */
    private String ip;

    /**
     * 创建时间（登录写入 Redis 时间）
     */
    private Long createTime;

    /**
     * 更新时间（会话对象写回 Redis 时间）
     */
    private Long updateTime;

    /**
     * 访问时间(每次请求系统走)
     */
    private Long accessTime;

    /**
     * userAgent
     */
    private String userAgent;

}
