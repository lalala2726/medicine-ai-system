package com.zhangyichuang.medicine.common.security.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zhangyichuang.medicine.common.core.constants.Constants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标准化的 {@link UserDetails} 实现，内部持有 {@link AuthUser}。
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysUserDetails implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = -5777762905473897401L;

    /**
     * 统一用户对象
     */
    private AuthUser user;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 权限集合
     */
    private Collection<SimpleGrantedAuthority> authorities = Collections.emptySet();

    public SysUserDetails(AuthUser user) {
        this.user = user;
        this.userId = user.getId();
        this.username = user.getUsername();
        Set<String> roles = user.getRoles();
        this.authorities = roles == null ? Collections.emptySet()
                : roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    @Override
    public @NonNull String getUsername() {
        return username;
    }

    private boolean isAccountAvailable() {
        return user == null || Objects.equals(user.getStatus(), Constants.ACCOUNT_UNLOCK_KEY);
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountAvailable();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountAvailable();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isAccountAvailable();
    }

    @Override
    public boolean isEnabled() {
        return isAccountAvailable();
    }
}
