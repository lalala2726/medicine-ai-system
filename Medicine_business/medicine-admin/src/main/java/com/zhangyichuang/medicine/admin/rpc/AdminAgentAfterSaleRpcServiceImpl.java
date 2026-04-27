package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.admin.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleTimelineDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.enums.AfterSaleStatusEnum;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import com.zhangyichuang.medicine.model.vo.AfterSaleDetailVo;
import com.zhangyichuang.medicine.model.vo.AfterSaleTimelineVo;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentAfterSaleRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 Agent 售后 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentAfterSaleRpcService.class, group = "medicine-admin", version = "1.0.0")
@RequiredArgsConstructor
public class AdminAgentAfterSaleRpcServiceImpl implements AdminAgentAfterSaleRpcService {

    /**
     * AI Context 工具单次批量查询上限。
     */
    private static final int CONTEXT_BATCH_LIMIT = 20;

    /**
     * AI Context 售后时间线最多返回节点数。
     */
    private static final int CONTEXT_TIMELINE_LIMIT = 5;

    private final MallAfterSaleService mallAfterSaleService;

    /**
     * 功能描述：提供给 Agent 端的售后列表分页查询能力。
     *
     * @param query 售后分页查询参数，包含分页参数与筛选条件
     * @return 返回售后分页结果，记录类型为 {@link MallAfterSaleListDto}
     * @throws RuntimeException 异常说明：当管理端售后服务查询异常时抛出运行时异常
     */
    @Override
    public Page<MallAfterSaleListDto> listAfterSales(MallAfterSaleListRequest query) {
        AfterSaleListRequest afterSaleListRequest = BeanCotyUtils.copyProperties(query, AfterSaleListRequest.class);
        return mallAfterSaleService.getAfterSaleList(afterSaleListRequest);
    }

    /**
     * 功能描述：根据售后单号批量查询售后详情并返回给 Agent。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回售后详情 DTO 列表
     * @throws RuntimeException 异常说明：当售后详情查询异常时抛出运行时异常
     */
    @Override
    public List<AfterSaleDetailDto> getAfterSaleDetailsByAfterSaleNos(List<String> afterSaleNos) {
        Assert.notEmpty(afterSaleNos, "售后单号不能为空");
        return afterSaleNos.stream()
                .map(this::getAfterSaleDetailByAfterSaleNo)
                .toList();
    }

    /**
     * 功能描述：根据售后单号批量查询智能体售后上下文。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回按售后单号分组的售后上下文
     * @throws RuntimeException 异常说明：当售后单号为空、超过上限或不存在时抛出运行时异常
     */
    @Override
    public Map<String, AfterSaleContextDto> getAfterSaleContextsByAfterSaleNos(List<String> afterSaleNos) {
        validateContextAfterSaleNos(afterSaleNos);
        Map<String, AfterSaleContextDto> result = new LinkedHashMap<>();
        for (String afterSaleNo : afterSaleNos) {
            AfterSaleDetailDto detail = getAfterSaleDetailByAfterSaleNo(afterSaleNo);
            result.put(afterSaleNo, buildAfterSaleContext(detail));
        }
        return result;
    }

    /**
     * 功能描述：根据售后单号查询售后详情并转换为 DTO。
     *
     * @param afterSaleNo 售后单号
     * @return 售后详情 DTO
     * @throws RuntimeException 异常说明：当售后单不存在或详情查询异常时抛出运行时异常
     */
    private AfterSaleDetailDto getAfterSaleDetailByAfterSaleNo(String afterSaleNo) {
        Assert.notEmpty(afterSaleNo, "售后单号不能为空");
        MallAfterSale afterSale = mallAfterSaleService.lambdaQuery()
                .eq(MallAfterSale::getAfterSaleNo, afterSaleNo)
                .one();
        Assert.notNull(afterSale, "售后单不存在: " + afterSaleNo);
        AfterSaleDetailVo detailVo = mallAfterSaleService.getAfterSaleDetail(afterSale.getId());
        return toAfterSaleDetailDto(detailVo);
    }

    /**
     * 功能描述：校验售后 Context 批量查询参数。
     *
     * @param afterSaleNos 售后单号列表
     * @throws RuntimeException 异常说明：当售后单号为空或超过上限时抛出运行时异常
     */
    private void validateContextAfterSaleNos(List<String> afterSaleNos) {
        Assert.notEmpty(afterSaleNos, "售后单号不能为空");
        Assert.isParamTrue(afterSaleNos.size() <= CONTEXT_BATCH_LIMIT, "售后单号最多支持20个");
    }

    /**
     * 功能描述：构建单个售后单的智能体上下文。
     *
     * @param detail 售后详情 DTO
     * @return 返回售后智能体上下文
     * @throws RuntimeException 异常说明：当售后详情为空时抛出运行时异常
     */
    private AfterSaleContextDto buildAfterSaleContext(AfterSaleDetailDto detail) {
        Assert.notNull(detail, "售后详情不能为空");
        AfterSaleStatusEnum statusEnum = AfterSaleStatusEnum.fromCode(detail.getAfterSaleStatus());
        return AfterSaleContextDto.builder()
                .afterSaleNo(detail.getAfterSaleNo())
                .orderNo(detail.getOrderNo())
                .statusCode(detail.getAfterSaleStatus())
                .statusText(detail.getAfterSaleStatusName())
                .typeCode(detail.getAfterSaleType())
                .typeText(detail.getAfterSaleTypeName())
                .refundAmount(detail.getRefundAmount())
                .reasonText(detail.getApplyReasonName())
                .productSummary(buildProductSummary(detail.getProductInfo()))
                .evidenceSummary(buildEvidenceSummary(detail.getEvidenceImages()))
                .timelineSummary(buildTimelineSummary(detail.getTimeline()))
                .aiHints(buildAfterSaleAiHints(statusEnum))
                .build();
    }

