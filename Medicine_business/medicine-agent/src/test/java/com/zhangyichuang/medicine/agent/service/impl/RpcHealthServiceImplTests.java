package com.zhangyichuang.medicine.agent.service.impl;

import com.zhangyichuang.medicine.rpc.admin.AdminAgentHealthRpcService;
import com.zhangyichuang.medicine.rpc.client.ClientAgentHealthRpcService;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RpcHealthServiceImplTests {

    @Mock
    private AdminAgentHealthRpcService adminAgentHealthRpcService;

    @Mock
    private ClientAgentHealthRpcService clientAgentHealthRpcService;

    @InjectMocks
    private RpcHealthServiceImpl rpcHealthService;

    @Test
    void checkRpcHealth_ShouldReturnUp_WhenAdminAndClientAreReachable() {
        when(adminAgentHealthRpcService.ping()).thenReturn(true);
        when(clientAgentHealthRpcService.ping()).thenReturn(true);

        var result = rpcHealthService.checkRpcHealth();

        assertEquals("UP", result.getOverallStatus());
        assertTrue(result.getAdmin().isReachable());
        assertEquals("UP", result.getAdmin().getStatus());
        assertEquals("OK", result.getAdmin().getReason());
        assertTrue(result.getClient().isReachable());
        assertEquals("UP", result.getClient().getStatus());
        assertEquals("OK", result.getClient().getReason());
    }

    @Test
    void checkRpcHealth_ShouldReturnDegraded_WhenAdminUnavailableAndClientStillReachable() {
        when(adminAgentHealthRpcService.ping()).thenThrow(new RpcException("No provider available for the service"));
        when(clientAgentHealthRpcService.ping()).thenReturn(true);

        var result = rpcHealthService.checkRpcHealth();

        assertEquals("DEGRADED", result.getOverallStatus());
        assertFalse(result.getAdmin().isReachable());
        assertEquals("DOWN", result.getAdmin().getStatus());
        assertEquals("UNAVAILABLE", result.getAdmin().getReason());
        assertTrue(result.getClient().isReachable());
        assertEquals("UP", result.getClient().getStatus());
        verify(clientAgentHealthRpcService, times(1)).ping();
    }

    @Test
    void checkRpcHealth_ShouldReturnDown_WhenAdminAndClientAreBothUnavailable() {
        when(adminAgentHealthRpcService.ping()).thenThrow(new RpcException("No provider available for the service"));
        when(clientAgentHealthRpcService.ping()).thenThrow(new RpcException("upstream request timeout"));

        var result = rpcHealthService.checkRpcHealth();

        assertEquals("DOWN", result.getOverallStatus());
        assertFalse(result.getAdmin().isReachable());
        assertEquals("UNAVAILABLE", result.getAdmin().getReason());
        assertFalse(result.getClient().isReachable());
        assertEquals("TIMEOUT", result.getClient().getReason());
    }

    @Test
    void checkRpcHealth_ShouldClassifyTimeout_WhenRpcThrowsTimeoutException() {
        when(adminAgentHealthRpcService.ping()).thenThrow(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "rpc timeout"));
        when(clientAgentHealthRpcService.ping()).thenReturn(true);

        var result = rpcHealthService.checkRpcHealth();

        assertEquals("TIMEOUT", result.getAdmin().getReason());
        assertEquals("DEGRADED", result.getOverallStatus());
    }

    @Test
    void checkRpcHealth_ShouldReturnError_WhenExceptionIsNotDubboSpecific() {
        when(adminAgentHealthRpcService.ping()).thenThrow(new IllegalStateException("unexpected"));
        when(clientAgentHealthRpcService.ping()).thenReturn(true);

        var result = rpcHealthService.checkRpcHealth();

        assertEquals("ERROR", result.getAdmin().getReason());
        assertEquals("DEGRADED", result.getOverallStatus());
    }
}
