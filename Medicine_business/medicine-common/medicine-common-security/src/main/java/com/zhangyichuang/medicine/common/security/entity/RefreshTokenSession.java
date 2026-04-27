package com.zhangyichuang.medicine.common.security.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新令牌会话信息。
 * <p>
 * 用于从刷新令牌反查用户与当前访问令牌，保证权限变更时可以同时清理 access token 和 refresh token。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 8794706603665793788L;

    /**
     * 刷新令牌会话ID。
     */
    private String refreshTokenId;

    /**
     * 当前绑定的访问令牌会话ID。
     */
    private String accessTokenId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 创建时间。
     */
    private Long createTime;

    /**
     * 更新时间。
     */
    private Long updateTime;
}
