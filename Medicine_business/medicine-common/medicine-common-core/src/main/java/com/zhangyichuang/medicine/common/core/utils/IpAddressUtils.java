package com.zhangyichuang.medicine.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * HTTP 请求 IP 提取工具。
 */
public final class IpAddressUtils {

    private static final String[] CANDIDATE_IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpAddressUtils() {
    }

    /**
     * 获取客户端真实 IP 地址。
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP；无法解析时返回空字符串
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        for (String headerName : CANDIDATE_IP_HEADERS) {
            String candidateIp = normalizeForwardedIp(request.getHeader(headerName));
            if (isValidIp(candidateIp)) {
                return candidateIp;
            }
        }
        return normalizeForwardedIp(request.getRemoteAddr());
    }

    private static String normalizeForwardedIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return "";
        }
        if (!ip.contains(",")) {
            return ip.trim();
        }

        String[] ips = ip.split(",");
        for (String singleIp : ips) {
            String candidateIp = singleIp.trim();
            if (isValidIp(candidateIp)) {
                return candidateIp;
            }
        }
        return ips.length == 0 ? "" : ips[0].trim();
    }

    private static boolean isValidIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        String normalizedIp = ip.trim().toLowerCase();
        String[] invalidValues = {"unknown", "null", "undefined", "localhost", "0:0:0:0:0:0:0:1", "::"};
        for (String invalidValue : invalidValues) {
            if (invalidValue.equals(normalizedIp)) {
                return false;
            }
        }
        if (isValidIPv4(normalizedIp)) {
            return !"0.0.0.0".equals(normalizedIp) && !"localhost".equals(normalizedIp);
        }
        return isValidIPv6(normalizedIp);
    }

    private static boolean isValidIPv4(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            return false;
        }
        try {
            for (String octet : octets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
                if (octet.length() > 1 && octet.startsWith("0")) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isValidIPv6(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        return ip.contains(":")
                && ip.length() >= 2
                && ip.length() <= 39
                && !"::".equals(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip);
    }
}
