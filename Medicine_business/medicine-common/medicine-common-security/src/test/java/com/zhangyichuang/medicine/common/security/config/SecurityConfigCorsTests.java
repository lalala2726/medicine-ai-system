package com.zhangyichuang.medicine.common.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig 跨域配置测试。
 */
class SecurityConfigCorsTests {

    /**
     * 验证 zhangyichuang.com 主域名与子域名都被允许跨域访问。
     */
    @Test
    void corsConfigurationSource_ShouldAllowZhangyichuangComAndSubdomains() {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null);

        CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource().getCorsConfiguration(createRequest());

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.checkOrigin("https://zhangyichuang.com")).isEqualTo("https://zhangyichuang.com");
        assertThat(corsConfiguration.checkOrigin("http://zhangyichuang.com")).isEqualTo("http://zhangyichuang.com");
        assertThat(corsConfiguration.checkOrigin("https://medicine-client.zhangyichuang.com")).isEqualTo("https://medicine-client.zhangyichuang.com");
        assertThat(corsConfiguration.checkOrigin("http://admin.zhangyichuang.com")).isEqualTo("http://admin.zhangyichuang.com");
        assertThat(corsConfiguration.checkOrigin("https://example.com")).isNull();
    }

    /**
     * 创建用于读取全局 CORS 配置的模拟请求。
     *
     * @return 根路径模拟请求
     */
    private MockHttpServletRequest createRequest() {
        return new MockHttpServletRequest("GET", "/");
    }
}
