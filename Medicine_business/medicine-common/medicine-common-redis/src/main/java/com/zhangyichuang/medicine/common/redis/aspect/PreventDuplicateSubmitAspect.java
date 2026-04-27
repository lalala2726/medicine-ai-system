package com.zhangyichuang.medicine.common.redis.aspect;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.DuplicateSubmitException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.redis.support.PreventDuplicateSubmitFingerprintBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面。
 * <p>
 * 该切面在进入业务方法前，基于当前登录用户、请求路径与参数指纹构建防抖 key，
 * 使用 Redis 的 NX + TTL 语义拦截短时间内的重复提交。
 * </p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreventDuplicateSubmitAspect {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(PreventDuplicateSubmitAspect.class);

    /**
     * 写请求方法集合。
     */
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    /**
     * Redis 模板。
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 参数指纹构建器。
     */
    private final PreventDuplicateSubmitFingerprintBuilder fingerprintBuilder;

    /**
     * 构造防重复提交切面。
     *
     * @param redisTemplate      Redis 模板
     * @param fingerprintBuilder 参数指纹构建器
     */
    public PreventDuplicateSubmitAspect(RedisTemplate<Object, Object> redisTemplate,
                                        PreventDuplicateSubmitFingerprintBuilder fingerprintBuilder) {
        this.redisTemplate = redisTemplate;
        this.fingerprintBuilder = fingerprintBuilder;
    }

    /**
     * 拦截标注了防重复提交注解的方法或类。
     *
     * @param joinPoint 切点信息
     * @return 目标方法执行结果
     * @throws Throwable 目标方法执行异常
     */
    @Around("@annotation(com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit) || "
            + "@within(com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        PreventDuplicateSubmit preventDuplicateSubmit = resolveAnnotation(joinPoint, method);
        validateIntervalMillis(preventDuplicateSubmit.intervalMillis());
        HttpServletRequest request = resolveRequest();
        validateRequest(request);
        String httpMethod = normalizeHttpMethod(request.getMethod());
        if (!WRITE_METHODS.contains(httpMethod)) {
            return joinPoint.proceed();
        }

        Long userId = resolveCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResponseCode.UNAUTHORIZED, "防重复提交未获取到用户ID");
        }

        String parameterFingerprint = fingerprintBuilder.buildFingerprint(
                ((MethodSignature) joinPoint.getSignature()).getParameterNames(),
                joinPoint.getArgs()
        );
        String redisKey = String.format(
                RedisConstants.DuplicateSubmit.KEY_TEMPLATE,
                userId,
                httpMethod,
                request.getRequestURI(),
                parameterFingerprint
        );

        Boolean setResult = redisTemplate.opsForValue().setIfAbsent(
                redisKey,
                String.valueOf(System.currentTimeMillis()),
                preventDuplicateSubmit.intervalMillis(),
                TimeUnit.MILLISECONDS
        );
        if (setResult == null) {
            throw new ServiceException(ResponseCode.SERVER_ERROR, "防重复提交写入失败");
        }
        if (!setResult) {
            log.warn("命中重复提交限制，userId={}，httpMethod={}，requestUri={}，redisKey={}",
                    userId,
                    httpMethod,
                    request.getRequestURI(),
                    redisKey);
            throw new DuplicateSubmitException(resolveFailMessage(preventDuplicateSubmit.failMessage()));
        }
        return joinPoint.proceed();
    }

    /**
     * 解析防重复提交注解配置。
     *
     * @param joinPoint 切点信息
     * @param method    当前方法
     * @return 防重复提交注解
     */
    private PreventDuplicateSubmit resolveAnnotation(ProceedingJoinPoint joinPoint, Method method) {
        PreventDuplicateSubmit annotation = method.getAnnotation(PreventDuplicateSubmit.class);
        if (annotation != null) {
            return annotation;
        }
        Method targetMethod = resolveTargetMethod(joinPoint, method);
        annotation = targetMethod.getAnnotation(PreventDuplicateSubmit.class);
        if (annotation != null) {
            return annotation;
        }
        Class<?> targetClass = joinPoint.getTarget() == null ? method.getDeclaringClass() : joinPoint.getTarget().getClass();
        annotation = targetClass.getAnnotation(PreventDuplicateSubmit.class);
        if (annotation == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "未找到防重复提交注解配置");
        }
        return annotation;
    }

    /**
     * 获取目标类中的具体方法定义。
     *
     * @param joinPoint 切点信息
     * @param method    方法签名中的方法
     * @return 目标类方法
     */
    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint, Method method) {
        Object target = joinPoint.getTarget();
        if (target == null) {
            return method;
        }
        try {
            return target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            log.debug("未获取到目标类方法定义，使用签名方法继续处理，method={}", method.getName(), ex);
            return method;
        }
    }

    /**
     * 解析当前请求对象。
     *
     * @return 当前请求对象
     */
    private HttpServletRequest resolveRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 校验当前请求是否合法。
     *
     * @param request 当前请求对象
     */
    private void validateRequest(HttpServletRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "防重复提交注解仅支持Web请求");
        }
        if (!StringUtils.hasText(request.getMethod())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "防重复提交未获取到请求方法");
        }
        if (!StringUtils.hasText(request.getRequestURI())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "防重复提交未获取到请求路径");
        }
    }

    /**
     * 归一化 HTTP 方法。
     *
     * @param httpMethod 原始 HTTP 方法
     * @return 归一化后的 HTTP 方法
     */
    private String normalizeHttpMethod(String httpMethod) {
        return httpMethod == null ? "" : httpMethod.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析当前登录用户ID。
     *
     * @return 当前登录用户ID
     */
    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return extractUserId(authentication.getPrincipal());
    }

    /**
     * 从认证主体中提取用户ID。
     *
     * @param principal 认证主体
     * @return 用户ID
     */
    private Long extractUserId(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        if (principal instanceof Map<?, ?> map) {
            return extractUserIdFromMap(map);
        }
        Long getterValue = extractUserIdFromGetter(principal);
        if (getterValue != null) {
            return getterValue;
        }
        return extractUserIdFromField(principal);
    }

    /**
     * 从 Map 认证主体中提取用户ID。
     *
     * @param principalMap Map 认证主体
     * @return 用户ID
     */
    private Long extractUserIdFromMap(Map<?, ?> principalMap) {
        Object userIdValue = principalMap.get("userId");
        if (userIdValue instanceof Number number) {
            return number.longValue();
        }
        if (userIdValue instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text.trim());
        }
        return null;
    }

    /**
     * 通过 getter 方法提取用户ID。
     *
     * @param principal 认证主体
     * @return 用户ID
     */
    private Long extractUserIdFromGetter(Object principal) {
        try {
            Method getter = principal.getClass().getMethod("getUserId");
            Object userIdValue = getter.invoke(principal);
            if (userIdValue instanceof Number number) {
                return number.longValue();
            }
            if (userIdValue instanceof String text && StringUtils.hasText(text)) {
                return Long.valueOf(text.trim());
            }
            return null;
        } catch (ReflectiveOperationException ex) {
            log.debug("认证主体未提供 getUserId 方法，principalClass={}", principal.getClass().getName(), ex);
            return null;
        }
    }

    /**
     * 通过字段提取用户ID。
     *
     * @param principal 认证主体
     * @return 用户ID
     */
    private Long extractUserIdFromField(Object principal) {
        try {
            Field userIdField = principal.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            Object userIdValue = userIdField.get(principal);
            if (userIdValue instanceof Number number) {
                return number.longValue();
            }
            if (userIdValue instanceof String text && StringUtils.hasText(text)) {
                return Long.valueOf(text.trim());
            }
            return null;
        } catch (ReflectiveOperationException ex) {
            log.debug("认证主体未提供 userId 字段，principalClass={}", principal.getClass().getName(), ex);
            return null;
        }
    }

    /**
     * 解析命中重复提交时的提示语。
     *
     * @param failMessage 原始提示语
     * @return 归一化后的提示语
     */
    private String resolveFailMessage(String failMessage) {
        return StringUtils.hasText(failMessage) ? failMessage.trim() : "请勿重复提交";
    }

    /**
     * 校验时间窗口是否合法。
     *
     * @param intervalMillis 时间窗口（毫秒）
     */
    private void validateIntervalMillis(long intervalMillis) {
        if (intervalMillis <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "防重复提交时间窗口必须大于0");
        }
    }
}
