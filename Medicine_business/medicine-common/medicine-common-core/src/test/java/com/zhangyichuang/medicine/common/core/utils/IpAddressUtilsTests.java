package com.zhangyichuang.medicine.common.core.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAddressUtilsTests {

    @Test
    void getIpAddress_ShouldPreferForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.8");
        request.addHeader("Proxy-Client-IP", "198.51.100.11");
        request.setRemoteAddr("192.168.0.10");

        assertEquals("203.0.113.8", IpAddressUtils.getIpAddress(request));
    }

    @Test
    void getIpAddress_ShouldUseFirstValidIpFromForwardedChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, 198.51.100.20, 10.0.0.1");
        request.setRemoteAddr("192.168.0.10");

        assertEquals("198.51.100.20", IpAddressUtils.getIpAddress(request));
    }

    @Test
    void getIpAddress_ShouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.10");

        assertEquals("192.168.0.10", IpAddressUtils.getIpAddress(request));
    }
}
