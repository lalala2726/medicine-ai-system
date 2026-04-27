package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.elasticsearch.service.AdminMallProductSearchService;
import com.zhangyichuang.medicine.admin.mapper.MallOrderItemMapper;
import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.admin.task.MallProductSearchIndexer;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.entity.MallProductImage;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallProductServiceImplGetMallProductByIdsTests {

    @Mock
    private MallProductMapper mallProductMapper;

    @Mock
    private MallCategoryService mallCategoryService;

    @Mock
    private MallProductImageService mallProductImageService;

    @Mock
    private MallProductCategoryRelService mallProductCategoryRelService;

    @Mock
    private MallMedicineDetailService medicineDetailService;

    @Mock
    private MallProductStatsService mallProductStatsService;

    @Mock
    private MallProductTagService mallProductTagService;

    @Mock
    private MallProductTagRelService mallProductTagRelService;

    @Mock
    private MallOrderItemMapper mallOrderItemMapper;

    @Mock
    private MallProductSearchIndexer mallProductSearchIndexer;

    @Mock
    private AdminMallProductSearchService adminMallProductSearchService;

    private MallProductServiceImpl mallProductService;

    @BeforeEach
    void setUp() {
        mallProductService = new MallProductServiceImpl(
                mallProductMapper,
                mallCategoryService,
                mallProductCategoryRelService,
                mallProductImageService,
                medicineDetailService,
                mallProductStatsService,
                mallProductTagService,
                mallProductTagRelService,
                mallOrderItemMapper,
                adminMallProductSearchService,
                mallProductSearchIndexer
        );
        lenient().when(mallProductTagService.listTagVoMapByProductIds(anyList())).thenReturn(java.util.Collections.<Long, List<MallProductTagVo>>emptyMap());
        lenient().when(mallProductCategoryRelService.listCategoryIdsMapByProductIds(anyList())).thenReturn(Collections.emptyMap());
    }

    @Test
    void getMallProductByIds_WhenInputIsNull_ShouldReturnEmptyList() {
        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMallProductByIds_WhenInputIsEmpty_ShouldReturnEmptyList() {
        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void getMallProductByIds_WhenProductsNotFound_ShouldReturnEmptyList() {
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(Collections.emptyList());

        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(List.of(1L, 2L));

        assertTrue(result.isEmpty());
        verify(mallProductMapper).getMallProductDetailByIds(anyList());
    }

    @Test
    void getMallProductByIds_WhenSingleProduct_ShouldReturnProductDetail() {
        // 准备商品数据
        MallProductDetailDto product = createMockProduct(1L, "商品1");
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(List.of(product));

        // 准备图片数据
        MallProductImage image = createMockImage(1L, "http://example.com/image1.jpg");
        mockProductImageService(List.of(image));

        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(List.of(1L));

        assertEquals(1, result.size());
        MallProductDetailDto detail = result.getFirst();

        assertEquals(1L, detail.getId());
        assertEquals("商品1", detail.getName());
        assertNotNull(detail.getImages());
        assertEquals(1, detail.getImages().size());
        assertEquals("http://example.com/image1.jpg", detail.getImages().getFirst());
        assertNotNull(detail.getTags());
        assertTrue(detail.getTags().isEmpty());
    }

    @Test
    void getMallProductByIds_WhenMultipleProducts_ShouldReturnAllProductDetails() {
        // 准备多个商品
        MallProductDetailDto product1 = createMockProduct(1L, "商品1");
        MallProductDetailDto product2 = createMockProduct(2L, "商品2");
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(List.of(product1, product2));

        // 准备图片数据
        MallProductImage image1 = createMockImage(1L, "http://example.com/image1.jpg");
        MallProductImage image2 = createMockImage(2L, "http://example.com/image2.jpg");
        mockProductImageService(List.of(image1, image2));

        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(List.of(1L, 2L));

        assertEquals(2, result.size());

        // 验证第一个商品
        MallProductDetailDto detail1 = result.get(0);
        assertEquals(1L, detail1.getId());
        assertEquals("商品1", detail1.getName());
        assertEquals(1, detail1.getImages().size());
        assertTrue(detail1.getTags().isEmpty());

        // 验证第二个商品
        MallProductDetailDto detail2 = result.get(1);
        assertEquals(2L, detail2.getId());
        assertEquals("商品2", detail2.getName());
        assertEquals(1, detail2.getImages().size());
        assertTrue(detail2.getTags().isEmpty());
    }

    @Test
    void getMallProductByIds_WhenProductHasNoImages_ShouldReturnProductWithEmptyImages() {
        // 准备商品数据
        MallProductDetailDto product = createMockProduct(1L, "商品1");
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(List.of(product));

        // 没有图片
        mockProductImageService(Collections.emptyList());

        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(List.of(1L));

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getImages());
        assertTrue(result.getFirst().getImages().isEmpty());
        assertTrue(result.getFirst().getTags().isEmpty());
    }

    @Test
    void getMallProductByIds_WhenProductHasMultipleImages_ShouldReturnAllImages() {
        // 准备商品数据
        MallProductDetailDto product = createMockProduct(1L, "商品1");
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(List.of(product));

        // 多张图片
        MallProductImage image1 = createMockImageWithSort(1L, "http://example.com/image1.jpg", 1);
        MallProductImage image2 = createMockImageWithSort(1L, "http://example.com/image2.jpg", 2);
        MallProductImage image3 = createMockImageWithSort(1L, "http://example.com/image3.jpg", 3);
        mockProductImageService(List.of(image1, image2, image3));

        List<MallProductDetailDto> result = mallProductService.getMallProductByIds(List.of(1L));

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getImages());
        assertEquals(3, result.getFirst().getImages().size());
        assertTrue(result.getFirst().getTags().isEmpty());
    }

    @Test
    void getMallProductByIds_ShouldCallMapperWithCorrectIds() {
        List<Long> inputIds = List.of(1L, 2L, 3L);
        when(mallProductMapper.getMallProductDetailByIds(anyList())).thenReturn(Collections.emptyList());

        mallProductService.getMallProductByIds(inputIds);

        verify(mallProductMapper).getMallProductDetailByIds(inputIds);
    }

    // ==================== Helper Methods ====================

    private MallProductDetailDto createMockProduct(Long id, String name) {
        MallProductDetailDto dto = new MallProductDetailDto();
        dto.setId(id);
        dto.setName(name);
        dto.setCategoryId(1L);
        dto.setPrice(BigDecimal.TEN);
        dto.setStock(100);
        return dto;
    }

    private MallProductImage createMockImage(Long productId, String imageUrl) {
        MallProductImage image = new MallProductImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setSort(1);
        return image;
    }

    private MallProductImage createMockImageWithSort(Long productId, String imageUrl, int sort) {
        MallProductImage image = new MallProductImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setSort(sort);
        return image;
    }

    @SuppressWarnings("unchecked")
    private void mockProductImageService(List<MallProductImage> images) {
        LambdaQueryChainWrapper<MallProductImage> mockWrapper = mock(LambdaQueryChainWrapper.class);
        when(mallProductImageService.lambdaQuery()).thenReturn(mockWrapper);
        when(mockWrapper.in(any(), anyList())).thenReturn(mockWrapper);
        when(mockWrapper.orderByAsc(any(com.baomidou.mybatisplus.core.toolkit.support.SFunction.class))).thenReturn(mockWrapper);
        when(mockWrapper.list()).thenReturn(images);
    }
}
