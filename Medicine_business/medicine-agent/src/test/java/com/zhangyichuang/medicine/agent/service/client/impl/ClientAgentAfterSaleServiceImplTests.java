package com.zhangyichuang.medicine.agent.service.client.impl;

import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import com.zhangyichuang.medicine.rpc.client.ClientAgentAfterSaleRpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAgentAfterSaleServiceImplTests {

    @Mock
    private ClientAgentAfterSaleRpcService clientAgentAfterSaleRpcService;

    @InjectMocks
    private ClientAgentAfterSaleServiceImpl service;

    @Test
    void getAfterSaleDetail_WhenRpcReturnsNull_ShouldReturnNull() {
        when(clientAgentAfterSaleRpcService.getAfterSaleDetail("AS1", 1L)).thenReturn(null);

        assertNull(service.getAfterSaleDetail("AS1", 1L));
    }

    @Test
    void checkAfterSaleEligibility_ShouldDelegateToRpc() {
        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        ClientAgentAfterSaleEligibilityDto result = ClientAgentAfterSaleEligibilityDto.builder().orderNo("O1").build();
        when(clientAgentAfterSaleRpcService.checkAfterSaleEligibility(request, 1L)).thenReturn(result);

        assertSame(result, service.checkAfterSaleEligibility(request, 1L));
    }
}
