package com.zhangyichuang.medicine.client.rpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAgentHealthRpcServiceImplTests {

    private final ClientAgentHealthRpcServiceImpl rpcService = new ClientAgentHealthRpcServiceImpl();

    @Test
    void ping_ShouldReturnTrue() {
        assertTrue(rpcService.ping());
    }
}
