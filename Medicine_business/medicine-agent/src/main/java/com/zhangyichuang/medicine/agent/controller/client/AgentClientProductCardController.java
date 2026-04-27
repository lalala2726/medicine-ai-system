package com.zhangyichuang.medicine.agent.controller.client;

import com.zhangyichuang.medicine.agent.model.request.ClientAgentProductPurchaseCardsRequest;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductCardsVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductPurchaseCardsVo;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentProductService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductCardsDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductPurchaseCardsDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductPurchaseQueryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户端智能体商品卡片控制器。
 */
@RestController
@RequestMapping("/agent/client/card")
@Tag(name = "客户端智能体商品卡片工具", description = "用于客户端智能体商品卡片补全接口")
@Validated
@RequiredArgsConstructor
public class AgentClientProductCardController extends BaseController {

    /**
     * 客户端智能体商品服务。
     */
    private final ClientAgentProductService clientAgentProductService;

    /**
     * 根据商品ID列表查询商品卡片补全结果。
     *
     * @param productIds 商品ID列表
     * @return 商品卡片补全结果
     */
    @GetMapping("/product/{productIds}")
    @Operation(summary = "获取商品卡片", description = "根据商品ID列表获取客户端智能体商品卡片补全结果")
    public AjaxResult<ClientAgentProductCardsVo> getProductCards(@PathVariable List<Long> productIds) {
        ClientAgentProductCardsDto cards = clientAgentProductService.getProductCards(productIds);
        ClientAgentProductCardsVo target = copyProperties(cards, ClientAgentProductCardsVo.class);
        target.setItems(copyListProperties(cards.getItems(), ClientAgentProductCardsVo.ClientAgentProductItemVo.class));
        return success(target);
    }

    /**
     * 根据商品ID和数量查询商品购买卡片。
     *
     * @param request 商品购买卡片请求
     * @return 商品购买卡片结果
     */
    @PostMapping("/purchase_cards")
    @Operation(summary = "获取商品购买卡片", description = "根据商品ID和数量获取客户端智能体商品购买卡片")
    public AjaxResult<ClientAgentProductPurchaseCardsVo> getProductPurchaseCards(
            @Valid @RequestBody ClientAgentProductPurchaseCardsRequest request
    ) {
        List<ClientAgentProductPurchaseQueryDto> items = request.getItems().stream()
                .map(item -> ClientAgentProductPurchaseQueryDto.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        ClientAgentProductPurchaseCardsDto cards = clientAgentProductService.getProductPurchaseCards(items);
        ClientAgentProductPurchaseCardsVo target = copyProperties(cards, ClientAgentProductPurchaseCardsVo.class);
        target.setItems(copyListProperties(cards.getItems(), ClientAgentProductPurchaseCardsVo.ClientAgentProductPurchaseItemVo.class));
        return success(target);
    }
}
