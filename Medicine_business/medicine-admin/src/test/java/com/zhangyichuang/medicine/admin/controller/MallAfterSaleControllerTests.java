package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.model.vo.MallAfterSaleListVo;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MallAfterSaleControllerTests {

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @InjectMocks
    private MallAfterSaleController controller;

    /**
     * 测试目的：验证管理端售后列表接口会将 DTO 分页数据转换为精简 VO，并输出原始码值字段。
     * 预期结果：响应成功，rows 中字段类型与值符合新 VO 约束，afterSaleType/afterSaleStatus/applyReason 为原始码值。
     */
    @Test
    void getAfterSaleList_ShouldConvertDtoToVoWithRawCodes() {
        AfterSaleListRequest request = new AfterSaleListRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        Page<MallAfterSaleListDto> page = createSamplePage();
        when(mallAfterSaleService.getAfterSaleList(request)).thenReturn(page);

        var result = controller.getAfterSaleList(request);

        assertEquals(200, result.getCode());
        verify(mallAfterSaleService).getAfterSaleList(request);
        assertNotNull(result.getData());

        TableDataResult tableData = result.getData();
        assertEquals(1L, tableData.getTotal());
        assertEquals(1L, tableData.getPageNum());
        assertEquals(10L, tableData.getPageSize());
        assertNotNull(tableData.getRows());
        assertEquals(1, tableData.getRows().size());

        MallAfterSaleListVo row = (MallAfterSaleListVo) tableData.getRows().get(0);
        assertEquals("REFUND_ONLY", row.getAfterSaleType());
        assertEquals("PENDING", row.getAfterSaleStatus());
        assertEquals("DAMAGED", row.getApplyReason());
        assertEquals("张三", row.getUserNickname());
        assertEquals("感冒药", row.getProductName());
    }

    /**
     * 测试目的：验证管理端售后列表 VO 已移除历史 name 映射字段，避免返回兼容字段。
     * 预期结果：访问 afterSaleTypeName、afterSaleStatusName、applyReasonName 字段时抛出 NoSuchFieldException。
     */
    @Test
    void mallAfterSaleListVo_ShouldNotContainLegacyNameFields() {
        assertThrows(NoSuchFieldException.class, () -> MallAfterSaleListVo.class.getDeclaredField("afterSaleTypeName"));
        assertThrows(NoSuchFieldException.class, () -> MallAfterSaleListVo.class.getDeclaredField("afterSaleStatusName"));
        assertThrows(NoSuchFieldException.class, () -> MallAfterSaleListVo.class.getDeclaredField("applyReasonName"));
    }

    /**
     * 功能描述：构造售后列表分页模拟数据，供列表转换场景测试复用。
     *
     * @return 返回包含一条记录的售后列表分页对象
     * @throws RuntimeException 异常说明：当对象构造过程发生异常时抛出运行时异常
     */
    private Page<MallAfterSaleListDto> createSamplePage() {
        Page<MallAfterSaleListDto> page = new Page<>(1, 10, 1);
        MallAfterSaleListDto item = new MallAfterSaleListDto();
        item.setId(1L);
        item.setAfterSaleNo("AS20251108001");
        item.setOrderId(1L);
        item.setOrderNo("O20251108001");
        item.setOrderItemId(11L);
        item.setUserId(1001L);
        item.setUserNickname("张三");
        item.setProductName("感冒药");
        item.setProductImage("https://example.com/image.jpg");
        item.setAfterSaleType("REFUND_ONLY");
        item.setAfterSaleStatus("PENDING");
        item.setRefundAmount(new BigDecimal("99.99"));
        item.setApplyReason("DAMAGED");
        item.setApplyTime(new Date());
        item.setAuditTime(new Date());
        page.setRecords(List.of(item));
        return page;
    }
}
