package com.zhangyichuang.medicine.agent.exception;

import com.zhangyichuang.medicine.agent.support.DubboUnavailableDetector;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.GlobalExceptionHandel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Agent 侧 Dubbo 异常处理。
 * <p>
 * 仅针对 /agent/admin/** 路由返回“依赖未就绪”语义化错误，避免影响其他模块路由。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "com.zhangyichuang.medicine.agent.controller")
public class AgentRpcExceptionAdvice {

    private static final String ADMIN_ROUTE_PREFIX = "/agent/admin/";
    private static final String CLIENT_ROUTE_PREFIX = "/agent/client/";

    private final GlobalExceptionHandel globalExceptionHandel;

    /**
     * 处理 Dubbo RPC 调用异常。
     * <p>
     * 当请求路由属于 /agent/admin/** 且异常判定为下游不可用时，
     * 返回统一的“业务模块未就绪”语义化错误；否则保持现有全局异常处理逻辑。
     *
     * @param exception RPC 调用异常
     * @param request   当前 HTTP 请求
     * @return 统一响应结果
     */
    @ExceptionHandler(RpcException.class)
    public AjaxResult<Void> handleRpcException(RpcException exception, HttpServletRequest request) {
        if (isAdminRoute(request) && DubboUnavailableDetector.isUnavailable(exception)) {
            log.warn("admin 依赖服务未就绪, uri={}, message={}", request.getRequestURI(), exception.getMessage());
            return AjaxResult.error(ResponseCode.SERVICE_UNAVAILABLE);
        }
        return globalExceptionHandel.exceptionHandel(exception);
    }

    /**
     * 判断当前请求是否属于 admin 依赖路由。
     *
     * @param request 当前请求
     * @return true 表示为 /agent/admin 或其子路径
     */
    private boolean isAdminRoute(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null
                && (uri.equals("/agent/admin")
                || uri.startsWith(ADMIN_ROUTE_PREFIX)
                || uri.equals("/agent/client")
                || uri.startsWith(CLIENT_ROUTE_PREFIX));
    }
}
