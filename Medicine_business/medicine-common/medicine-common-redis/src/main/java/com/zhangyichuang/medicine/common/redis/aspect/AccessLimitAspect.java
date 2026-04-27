package com.zhangyichuang.medicine.common.redis.aspect;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.AccessLimitException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.IpAddressUtils;
import com.zhangyichuang.medicine.common.redis.annotation.AccessLimit;
import com.zhangyichuang.medicine.common.redis.annotation.AccessLimitRule;
import com.zhangyichuang.medicine.common.redis.enums.AccessLimitDimension;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 访问限流切面。
 * <p>
 * 该切面基于 Redis ZSet + Lua 脚本实现滑动窗口限流，支持用户和 IP 维度限流。
 * </p>
 *
 * @author Chuang
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLimitAspect {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(AccessLimitAspect.class);

    /**
     * 参数名发现器。
     */
    private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    /**
     * SpEL 解析器。
     */
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    /**
     * Redis Lua 脚本执行成功标识。
     */
    private static final long LUA_RESULT_ALLOWED = 1L;

    /**
     * Redis Lua 脚本执行拒绝标识。
     */
    private static final long LUA_RESULT_REJECTED = 0L;

    /**
     * 滑动窗口限流 Lua 脚本。
     */
    private static final DefaultRedisScript<Long> ACCESS_LIMIT_SCRIPT = buildAccessLimitScript();

    /**
     * String 类型 Redis 模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * BeanFactory 解析器。
     */
    private final BeanFactoryResolver beanFactoryResolver;

    /**
     * 构造访问限流切面。
     *
     * @param stringRedisTemplate String 类型 Redis 模板
     * @param beanFactory         Spring BeanFactory
     */
    public AccessLimitAspect(StringRedisTemplate stringRedisTemplate, BeanFactory beanFactory) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.beanFactoryResolver = new BeanFactoryResolver(beanFactory);
    }

    /**
     * 构建限流 Lua 脚本对象。
     *
     * @return 限流 Lua 脚本对象
     */
    private static DefaultRedisScript<Long> buildAccessLimitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText("""
                local nowMillis = tonumber(ARGV[1])
                local requestId = ARGV[2]
                local ruleCount = tonumber(ARGV[3])
                if nowMillis == nil or requestId == nil or ruleCount == nil or ruleCount <= 0 then
                    return -1
                end
                local maxWindowMillis = 0
                for ruleIndex = 1, ruleCount do
                    local baseArgIndex = 3 + (ruleIndex - 1) * 2
                    local windowMillis = tonumber(ARGV[baseArgIndex + 1])
                    if windowMillis == nil or windowMillis <= 0 then
                        return -1
                    end
                    if windowMillis > maxWindowMillis then
                        maxWindowMillis = windowMillis
                    end
                end
                for keyIndex = 1, #KEYS do
                    local redisKey = KEYS[keyIndex]
                    local minScore = nowMillis - maxWindowMillis
                    redis.call('ZREMRANGEBYSCORE', redisKey, '-inf', minScore)
                    for ruleIndex = 1, ruleCount do
                        local baseArgIndex = 3 + (ruleIndex - 1) * 2
                        local windowMillis = tonumber(ARGV[baseArgIndex + 1])
                        local limitCount = tonumber(ARGV[baseArgIndex + 2])
                        if limitCount == nil or limitCount <= 0 then
                            return -1
                        end
                        local windowStart = nowMillis - windowMillis + 1
                        local requestCount = redis.call('ZCOUNT', redisKey, windowStart, nowMillis)
                        if requestCount >= limitCount then
                            return 0
                        end
                    end
                end
                for keyIndex = 1, #KEYS do
                    local redisKey = KEYS[keyIndex]
                    redis.call('ZADD', redisKey, nowMillis, requestId)
                    redis.call('PEXPIRE', redisKey, maxWindowMillis)
                end
                return 1
                """);
        return redisScript;
    }

    /**
     * 拦截标注了访问限流注解的方法或类。
     *
     * @param joinPoint 切点信息
     * @return 目标方法执行结果
     * @throws Throwable 目标方法执行异常
     */
    @Around("@annotation(com.zhangyichuang.medicine.common.redis.annotation.AccessLimit) || "
            + "@within(com.zhangyichuang.medicine.common.redis.annotation.AccessLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AccessLimit accessLimit = resolveAnnotation(joinPoint, method);
        AccessLimitRule[] accessLimitRules = resolveAndValidateRules(accessLimit.rules());
        HttpServletRequest request = resolveRequest();
        validateRequest(request);

        String resourceKey = resolveResourceKey(joinPoint, method, request, accessLimit);
        List<LimitTarget> limitTargets = resolveLimitTargets(accessLimit.dimension(), request);
        List<String> redisKeys = buildRedisKeys(limitTargets, resourceKey);
        long currentTimeMillis = System.currentTimeMillis();
        String requestId = buildRequestId();

        boolean allowed = executeAccessLimitScript(redisKeys, accessLimitRules, currentTimeMillis, requestId);
        if (!allowed) {
            log.warn("命中访问限流，dimension={}，resourceKey={}，redisKeys={}",
                    accessLimit.dimension(),
                    resourceKey,
                    redisKeys);
            throw new AccessLimitException(resolveFailMessage(accessLimit.failMessage()));
        }
        return joinPoint.proceed();
    }

    /**
     * 解析访问限流注解配置。
     *
     * @param joinPoint 切点信息
     * @param method    当前方法
     * @return 访问限流注解
     */
    private AccessLimit resolveAnnotation(ProceedingJoinPoint joinPoint, Method method) {
        AccessLimit annotation = method.getAnnotation(AccessLimit.class);
        if (annotation != null) {
            return annotation;
        }
        Method targetMethod = resolveTargetMethod(joinPoint, method);
        annotation = targetMethod.getAnnotation(AccessLimit.class);
        if (annotation != null) {
            return annotation;
        }
        Class<?> targetClass = joinPoint.getTarget() == null ? method.getDeclaringClass() : joinPoint.getTarget().getClass();
        annotation = targetClass.getAnnotation(AccessLimit.class);
        if (annotation == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "未找到访问限流注解配置");
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
        } catch (NoSuchMethodException exception) {
            log.debug("未获取到目标类方法定义，使用签名方法继续处理，method={}", method.getName(), exception);
            return method;
        }
    }

    /**
     * 解析并校验限流规则。
     *
     * @param accessLimitRules 注解中的限流规则数组
     * @return 校验后的限流规则数组
     */
    private AccessLimitRule[] resolveAndValidateRules(AccessLimitRule[] accessLimitRules) {
        if (accessLimitRules == null || accessLimitRules.length == 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则不能为空");
        }
        for (AccessLimitRule accessLimitRule : accessLimitRules) {
            validateRule(accessLimitRule);
        }
        return accessLimitRules;
    }

    /**
     * 校验单个限流规则是否合法。
     *
     * @param accessLimitRule 限流规则
     */
    private void validateRule(AccessLimitRule accessLimitRule) {
        if (accessLimitRule == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则不能为空");
        }
        if (accessLimitRule.limit() <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则 limit 必须大于0");
        }
        if (accessLimitRule.interval() <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则 interval 必须大于0");
        }
        if (accessLimitRule.unit() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则 unit 不能为空");
        }
        long windowMillis = accessLimitRule.unit().toMillis(accessLimitRule.interval());
        if (windowMillis <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流规则时间窗口必须大于0毫秒");
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
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流注解仅支持Web请求");
        }
        if (!StringUtils.hasText(request.getMethod())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流未获取到请求方法");
        }
        if (!StringUtils.hasText(request.getRequestURI())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流未获取到请求路径");
        }
    }

    /**
     * 解析限流资源键。
     *
     * @param joinPoint   切点信息
     * @param method      当前方法
     * @param request     当前请求对象
     * @param accessLimit 限流注解
     * @return 限流资源键
     */
    private String resolveResourceKey(ProceedingJoinPoint joinPoint,
                                      Method method,
                                      HttpServletRequest request,
                                      AccessLimit accessLimit) {
        String baseResource = resolveBaseResource(request, accessLimit.resource());
        if (!StringUtils.hasText(accessLimit.key())) {
            return baseResource;
        }
        String businessKey = resolveBusinessKey(joinPoint, method, accessLimit.key());
        return baseResource + ":" + businessKey;
    }

    /**
     * 解析基础资源键。
     *
     * @param request            当前请求对象
     * @param annotationResource 注解中配置的资源键
     * @return 基础资源键
     */
    private String resolveBaseResource(HttpServletRequest request, String annotationResource) {
        if (StringUtils.hasText(annotationResource)) {
            return annotationResource.trim();
        }
        String httpMethod = normalizeHttpMethod(request.getMethod());
        String requestUri = request.getRequestURI().trim();
        return httpMethod + ":" + requestUri;
    }

    /**
     * 解析业务键表达式。
     *
     * @param joinPoint     切点信息
     * @param method        当前方法
     * @param keyExpression 业务键表达式
     * @return 业务键
     */
    private String resolveBusinessKey(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        Object expressionValue = evaluateBusinessKey(joinPoint, method, keyExpression);
        if (expressionValue == null || !StringUtils.hasText(expressionValue.toString())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流 key 表达式解析结果不能为空");
        }
        return expressionValue.toString().trim();
    }

    /**
     * 执行业务键 SpEL 表达式。
     *
     * @param joinPoint     切点信息
     * @param method        当前方法
     * @param keyExpression 业务键表达式
     * @return 表达式解析结果
     */
    private Object evaluateBusinessKey(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                PARAMETER_NAME_DISCOVERER
        );
        evaluationContext.setBeanResolver(beanFactoryResolver);
        Expression expression = EXPRESSION_PARSER.parseExpression(keyExpression);
        return expression.getValue(evaluationContext);
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
     * 根据限流维度解析限流主体列表。
     *
     * @param accessLimitDimension 限流维度
     * @param request              当前请求对象
     * @return 限流主体列表
     */
    private List<LimitTarget> resolveLimitTargets(AccessLimitDimension accessLimitDimension, HttpServletRequest request) {
        return switch (accessLimitDimension) {
            case USER -> List.of(resolveUserTarget());
            case IP -> List.of(resolveIpTarget(request));
            case USER_OR_IP -> List.of(resolveUserOrIpTarget(request));
            case USER_AND_IP -> resolveUserAndIpTargets(request);
        };
    }

    /**
     * 解析用户维度限流主体。
     *
     * @return 用户维度限流主体
     */
    private LimitTarget resolveUserTarget() {
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            throw new ServiceException(ResponseCode.UNAUTHORIZED, "访问限流未获取到用户ID");
        }
        return new LimitTarget(RedisConstants.AccessLimit.IDENTITY_USER, String.valueOf(userId));
    }

    /**
     * 解析 IP 维度限流主体。
     *
     * @param request 当前请求对象
     * @return IP 维度限流主体
     */
    private LimitTarget resolveIpTarget(HttpServletRequest request) {
        String clientIp = IpAddressUtils.getIpAddress(request);
        if (!StringUtils.hasText(clientIp)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流未获取到客户端IP");
        }
        return new LimitTarget(RedisConstants.AccessLimit.IDENTITY_IP, clientIp.trim());
    }

    /**
     * 解析优先用户 ID 否则 IP 的限流主体。
     *
     * @param request 当前请求对象
     * @return 限流主体
     */
    private LimitTarget resolveUserOrIpTarget(HttpServletRequest request) {
        Long userId = resolveCurrentUserId();
        if (userId != null) {
            return new LimitTarget(RedisConstants.AccessLimit.IDENTITY_USER, String.valueOf(userId));
        }
        return resolveIpTarget(request);
    }

    /**
     * 解析用户与 IP 双维度限流主体。
     *
     * @param request 当前请求对象
     * @return 双维度限流主体列表
     */
    private List<LimitTarget> resolveUserAndIpTargets(HttpServletRequest request) {
        LimitTarget userTarget = resolveUserTarget();
        LimitTarget ipTarget = resolveIpTarget(request);
        return List.of(userTarget, ipTarget);
    }

    /**
     * 构建 Redis 限流键列表。
     *
     * @param limitTargets 限流主体列表
     * @param resourceKey  资源键
     * @return Redis 限流键列表
     */
    private List<String> buildRedisKeys(List<LimitTarget> limitTargets, String resourceKey) {
        if (!StringUtils.hasText(resourceKey)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流资源键不能为空");
        }
        Set<String> keySet = new LinkedHashSet<>();
        for (LimitTarget limitTarget : limitTargets) {
            if (!StringUtils.hasText(limitTarget.identityType())) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流主体类型不能为空");
            }
            if (!StringUtils.hasText(limitTarget.identityValue())) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流主体标识不能为空");
            }
            keySet.add(String.format(
                    RedisConstants.AccessLimit.KEY_TEMPLATE,
                    limitTarget.identityType(),
                    limitTarget.identityValue(),
                    resourceKey
            ));
        }
        return new ArrayList<>(keySet);
    }

    /**
     * 执行 Redis Lua 限流脚本。
     *
     * @param redisKeys         Redis 限流键列表
     * @param accessLimitRules  限流规则数组
     * @param currentTimeMillis 当前时间戳（毫秒）
     * @param requestId         请求唯一标识
     * @return true 表示放行，false 表示拒绝
     */
    private boolean executeAccessLimitScript(List<String> redisKeys,
                                             AccessLimitRule[] accessLimitRules,
                                             long currentTimeMillis,
                                             String requestId) {
        List<String> scriptArguments = buildScriptArguments(accessLimitRules, currentTimeMillis, requestId);
        Long executeResult = stringRedisTemplate.execute(
                ACCESS_LIMIT_SCRIPT,
                redisKeys,
                scriptArguments.toArray()
        );
        if (executeResult == null) {
            throw new ServiceException(ResponseCode.SERVER_ERROR, "访问限流脚本执行失败");
        }
        if (executeResult == LUA_RESULT_REJECTED) {
            return false;
        }
        if (executeResult == LUA_RESULT_ALLOWED) {
            return true;
        }
        throw new ServiceException(ResponseCode.SERVER_ERROR, "访问限流脚本返回非法结果");
    }

    /**
     * 构建 Lua 脚本参数列表。
     *
     * @param accessLimitRules  限流规则数组
     * @param currentTimeMillis 当前时间戳（毫秒）
     * @param requestId         请求唯一标识
     * @return Lua 脚本参数列表
     */
    private List<String> buildScriptArguments(AccessLimitRule[] accessLimitRules, long currentTimeMillis, String requestId) {
        List<String> scriptArguments = new ArrayList<>(3 + accessLimitRules.length * 2);
        scriptArguments.add(String.valueOf(currentTimeMillis));
        scriptArguments.add(requestId);
        scriptArguments.add(String.valueOf(accessLimitRules.length));
        for (AccessLimitRule accessLimitRule : accessLimitRules) {
            long windowMillis = accessLimitRule.unit().toMillis(accessLimitRule.interval());
            scriptArguments.add(String.valueOf(windowMillis));
            scriptArguments.add(String.valueOf(accessLimitRule.limit()));
        }
        return scriptArguments;
    }

    /**
     * 构建请求唯一标识。
     *
     * @return 请求唯一标识
     */
    private String buildRequestId() {
        return UUID.randomUUID() + ":" + Thread.currentThread().getId();
    }

    /**
     * 解析命中限流时的提示语。
     *
     * @param failMessage 原始提示语
     * @return 归一化后的提示语
     */
    private String resolveFailMessage(String failMessage) {
        if (!StringUtils.hasText(failMessage)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "访问限流失败提示语不能为空");
        }
        return failMessage.trim();
    }

    /**
     * 解析当前登录用户 ID。
     *
     * @return 当前登录用户 ID
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
     * 从认证主体中提取用户 ID。
     *
     * @param principal 认证主体
     * @return 用户 ID
     */
    private Long extractUserId(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        if (principal instanceof Map<?, ?> principalMap) {
            return extractUserIdFromMap(principalMap);
        }
        Long getterValue = extractUserIdFromGetter(principal);
        if (getterValue != null) {
            return getterValue;
        }
        return extractUserIdFromField(principal);
    }

    /**
     * 从 Map 认证主体中提取用户 ID。
     *
     * @param principalMap Map 认证主体
     * @return 用户 ID
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
     * 通过 getter 方法提取用户 ID。
     *
     * @param principal 认证主体
     * @return 用户 ID
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
        } catch (ReflectiveOperationException exception) {
            log.debug("认证主体未提供 getUserId 方法，principalClass={}", principal.getClass().getName(), exception);
            return null;
        }
    }

    /**
     * 通过字段提取用户 ID。
     *
     * @param principal 认证主体
     * @return 用户 ID
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
        } catch (ReflectiveOperationException exception) {
            log.debug("认证主体未提供 userId 字段，principalClass={}", principal.getClass().getName(), exception);
            return null;
        }
    }

    /**
     * 限流主体定义。
     *
     * @param identityType  限流主体类型
     * @param identityValue 限流主体标识值
     */
    private record LimitTarget(String identityType, String identityValue) {
    }
}
