package com.zhangyichuang.medicine.common.security.filter;

import com.zhangyichuang.medicine.common.core.constants.SecurityConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.utils.ResponseUtils;
import com.zhangyichuang.medicine.common.security.config.SecurityProperties;
import com.zhangyichuang.medicine.common.security.token.TokenService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 令牌前缀
     */
    @Value("${security.session.token-prefix}")
    private static final String TOKEN_PREFIX = "Bearer";
    private final SecurityProperties securityProperties;
    private final TokenService tokenService;

    /**
     * 校验Token，包括验签和是否过期
     *
     * @param request     请求参数
     * @param response    响应参数
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String header = securityProperties.getHeader();
        String token = request.getHeader(header);
        try {
            if (StringUtils.isNotBlank(token)) {
                // 如果有 TOKEN_PREFIX 前缀则移除
                if (StringUtils.isNotBlank(TOKEN_PREFIX) && token.startsWith(TOKEN_PREFIX.trim() + " ")) {
                    token = token.substring(TOKEN_PREFIX.length()).trim();
                }
                // 直接解析令牌（内部会做验签与有效性检查），避免重复解析
                Authentication authentication = tokenService.parseAccessToken(token);
                if (authentication == null) {
                    log.warn("解析访问令牌失败: {}", token);
                    ResponseUtils.writeErrMsg(response, ResponseCode.ACCESS_TOKEN_INVALID, HttpStatus.OK);
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 安全上下文清除保障（防止上下文残留）
            log.warn("安全上下文清理", ex);
            SecurityContextHolder.clearContext();
            ResponseUtils.writeErrMsg(response, ResponseCode.ACCESS_TOKEN_INVALID, HttpStatus.OK);
            return;
        }

        // 继续后续过滤器链执行
        filterChain.doFilter(request, response);
    }

    /**
     * 此方法用于排除不需要过滤的请求，防止过滤器对某些请求进行处理。
     *
     * @param request current HTTP request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        AntPathMatcher pathMatcher = new AntPathMatcher();

        // 检查普通白名单
        if (isPathMatchAny(path, SecurityConstants.WHITELIST, pathMatcher)) {
            return true;
        }

        // 检查静态资源白名单
        if (isPathMatchAny(path, SecurityConstants.STATIC_RESOURCES_WHITELIST, pathMatcher)) {
            return true;
        }

        // 检查Swagger白名单
        return isPathMatchAny(path, SecurityConstants.SWAGGER_WHITELIST, pathMatcher);
    }

    /**
     * 检查路径是否匹配任一白名单规则
     *
     * @param path        请求路径
     * @param patterns    匹配模式数组
     * @param pathMatcher 路径匹配器
     * @return 是否匹配
     */
    private boolean isPathMatchAny(String path, String[] patterns, AntPathMatcher pathMatcher) {
        return Arrays.stream(patterns)
                .anyMatch(pattern -> pathMatcher.match(pattern, path.trim()));
    }
}
