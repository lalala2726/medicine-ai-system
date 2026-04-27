package com.zhangyichuang.medicine.agent.service.impl;

import com.zhangyichuang.medicine.agent.model.vo.health.RpcHealthVo;
import com.zhangyichuang.medicine.agent.service.RpcHealthService;
import com.zhangyichuang.medicine.agent.support.DubboUnavailableDetector;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentHealthRpcService;
import com.zhangyichuang.medicine.rpc.client.ClientAgentHealthRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Agent RPC 健康检查服务实现。
 */
@Service
public class RpcHealthServiceImpl implements RpcHealthService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String REASON_OK = "OK";
    private static final String REASON_ERROR = "ERROR";
    private static final String REASON_TIMEOUT = "TIMEOUT";
    private static final String REASON_UNAVAILABLE = "UNAVAILABLE";

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 3000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentHealthRpcService adminAgentHealthRpcService;

    @DubboReference(group = "medicine-client", version = "1.0.0", check = false, timeout = 3000, retries = 0,
            url = "${dubbo.references.medicine-client.url:}")
    private ClientAgentHealthRpcService clientAgentHealthRpcService;

    @Override
    public RpcHealthVo checkRpcHealth() {
        RpcHealthVo.DependencyHealthVo admin = probe(adminAgentHealthRpcService::ping);
        RpcHealthVo.DependencyHealthVo client = probe(clientAgentHealthRpcService::ping);

        RpcHealthVo result = new RpcHealthVo();
        result.setAdmin(admin);
        result.setClient(client);
        result.setOverallStatus(resolveOverallStatus(admin.isReachable(), client.isReachable()));
        return result;
    }

    private RpcHealthVo.DependencyHealthVo probe(BooleanSupplier ping) {
        long start = System.nanoTime();
        RpcHealthVo.DependencyHealthVo result = new RpcHealthVo.DependencyHealthVo();
        try {
            boolean reachable = ping.getAsBoolean();
            result.setReachable(reachable);
            result.setStatus(reachable ? STATUS_UP : STATUS_DOWN);
            result.setReason(reachable ? REASON_OK : REASON_ERROR);
        } catch (Exception exception) {
            result.setReachable(false);
            result.setStatus(STATUS_DOWN);
            result.setReason(resolveReason(exception));
        }
        result.setLatencyMs((System.nanoTime() - start) / 1_000_000);
        return result;
    }

    private String resolveOverallStatus(boolean adminReachable, boolean clientReachable) {
        if (adminReachable && clientReachable) {
            return STATUS_UP;
        }
        if (!adminReachable && !clientReachable) {
            return STATUS_DOWN;
        }
        return STATUS_DEGRADED;
    }

    private String resolveReason(Throwable exception) {
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (current instanceof RpcException rpcException && rpcException.isTimeout()) {
                return REASON_TIMEOUT;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
                return REASON_TIMEOUT;
            }
            current = current.getCause();
        }
        if (DubboUnavailableDetector.isUnavailable(exception)) {
            return REASON_UNAVAILABLE;
        }
        return REASON_ERROR;
    }
}
