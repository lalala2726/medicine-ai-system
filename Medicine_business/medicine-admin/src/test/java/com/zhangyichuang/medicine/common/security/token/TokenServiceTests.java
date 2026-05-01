package com.zhangyichuang.medicine.common.security.token;

import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.OnlineLoginUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zhangyichuang.medicine.common.core.constants.SecurityConstants.CLAIM_KEY_SESSION_ID;
import static com.zhangyichuang.medicine.common.core.constants.SecurityConstants.CLAIM_KEY_USERNAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTests {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTokenStore redisTokenStore;

    @Mock
    private ObjectProvider<UserDetailsService> userDetailsServices;

    @Mock
    private UserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createToken_ShouldSplitRolesAndPermissionsIntoSession() {
        TokenService tokenService = new TokenService(jwtTokenProvider, redisTokenStore, userDetailsServices);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuthUser authUser = AuthUser.builder()
                .id(1L)
                .username("admin")
                .password("encoded")
                .roles(Set.of("admin"))
                .permissions(Set.of("system:user:list"))
                .build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(Set.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("system:user:list")
        ));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(jwtTokenProvider.createJwt(anyString(), eq("admin")))
                .thenReturn("access-jwt", "refresh-jwt");

        var session = tokenService.createToken(authentication);

        assertEquals("access-jwt", session.getAccessToken());
        assertEquals("refresh-jwt", session.getRefreshToken());
        ArgumentCaptor<OnlineLoginUser> sessionCaptor = ArgumentCaptor.forClass(OnlineLoginUser.class);
        verify(redisTokenStore).saveLoginSession(anyString(), anyString(), sessionCaptor.capture());
        OnlineLoginUser onlineLoginUser = sessionCaptor.getValue();
        assertEquals(Set.of("ROLE_admin"), onlineLoginUser.getRoles());
        assertEquals(Set.of("system:user:list"), onlineLoginUser.getPermissions());
        assertNotNull(onlineLoginUser.getUser());
        assertEquals(Set.of(), onlineLoginUser.getUser().getRoles());
        assertEquals(Set.of(), onlineLoginUser.getUser().getPermissions());
        assertNull(onlineLoginUser.getUser().getPassword());
        assertNotNull(onlineLoginUser.getCreateTime());
        assertNotNull(onlineLoginUser.getUpdateTime());
        assertNotNull(onlineLoginUser.getAccessTime());
        String sessionJson = JSONUtils.toJson(onlineLoginUser);
        assertFalse(sessionJson.contains("\"password\""));
        assertFalse(sessionJson.contains("\"location\""));
    }

    @Test
    void refreshToken_ShouldReloadAuthoritiesFromUserDetails() {
        TokenService tokenService = new TokenService(jwtTokenProvider, redisTokenStore, userDetailsServices);

        Claims claims = Jwts.claims();
        claims.put(CLAIM_KEY_SESSION_ID, "refresh-session-1");
        claims.put(CLAIM_KEY_USERNAME, "alice");
        when(jwtTokenProvider.getClaimsFromToken("refresh-jwt")).thenReturn(claims);
        when(redisTokenStore.isValidRefreshToken("refresh-session-1")).thenReturn(true);
        when(userDetailsServices.iterator()).thenReturn(List.of(userDetailsService).iterator());

        AuthUser authUser = AuthUser.builder()
                .id(5L)
                .username("alice")
                .password("encoded")
                .roles(Set.of("admin"))
                .permissions(Set.of("system:user:list"))
                .build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(Set.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("system:user:list")
        ));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        when(jwtTokenProvider.createJwt(anyString(), eq("alice"))).thenReturn("new-access-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var token = tokenService.refreshToken("refresh-jwt");

        assertEquals("new-access-token", token.getAccessToken());
        assertEquals("refresh-jwt", token.getRefreshToken());

        ArgumentCaptor<OnlineLoginUser> onlineUserCaptor = ArgumentCaptor.forClass(OnlineLoginUser.class);
        ArgumentCaptor<String> accessTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTokenStore).replaceAccessToken(
                eq("refresh-session-1"),
                accessTokenCaptor.capture(),
                onlineUserCaptor.capture()
        );
        OnlineLoginUser onlineLoginUser = onlineUserCaptor.getValue();
        assertEquals(Set.of("ROLE_admin"), onlineLoginUser.getRoles());
        assertEquals(Set.of("system:user:list"), onlineLoginUser.getPermissions());
        assertNotNull(onlineLoginUser.getUser());
        assertEquals(Set.of(), onlineLoginUser.getUser().getRoles());
        assertEquals(Set.of(), onlineLoginUser.getUser().getPermissions());
        assertNull(onlineLoginUser.getUser().getPassword());
        assertNotNull(onlineLoginUser.getCreateTime());
        assertNotNull(onlineLoginUser.getUpdateTime());
        assertNotNull(onlineLoginUser.getAccessTime());
        String sessionJson = JSONUtils.toJson(onlineLoginUser);
        assertFalse(sessionJson.contains("\"password\""));
        assertFalse(sessionJson.contains("\"location\""));

        assertFalse(accessTokenCaptor.getValue().isBlank());
    }

    @Test
    void parseAccessToken_ShouldLoadRolesAndPermissionsFromRedis() {
        TokenService tokenService = new TokenService(jwtTokenProvider, redisTokenStore, userDetailsServices);

        Claims claims = Jwts.claims();
        claims.put(CLAIM_KEY_SESSION_ID, "access-session-1");
        when(jwtTokenProvider.getClaimsFromToken("access-jwt")).thenReturn(claims);

        OnlineLoginUser onlineLoginUser = OnlineLoginUser.builder()
                .accessTokenId("access-session-1")
                .userId(8L)
                .username("admin")
                .roles(Set.of("ROLE_admin"))
                .permissions(Set.of("system:user:list"))
                .build();
        when(redisTokenStore.getAccessToken("access-session-1")).thenReturn(onlineLoginUser);
        when(redisTokenStore.updateAccessTime("access-session-1")).thenReturn(true);

        Authentication authentication = tokenService.parseAccessToken("access-jwt");

        assertNotNull(authentication);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_admin", "system:user:list"), authorities);

        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        assertEquals(Set.of("admin"), userDetails.getUser().getRoles());
        assertEquals(Set.of("system:user:list"), userDetails.getUser().getPermissions());
    }

    @Test
    void parseAccessToken_ShouldFallbackToSplitLegacyMixedAuthorities() {
        TokenService tokenService = new TokenService(jwtTokenProvider, redisTokenStore, userDetailsServices);

        Claims claims = Jwts.claims();
        claims.put(CLAIM_KEY_SESSION_ID, "legacy-session-1");
        when(jwtTokenProvider.getClaimsFromToken("legacy-jwt")).thenReturn(claims);

        OnlineLoginUser legacySession = OnlineLoginUser.builder()
                .accessTokenId("legacy-session-1")
                .userId(11L)
                .username("legacy")
                .roles(Set.of("admin", "ROLE_super_admin", "system:user:list"))
                .build();
        when(redisTokenStore.getAccessToken("legacy-session-1")).thenReturn(legacySession);
        when(redisTokenStore.updateAccessTime("legacy-session-1")).thenReturn(true);

        Authentication authentication = tokenService.parseAccessToken("legacy-jwt");

        assertNotNull(authentication);
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toSet());
        assertTrue(authorities.contains("ROLE_admin"));
        assertTrue(authorities.contains("ROLE_super_admin"));
        assertTrue(authorities.contains("system:user:list"));

        SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();
        assertEquals(Set.of("admin", "super_admin"), userDetails.getUser().getRoles());
        assertEquals(Set.of("system:user:list"), userDetails.getUser().getPermissions());
    }
}
