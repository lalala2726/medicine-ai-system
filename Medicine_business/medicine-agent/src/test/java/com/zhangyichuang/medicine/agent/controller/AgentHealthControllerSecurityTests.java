package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.agent.advice.AgentResponseDescriptionAdvice;
import com.zhangyichuang.medicine.agent.model.vo.health.RpcHealthVo;
import com.zhangyichuang.medicine.agent.service.RpcHealthService;
import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.security.config.SecurityConfig;
import com.zhangyichuang.medicine.common.security.config.SecurityProperties;
import com.zhangyichuang.medicine.common.security.filter.TokenAuthenticationFilter;
import com.zhangyichuang.medicine.common.security.handel.AccessDeniedHandlerImpl;
import com.zhangyichuang.medicine.common.security.handel.AuthenticationEntryPointImpl;
import com.zhangyichuang.medicine.common.security.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AgentHealthControllerSecurityTests.TestConfig.class)
@WebAppConfiguration
@TestExecutionListeners(
        listeners = {
                ServletTestExecutionListener.class,
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class AgentHealthControllerSecurityTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rpcHealth_ShouldAllowAnonymousAccess_AndReturnStructuredResponse() throws Exception {
        mockMvc.perform(get("/agent/health/rpc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.overallStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.data.admin.reachable").value(false))
                .andExpect(jsonPath("$.data.admin.status").value("DOWN"))
                .andExpect(jsonPath("$.data.admin.reason").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.client.reachable").value(true))
                .andExpect(jsonPath("$.data.client.status").value("UP"))
                .andExpect(jsonPath("$.data.client.reason").value("OK"));
    }

    /**
     * 纯 Spring MVC + Security 测试配置，避免触发 Spring Boot 的 Mockito 监听器。
     */
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            AuthenticationEntryPointImpl.class,
            AccessDeniedHandlerImpl.class
    })
    static class TestConfig {
        /**
         * 构造固定的健康检查结果。
         *
         * @return RPC 健康检查结果
         */
        private static RpcHealthVo createHealth() {
            RpcHealthVo health = new RpcHealthVo();
            health.setOverallStatus("DEGRADED");
            health.setAdmin(createDependency(false, "DOWN", 21L, "UNAVAILABLE"));
            health.setClient(createDependency(true, "UP", 7L, "OK"));
            return health;
        }

        /**
         * 构造单个依赖的健康检查结果。
         *
         * @param reachable 是否可达
         * @param status    状态
         * @param latencyMs 延迟毫秒数
         * @param reason    原因描述
         * @return 单个依赖健康检查结果
         */
        private static RpcHealthVo.DependencyHealthVo createDependency(boolean reachable,
                                                                       String status,
                                                                       long latencyMs,
                                                                       String reason) {
            RpcHealthVo.DependencyHealthVo dependency = new RpcHealthVo.DependencyHealthVo();
            dependency.setReachable(reachable);
            dependency.setStatus(status);
            dependency.setLatencyMs(latencyMs);
            dependency.setReason(reason);
            return dependency;
        }

        /**
         * 注册健康检查控制器。
         *
         * @param rpcHealthService RPC 健康检查服务
         * @return 控制器实例
         */
        @Bean
        public AgentHealthController agentHealthController(RpcHealthService rpcHealthService) {
            return new AgentHealthController(rpcHealthService);
        }

        /**
         * 提供固定的 RPC 健康检查结果。
         *
         * @return RPC 健康检查服务
         */
        @Bean
        public RpcHealthService rpcHealthService() {
            return TestConfig::createHealth;
        }

        /**
         * 注册 Agent VO 描述解析器。
         *
         * @return Agent VO 描述解析器
         */
        @Bean
        public AgentVoDescriptionResolver agentVoDescriptionResolver() {
            return new AgentVoDescriptionResolver();
        }

        /**
         * 注册 JSON 序列化器。
         *
         * @return ObjectMapper 实例
         */
        @Bean
        public ObjectMapper objectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }

        /**
         * 注册响应描述增强 Advice。
         *
         * @param descriptionResolver 描述解析器
         * @param objectMapper        JSON 序列化器
         * @return 响应描述增强 Advice
         */
        @Bean
        public AgentResponseDescriptionAdvice agentResponseDescriptionAdvice(AgentVoDescriptionResolver descriptionResolver,
                                                                             ObjectMapper objectMapper) {
            return new AgentResponseDescriptionAdvice(descriptionResolver, objectMapper);
        }

        /**
         * 提供安全配置属性。
         *
         * @return 安全配置属性
         */
        @Bean
        public SecurityProperties securityProperties() {
            SecurityProperties securityProperties = new SecurityProperties();
            securityProperties.setHeader("Authorization");
            return securityProperties;
        }

        /**
         * 提供不会解析真实 Token 的测试服务。
         *
         * @return TokenService 测试实现
         */
        @Bean
        public TokenService tokenService() {
            return new NoOpTokenService();
        }

        /**
         * 注册 Token 认证过滤器，让安全链与正式配置保持一致。
         *
         * @param securityProperties 安全配置属性
         * @param tokenService       Token 服务
         * @return Token 认证过滤器
         */
        @Bean
        public TokenAuthenticationFilter tokenAuthenticationFilter(SecurityProperties securityProperties,
                                                                   TokenService tokenService) {
            return new TokenAuthenticationFilter(securityProperties, tokenService);
        }
    }

    /**
     * 不执行真实 Token 解析的测试实现。
     */
    private static final class NoOpTokenService extends TokenService {
        /**
         * 创建测试专用 TokenService。
         */
        private NoOpTokenService() {
            super(null, null, null);
        }

        /**
         * 测试场景下无需解析 Token，直接返回空认证信息。
         *
         * @param accessToken 访问令牌
         * @return 固定返回 null
         */
        @Override
        public Authentication parseAccessToken(String accessToken) {
            return null;
        }
    }
}
