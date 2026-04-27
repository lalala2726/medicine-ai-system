package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentAfterSaleRpcServiceImplTests {

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @InjectMocks
    private AdminAgentAfterSaleRpcServiceImpl rpcService;

    /**
     * 测试目的：验证 RPC 列表查询仅做对象属性复制，不对分页和筛选字段做额外归一化处理。
     * 预期结果：传入服务层的请求参数与入参保持一致，包括 pageNum/pageSize、编码包装字符串与空白字符。
     */
    @Test
    void listAfterSales_ShouldCopyRawQueryValues() {
        MallAfterSaleListRequest query = new MallAfterSaleListRequest();
        query.setPageNum(0);
        query.setPageSize(0);
        query.setAfterSaleType("{\"value\":\"REFUND_ONLY\",\"description\":\"仅退款\"}");
        query.setAfterSaleStatus("{value=PENDING, description=待审核}");
        query.setApplyReason("收到商品损坏了");
        query.setOrderNo("  O20251108001  ");
        query.setUserId(1001L);

        when(mallAfterSaleService.getAfterSaleList(any(AfterSaleListRequest.class)))
                .thenReturn(new Page<MallAfterSaleListDto>(1, 10, 0));

        rpcService.listAfterSales(query);

        ArgumentCaptor<AfterSaleListRequest> captor = ArgumentCaptor.forClass(AfterSaleListRequest.class);
        verify(mallAfterSaleService).getAfterSaleList(captor.capture());
        AfterSaleListRequest actual = captor.getValue();

        assertEquals(0, actual.getPageNum());
        assertEquals(0, actual.getPageSize());
        assertEquals("{\"value\":\"REFUND_ONLY\",\"description\":\"仅退款\"}", actual.getAfterSaleType());
        assertEquals("{value=PENDING, description=待审核}", actual.getAfterSaleStatus());
        assertEquals("收到商品损坏了", actual.getApplyReason());
        assertEquals("  O20251108001  ", actual.getOrderNo());
        assertEquals(1001L, actual.getUserId());
    }

    /**
     * 测试目的：验证当查询参数为 null 时，RPC 层会将 null 直接传递给服务层。
     * 预期结果：服务层 getAfterSaleList 方法收到 null 参数。
     */
    @Test
    void listAfterSales_WithNullQuery_ShouldPassNullToService() {
        rpcService.listAfterSales(null);
        verify(mallAfterSaleService).getAfterSaleList(null);
        verifyNoMoreInteractions(mallAfterSaleService);
    }

    /**
     * 测试目的：验证售后 context 聚合会通过售后单号定位详情，并压缩凭证和时间线。
     * 预期结果：返回 map key 为售后单号，凭证只保留数量和首图，时间线最多 5 条。
     */
    @SuppressWarnings("unchecked")
    @Test
    void getAfterSaleContextsByAfterSaleNos_ShouldBuildContext() {
        LambdaQueryChainWrapper<MallAfterSale> queryWrapper = mock(LambdaQueryChainWrapper.class);
        when(mallAfterSaleService.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(MallAfterSale.builder().id(11L).afterSaleNo("AS20251108001").build());
        when(mallAfterSaleService.getAfterSaleDetail(11L)).thenReturn(createAfterSaleDetailVo());

        Map<String, AfterSaleContextDto> result = rpcService.getAfterSaleContextsByAfterSaleNos(List.of("AS20251108001"));

        assertNotNull(result);
        assertTrue(result.containsKey("AS20251108001"));
        AfterSaleContextDto context = result.get("AS20251108001");
        assertEquals("O20251108001", context.getOrderNo());
        assertEquals("PENDING", context.getStatusCode());
        assertEquals(2, context.getEvidenceSummary().getEvidenceCount());
        assertEquals("https://example.com/1.jpg", context.getEvidenceSummary().getFirstEvidenceImage());
        assertEquals(5, context.getTimelineSummary().size());
        assertTrue(context.getAiHints().getWaitingAudit());
        verify(mallAfterSaleService).getAfterSaleDetail(11L);
    }

    /**
     * 测试目的：验证售后 context 批量上限按参数异常处理。
     * 预期结果：超过 20 个售后单号时直接抛出 ParamException。
     */
    @Test
    void getAfterSaleContextsByAfterSaleNos_WhenOverLimit_ShouldThrowParamException() {
        List<String> afterSaleNos = IntStream.range(0, 21)
                .mapToObj(index -> "AS20251108" + index)
                .toList();

        assertThrows(ParamException.class, () -> rpcService.getAfterSaleContextsByAfterSaleNos(afterSaleNos));
        verifyNoInteractions(mallAfterSaleService);
    }

    /**
     * 功能描述：构造售后详情模拟数据，供 context 聚合测试复用。
     *
     * @return 返回售后详情 VO
     */
    private AfterSaleDetailVo createAfterSaleDetailVo() {
        return AfterSaleDetailVo.builder()
                .afterSaleNo("AS20251108001")
                .orderNo("O20251108001")
                .afterSaleType("REFUND_ONLY")
                .afterSaleTypeName("仅退款")
                .afterSaleStatus("PENDING")
                .afterSaleStatusName("待审核")
                .refundAmount(new BigDecimal("99.99"))
                .applyReasonName("商品损坏")
                .evidenceImages(List.of("https://example.com/1.jpg", "https://example.com/2.jpg"))
                .productInfo(AfterSaleDetailVo.ProductInfo.builder()
                        .productId(100L)
                        .productName("感冒药")
                        .quantity(1)
                        .totalPrice(new BigDecimal("99.99"))
                        .build())
                .timeline(createTimelineVos())
                .build();
    }

    /**
     * 功能描述：构造超过 context 摘要上限的售后时间线，用于验证截断规则。
     *
     * @return 返回 6 条售后时间线 VO
     */
    private List<AfterSaleTimelineVo> createTimelineVos() {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(index -> AfterSaleTimelineVo.builder()
                        .id((long) index)
                        .eventType("STEP_" + index)
                        .eventTypeName("节点" + index)
                        .eventStatus("PENDING")
                        .operatorType("USER")
                        .operatorTypeName("用户")
                        .description("售后处理节点" + index)
                        .createTime(new Date())
                        .build())
                .toList();
    }
}
