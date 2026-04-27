package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentDrugDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductListVo;
import com.zhangyichuang.medicine.agent.service.MallProductService;
import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管理端智能体商品工具控制器单元测试类。
 * <p>
 * 测试目标：验证 {@link AgentProductController} 的商品查询功能是否正确委托给 Service 层
 * 测试接口：
 * <ul>
 *     <li>GET /agent/admin/product/search - Elasticsearch 商品搜索接口</li>
 *     <li>GET /agent/admin/product/list - 获取商品列表</li>
 *     <li>GET /agent/admin/product/{productIds} - 获取商品详情</li>
 *     <li>GET /agent/admin/product/drug/{productIds} - 获取药品详情</li>
 * </ul>
 *
 * @author Chuang
 */
class AgentProductControllerTests {

    private final StubMallProductService productService = new StubMallProductService();
    private final AgentProductController controller = new AgentProductController(productService);

    /**
     * 测试商品搜索是否正确委托给 Elasticsearch 搜索服务。
     * <p>
     * 测试目的：验证 Controller 调用 Service 层的 searchProducts 方法，
     * 并将查询参数完整传递给搜索链路。
     * 测试接口：GET /agent/admin/product/search
     * 预期结果：返回状态码 200，Service 接收到正确的请求参数
     */
    @Test
    void searchProduct_ShouldDelegateToSearchService() {
        MallProductListQueryRequest request = new MallProductListQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setName("维生素");
        productService.searchPage = createSampleProductPage();

        var result = controller.searchProduct(request);

        assertEquals(200, result.getCode());
        assertTrue(productService.searchProductsInvoked);
        assertEquals(request, productService.capturedSearchRequest);
    }

    /**
     * 测试商品搜索结果也会正确设置封面图片。
     * <p>
     * 测试目的：验证 Elasticsearch 搜索接口返回的商品列表，
     * 同样会从图片列表中提取第一张图片作为封面图片。
     * 测试接口：GET /agent/admin/product/search
     * 预期结果：返回的商品列表中，coverImage 字段包含第一张图片的 URL
     */
    @Test
    void searchProduct_ShouldSetCoverImage() {
        MallProductListQueryRequest request = new MallProductListQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        productService.searchPage = createProductPageWithImages();

        var result = controller.searchProduct(request);

        assertEquals(200, result.getCode());
        var data = result.getData();
        assertNotNull(data);
        assertNotNull(data.getRows());
        assertEquals(1, data.getRows().size());

        @SuppressWarnings("unchecked")
        var rows = (List<?>) data.getRows();
        var firstRow = rows.getFirst();
        assertInstanceOf(AgentProductListVo.class, firstRow);
        AgentProductListVo vo = (AgentProductListVo) firstRow;
        assertEquals("https://example.com/image1.jpg", vo.getCoverImage());
    }


    /**
     * 测试商品列表查询是否正确委托给 Service。
     * <p>
     * 测试目的：验证 Controller 正确调用 Service 层的 listProducts 方法，
     * 并将查询参数传递给 Service。
     * 测试接口：GET /agent/admin/product/list
     * 预期结果：返回状态码 200，Service 接收到正确的请求参数
     */
    @Test
    void searchProducts_ShouldDelegateToService() {
        MallProductListQueryRequest request = new MallProductListQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setName("维生素");
        productService.productPage = createSampleProductPage();

        var result = controller.searchProducts(request);

        assertEquals(200, result.getCode());
        assertTrue(productService.listProductsInvoked);
        assertEquals(request, productService.capturedRequest);
    }

    /**
     * 测试封面图片设置逻辑。
     * <p>
     * 测试目的：验证 Controller 能够正确从商品图片列表中提取第一张图片作为封面图片，
     * 并设置到返回的 VO 对象中。
     * 测试接口：GET /agent/admin/product/list
     * 预期结果：返回的商品列表中，coverImage 字段包含第一张图片的 URL
     */
    @Test
    void searchProducts_ShouldSetCoverImage() {
        MallProductListQueryRequest request = new MallProductListQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        productService.productPage = createProductPageWithImages();

        var result = controller.searchProducts(request);

        assertEquals(200, result.getCode());
        var data = result.getData();
        assertNotNull(data);
        assertNotNull(data.getRows());
        assertEquals(1, data.getRows().size());

        @SuppressWarnings("unchecked")
        var rows = (List<?>) data.getRows();
        var firstRow = rows.getFirst();
        assertInstanceOf(AgentProductListVo.class, firstRow);
        AgentProductListVo vo = (AgentProductListVo) firstRow;
        assertEquals("https://example.com/image1.jpg", vo.getCoverImage());
    }

    /**
     * 测试商品详情查询是否正确委托给 Service。
     * <p>
     * 测试目的：验证 Controller 正确调用 Service 层的 getProductDetail 方法，
     * 并将商品 ID 列表传递给 Service。
     * 测试接口：GET /agent/admin/product/{productIds}
     * 预期结果：返回状态码 200，包含商品详情列表
     */
    @Test
    void getProductDetail_ShouldDelegateToService() {
        List<Long> productIds = List.of(1L, 2L);
        productService.productDetails = createSampleProductDetails();

        var result = controller.getProductDetail(productIds);

        assertEquals(200, result.getCode());
        assertTrue(productService.getProductDetailInvoked);
        assertEquals(productIds, productService.capturedProductIds);
        assertEquals(2, result.getData().size());
    }

