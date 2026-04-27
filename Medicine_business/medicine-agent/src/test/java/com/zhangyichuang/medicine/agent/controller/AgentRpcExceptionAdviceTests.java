package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.agent.exception.AgentRpcExceptionAdvice;
import com.zhangyichuang.medicine.common.core.exception.GlobalExceptionHandel;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentRpcExceptionAdviceTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminRpcController(), new ClientRpcController(), new NonAdminRpcController())
                .setControllerAdvice(new AgentRpcExceptionAdvice(new GlobalExceptionHandel()))
                .build();
    }

    @Test
    void adminRoute_ShouldReturn503_WhenRpcUnavailable() throws Exception {
        mockMvc.perform(get("/agent/admin/mock/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("业务模块未就绪，请稍后再试"));
    }

    @Test
    void adminRoute_ShouldKeepOriginalSemantics_WhenRpcIsNotUnavailable() throws Exception {
        mockMvc.perform(get("/agent/admin/mock/other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("rpc invoke failed"));
    }

    @Test
    void clientRoute_ShouldReturn503_WhenRpcUnavailable() throws Exception {
        mockMvc.perform(get("/agent/client/mock/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("业务模块未就绪，请稍后再试"));
    }

    @Test
    void nonAdminRoute_ShouldNotUseAdminFallback_WhenRpcUnavailable() throws Exception {
        mockMvc.perform(get("/agent/authorization/mock/unavailable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("No provider available")));
    }

    @RestController
    @RequestMapping("/agent/admin/mock")
    static class AdminRpcController {

        @GetMapping("/unavailable")
        public String unavailable() {
            throw new RpcException("No provider available for the service");
        }

        @GetMapping("/other")
        public String other() {
            throw new RpcException("rpc invoke failed");
        }
    }

    @RestController
    @RequestMapping("/agent/client/mock")
    static class ClientRpcController {

        @GetMapping("/unavailable")
        public String unavailable() {
            throw new RpcException("No provider available for the service");
        }
    }

    @RestController
    @RequestMapping("/agent/authorization/mock")
    static class NonAdminRpcController {

        @GetMapping("/unavailable")
        public String unavailable() {
            throw new RpcException("No provider available for the service");
        }
    }
}
