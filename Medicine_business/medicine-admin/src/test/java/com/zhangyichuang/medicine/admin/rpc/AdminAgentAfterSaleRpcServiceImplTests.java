package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleTimelineService;
import com.zhangyichuang.medicine.admin.service.MallOrderItemService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallAfterSaleTimeline;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.AfterSaleReasonEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.enums.AfterSaleTypeEnum;
import com.zhangyichuang.medicine.model.enums.ReceiveStatusEnum;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAgentAfterSaleRpcServiceImplTests {

    @Mock
    private MallAfterSaleService mallAfterSaleService;

    @Mock
    private MallAfterSaleTimelineService mallAfterSaleTimelineService;

    @Mock
    private MallOrderItemService mallOrderItemService;

    @Mock
    private UserService userService;

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
        when(queryWrapper.in(any(), anyCollection())).thenReturn(queryWrapper);
        when(queryWrapper.list()).thenReturn(List.of(createAfterSale()));

        LambdaQueryChainWrapper<User> userQuery = mock(LambdaQueryChainWrapper.class);
        when(userService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.in(any(), anyCollection())).thenReturn(userQuery);
        when(userQuery.list()).thenReturn(List.of(createUser()));

        LambdaQueryChainWrapper<MallOrderItem> orderItemQuery = mock(LambdaQueryChainWrapper.class);
        when(mallOrderItemService.lambdaQuery()).thenReturn(orderItemQuery);
        when(orderItemQuery.in(any(), anyCollection())).thenReturn(orderItemQuery);
        when(orderItemQuery.list()).thenReturn(List.of(createOrderItem()));

        LambdaQueryChainWrapper<MallAfterSaleTimeline> timelineQuery = mock(LambdaQueryChainWrapper.class);
        when(mallAfterSaleTimelineService.lambdaQuery()).thenReturn(timelineQuery);
        when(timelineQuery.in(any(), anyCollection())).thenReturn(timelineQuery);
        when(timelineQuery.orderByDesc((SFunction<MallAfterSaleTimeline, ?>) any())).thenReturn(timelineQuery);
        when(timelineQuery.list()).thenReturn(createTimelines());

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
        verify(mallAfterSaleService, never()).getAfterSaleDetail(anyLong());
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
        verifyNoInteractions(mallAfterSaleService, mallAfterSaleTimelineService, mallOrderItemService, userService);
    }

    /**
     * 功能描述：构造售后实体模拟数据，供 context 聚合测试复用。
     *
     * @return 返回售后实体
     */
    private MallAfterSale createAfterSale() {
        return MallAfterSale.builder()
                .id(11L)
                .afterSaleNo("AS20251108001")
                .orderId(1L)
                .orderNo("O20251108001")
                .orderItemId(21L)
                .userId(31L)
                .afterSaleType(AfterSaleTypeEnum.REFUND_ONLY.getType())
                .afterSaleStatus(AfterSaleStatusEnum.PENDING.getStatus())
                .refundAmount(new BigDecimal("99.99"))
                .applyReason(AfterSaleReasonEnum.DAMAGED.getReason())
                .receiveStatus(ReceiveStatusEnum.RECEIVED.getStatus())
                .evidenceImages("[\"https://example.com/1.jpg\",\"https://example.com/2.jpg\"]")
                .build();
    }

    /**
     * 功能描述：构造用户实体模拟数据，供售后详情批量组装复用。
     *
     * @return 返回用户实体
     */
    private User createUser() {
        User user = new User();
        user.setId(31L);
        user.setNickname("张三");
        return user;
    }

    /**
     * 功能描述：构造订单项实体模拟数据，供售后商品摘要测试复用。
     *
     * @return 返回订单项实体
     */
    private MallOrderItem createOrderItem() {
        return MallOrderItem.builder()
                .id(21L)
                .productId(100L)
                .productName("感冒药")
                .quantity(1)
                .totalPrice(new BigDecimal("99.99"))
                .build();
    }

    /**
     * 功能描述：构造超过 context 摘要上限的售后时间线，用于验证截断规则。
     *
     * @return 返回 6 条售后时间线实体
     */
    private List<MallAfterSaleTimeline> createTimelines() {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(index -> MallAfterSaleTimeline.builder()
                        .id((long) index)
                        .afterSaleId(11L)
                        .eventType("AFTER_SALE_APPLIED")
                        .eventStatus("PENDING")
                        .operatorType("USER")
                        .description("售后处理节点" + index)
                        .createTime(new Date())
                        .build())
                .toList();
    }
}
