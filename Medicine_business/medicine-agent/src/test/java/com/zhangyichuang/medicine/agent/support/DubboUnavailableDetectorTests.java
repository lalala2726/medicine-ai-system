package com.zhangyichuang.medicine.agent.support;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.StatusRpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DubboUnavailableDetectorTests {

    @Test
    void isUnavailable_ShouldReturnTrue_WhenStatusIsUnavailable() {
        StatusRpcException exception = new StatusRpcException(TriRpcStatus.UNAVAILABLE);

        assertTrue(DubboUnavailableDetector.isUnavailable(exception));
    }

    @Test
    void isUnavailable_ShouldReturnTrue_WhenNoProviderAvailableMessageExists() {
        RpcException exception = new RpcException("No provider available for the service");

        assertTrue(DubboUnavailableDetector.isUnavailable(exception));
    }

    @Test
    void isUnavailable_ShouldReturnTrue_WhenValidInvokersIsZero() {
        RpcException exception = new RpcException("Directory(validInvokers: 0[], invokersToReconnect: 1[localhost:20880])");

        assertTrue(DubboUnavailableDetector.isUnavailable(exception));
    }

    @Test
    void isUnavailable_ShouldReturnFalse_WhenNormalRpcException() {
        RpcException exception = new RpcException(RpcException.NETWORK_EXCEPTION, "network read timeout");

        assertFalse(DubboUnavailableDetector.isUnavailable(exception));
    }
}
