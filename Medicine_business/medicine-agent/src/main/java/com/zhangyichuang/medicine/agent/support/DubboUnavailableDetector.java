package com.zhangyichuang.medicine.agent.support;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Locale;

/**
 * Dubbo 不可用异常判定器。
 * <p>
 * 将“服务未就绪/不可用”判断集中处理，避免业务层散落字符串判断。
 */
public final class DubboUnavailableDetector {

    private DubboUnavailableDetector() {
    }

    /**
     * 判断异常链中是否存在 Dubbo 下游服务不可用信号。
     * <p>
     * 会沿 cause 链向下检查，避免包装异常导致漏判。
     *
     * @param exception 待判定异常
     * @return true 表示命中“服务未就绪/不可用”特征
     */
    public static boolean isUnavailable(Throwable exception) {
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (isUnavailableCause(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 判定单个异常对象是否属于 Dubbo 不可用场景。
     * <p>
     * 判定条件包含状态码特征和常见错误消息特征。
     *
     * @param throwable 当前异常
     * @return true 表示不可用
     */
    private static boolean isUnavailableCause(Throwable throwable) {
        if (throwable instanceof StatusRpcException statusRpcException) {
            TriRpcStatus status = statusRpcException.getStatus();
            if (status != null && status.code == TriRpcStatus.Code.UNAVAILABLE) {
                return true;
            }
        }

        if (throwable instanceof RpcException rpcException && rpcException.isNoInvokerAvailableAfterFilter()) {
            return true;
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("no provider available")
                || normalized.contains("validinvokers: 0")
                || (normalized.contains("upstream") && normalized.contains("is unavailable"));
    }
}
