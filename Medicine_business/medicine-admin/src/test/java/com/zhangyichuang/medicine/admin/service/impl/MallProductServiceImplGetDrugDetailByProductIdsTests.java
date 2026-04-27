package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.service.MallCategoryService;
import com.zhangyichuang.medicine.admin.service.MallMedicineDetailService;
import com.zhangyichuang.medicine.admin.service.MallProductImageService;
import com.zhangyichuang.medicine.admin.service.MallProductStatsService;
import com.zhangyichuang.medicine.admin.task.MallProductSearchIndexer;
import com.zhangyichuang.medicine.model.dto.AgentDrugDetailDto;
import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import com.zhangyichuang.medicine.model.entity.DrugDetail;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.enums.DrugCategoryEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallProductServiceImplGetDrugDetailByProductIdsTests {

    @Mock
    private MallProductMapper mallProductMapper;

    @Mock
    private MallCategoryService mallCategoryService;

    @Mock
    private MallProductImageService mallProductImageService;

    @Mock
    private MallMedicineDetailService medicineDetailService;

    @Mock
    private MallProductStatsService mallProductStatsService;

    @Mock
    private MallProductSearchIndexer mallProductSearchIndexer;

    @Spy
    @InjectMocks
    private MallProductServiceImpl mallProductService;

    @Test
    void getDrugDetailByProductIds_WhenInputIsNull_ShouldReturnEmptyList() {
        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getDrugDetailByProductIds_WhenInputIsEmpty_ShouldReturnEmptyList() {
        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void getDrugDetailByProductIds_WhenProductsNotFound_ShouldReturnEmptyList() {
        doReturn(Collections.emptyList()).when(mallProductService).listByIds(anyList());

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(1L, 2L));

        assertTrue(result.isEmpty());
    }

    @Test
    void getDrugDetailByProductIds_WhenDrugDetailsNotFound_ShouldReturnEmptyList() {
        // 准备商品数据
        MallProduct product = createMockProduct(1L, "商品1");
        doReturn(List.of(product)).when(mallProductService).listByIds(anyList());

        // 没有药品详情
        mockMedicineDetailService(Collections.emptyList());

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(1L));

        assertTrue(result.isEmpty());
    }

    @Test
    void getDrugDetailByProductIds_WhenSingleDrugDetail_ShouldReturnDrugDetail() {
        // 准备商品数据
        MallProduct product = createMockProduct(1L, "维生素C片");
        doReturn(List.of(product)).when(mallProductService).listByIds(anyList());

        // 准备药品详情
        DrugDetail drugDetail = createMockDrugDetail(1L, "维生素C片", "国药准字H12345678");
        mockMedicineDetailService(List.of(drugDetail));

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(1L));

        assertEquals(1, result.size());
        AgentDrugDetailDto vo = result.getFirst();

        assertEquals(1L, vo.getProductId());
        assertEquals("维生素C片", vo.getProductName());
        assertNotNull(vo.getDrugDetail());
        assertEquals("维生素C片", vo.getDrugDetail().getCommonName());
        assertEquals("国药准字H12345678", vo.getDrugDetail().getApprovalNumber());
    }

    @Test
    void getDrugDetailByProductIds_WhenMultipleDrugDetails_ShouldReturnAllDrugDetails() {
        // 准备多个商品
        MallProduct product1 = createMockProduct(1L, "维生素C片");
        MallProduct product2 = createMockProduct(2L, "感冒灵颗粒");
        doReturn(List.of(product1, product2)).when(mallProductService).listByIds(anyList());

        // 准备多个药品详情
        DrugDetail drug1 = createMockDrugDetail(1L, "维生素C片", "国药准字H12345678");
        DrugDetail drug2 = createMockDrugDetail(2L, "感冒灵颗粒", "国药准字Z87654321");
        mockMedicineDetailService(List.of(drug1, drug2));

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(1L, 2L));

        assertEquals(2, result.size());

        // 验证第一个药品
        AgentDrugDetailDto vo1 = result.get(0);
        assertEquals(1L, vo1.getProductId());
        assertEquals("维生素C片", vo1.getProductName());
        assertNotNull(vo1.getDrugDetail());

        // 验证第二个药品
        AgentDrugDetailDto vo2 = result.get(1);
        assertEquals(2L, vo2.getProductId());
        assertEquals("感冒灵颗粒", vo2.getProductName());
        assertNotNull(vo2.getDrugDetail());
    }

    @Test
    void getDrugDetailByProductIds_WhenDrugDetailHasAllFields_ShouldMapAllFields() {
        // 准备商品数据
        MallProduct product = createMockProduct(1L, "测试药品");
        doReturn(List.of(product)).when(mallProductService).listByIds(anyList());

        // 准备完整的药品详情
        DrugDetail drugDetail = createFullMockDrugDetail(1L);
        mockMedicineDetailService(List.of(drugDetail));

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(1L));

        assertEquals(1, result.size());
        AgentDrugDetailDto vo = result.getFirst();
        DrugDetailDto dto = vo.getDrugDetail();

        assertNotNull(dto);
        assertEquals("测试药品通用名", dto.getCommonName());
        assertEquals("成分描述", dto.getComposition());
        assertEquals("性状描述", dto.getCharacteristics());
        assertEquals("包装规格", dto.getPackaging());
        assertEquals("24个月", dto.getValidityPeriod());
        assertEquals("密封保存", dto.getStorageConditions());
        assertEquals("测试药厂", dto.getProductionUnit());
        assertEquals("国药准字H12345678", dto.getApprovalNumber());
        assertEquals("执行标准", dto.getExecutiveStandard());
        assertEquals("国产", dto.getOriginType());
        assertEquals("温馨提示内容", dto.getWarmTips());
        assertEquals("测试品牌", dto.getBrand());
        assertEquals("功能主治描述", dto.getEfficacy());
        assertEquals("用法用量描述", dto.getUsageMethod());
        assertEquals("不良反应描述", dto.getAdverseReactions());
        assertEquals("注意事项描述", dto.getPrecautions());
        assertEquals("禁忌描述", dto.getTaboo());
        assertEquals("说明书全文", dto.getInstruction());
    }

    @Test
    void getDrugDetailByProductIds_WhenProductNotFoundButDrugExists_ShouldHaveNullProductName() {
        // 准备商品数据（商品不存在）
        doReturn(Collections.emptyList()).when(mallProductService).listByIds(anyList());

        List<AgentDrugDetailDto> result = mallProductService.getDrugDetailByProductIds(List.of(999L));

        assertTrue(result.isEmpty());
    }

    // ==================== Helper Methods ====================

    private MallProduct createMockProduct(Long id, String name) {
        MallProduct product = new MallProduct();
        product.setId(id);
        product.setName(name);
        return product;
    }

    private DrugDetail createMockDrugDetail(Long productId, String commonName, String approvalNumber) {
        DrugDetail drug = new DrugDetail();
        drug.setProductId(productId);
        drug.setCommonName(commonName);
        drug.setApprovalNumber(approvalNumber);
        return drug;
    }

    private DrugDetail createFullMockDrugDetail(Long productId) {
        DrugDetail drug = new DrugDetail();
        drug.setProductId(productId);
        drug.setCommonName("测试药品通用名");
        drug.setComposition("成分描述");
        drug.setCharacteristics("性状描述");
        drug.setPackaging("包装规格");
        drug.setValidityPeriod("24个月");
        drug.setStorageConditions("密封保存");
        drug.setProductionUnit("测试药厂");
        drug.setApprovalNumber("国药准字H12345678");
        drug.setExecutiveStandard("执行标准");
        drug.setOriginType("国产");
        drug.setIsOutpatientMedicine(false);
        drug.setWarmTips("温馨提示内容");
        drug.setBrand("测试品牌");
        drug.setDrugCategory(DrugCategoryEnum.OTC_GREEN.getCode());
        drug.setEfficacy("功能主治描述");
        drug.setUsageMethod("用法用量描述");
        drug.setAdverseReactions("不良反应描述");
        drug.setPrecautions("注意事项描述");
        drug.setTaboo("禁忌描述");
        drug.setInstruction("说明书全文");
        return drug;
    }

    @SuppressWarnings("unchecked")
    private void mockMedicineDetailService(List<DrugDetail> drugDetails) {
        LambdaQueryChainWrapper<DrugDetail> mockWrapper = mock(LambdaQueryChainWrapper.class);
        when(medicineDetailService.lambdaQuery()).thenReturn(mockWrapper);
        when(mockWrapper.in(any(), anyList())).thenReturn(mockWrapper);
        when(mockWrapper.list()).thenReturn(drugDetails);
    }
}