    /**
     * 功能描述：构建售后商品摘要。
     *
     * @param productInfo 售后商品信息 DTO
     * @return 返回售后商品摘要
     */
    private AfterSaleContextDto.ProductSummary buildProductSummary(AfterSaleDetailDto.ProductInfo productInfo) {
        if (productInfo == null) {
            return null;
        }
        return AfterSaleContextDto.ProductSummary.builder()
                .productId(productInfo.getProductId())
                .productName(productInfo.getProductName())
                .productImage(productInfo.getProductImage())
                .quantity(productInfo.getQuantity())
                .totalPrice(productInfo.getTotalPrice())
                .build();
    }

    /**
     * 功能描述：构建售后凭证摘要。
     *
     * @param evidenceImages 售后凭证图片列表
     * @return 返回凭证数量与首图摘要
     */
    private AfterSaleContextDto.EvidenceSummary buildEvidenceSummary(List<String> evidenceImages) {
        int evidenceCount = CollectionUtils.isEmpty(evidenceImages) ? 0 : evidenceImages.size();
        String firstEvidenceImage = CollectionUtils.isEmpty(evidenceImages) ? null : evidenceImages.get(0);
        return AfterSaleContextDto.EvidenceSummary.builder()
                .evidenceCount(evidenceCount)
                .firstEvidenceImage(firstEvidenceImage)
                .build();
    }

    /**
     * 功能描述：构建最近售后处理时间线摘要。
     *
     * @param timeline 售后时间线 DTO 列表
     * @return 返回最多 5 条最近处理节点
     */
    private List<AfterSaleContextDto.TimelineItem> buildTimelineSummary(List<AfterSaleTimelineDto> timeline) {
        if (CollectionUtils.isEmpty(timeline)) {
            return List.of();
        }
        return timeline.stream()
                .limit(CONTEXT_TIMELINE_LIMIT)
                .map(item -> AfterSaleContextDto.TimelineItem.builder()
                        .eventType(item.getEventType())
                        .eventTypeName(item.getEventTypeName())
                        .eventStatus(item.getEventStatus())
                        .operatorType(item.getOperatorType())
                        .operatorTypeName(item.getOperatorTypeName())
                        .description(item.getDescription())
                        .eventTime(item.getCreateTime())
                        .build())
                .toList();
    }

    /**
     * 功能描述：构建售后智能体决策提示。
     *
     * @param statusEnum 售后状态枚举
     * @return 返回智能体决策提示
     */
    private AfterSaleContextDto.AiHints buildAfterSaleAiHints(AfterSaleStatusEnum statusEnum) {
        return AfterSaleContextDto.AiHints.builder()
                .waitingAudit(statusEnum == AfterSaleStatusEnum.PENDING)
                .processing(statusEnum == AfterSaleStatusEnum.APPROVED || statusEnum == AfterSaleStatusEnum.PROCESSING)
                .completed(statusEnum == AfterSaleStatusEnum.COMPLETED)
                .rejected(statusEnum == AfterSaleStatusEnum.REJECTED)
                .canCancel(statusEnum == AfterSaleStatusEnum.PENDING || statusEnum == AfterSaleStatusEnum.APPROVED)
                .canReapply(statusEnum == AfterSaleStatusEnum.REJECTED || statusEnum == AfterSaleStatusEnum.CANCELLED)
                .build();
    }

    /**
     * 功能描述：将售后详情 VO 转换为 RPC 传输 DTO，避免 RPC 层直接暴露 VO。
     *
     * @param source 源售后详情 VO，类型为 {@link AfterSaleDetailVo}
     * @return 返回售后详情 DTO；当 source 为空时返回 null
     * @throws RuntimeException 异常说明：当属性转换异常时抛出运行时异常
     */
    private AfterSaleDetailDto toAfterSaleDetailDto(AfterSaleDetailVo source) {
        if (source == null) {
            return null;
        }
        AfterSaleDetailDto target = BeanCotyUtils.copyProperties(source, AfterSaleDetailDto.class);
        target.setProductInfo(toProductInfoDto(source.getProductInfo()));
        target.setTimeline(toTimelineDtos(source.getTimeline()));
        return target;
    }

    /**
     * 功能描述：将售后详情中的商品信息 VO 转换为 DTO。
     *
     * @param source 源商品信息 VO，类型为 {@link AfterSaleDetailVo.ProductInfo}
     * @return 返回商品信息 DTO；当 source 为空时返回 null
     * @throws RuntimeException 异常说明：当属性转换异常时抛出运行时异常
     */
    private AfterSaleDetailDto.ProductInfo toProductInfoDto(AfterSaleDetailVo.ProductInfo source) {
        return BeanCotyUtils.copyProperties(source, AfterSaleDetailDto.ProductInfo.class);
    }

    /**
     * 功能描述：将售后时间线 VO 列表转换为 DTO 列表。
     *
     * @param source 源时间线 VO 列表，元素类型为 {@link AfterSaleTimelineVo}
     * @return 返回时间线 DTO 列表；当 source 为空时返回空列表
     * @throws RuntimeException 异常说明：当属性转换异常时抛出运行时异常
     */
    private List<AfterSaleTimelineDto> toTimelineDtos(List<AfterSaleTimelineVo> source) {
        return BeanCotyUtils.copyListProperties(source, AfterSaleTimelineDto.class);
    }
}
