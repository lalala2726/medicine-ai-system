package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.client.mapper.UserMapper;
import com.zhangyichuang.medicine.client.publisher.LoginLogPublisher;
import com.zhangyichuang.medicine.client.service.AuthService;
import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.client.task.AsyncUserLogService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.LoginException;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.IpAddressUtils;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.common.security.login.LoginFailureResult;
import com.zhangyichuang.medicine.common.security.login.LoginLockStatus;
import com.zhangyichuang.medicine.common.security.login.LoginSecurityService;
import com.zhangyichuang.medicine.common.security.token.JwtTokenProvider;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.common.security.token.TokenService;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.mq.LoginLogMessage;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.zhangyichuang.medicine.common.core.constants.SecurityConstants.CLAIM_KEY_SESSION_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService, BaseService {
    /**
     * 用户代理请求头名称。
     */
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * 密码登录类型编码。
     */
    private static final String LOGIN_TYPE_PASSWORD = "password";

    /**
     * 客户端登录来源编码。
     */
    private static final String LOGIN_SOURCE_CLIENT = "client";

    /**
     * 当前用户不存在提示语。
     */
    private static final String CURRENT_USER_NOT_FOUND_MESSAGE = "当前用户不存在";

    /**
     * 原密码错误提示语。
     */
    private static final String OLD_PASSWORD_INCORRECT_MESSAGE = "原密码错误";

    /**
     * 新旧密码相同提示语。
     */
    private static final String SAME_PASSWORD_MESSAGE = "新密码不能与原密码相同";

    /**
     * 密码更新失败提示语。
     */
    private static final String CHANGE_PASSWORD_FAIL_MESSAGE = "密码修改失败，请稍后重试";

    /**
     * 手机号格式正则。
     */
    private static final String PHONE_NUMBER_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 手机号验证码 Redis key 模板。
     */
    private static final String PHONE_CHANGE_CODE_KEY_TEMPLATE = "auth:phone:change:code:%s:%s";

    /**
     * 手机号验证码有效期（秒）。
     */
    private static final long PHONE_CHANGE_CODE_TTL_SECONDS = 300L;

    /**
     * 手机号验证码位数。
     */
    private static final int PHONE_CHANGE_CODE_LENGTH = 6;

    /**
     * 手机号格式错误提示语。
     */
    private static final String PHONE_NUMBER_INVALID_MESSAGE = "手机号格式不正确";

    /**
     * 新旧手机号相同提示语。
     */
    private static final String SAME_PHONE_NUMBER_MESSAGE = "新手机号不能与当前手机号相同";

    /**
     * 手机号已占用提示语。
     */
    private static final String PHONE_NUMBER_ALREADY_USED_MESSAGE = "手机号已被其他账号使用";

    /**
     * 手机验证码错误提示语。
     */
    private static final String PHONE_VERIFICATION_CODE_INCORRECT_MESSAGE = "验证码错误";

    /**
     * 手机验证码过期提示语。
     */
    private static final String PHONE_VERIFICATION_CODE_EXPIRED_MESSAGE = "验证码已过期，请重新发送";

    /**
     * 手机号修改失败提示语。
     */
    private static final String CHANGE_PHONE_FAIL_MESSAGE = "手机号修改失败，请稍后重试";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisCache redisCache;
    private final RedisTokenStore redisTokenStore;
    private final JwtTokenProvider jwtTokenProvider;
    private final AsyncUserLogService asyncUserLogService;
    private final UserMapper userMapper;
    private final LoginLogPublisher loginLogPublisher;
    private final CaptchaService captchaService;
    private final LoginSecurityService loginSecurityService;

    @Override
    public Long register(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new ParamException("用户名或密码不能为空");
        }
        User exists = userService.lambdaQuery().eq(User::getUsername, username).one();
        if (exists != null) {
            throw new ServiceException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password.trim()));
        userService.save(user);

        Long roleId = userMapper.selectRoleIdByRoleCode(RolesConstant.USER);
        if (roleId == null) {
            throw new ServiceException("默认用户角色不存在，请先初始化RBAC数据");
        }
        userMapper.insertUserRole(user.getId(), roleId);

        return user.getId();
    }

    @Override
    public AuthTokenVo login(String username, String password, String captchaVerificationId) {
        Assert.hasText(username, "用户名不能为空");
        Assert.hasText(password, "密码不能为空");
        String trimmedUsername = username.trim();
        LoginLockStatus lockStatus = loginSecurityService.checkLockStatus(LoginSecurityService.LOGIN_SOURCE_CLIENT,
                trimmedUsername);
        if (lockStatus.isLocked()) {
            String lockMessage = loginSecurityService.buildLockMessage(lockStatus.getRemainingSeconds());
            recordLoginLog(null, trimmedUsername, false, lockMessage);
            throw new LoginException(ResponseCode.ACCOUNT_LOCKED, lockMessage);
        }
        captchaService.validateLoginCaptcha(captchaVerificationId);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(trimmedUsername,
                password.trim());
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(token);
            var session = tokenService.createToken(authentication);
            SysUserDetails sysUserDetails = (SysUserDetails) authentication.getPrincipal();
            Long userId = null;
            if (sysUserDetails != null) {
                userId = sysUserDetails.getUserId();
            }
            // 记录用户表最后登录信息（保留现有逻辑）
            HttpServletRequest request = resolveRequest();
            String ipAddress = request != null ? IpAddressUtils.getIpAddress(request) : null;
            asyncUserLogService.recordUserLoginLog(userId, ipAddress);
            loginSecurityService.clearLoginState(LoginSecurityService.LOGIN_SOURCE_CLIENT, trimmedUsername);
            recordLoginLog(authentication, trimmedUsername, true, null);
            return AuthTokenVo.builder()
                    .accessToken(session.getAccessToken())
                    .refreshToken(session.getRefreshToken())
                    .build();
        } catch (BadCredentialsException e) {
            LoginFailureResult failureResult = loginSecurityService.recordLoginFailure(
                    LoginSecurityService.LOGIN_SOURCE_CLIENT,
                    trimmedUsername);
            if (failureResult.isLocked()) {
                String lockMessage = loginSecurityService.buildLockMessage(failureResult.getRemainingSeconds());
                recordLoginLog(null, trimmedUsername, false, lockMessage);
                throw new LoginException(ResponseCode.ACCOUNT_LOCKED, lockMessage);
            }
            recordLoginLog(null, trimmedUsername, false, "账号或密码错误");
            throw new LoginException("账号或密码错误");
        } catch (Exception ex) {
            recordLoginLog(authentication, trimmedUsername, false, ex.getMessage());
            throw ex;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public AuthTokenVo refresh(String refreshToken) {
        Assert.hasText(refreshToken, "刷新令牌不能为空");
        return tokenService.refreshToken(refreshToken);
    }

    /**
     * 发送修改手机号验证码。
     *
     * @param phoneNumber           新手机号
     * @param captchaVerificationId 验证码校验凭证
     */
    @Override
    public void sendPhoneVerificationCode(String phoneNumber, String captchaVerificationId) {
        Assert.hasText(phoneNumber, "手机号不能为空");
        Assert.hasText(captchaVerificationId, "验证码校验凭证不能为空");

        String normalizedPhoneNumber = phoneNumber.trim();
        captchaService.validateLoginCaptcha(captchaVerificationId.trim());

        User currentUser = getCurrentUserOrThrow();
        validatePhoneNumberForChange(currentUser, normalizedPhoneNumber);

        String verificationCode = generatePhoneVerificationCode();
        redisCache.setCacheObject(
                buildPhoneChangeVerificationCodeKey(currentUser.getId(), normalizedPhoneNumber),
                verificationCode,
                PHONE_CHANGE_CODE_TTL_SECONDS
        );
        log.info(
                "客户端手机号变更验证码已生成，userId={}, phoneNumber={}, verificationCode={}",
                currentUser.getId(),
                normalizedPhoneNumber,
                verificationCode
        );
    }

    /**
     * 修改当前登录用户手机号。
     *
     * @param phoneNumber       新手机号
     * @param verificationCode 手机验证码
     */
    @Override
    public void changePhone(String phoneNumber, String verificationCode) {
        Assert.hasText(phoneNumber, "手机号不能为空");
        Assert.hasText(verificationCode, "验证码不能为空");

        String normalizedPhoneNumber = phoneNumber.trim();
        String normalizedVerificationCode = verificationCode.trim();
        User currentUser = getCurrentUserOrThrow();
        validatePhoneNumberForChange(currentUser, normalizedPhoneNumber);

        String verificationCodeKey = buildPhoneChangeVerificationCodeKey(currentUser.getId(), normalizedPhoneNumber);
        String cachedVerificationCode = redisCache.getCacheObject(verificationCodeKey);
        if (cachedVerificationCode == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PHONE_VERIFICATION_CODE_EXPIRED_MESSAGE);
        }
        if (!normalizedVerificationCode.equals(cachedVerificationCode)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PHONE_VERIFICATION_CODE_INCORRECT_MESSAGE);
        }

        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setPhoneNumber(normalizedPhoneNumber);
        boolean updateResult = userService.updateById(updateUser);
        if (!updateResult) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, CHANGE_PHONE_FAIL_MESSAGE);
        }

        redisCache.deleteObject(verificationCodeKey);
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param oldPassword           原密码
     * @param newPassword           新密码
     * @param captchaVerificationId 验证码校验凭证
     */
    @Override
    public void changePassword(String oldPassword, String newPassword, String captchaVerificationId) {
        Assert.hasText(oldPassword, "原密码不能为空");
        Assert.hasText(newPassword, "新密码不能为空");
        Assert.hasText(captchaVerificationId, "验证码校验凭证不能为空");

        String trimmedOldPassword = oldPassword.trim();
        String trimmedNewPassword = newPassword.trim();
        captchaService.validateLoginCaptcha(captchaVerificationId.trim());

        Long userId = getUserId();
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, CURRENT_USER_NOT_FOUND_MESSAGE);
        }
        if (!passwordEncoder.matches(trimmedOldPassword, user.getPassword())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, OLD_PASSWORD_INCORRECT_MESSAGE);
        }
        if (trimmedOldPassword.equals(trimmedNewPassword)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, SAME_PASSWORD_MESSAGE);
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(trimmedNewPassword));
        boolean updateResult = userService.updateById(updateUser);
        if (!updateResult) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, CHANGE_PASSWORD_FAIL_MESSAGE);
        }

        redisTokenStore.deleteSessionsByUserIds(Set.of(userId));
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户
     */
    private User getCurrentUserOrThrow() {
        Long userId = getUserId();
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, CURRENT_USER_NOT_FOUND_MESSAGE);
        }
        return user;
    }

    /**
     * 校验手机号是否允许修改。
     *
     * @param currentUser 当前登录用户
     * @param phoneNumber 新手机号
     * @return 无返回值
     */
    private void validatePhoneNumberForChange(User currentUser, String phoneNumber) {
        if (!phoneNumber.matches(PHONE_NUMBER_REGEX)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, PHONE_NUMBER_INVALID_MESSAGE);
        }
        if (phoneNumber.equals(StringUtils.trimToEmpty(currentUser.getPhoneNumber()))) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, SAME_PHONE_NUMBER_MESSAGE);
        }

        User existingUser = userService.lambdaQuery()
                .eq(User::getPhoneNumber, phoneNumber)
                .one();
        if (existingUser != null && !currentUser.getId().equals(existingUser.getId())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, PHONE_NUMBER_ALREADY_USED_MESSAGE);
        }
    }

    /**
     * 构建手机号验证码缓存 key。
     *
     * @param userId 当前用户ID
     * @param phoneNumber 新手机号
     * @return Redis key
     */
    private String buildPhoneChangeVerificationCodeKey(Long userId, String phoneNumber) {
        return String.format(PHONE_CHANGE_CODE_KEY_TEMPLATE, userId, phoneNumber);
    }

    /**
     * 生成指定位数的手机号验证码。
     *
     * @return 手机验证码
     */
    private String generatePhoneVerificationCode() {
        int randomNumber = ThreadLocalRandom.current().nextInt((int) Math.pow(10, PHONE_CHANGE_CODE_LENGTH - 1),
                (int) Math.pow(10, PHONE_CHANGE_CODE_LENGTH));
        return String.format("%0" + PHONE_CHANGE_CODE_LENGTH + "d", randomNumber);
    }

    @Override
    public void logout(String accessToken) {
        Assert.hasText(accessToken, "访问令牌不能为空");

        try {
            // 1. 解析访问令牌获取会话ID
            Claims claims = jwtTokenProvider.getClaimsFromToken(accessToken);
            if (claims == null) {
                log.warn("退出登录时解析访问令牌失败");
                return;
            }

            String accessTokenId = claims.get(CLAIM_KEY_SESSION_ID, String.class);
            if (StringUtils.isBlank(accessTokenId)) {
                log.warn("退出登录时访问令牌中没有会话ID");
                return;
            }

            // 2. 通过统一会话入口删除访问令牌、刷新令牌与用户索引。
            redisTokenStore.deleteSessionByAccessId(accessTokenId);
            log.info("用户退出登录成功，已清理会话信息，会话ID: {}", accessTokenId);

            // 3. 清空Spring Security上下文
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("退出登录发生异常", e);
            // 即使发生异常，也要确保清空Security上下文
            SecurityContextHolder.clearContext();
        }
    }

    private void recordLoginLog(Authentication authentication,
                                String username,
                                boolean success,
                                String failReason) {
        try {
            LoginLogMessage message = new LoginLogMessage();
            message.setUsername(username);
            message.setLoginStatus(success ? 1 : 0);
            message.setFailReason(success ? null : failReason);
            message.setLoginSource(LOGIN_SOURCE_CLIENT);
            message.setLoginType(LOGIN_TYPE_PASSWORD);
            message.setLoginTime(new Date());

            if (authentication != null && authentication.getPrincipal() instanceof SysUserDetails userDetails) {
                message.setUserId(userDetails.getUserId());
                if (StringUtils.isNotBlank(userDetails.getUsername())) {
                    message.setUsername(userDetails.getUsername());
                }
            }

            HttpServletRequest request = resolveRequest();
            if (request != null) {
                String ip = IpAddressUtils.getIpAddress(request);
                message.setIpAddress(ip);
                String userAgent = request.getHeader(USER_AGENT_HEADER);
                message.setUserAgent(userAgent);
                fillUserAgentInfo(message, userAgent);
            }
            loginLogPublisher.publish(message);
        } catch (Exception ex) {
            log.debug("Failed to record client login log", ex);
        }
    }

    private HttpServletRequest resolveRequest() {
        try {
            return SecurityUtils.getHttpServletRequest();
        } catch (Exception ex) {
            return null;
        }
    }

    private void fillUserAgentInfo(LoginLogMessage message, String userAgent) {
        if (!org.springframework.util.StringUtils.hasText(userAgent)) {
            return;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            message.setDeviceType("mobile");
        } else {
            message.setDeviceType("pc");
        }

        if (ua.contains("windows")) {
            message.setOs("Windows");
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            message.setOs("macOS");
        } else if (ua.contains("android")) {
            message.setOs("Android");
        } else if (ua.contains("iphone") || ua.contains("ios")) {
            message.setOs("iOS");
        } else if (ua.contains("linux")) {
            message.setOs("Linux");
        } else {
            message.setOs("Unknown");
        }

        if (ua.contains("edg/")) {
            message.setBrowser("Edge");
        } else if (ua.contains("chrome/")) {
            message.setBrowser("Chrome");
        } else if (ua.contains("firefox/")) {
            message.setBrowser("Firefox");
        } else if (ua.contains("safari/") && !ua.contains("chrome/")) {
            message.setBrowser("Safari");
        } else {
            message.setBrowser("Unknown");
        }
    }
}
