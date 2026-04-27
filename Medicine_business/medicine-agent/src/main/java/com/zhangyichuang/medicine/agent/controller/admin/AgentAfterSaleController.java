package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleListVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentAfterSaleTimelineVo;
import com.zhangyichuang.medicine.agent.service.MallAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.AfterSaleContextDto;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.MallAfterSaleListDto;
import com.zhangyichuang.medicine.model.request.MallAfterSaleListRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端智能体售后工具控制器。
 * <p>
 * 提供给管理端智能体使用的售后查询工具接口，
 * 支持售后列表与售后详情查询。
 */
@RestController
@RequestMapping("/agent/admin/after-sale")
@Tag(name = "管理端智能体售后工具", description = "用于管理端智能体售后查询接口")
@RequiredArgsConstructor
public class AgentAfterSaleController extends BaseController {


    private final MallAfterSaleService mallAfterSaleService;

    /**
     * 分页查询售后列表。
     * <p>
     * 功能描述：分页查询售后申请列表，并在控制层将 DTO 转换为智能体专用 VO。
     *
     * @param request 查询参数，包含分页信息与筛选条件；允许为空
     * @return 返回售后分页数据，rows 为 {@link AgentAfterSaleListVo} 集合
     * @throws RuntimeException 异常说明：当下游服务调用或数据转换失败时抛出运行时异常
     */
    @GetMapping("/list")
    @Operation(summary = "售后列表", description = "分页查询售后申请列表")
    @PreAuthorize("hasAuthority('mall:after_sale:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listAfterSales(MallAfterSaleListRequest request) {
        MallAfterSaleListRequest safeRequest = request == null ? new MallAfterSaleListRequest() : request;
        Page<MallAfterSaleListDto> page = mallAfterSaleService.listAfterSales(safeRequest);
        List<AgentAfterSaleListVo> vos = copyListProperties(page, AgentAfterSaleListVo.class);
        return getTableData(page, vos);
    }

    /**
     * 批量查询售后聚合上下文。
     * <p>
     * 功能描述：根据售后单号批量查询智能体优先使用的售后摘要，
     * 用于判断售后状态、处理节点、凭证数量和下一步动作。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回按售后单号分组的售后聚合上下文
     * @throws RuntimeException 异常说明：当售后上下文查询失败或参数异常时抛出运行时异常
     */
    @GetMapping("/context/{afterSaleNos}")
    @Operation(summary = "售后聚合上下文", description = "根据售后单号批量查询智能体售后聚合上下文")
    @PreAuthorize("hasAuthority('mall:after_sale:query') or hasRole('super_admin')")
    public AjaxResult<Map<String, AfterSaleContextDto>> getAfterSaleContext(
            @Parameter(description = "售后单号，多个可用逗号分隔", required = true)
            @PathVariable List<String> afterSaleNos) {
        return success(mallAfterSaleService.getAfterSaleContexts(afterSaleNos));
    }

    /**
     * 批量查询售后详情。
     * <p>
     * 功能描述：根据售后单号批量查询售后详情并转换为智能体详情 VO。
     *
     * @param afterSaleNos 售后单号列表
     * @return 返回智能体售后详情对象列表
     * @throws RuntimeException 异常说明：当售后详情查询失败或数据转换失败时抛出运行时异常
     */
    @GetMapping("/detail/{afterSaleNos}")
    @Operation(summary = "售后详情", description = "根据售后单号批量查询售后详情")
    @PreAuthorize("hasAuthority('mall:after_sale:query') or hasRole('super_admin')")
    public AjaxResult<List<AgentAfterSaleDetailVo>> getAfterSaleDetail(
            @Parameter(description = "售后单号，多个可用逗号分隔", required = true)
            @PathVariable List<String> afterSaleNos) {
        List<AgentAfterSaleDetailVo> detailVos = mallAfterSaleService.getAfterSaleDetails(afterSaleNos).stream()
                .map(this::buildAfterSaleDetailVo)
                .toList();
        return success(detailVos);
    }

    /**
     * 将售后详情 DTO 转换为智能体售后详情 VO。
     *
     * @param detailDto 售后详情 DTO
     * @return 智能体售后详情 VO
     */
    private AgentAfterSaleDetailVo buildAfterSaleDetailVo(AfterSaleDetailDto detailDto) {
        AgentAfterSaleDetailVo detailVo = copyProperties(detailDto, AgentAfterSaleDetailVo.class);
        detailVo.setProductInfo(copyProperties(detailDto.getProductInfo(), AgentAfterSaleDetailVo.ProductInfo.class));
        detailVo.setTimeline(copyListProperties(detailDto.getTimeline(), AgentAfterSaleTimelineVo.class));
        return detailVo;
    }

}
