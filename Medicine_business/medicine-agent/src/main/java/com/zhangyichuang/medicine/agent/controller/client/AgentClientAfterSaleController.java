package com.zhangyichuang.medicine.agent.controller.client;

import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentAfterSaleDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentAfterSaleEligibilityVo;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentAfterSaleService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.AfterSaleDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentAfterSaleEligibilityDto;
import com.zhangyichuang.medicine.model.request.ClientAgentAfterSaleEligibilityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端智能体售后工具控制器。
 */
@RestController
@RequestMapping("/agent/client/after-sale")
@Tag(name = "客户端智能体售后工具", description = "用于客户端智能体售后查询接口")
@Validated
@RequiredArgsConstructor
public class AgentClientAfterSaleController extends BaseController {

    /**
     * 客户端智能体售后服务。
     */
    private final ClientAgentAfterSaleService clientAgentAfterSaleService;

    /**
     * 根据售后单号查询当前登录用户售后详情。
     *
     * @param afterSaleNo 售后单号
     * @return 售后详情
     */
    @GetMapping("/{afterSaleNo}")
    @Operation(summary = "获取售后详情", description = "根据售后单号获取当前登录用户的售后详情")
    public AjaxResult<ClientAgentAfterSaleDetailVo> getAfterSaleDetail(
            @Parameter(description = "售后单号", required = true)
            @PathVariable String afterSaleNo
    ) {
        AfterSaleDetailDto detail = clientAgentAfterSaleService.getAfterSaleDetail(afterSaleNo, getUserId());
        ClientAgentAfterSaleDetailVo target = copyProperties(detail, ClientAgentAfterSaleDetailVo.class);
        target.setProductInfo(copyProperties(detail.getProductInfo(), ClientAgentAfterSaleDetailVo.ProductInfo.class));
        target.setTimeline(copyListProperties(detail.getTimeline(), ClientAgentAfterSaleDetailVo.TimelineItem.class));
        return success(target);
    }

    /**
     * 校验当前登录用户订单或订单项是否满足售后资格。
     *
     * @param request 校验请求
     * @return 售后资格
     */
    @GetMapping("/eligibility")
    @Operation(summary = "校验售后资格", description = "校验当前登录用户订单或订单项是否满足售后资格")
    public AjaxResult<ClientAgentAfterSaleEligibilityVo> checkAfterSaleEligibility(
            @Validated ClientAgentAfterSaleEligibilityRequest request
    ) {
        ClientAgentAfterSaleEligibilityDto result = clientAgentAfterSaleService.checkAfterSaleEligibility(request, getUserId());
        return success(copyProperties(result, ClientAgentAfterSaleEligibilityVo.class));
    }
}
