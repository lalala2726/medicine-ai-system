package com.zhangyichuang.medicine.agent.controller.client;

import com.zhangyichuang.medicine.agent.advice.AgentResponseDescriptionAdvice;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentAfterSaleService;
import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.core.exception.GlobalExceptionHandel;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleTimelineDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentClientAfterSaleControllerTests {

    private final StubClientAgentAfterSaleService afterSaleService = new StubClientAgentAfterSaleService();
    private final AgentClientAfterSaleController controller = new AgentClientAfterSaleController(afterSaleService);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AgentResponseDescriptionAdvice descriptionAdvice =
                new AgentResponseDescriptionAdvice(new AgentVoDescriptionResolver(), objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandel(), descriptionAdvice)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAfterSaleDetail_ShouldUseCurrentUserId() {
        setupAuthentication(66L);
        afterSaleService.detail = createAfterSaleDetail();

        var result = controller.getAfterSaleDetail("AS202511130001");

        assertEquals(200, result.getCode());
        assertEquals("AS202511130001", afterSaleService.capturedAfterSaleNo);
        assertEquals(66L, afterSaleService.capturedUserId);
        assertNotNull(result.getData());
        assertEquals("待审核", result.getData().getAfterSaleStatusName());
        assertEquals(1, result.getData().getTimeline().size());
    }

    @Test
    void getAfterSaleDetail_ShouldAppendMeta() throws Exception {
        setupAuthentication(66L);
        afterSaleService.detail = createAfterSaleDetail();

        mockMvc.perform(get("/agent/client/after-sale/AS202511130001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.afterSaleStatus.value").value("PENDING"))
                .andExpect(jsonPath("$.data.afterSaleStatus.description").value("待审核"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("客户端智能体售后详情"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.afterSaleNo").value("售后单号"));
    }

    @Test
    void getAfterSaleDetail_ShouldRejectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/agent/client/after-sale/AS202511130001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户未登录"));
    }

    @Test
    void checkAfterSaleEligibility_ShouldUseCurrentUserId() {
        setupAuthentication(66L);
        afterSaleService.eligibility = ClientAgentAfterSaleEligibilityDto.builder()
                .orderNo("O202511130001")
                .orderItemId(9L)
                .scope("ITEM")
                .orderStatus("COMPLETED")
                .orderStatusName("已完成")
                .eligible(true)
                .reasonCode("ELIGIBLE")
                .reasonMessage("该商品满足售后条件")
                .refundableAmount(new BigDecimal("29.90"))
                .build();

        ClientAgentAfterSaleEligibilityRequest request = new ClientAgentAfterSaleEligibilityRequest();
        request.setOrderNo("O202511130001");
        request.setOrderItemId(9L);
        var result = controller.checkAfterSaleEligibility(request);

        assertEquals(200, result.getCode());
        assertEquals("O202511130001", afterSaleService.capturedEligibilityRequest.getOrderNo());
        assertEquals(9L, afterSaleService.capturedEligibilityRequest.getOrderItemId());
        assertEquals(66L, afterSaleService.capturedEligibilityUserId);
        assertNotNull(result.getData());
        assertEquals("ELIGIBLE", result.getData().getReasonCode());
    }

    private void setupAuthentication(Long userId) {
        AuthUser authUser = AuthUser.builder().id(userId).username("client_user").build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private AfterSaleDetailDto createAfterSaleDetail() {
        AfterSaleTimelineDto timeline = AfterSaleTimelineDto.builder()
                .id(1L)
                .eventType("REFUND_APPLY")
                .eventTypeName("退款申请")
                .eventStatus("PENDING")
                .operatorType("USER")
                .operatorTypeName("用户")
                .description("用户申请退款")
                .createTime(new Date())
                .build();

        AfterSaleDetailDto.ProductInfo productInfo = AfterSaleDetailDto.ProductInfo.builder()
                .productId(9L)
                .productName("999感冒灵颗粒")
                .productPrice(new BigDecimal("29.90"))
                .quantity(1)
                .totalPrice(new BigDecimal("29.90"))
                .build();

        return AfterSaleDetailDto.builder()
                .id(1L)
                .afterSaleNo("AS202511130001")
                .orderNo("O202511130001")
                .userId(66L)
                .afterSaleType("REFUND_ONLY")
                .afterSaleTypeName("仅退款")
                .afterSaleStatus("PENDING")
                .afterSaleStatusName("待审核")
                .refundAmount(new BigDecimal("29.90"))
                .productInfo(productInfo)
                .timeline(List.of(timeline))
                .build();
    }

    private static class StubClientAgentAfterSaleService implements ClientAgentAfterSaleService {

        private AfterSaleDetailDto detail;
        private ClientAgentAfterSaleEligibilityDto eligibility;
        private String capturedAfterSaleNo;
        private Long capturedUserId;
        private ClientAgentAfterSaleEligibilityRequest capturedEligibilityRequest;
        private Long capturedEligibilityUserId;

        @Override
        public AfterSaleDetailDto getAfterSaleDetail(String afterSaleNo, Long userId) {
            this.capturedAfterSaleNo = afterSaleNo;
            this.capturedUserId = userId;
            return detail;
        }

        @Override
        public ClientAgentAfterSaleEligibilityDto checkAfterSaleEligibility(ClientAgentAfterSaleEligibilityRequest request, Long userId) {
            this.capturedEligibilityRequest = request;
            this.capturedEligibilityUserId = userId;
            return eligibility;
        }
    }
}
