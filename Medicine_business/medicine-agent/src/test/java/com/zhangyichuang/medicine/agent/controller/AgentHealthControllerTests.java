package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.agent.model.vo.health.RpcHealthVo;
import com.zhangyichuang.medicine.agent.service.RpcHealthService;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentHealthControllerTests {

    private final RpcHealthService rpcHealthService = mock(RpcHealthService.class);
    private final AgentHealthController controller = new AgentHealthController(rpcHealthService);

    @Test
    void rpcHealth_ShouldReturnServiceResult() {
        RpcHealthVo health = createHealth("UP", true, "OK", true, "OK");
        when(rpcHealthService.checkRpcHealth()).thenReturn(health);

        var result = controller.rpcHealth();

        assertEquals(200, result.getCode());
        assertEquals("UP", result.getData().getOverallStatus());
        assertEquals("OK", result.getData().getAdmin().getReason());
        assertEquals("OK", result.getData().getClient().getReason());
    }

    @Test
    void rpcHealth_ShouldBeAnnotatedWithAnonymous() throws NoSuchMethodException {
        var method = AgentHealthController.class.getMethod("rpcHealth");

        assertNotNull(AnnotationUtils.findAnnotation(method, Anonymous.class));
    }

    private RpcHealthVo createHealth(String overallStatus,
                                     boolean adminReachable,
                                     String adminReason,
                                     boolean clientReachable,
                                     String clientReason) {
        RpcHealthVo health = new RpcHealthVo();
        health.setOverallStatus(overallStatus);
        health.setAdmin(createDependency(adminReachable, adminReason));
        health.setClient(createDependency(clientReachable, clientReason));
        return health;
    }

    private RpcHealthVo.DependencyHealthVo createDependency(boolean reachable, String reason) {
        RpcHealthVo.DependencyHealthVo dependency = new RpcHealthVo.DependencyHealthVo();
        dependency.setReachable(reachable);
        dependency.setStatus(reachable ? "UP" : "DOWN");
        dependency.setLatencyMs(12L);
        dependency.setReason(reason);
        return dependency;
    }
}
