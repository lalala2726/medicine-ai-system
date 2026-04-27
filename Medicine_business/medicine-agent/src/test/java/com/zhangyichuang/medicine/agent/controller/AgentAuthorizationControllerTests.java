package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentAuthorizationControllerTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_ShouldUseSessionContext() {
        AgentAuthorizationController controller = new AgentAuthorizationController();
        setupAuthentication(sampleAuthUser(), Set.of("ROLE_admin", "system:user:query"));

        var result = controller.getCurrentUser();

        assertEquals(200, result.getCode());
        HashMap<String, Object> data = result.getData();
        assertNotNull(data);

        Object userObject = data.get("user");
        assertInstanceOf(AuthUser.class, userObject);
        AuthUser user = (AuthUser) userObject;
        assertEquals(7L, user.getId());
        assertEquals("agent_user", user.getUsername());
        assertEquals("智能体用户", user.getNickname());
        assertEquals("agent@example.com", user.getEmail());
        assertEquals("13800000000", user.getPhoneNumber());

        Object rolesObject = data.get("roles");
        assertInstanceOf(Set.class, rolesObject);
        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) rolesObject;
        assertEquals(Set.of("admin"), roles);

        Object permissionsObject = data.get("permissions");
        assertInstanceOf(Set.class, permissionsObject);
        @SuppressWarnings("unchecked")
        Set<String> permissions = (Set<String>) permissionsObject;
        assertEquals(Set.of("system:user:query"), permissions);
    }

    private void setupAuthentication(AuthUser authUser, Set<String> authorities) {
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toSet()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private AuthUser sampleAuthUser() {
        Date now = new Date();
        return AuthUser.builder()
                .id(7L)
                .username("agent_user")
                .status(0)
                .nickname("智能体用户")
                .avatar("https://example.com/avatar.png")
                .email("agent@example.com")
                .phoneNumber("13800000000")
                .gender(1)
                .birthday(now)
                .realName("智能体")
                .idCard("310101199001010011")
                .lastLoginTime(now)
                .lastLoginIp("localhost")
                .createTime(now)
                .updateTime(now)
                .createBy("system")
                .updateBy("system")
                .isDelete(0)
                .build();
    }
}
