package com.zhangyichuang.medicine.agent.controller.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.request.ClientAgentProductDetailsRequest;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchTagFilterOptionVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchTagFilterVo;
import com.zhangyichuang.medicine.agent.model.vo.client.ClientAgentProductSearchVo;
import com.zhangyichuang.medicine.agent.service.client.ClientAgentProductService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductDetailDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductSearchDto;
import com.zhangyichuang.medicine.model.dto.ClientAgentProductSearchTagFilterDto;
import com.zhangyichuang.medicine.model.request.ClientAgentProductSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户端智能体商品工具控制器。
 */
@RestController
@RequestMapping("/agent/client/product")
@Tag(name = "客户端智能体商品工具", description = "用于客户端智能体商品查询接口")
@Validated
@RequiredArgsConstructor
public class AgentClientProductController extends BaseController {


    /**
     * 客户端智能体商品服务。
     */
    private final ClientAgentProductService clientAgentProductService;

    /**
     * 按关键词搜索商品，供 AI 先定位商品后再进行详情查询。
     *
     * @param request 搜索参数
     * @return 商品搜索结果
     */
    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "按关键词分页搜索商品")
    public AjaxResult<TableDataResult> searchProducts(@Validated ClientAgentProductSearchRequest request) {
        ClientAgentProductSearchRequest safeRequest = ClientAgentProductSearchRequest.sanitize(request);
        Page<ClientAgentProductSearchDto> page = clientAgentProductService.searchProducts(safeRequest);
        List<ClientAgentProductSearchVo> rows = copyListProperties(page, ClientAgentProductSearchVo.class);
        return getTableData(page, rows);
    }

    /**
     * 查询商品搜索标签目录，供 AI 组织更精确的商品搜索词。
     *
     * @return 标签分组列表
     */
    @GetMapping("/search/tag-filters")
    @Operation(summary = "获取商品搜索标签目录", description = "查询客户端智能体可用的商品搜索标签目录")
    public AjaxResult<List<ClientAgentProductSearchTagFilterVo>> listProductSearchTagFilters() {
        List<ClientAgentProductSearchTagFilterDto> filterGroups = clientAgentProductService.listProductSearchTagFilters();
        List<ClientAgentProductSearchTagFilterVo> rows = copyListProperties(
                filterGroups,
                ClientAgentProductSearchTagFilterVo.class
        );
        for (int index = 0; index < rows.size(); index++) {
            ClientAgentProductSearchTagFilterVo target = rows.get(index);
            ClientAgentProductSearchTagFilterDto source = filterGroups.get(index);
            target.setOptions(copyListProperties(
                    source.getOptions(),
                    ClientAgentProductSearchTagFilterOptionVo.class
            ));
        }
        return success(rows);
    }

    /**
     * 批量查询统一药品详情。
     *
     * @param request 商品ID列表请求
     * @return 商品详情列表
     */
    @PostMapping("/details")
    @Operation(summary = "批量获取药品详情", description = "根据商品ID列表批量获取客户端智能体统一药品详情")
    public AjaxResult<List<ClientAgentProductDetailVo>> getProductDetails(
            @Valid @RequestBody ClientAgentProductDetailsRequest request
    ) {
        List<ClientAgentProductDetailDto> details = clientAgentProductService.getProductDetails(request.getProductIds());
        return success(copyListProperties(details, ClientAgentProductDetailVo.class));
    }
}
