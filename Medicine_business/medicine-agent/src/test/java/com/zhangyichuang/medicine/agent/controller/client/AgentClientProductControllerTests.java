package com.zhangyichuang.medicine.agent.controller.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.advice.AgentResponseDescriptionAdvice;
import com.zhangyichuang.medicine.agent.model.request.ClientAgentProductDetailsRequest;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchTagFilterOptionVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchTagFilterVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchVo;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentProductService;
import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.core.exception.GlobalExceptionHandel;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.entity.SysUserDetails;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentClientProductControllerTests {

    private final StubClientAgentProductService productService = new StubClientAgentProductService();
    private final AgentClientProductController controller = new AgentClientProductController(productService);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AgentResponseDescriptionAdvice descriptionAdvice =
                new AgentResponseDescriptionAdvice(new AgentVoDescriptionResolver(), objectMapper);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandel(), descriptionAdvice)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchProducts_ShouldDelegateToService() {
        setupAuthentication(99L);
        ClientAgentProductSearchRequest request = new ClientAgentProductSearchRequest();
        request.setKeyword("  感冒灵  ");
        request.setCategoryName(" 感冒药 ");
        request.setUsage(" 缓解头痛 ");
        request.setPageNum(0);
        request.setPageSize(0);
        productService.searchPage = createSearchPage();

        var result = controller.searchProducts(request);

        assertEquals(200, result.getCode());
        assertTrue(productService.searchInvoked);
        assertEquals("感冒灵", productService.capturedRequest.getKeyword());
        assertEquals("感冒药", productService.capturedRequest.getCategoryName());
        assertEquals("缓解头痛", productService.capturedRequest.getUsage());
        assertEquals(1, productService.capturedRequest.getPageNum());
        assertEquals(1, productService.capturedRequest.getPageSize());
        assertEquals("  感冒灵  ", request.getKeyword());
        assertEquals(" 感冒药 ", request.getCategoryName());
        assertEquals(" 缓解头痛 ", request.getUsage());
        assertEquals(0, request.getPageNum());
        assertEquals(0, request.getPageSize());
        assertNotSame(request, productService.capturedRequest);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRows().size());
        assertInstanceOf(ClientAgentProductSearchVo.class, result.getData().getRows().getFirst());
    }

    @Test
    void getProductDetails_ShouldMapDrugDetail() {
        setupAuthentication(99L);
        productService.productDetails = List.of(createProductDetail());
        ClientAgentProductDetailsRequest request = new ClientAgentProductDetailsRequest();
        request.setProductIds(List.of(1L));

        var result = controller.getProductDetails(request);

        assertEquals(200, result.getCode());
        assertTrue(productService.detailsInvoked);
        assertEquals(List.of(1L), productService.capturedDetailProductIds);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("999感冒灵颗粒", result.getData().getFirst().getProductName());
        assertEquals("缓解普通感冒症状", result.getData().getFirst().getEfficacy());
        assertEquals(List.of("感冒药"), result.getData().getFirst().getCategoryNames());
    }

    @Test
    void getProductDetails_ShouldExposeFlatFields() {
        setupAuthentication(99L);
        productService.productDetails = List.of(createProductDetail());
        ClientAgentProductDetailsRequest request = new ClientAgentProductDetailsRequest();
        request.setProductIds(List.of(1L));

        var result = controller.getProductDetails(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("10g*9袋", result.getData().getFirst().getPackaging());
        assertEquals("三叉苦", result.getData().getFirst().getComposition());
    }

    @Test
    void searchProducts_ShouldAppendMeta() throws Exception {
        setupAuthentication(99L);
        productService.searchPage = createSearchPage();

        mockMvc.perform(get("/agent/client/product/search").param("keyword", "感冒灵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].productName").value("999感冒灵颗粒"))
                .andExpect(jsonPath("$.data.meta.entityDescription").value("客户端智能体商品搜索结果"))
                .andExpect(jsonPath("$.data.meta.fieldDescriptions.productName").value("商品名称"));
    }

    @Test
    void searchProducts_ShouldRejectBlankKeyword() throws Exception {
        setupAuthentication(99L);

        mockMvc.perform(get("/agent/client/product/search").param("keyword", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("搜索关键词、分类名称、用途、标签不能同时为空"));
    }

    @Test
    void searchProducts_ShouldAllowCategoryNameSearch() {
        setupAuthentication(99L);
        ClientAgentProductSearchRequest request = new ClientAgentProductSearchRequest();
        request.setCategoryName(" 感冒药 ");
        request.setPageNum(1);
        request.setPageSize(10);
        productService.searchPage = createSearchPage();

        var result = controller.searchProducts(request);

        assertEquals(200, result.getCode());
        assertEquals("感冒药", productService.capturedRequest.getCategoryName());
    }

    @Test
    void searchProducts_ShouldRejectPageSizeAboveLimit() throws Exception {
        setupAuthentication(99L);

        mockMvc.perform(get("/agent/client/product/search")
                        .param("keyword", "感冒灵")
                        .param("pageSize", "21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("每页数量不能超过20"));
    }

    @Test
    void listProductSearchTagFilters_ShouldReturnMappedFilters() {
        setupAuthentication(99L);
        productService.tagFilterResult = createTagFilterGroups();

        var result = controller.listProductSearchTagFilters();

        assertEquals(200, result.getCode());
        assertTrue(productService.tagFiltersInvoked);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        ClientAgentProductSearchTagFilterVo group = result.getData().getFirst();
        assertEquals(10L, group.getTypeId());
        assertEquals("EFFICACY", group.getTypeCode());
        assertEquals("功效", group.getTypeName());
        assertNotNull(group.getOptions());
        assertEquals(2, group.getOptions().size());
        ClientAgentProductSearchTagFilterOptionVo firstOption = group.getOptions().getFirst();
        assertEquals(1L, firstOption.getTagId());
        assertEquals("退烧", firstOption.getTagName());
        assertEquals(12L, firstOption.getCount());
    }

    @Test
    void listProductSearchTagFilters_ShouldReturnEmptyWhenNoFilters() {
        setupAuthentication(99L);
        productService.tagFilterResult = List.of();

        var result = controller.listProductSearchTagFilters();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void getProductDetails_ShouldMapTagNames() {
        setupAuthentication(99L);
        ClientAgentProductDetailDto detail = createProductDetail();
        detail.setTagNames(List.of("退烧"));
        productService.productDetails = List.of(detail);
        ClientAgentProductDetailsRequest request = new ClientAgentProductDetailsRequest();
        request.setProductIds(List.of(1L));

        var result = controller.getProductDetails(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().getFirst().getTagNames());
        assertEquals(1, result.getData().getFirst().getTagNames().size());
        assertEquals("退烧", result.getData().getFirst().getTagNames().getFirst());
    }

    @Test
    void getProductDetails_ShouldHandleNullTagNames() {
        setupAuthentication(99L);
        ClientAgentProductDetailDto detail = createProductDetail();
        detail.setTagNames(null);
        productService.productDetails = List.of(detail);
        ClientAgentProductDetailsRequest request = new ClientAgentProductDetailsRequest();
        request.setProductIds(List.of(1L));

        var result = controller.getProductDetails(request);

        assertEquals(200, result.getCode());
        assertNull(result.getData().getFirst().getTagNames());
    }

    @Test
    void searchProducts_ShouldAllowAnonymousAtControllerLayer() throws Exception {
        productService.searchPage = createSearchPage();

        mockMvc.perform(get("/agent/client/product/search").param("keyword", "感冒灵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.rows[0].productName").value("999感冒灵颗粒"));
    }

    private void setupAuthentication(Long userId) {
        AuthUser authUser = AuthUser.builder().id(userId).username("client_user").build();
        SysUserDetails userDetails = new SysUserDetails(authUser);
        userDetails.setAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    private Page<ClientAgentProductSearchDto> createSearchPage() {
        Page<ClientAgentProductSearchDto> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(ClientAgentProductSearchDto.builder()
                .productId(1L)
                .productName("999感冒灵颗粒")
                .commonName("感冒灵颗粒")
                .price(new BigDecimal("29.90"))
                .build()));
        return page;
    }

    private List<ClientAgentProductSearchTagFilterDto> createTagFilterGroups() {
        ClientAgentProductSearchTagFilterOptionDto option1 = ClientAgentProductSearchTagFilterOptionDto.builder()
                .tagId(1L)
                .tagName("退烧")
                .count(12L)
                .build();
        ClientAgentProductSearchTagFilterOptionDto option2 = ClientAgentProductSearchTagFilterOptionDto.builder()
                .tagId(2L)
                .tagName("止咳")
                .count(8L)
                .build();
        ClientAgentProductSearchTagFilterDto group = ClientAgentProductSearchTagFilterDto.builder()
                .typeId(10L)
                .typeCode("EFFICACY")
                .typeName("功效")
                .options(List.of(option1, option2))
                .build();
        return List.of(group);
    }

    private ClientAgentProductDetailDto createProductDetail() {
        return ClientAgentProductDetailDto.builder()
                .productId(1L)
                .productName("999感冒灵颗粒")
                .categoryNames(List.of("感冒药"))
                .unit("盒")
                .price(new BigDecimal("29.90"))
                .stock(100)
                .status(1)
                .deliveryType(2)
                .sales(99)
                .commonName("复方感冒灵颗粒")
                .efficacy("缓解普通感冒症状")
                .usageMethod("开水冲服")
                .precautions("详见说明书")
                .composition("三叉苦")
                .packaging("10g*9袋")
                .instruction("完整说明书")
                .build();
    }

    private static class StubClientAgentProductService implements ClientAgentProductService {

        private Page<ClientAgentProductSearchDto> searchPage = new Page<>();
        private List<ClientAgentProductSearchTagFilterDto> tagFilterResult = List.of();
        private List<ClientAgentProductDetailDto> productDetails = List.of();
        private boolean searchInvoked;
        private boolean tagFiltersInvoked;
        private boolean detailsInvoked;
        private ClientAgentProductSearchRequest capturedRequest;
        private List<Long> capturedDetailProductIds;

        @Override
        public Page<ClientAgentProductSearchDto> searchProducts(ClientAgentProductSearchRequest request) {
            this.searchInvoked = true;
            this.capturedRequest = request;
            return searchPage;
        }

        @Override
        public List<ClientAgentProductSearchTagFilterDto> listProductSearchTagFilters() {
            this.tagFiltersInvoked = true;
            return tagFilterResult;
        }

        @Override
        public List<ClientAgentProductDetailDto> getProductDetails(List<Long> productIds) {
            this.detailsInvoked = true;
            this.capturedDetailProductIds = productIds;
            return productDetails;
        }

        @Override
        public ClientAgentProductCardsDto getProductCards(List<Long> productIds) {
            throw new UnsupportedOperationException("not needed in this test");
        }

        @Override
        public ClientAgentProductPurchaseCardsDto getProductPurchaseCards(List<ClientAgentProductPurchaseQueryDto> items) {
            throw new UnsupportedOperationException("not needed in this test");
        }
    }
}