    /**
     * 测试药品详情查询是否正确委托给 Service。
     * <p>
     * 测试目的：验证 Controller 正确调用 Service 层的 getDrugDetail 方法，
     * 并将商品 ID 列表传递给 Service。
     * 测试接口：GET /agent/admin/product/drug/{productIds}
     * 预期结果：返回状态码 200，包含药品详情列表
     */
    @Test
    void getDrugDetail_ShouldDelegateToService() {
        List<Long> productIds = List.of(1L);
        productService.drugDetails = createSampleDrugDetails();

        var result = controller.getDrugDetail(productIds);

        assertEquals(200, result.getCode());
        assertTrue(productService.getDrugDetailInvoked);
        assertEquals(productIds, productService.capturedDrugProductIds);
        assertEquals(1, result.getData().size());
        assertEquals("维生素C片", result.getData().getFirst().getProductName());
    }

    // ==================== Helper Methods ====================

    private Page<MallProductDetailDto> createSampleProductPage() {
        Page<MallProductDetailDto> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(createSampleProductDto()));
        return page;
    }

    private Page<MallProductDetailDto> createProductPageWithImages() {
        Page<MallProductDetailDto> page = new Page<>(1, 10);
        page.setTotal(1);

        MallProductDetailDto dto = new MallProductDetailDto();
        dto.setId(1L);
        dto.setName("维生素C片");
        dto.setCategoryId(1L);
        dto.setCategoryNames(List.of("保健品"));
        dto.setPrice(new BigDecimal("29.90"));
        dto.setStock(100);
        dto.setStatus(1);
        dto.setCreateTime(new Date());
        dto.setImages(List.of(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
        ));

        page.setRecords(List.of(dto));
        return page;
    }

    private MallProductDetailDto createSampleProductDto() {
        MallProductDetailDto dto = new MallProductDetailDto();
        dto.setId(1L);
        dto.setName("维生素C片");
        dto.setCategoryId(1L);
        dto.setCategoryNames(List.of("保健品"));
        dto.setPrice(new BigDecimal("29.90"));
        dto.setStock(100);
        dto.setStatus(1);
        dto.setCreateTime(new Date());
        return dto;
    }

    private List<AgentProductDetailVo> createSampleProductDetails() {
        AgentProductDetailVo detail1 = new AgentProductDetailVo();
        detail1.setId(1L);
        detail1.setName("维生素C片");
        detail1.setCategoryName("保健品");
        detail1.setPrice(new BigDecimal("29.90"));

        AgentProductDetailVo detail2 = new AgentProductDetailVo();
        detail2.setId(2L);
        detail2.setName("维生素B片");
        detail2.setCategoryName("保健品");
        detail2.setPrice(new BigDecimal("19.90"));

        return List.of(detail1, detail2);
    }

    private List<AgentDrugDetailVo> createSampleDrugDetails() {
        AgentDrugDetailVo detail = new AgentDrugDetailVo();
        detail.setProductId(1L);
        detail.setProductName("维生素C片");

        DrugDetailDto drugDetail = new DrugDetailDto();
        detail.setDrugDetail(drugDetail);

        return List.of(detail);
    }

    // ==================== Stub Service ====================

    private static class StubMallProductService implements MallProductService {

        private Page<MallProductDetailDto> searchPage = new Page<>();
        private Page<MallProductDetailDto> productPage = new Page<>();
        private List<AgentProductDetailVo> productDetails = List.of();
        private List<AgentDrugDetailVo> drugDetails = List.of();

        private boolean searchProductsInvoked;
        private boolean listProductsInvoked;
        private boolean getProductDetailInvoked;
        private boolean getDrugDetailInvoked;

        private MallProductListQueryRequest capturedSearchRequest;
        private MallProductListQueryRequest capturedRequest;
        private List<Long> capturedProductIds;
        private List<Long> capturedDrugProductIds;

        /**
         * 执行 Elasticsearch 商品搜索。
         *
         * @param request 搜索请求参数
         * @return 商品搜索分页结果
         */
        @Override
        public Page<MallProductDetailDto> searchProducts(MallProductListQueryRequest request) {
            this.searchProductsInvoked = true;
            this.capturedSearchRequest = request;
            return searchPage;
        }

        /**
         * 查询商品列表。
         *
         * @param request 查询请求参数
         * @return 商品列表分页结果
         */
        @Override
        public Page<MallProductDetailDto> listProducts(MallProductListQueryRequest request) {
            this.listProductsInvoked = true;
            this.capturedRequest = request;
            return productPage;
        }

        /**
         * 查询商品详情。
         *
         * @param productIds 商品ID列表
         * @return 商品详情列表
         */
        @Override
        public List<AgentProductDetailVo> getProductDetail(List<Long> productIds) {
            this.getProductDetailInvoked = true;
            this.capturedProductIds = productIds;
            return productDetails;
        }

        /**
         * 查询药品详情。
         *
         * @param productIds 商品ID列表
         * @return 药品详情列表
         */
        @Override
        public List<AgentDrugDetailVo> getDrugDetail(List<Long> productIds) {
            this.getDrugDetailInvoked = true;
            this.capturedDrugProductIds = productIds;
            return drugDetails;
        }
    }
}
