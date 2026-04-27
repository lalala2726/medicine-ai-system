package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleListVo;
import com.zhangyichuang.medicine.agent.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleTimelineDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentAfterSaleControllerTests {

    private final StubMallAfterSaleService mallAfterSaleService = new StubMallAfterSaleService();
    private final AgentAfterSaleController controller = new AgentAfterSaleController(mallAfterSaleService);

    /**
     * 测试目的：验证列表接口会委托 service 查询，并将 DTO 转换为精简 VO 原始码值结构。
     * 预期结果：响应成功，rows 中 afterSaleType/afterSaleStatus/applyReason 为原始码值，不包含中文映射值。
     */
    @Test
    void listAfterSales_ShouldDelegateToService() {
        MallAfterSaleListRequest request = new MallAfterSaleListRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        mallAfterSaleService.page = createSamplePage();

        var result = controller.listAfterSales(request);

        assertEquals(200, result.getCode());
        assertTrue(mallAfterSaleService.listAfterSalesInvoked);
        assertEquals(request, mallAfterSaleService.capturedRequest);
        assertNotNull(result.getData());

        TableDataResult tableData = result.getData();
        assertNotNull(tableData.getRows());
        assertEquals(1, tableData.getRows().size());

        AgentAfterSaleListVo row = (AgentAfterSaleListVo) tableData.getRows().get(0);
        assertEquals("REFUND_ONLY", row.getAfterSaleType());
        assertEquals("PENDING", row.getAfterSaleStatus());
        assertEquals("DAMAGED", row.getApplyReason());
    }

    /**
     * 测试目的：验证当请求参数为空时，控制器会创建默认请求对象并继续查询流程。
     * 预期结果：响应成功，service 被调用且接收到的请求对象不为空。
     */
    @Test
    void listAfterSales_WithNullRequest_ShouldUseDefault() {
        mallAfterSaleService.page = createSamplePage();

        var result = controller.listAfterSales(null);

        assertEquals(200, result.getCode());
        assertTrue(mallAfterSaleService.listAfterSalesInvoked);
        assertNotNull(mallAfterSaleService.capturedRequest);
    }

    /**
     * 测试目的：验证当 Dubbo 记录被反序列化为 Map 结构时，控制层仍能返回同等数量的列表结果。
     * 预期结果：响应成功且 rows 数量正确；由于当前复制策略基于 BeanUtils，Map 字段不自动映射到 VO 属性。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void listAfterSales_WithMapRecord_ShouldConvertSuccessfully() {
        Page<MallAfterSaleListDto> page = new Page<>(1, 10);
        page.setTotal(1);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("afterSaleNo", "AS20251108001");
        row.put("orderNo", "O20251108001");
        row.put("userId", 1001L);
        row.put("userNickname", "张三");
        row.put("productName", "感冒药");
        row.put("afterSaleType", "REFUND_ONLY");
        row.put("afterSaleStatus", "PENDING");
        row.put("applyReason", "DAMAGED");
        page.setRecords((List) List.of(row));
        mallAfterSaleService.page = page;

        var result = controller.listAfterSales(new MallAfterSaleListRequest());

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getRows());
        assertEquals(1, result.getData().getRows().size());

        AgentAfterSaleListVo vo = (AgentAfterSaleListVo) result.getData().getRows().get(0);
        assertNull(vo.getId());
        assertNull(vo.getAfterSaleNo());
        assertNull(vo.getAfterSaleType());
        assertNull(vo.getAfterSaleStatus());
        assertNull(vo.getApplyReason());
    }

    /**
     * 测试目的：验证详情接口会透传售后单号列表到 service，并返回 service 提供的详情数据。
     * 预期结果：响应成功，service 被调用且返回详情中的售后单号正确。
     */
    @Test
    void getAfterSaleDetail_ShouldDelegateToService() {
        List<String> afterSaleNos = List.of("AS20251108001", "AS20251108002");
        mallAfterSaleService.details = List.of(createSampleDetail());

        var result = controller.getAfterSaleDetail(afterSaleNos);

        assertEquals(200, result.getCode());
        assertTrue(mallAfterSaleService.getAfterSaleDetailsInvoked);
        assertEquals(afterSaleNos, mallAfterSaleService.capturedDetailAfterSaleNos);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        AgentAfterSaleDetailVo detailVo = result.getData().getFirst();
        assertEquals("AS20251108001", detailVo.getAfterSaleNo());
        assertEquals("REFUND_ONLY", detailVo.getAfterSaleType());
        assertEquals("PENDING", detailVo.getAfterSaleStatus());
    }

    /**
     * 测试目的：验证售后聚合上下文接口会批量透传售后单号到 service。
     * 预期结果：响应成功，返回按售后单号分组的售后 context。
     */
    @Test
    void getAfterSaleContext_ShouldDelegateToService() {
        List<String> afterSaleNos = List.of("AS20251108001", "AS20251108002");
        mallAfterSaleService.contexts = createSampleAfterSaleContexts();

        var result = controller.getAfterSaleContext(afterSaleNos);

        assertEquals(200, result.getCode());
        assertTrue(mallAfterSaleService.getAfterSaleContextsInvoked);
        assertEquals(afterSaleNos, mallAfterSaleService.capturedContextAfterSaleNos);
        assertNotNull(result.getData());
        assertTrue(result.getData().containsKey("AS20251108001"));
        assertEquals("PENDING", result.getData().get("AS20251108001").getStatusCode());
    }

    /**
     * 测试目的：验证智能体售后列表 VO 不再保留历史 name 字段，确保结构已收敛到精简版本。
     * 预期结果：访问 afterSaleTypeName、afterSaleStatusName、applyReasonName 字段会抛出 NoSuchFieldException。
     */
    @Test
    void agentAfterSaleListVo_ShouldNotContainLegacyNameFields() {
        assertThrows(NoSuchFieldException.class, () -> AgentAfterSaleListVo.class.getDeclaredField("afterSaleTypeName"));
        assertThrows(NoSuchFieldException.class, () -> AgentAfterSaleListVo.class.getDeclaredField("afterSaleStatusName"));
        assertThrows(NoSuchFieldException.class, () -> AgentAfterSaleListVo.class.getDeclaredField("applyReasonName"));
    }

    /**
     * 功能描述：构造售后列表分页模拟数据，供列表相关测试复用。
     *
     * @return 返回包含一条记录的售后分页数据
     * @throws RuntimeException 异常说明：当对象构造过程异常时抛出运行时异常
     */
    private Page<MallAfterSaleListDto> createSamplePage() {
        Page<MallAfterSaleListDto> page = new Page<>(1, 10);
        page.setTotal(1);

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
        page.setRecords(List.of(item));
        return page;
    }

    /**
     * 功能描述：构造售后详情模拟数据，供详情接口测试复用。
     *
     * @return 返回售后详情对象
     * @throws RuntimeException 异常说明：当对象构造过程异常时抛出运行时异常
     */
    private AfterSaleDetailDto createSampleDetail() {
        AfterSaleDetailDto detailDto = new AfterSaleDetailDto();
        detailDto.setId(1L);
        detailDto.setAfterSaleNo("AS20251108001");
        detailDto.setOrderNo("O20251108001");
        detailDto.setAfterSaleType("REFUND_ONLY");
        detailDto.setAfterSaleTypeName("仅退款");
        detailDto.setAfterSaleStatus("PENDING");
        detailDto.setAfterSaleStatusName("待审核");
        detailDto.setRefundAmount(new BigDecimal("99.99"));
        detailDto.setProductInfo(AfterSaleDetailDto.ProductInfo.builder()
                .productId(100L)
                .productName("感冒药")
                .productPrice(new BigDecimal("99.99"))
                .quantity(1)
                .totalPrice(new BigDecimal("99.99"))
                .build());
        detailDto.setTimeline(List.of(AfterSaleTimelineDto.builder()
                .id(1L)
                .eventType("APPLY")
                .eventTypeName("用户申请")
                .eventStatus("PENDING")
                .operatorType("USER")
                .operatorTypeName("用户")
                .description("用户提交售后申请")
                .createTime(new Date())
                .build()));
        return detailDto;
    }

    /**
     * 功能描述：构造售后聚合上下文模拟数据，供 context 接口测试复用。
     *
     * @return 返回按售后单号分组的售后聚合上下文
     */
    private Map<String, AfterSaleContextDto> createSampleAfterSaleContexts() {
        AfterSaleContextDto context = AfterSaleContextDto.builder()
                .afterSaleNo("AS20251108001")
                .orderNo("O20251108001")
                .statusCode("PENDING")
                .statusText("待审核")
                .typeCode("REFUND_ONLY")
                .typeText("仅退款")
                .refundAmount(new BigDecimal("99.99"))
                .timelineSummary(List.of(AfterSaleContextDto.TimelineItem.builder()
                        .eventType("APPLY")
                        .eventStatus("PENDING")
                        .operatorType("USER")
                        .description("用户提交售后申请")
                        .eventTime(new Date())
                        .build()))
                .build();
        return Map.of("AS20251108001", context);
    }

    private static class StubMallAfterSaleService implements MallAfterSaleService {

        private Page<MallAfterSaleListDto> page = new Page<>();
        private List<AfterSaleDetailDto> details = List.of();
        private Map<String, AfterSaleContextDto> contexts = Map.of();

        private boolean listAfterSalesInvoked;
        private boolean getAfterSaleDetailsInvoked;
        private boolean getAfterSaleContextsInvoked;

        private MallAfterSaleListRequest capturedRequest;
        private List<String> capturedDetailAfterSaleNos;
        private List<String> capturedContextAfterSaleNos;

        @Override
        public Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest request) {
            this.listAfterSalesInvoked = true;
            this.capturedRequest = request;
            return page;
        }

        @Override
        public List<AfterSaleDetailDto> getAfterSaleDetails(List<String> afterSaleNos) {
            this.getAfterSaleDetailsInvoked = true;
            this.capturedDetailAfterSaleNos = afterSaleNos;
            return details;
        }

        @Override
        public Map<String, AfterSaleContextDto> getAfterSaleContexts(List<String> afterSaleNos) {
            this.getAfterSaleContextsInvoked = true;
            this.capturedContextAfterSaleNos = afterSaleNos;
            return contexts;
        }
    }
}
