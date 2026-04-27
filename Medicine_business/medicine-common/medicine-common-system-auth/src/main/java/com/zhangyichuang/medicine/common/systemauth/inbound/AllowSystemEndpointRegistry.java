package com.zhangyichuang.medicine.common.systemauth.inbound;

import com.zhangyichuang.medicine.common.systemauth.annotation.AllowSystem;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 维护标记了 {@link AllowSystem} 的接口集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllowSystemEndpointRegistry implements SmartInitializingSingleton {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 路径模式解析器。
     */
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    /**
     * 当前 @AllowSystem 接口快照。
     */
    private volatile List<AllowSystemEndpoint> endpoints = List.of();

    @Override
    public void afterSingletonsInstantiated() {
        this.endpoints = discoverEndpoints();
        log.info("Loaded {} @AllowSystem endpoints", this.endpoints.size());
    }

    /**
     * 判断请求是否命中 @AllowSystem 接口。
     *
     * @param request 当前 HTTP 请求
     * @return true 表示该请求需要系统签名认证
     */
    public boolean requiresSystemAuth(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        List<AllowSystemEndpoint> snapshot = this.endpoints;
        if (snapshot.isEmpty()) {
            return false;
        }
        String requestMethod = request.getMethod();
        PathContainer path = PathContainer.parsePath(request.getRequestURI());
        for (AllowSystemEndpoint endpoint : snapshot) {
            if (endpoint.matches(path, requestMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 Spring MVC 映射中收集所有 @AllowSystem 接口。
     */
    private List<AllowSystemEndpoint> discoverEndpoints() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        List<AllowSystemEndpoint> endpointList = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (!hasAllowSystem(handlerMethod)) {
                continue;
            }
            Set<String> methods = entry.getKey()
                    .getMethodsCondition()
                    .getMethods()
                    .stream()
                    .map(RequestMethod::name)
                    .collect(Collectors.toSet());
            for (String patternValue : entry.getKey().getPatternValues()) {
                try {
                    PathPattern pathPattern = pathPatternParser.parse(patternValue);
                    endpointList.add(new AllowSystemEndpoint(pathPattern, methods));
                } catch (Exception ex) {
                    log.warn("Skip invalid allow-system pattern: {}", patternValue);
                }
            }
        }
        return List.copyOf(endpointList);
    }

    /**
     * 判断类或方法上是否标记了 @AllowSystem。
     */
    private boolean hasAllowSystem(HandlerMethod handlerMethod) {
        AllowSystem methodAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), AllowSystem.class);
        AllowSystem classAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), AllowSystem.class);
        return methodAnnotation != null || classAnnotation != null;
    }

    /**
     * 单个 @AllowSystem 接口匹配项。
     */
    private record AllowSystemEndpoint(PathPattern pathPattern, Set<String> methods) {
        /**
         * 判断当前请求路径和方法是否命中该匹配项。
         */
        private boolean matches(PathContainer requestPath, String requestMethod) {
            if (!pathPattern.matches(requestPath)) {
                return false;
            }
            if (methods == null || methods.isEmpty()) {
                return true;
            }
            return methods.contains(requestMethod);
        }
    }
}
