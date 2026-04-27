package com.zhangyichuang.medicine.agent.service.client.impl;

import com.zhangyichuang.medicine.model.dto.ClientAgentOrderCancelCheckDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentOrderCardSummaryDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentOrderShippingDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentOrderTimelineDto;
import com.zhangyichuang.medicine.rpc.client.ClientAgentOrderRpcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAgentOrderServiceImplTests {

    @Mock
    private ClientAgentOrderRpcService clientAgentOrderRpcService;

    @InjectMocks
    private ClientAgentOrderServiceImpl service;

    @Test
    void getOrderCardSummary_ShouldDelegateToRpc() {
        ClientAgentOrderCardSummaryDto summaryDto = ClientAgentOrderCardSummaryDto.builder().orderNo("O1").build();
        when(clientAgentOrderRpcService.getOrderCardSummary("O1", 1L)).thenReturn(summaryDto);

        assertSame(summaryDto, service.getOrderCardSummary("O1", 1L));
    }

    @Test
    void getOrderDetail_WhenRpcReturnsNull_ShouldReturnNull() {
        when(clientAgentOrderRpcService.getOrderDetail("O1", 1L)).thenReturn(null);

        assertNull(service.getOrderDetail("O1", 1L));
    }

    @Test
    void getOrderShipping_ShouldDelegateToRpc() {
        ClientAgentOrderShippingDto shippingDto = ClientAgentOrderShippingDto.builder().orderNo("O1").build();
        when(clientAgentOrderRpcService.getOrderShipping("O1", 1L)).thenReturn(shippingDto);

        assertSame(shippingDto, service.getOrderShipping("O1", 1L));
    }

    @Test
    void getOrderTimeline_ShouldDelegateToRpc() {
        ClientAgentOrderTimelineDto timelineDto = ClientAgentOrderTimelineDto.builder().orderNo("O1").build();
        when(clientAgentOrderRpcService.getOrderTimeline("O1", 1L)).thenReturn(timelineDto);

        assertSame(timelineDto, service.getOrderTimeline("O1", 1L));
    }

    @Test
    void checkOrderCancelable_ShouldDelegateToRpc() {
        ClientAgentOrderCancelCheckDto result = ClientAgentOrderCancelCheckDto.builder().orderNo("O1").build();
        when(clientAgentOrderRpcService.checkOrderCancelable("O1", 1L)).thenReturn(result);

        assertSame(result, service.checkOrderCancelable("O1", 1L));
    }
}
