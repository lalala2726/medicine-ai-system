package com.zhangyichuang.medicine.admin.security;

import com.zhangyichuang.medicine.admin.service.PermissionService;
import com.zhangyichuang.medicine.admin.service.RoleService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.constants.Constants;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSecurityUserServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AdminSecurityUserService adminSecurityUserService;

    @Test
    void loadUserByUsername_WhenUserNotExist_ShouldThrowException() {
        when(userService.getUserByUsername("not-found")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> adminSecurityUserService.loadUserByUsername("not-found"));
    }

    @Test
    void loadUserByUsername_ShouldAssembleRoleAndPermissionAuthorities() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("pwd");
        user.setStatus(Constants.ACCOUNT_UNLOCK_KEY);

        when(userService.getUserByUsername("admin")).thenReturn(user);
        when(roleService.getUserRoleByUserId(1L)).thenReturn(Set.of("admin", "super_admin"));
        when(permissionService.getPermissionCodesByUserId(1L)).thenReturn(Set.of("system:user:list"));

        var userDetails = adminSecurityUserService.loadUserByUsername("admin");
        var sysUserDetails = (SysUserDetails) userDetails;

        Set<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority.class::cast)
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals("admin", userDetails.getUsername());
        assertTrue(authorities.contains("ROLE_admin"));
        assertTrue(authorities.contains("ROLE_super_admin"));
        assertTrue(authorities.contains("system:user:list"));
        assertEquals(Set.of("admin", "super_admin"), sysUserDetails.getUser().getRoles());
        assertEquals(Set.of("system:user:list"), sysUserDetails.getUser().getPermissions());
    }

    @Test
    void loadUserByUsername_ShouldNormalizeDirtyRoleAndPermissionCodes() {
        User user = new User();
        user.setId(2L);
        user.setUsername("cleaner");
        user.setPassword("pwd");
        user.setStatus(Constants.ACCOUNT_UNLOCK_KEY);

        when(userService.getUserByUsername("cleaner")).thenReturn(user);
        when(roleService.getUserRoleByUserId(2L)).thenReturn(Set.of("admin", " ", "ROLE_super_admin"));
        when(permissionService.getPermissionCodesByUserId(2L)).thenReturn(Set.of("system:user:list", "   "));

        var userDetails = (SysUserDetails) adminSecurityUserService.loadUserByUsername("cleaner");
        Set<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority.class::cast)
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("admin", "super_admin"), userDetails.getUser().getRoles());
        assertEquals(Set.of("system:user:list"), userDetails.getUser().getPermissions());
        assertTrue(authorities.contains("ROLE_admin"));
        assertTrue(authorities.contains("ROLE_super_admin"));
        assertTrue(authorities.contains("system:user:list"));
    }
}
