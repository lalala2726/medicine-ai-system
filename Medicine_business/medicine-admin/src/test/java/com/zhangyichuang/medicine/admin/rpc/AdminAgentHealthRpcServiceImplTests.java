package com.zhangyichuang.medicine.admin.rpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAgentHealthRpcServiceImplTests {

    private final AdminAgentHealthRpcServiceImpl rpcService = new AdminAgentHealthRpcServiceImpl();

    @Test
    void ping_ShouldReturnTrue() {
        assertTrue(rpcService.ping());
    }
}
