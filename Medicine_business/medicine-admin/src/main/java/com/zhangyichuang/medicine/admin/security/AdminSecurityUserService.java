package com.zhangyichuang.medicine.admin.security;

import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 管理端的用户查询实现，负责将业务用户转换为通用˚的安全模型。
 */
@Service
@RequiredArgsConstructor
public class AdminSecurityUserService implements UserDetailsService {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        Set<String> roles = SecurityUtils.normalizeRoleCodes(Optional.ofNullable(roleService.getUserRoleByUserId(user.getId()))
                .filter(set -> !set.isEmpty())
                .orElseGet(Collections::emptySet));
        Set<String> permissions = SecurityUtils.toPermissionAuthorities(
                Optional.ofNullable(permissionService.getPermissionCodesByUserId(user.getId()))
                        .filter(set -> !set.isEmpty())
                        .orElseGet(Collections::emptySet));

        AuthUser authUser = BeanCotyUtils.copyProperties(user, AuthUser.class);
        authUser.setPassword(user.getPassword());
        authUser.setRoles(roles);
        authUser.setPermissions(permissions);

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        SecurityUtils.toRoleAuthorities(roles)
                .forEach(roleAuthority -> authorities.add(new SimpleGrantedAuthority(roleAuthority)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(authorities);
        return userDetails;
    }
}
