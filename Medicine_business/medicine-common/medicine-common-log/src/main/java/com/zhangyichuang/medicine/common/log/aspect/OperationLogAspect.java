package com.zhangyichuang.medicine.common.log.aspect;

import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.executor.OperationLogExecutor;
import com.zhangyichuang.medicine.common.log.model.OperationLogInfo;
import com.zhangyichuang.medicine.common.log.support.OperationLogUtils;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志切面。
 */
@Aspect
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final OperationLogExecutor executor;

    public OperationLogAspect(OperationLogExecutor executor) {
        this.executor = executor;
    }

    /**
     * 拦截显式标注 @OperationLog 的方法/类。
     */
    @Around("@annotation(com.zhangyichuang.medicine.common.log.annotation.OperationLog) || "
            + "@within(com.zhangyichuang.medicine.common.log.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            try {
                OperationLog resolved = resolveOperationLog(joinPoint);
                HttpServletRequest request = getRequest();
                if (resolved != null && shouldRecord(request)) {
                    long costTime = System.currentTimeMillis() - startTime;
                    OperationLogInfo logInfo = buildLogInfo(joinPoint, resolved, result, error, costTime, request);
                    executor.record(logInfo, request);
                }
            } catch (Exception ex) {
                log.warn("Failed to record operation log", ex);
            }
        }
    }

    private OperationLog resolveOperationLog(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog methodAnnotation = method.getAnnotation(OperationLog.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        Class<?> declaringType = signature.getDeclaringType();
        return declaringType.getAnnotation(OperationLog.class);
    }

    private boolean shouldRecord(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String uri = request.getRequestURI();
        if ("/auth/login".equals(uri) || "/auth/refresh".equals(uri) || "/auth/logout".equals(uri)) {
            return false;
        }
        String method = request.getMethod();
        return method == null || WRITE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }

    private OperationLogInfo buildLogInfo(ProceedingJoinPoint joinPoint,
                                          OperationLog operationLog,
                                          Object result,
                                          Throwable error,
                                          long costTime,
                                          HttpServletRequest request) {
        OperationLogInfo logInfo = new OperationLogInfo();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String module = operationLog.module();
        if (!StringUtils.hasText(module)) {
            module = signature.getDeclaringType().getSimpleName();
        }
        String action = operationLog.action();
        if (!StringUtils.hasText(action)) {
            action = operationLog.type().name();
        }

        logInfo.setModule(module);
        logInfo.setAction(action);
        logInfo.setMethodName(signature.getDeclaringTypeName() + "." + signature.getName());
        logInfo.setCostTime(costTime);
        logInfo.setSuccess(error == null ? 1 : 0);
        if (error != null) {
            String errorMsg = error.getMessage();
            logInfo.setErrorMsg(StringUtils.hasText(errorMsg) ? errorMsg : error.getClass().getName());
        }

        if (request != null) {
            logInfo.setRequestUri(request.getRequestURI());
            logInfo.setHttpMethod(request.getMethod());
        }

        fillUserInfo(logInfo);

        if (operationLog.recordParams()) {
            logInfo.setRequestParams(resolveRequestParams(joinPoint, request));
        }
        if (operationLog.recordResult()) {
            logInfo.setResponseResult(resolveResponseResult(result));
        }
        return logInfo;
    }

    private void fillUserInfo(OperationLogInfo logInfo) {
        try {
            if (SecurityUtils.isAuthenticated()) {
                logInfo.setUserId(SecurityUtils.getUserId());
                logInfo.setUsername(SecurityUtils.getUsername());
            }
        } catch (Exception ex) {
            log.debug("Failed to resolve user info for operation log", ex);
        }
    }

    private String resolveRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return resolveRequestParamMap(request);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (OperationLogUtils.isFilterObject(arg)) {
                continue;
            }
            String name = (paramNames != null && paramNames.length > i) ? paramNames[i] : "arg" + i;
            params.put(name, arg);
        }
        if (params.isEmpty()) {
            return resolveRequestParamMap(request);
        }
        return OperationLogUtils.toJson(params);
    }

    private String resolveRequestParamMap(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null || values.length == 0) {
                params.put(entry.getKey(), null);
            } else if (values.length == 1) {
                params.put(entry.getKey(), values[0]);
            } else {
                params.put(entry.getKey(), values);
            }
        }
        return OperationLogUtils.toJson(params);
    }

    private String resolveResponseResult(Object result) {
        if (OperationLogUtils.isFilterObject(result)) {
            return null;
        }
        return OperationLogUtils.toJson(result);
    }

    private HttpServletRequest getRequest() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                return attributes.getRequest();
            }
        } catch (Exception ex) {
            log.debug("Failed to resolve request for operation log", ex);
        }
        return null;
    }
}
