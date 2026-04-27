package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentDrugDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductDetailVo;
import com.zhangyichuang.medicine.agent.model.vo.admin.AgentProductListVo;
import com.zhangyichuang.medicine.agent.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
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

/**
 * 管理端智能体商品工具控制器。
 * <p>
 * 提供给管理端智能体使用的商品查询工具接口，
 * 支持商品搜索、列表查询和详情查询等功能。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/agent/admin/product")
@Tag(name = "管理端智能体商品工具", description = "用于管理端智能体商品查询接口")
@RequiredArgsConstructor
public class AgentProductController extends BaseController {

    private final MallProductService agentProductService;

    /**
     * 使用 Elasticsearch 搜索商品。
     * <p>
     * 支持按关键词、分类、标签、状态和价格区间搜索商品，
     * 返回管理端智能体可直接消费的商品分页结果。
     *
     * @param request 查询请求参数
     * @return 商品搜索分页数据
     */
    @GetMapping("/search")
    @Operation(summary = "商品搜索", description = "根据关键词和分类搜索商品")
    @PreAuthorize("hasAuthority('mall:product:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> searchProduct(MallProductListQueryRequest request) {
        MallProductListQueryRequest safeRequest = request == null ? new MallProductListQueryRequest() : request;
        Page<MallProductDetailDto> page = agentProductService.searchProducts(safeRequest);
        return buildProductTableResult(page);
    }

    /**
     * 根据条件分页查询商品列表。
     * <p>
     * 支持按关键词、分类等条件筛选商品，
     * 返回商品基本信息及封面图片，按分页形式返回。
     *
     * @param request 查询请求参数
     * @return 商品列表分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "根据关键词和分类搜索商品")
    @PreAuthorize("hasAuthority('mall:product:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> searchProducts(MallProductListQueryRequest request) {
        MallProductListQueryRequest safeRequest = request == null ? new MallProductListQueryRequest() : request;
        Page<MallProductDetailDto> page = agentProductService.listProducts(safeRequest);
        return buildProductTableResult(page);
    }

    /**
     * 根据商品 ID 批量查询商品详情。
     * <p>
     * 返回商品的详细信息，包括基本信息、分类、图片列表等，
     * 不包含药品的详细说明书信息。
     *
     * @param productIds 商品 ID 列表，支持批量查询
     * @return 商品详情列表
     */
    @GetMapping("/{productIds}")
    @Operation(summary = "获取商品详情", description = "根据商品ID获取详细信息（不含药品详情）")
    @PreAuthorize("hasAuthority('mall:product:query') or hasRole('super_admin')")
    public AjaxResult<List<AgentProductDetailVo>> getProductDetail(
            @Parameter(description = "商品ID")
            @PathVariable List<Long> productIds
    ) {
        return success(agentProductService.getProductDetail(productIds));
    }

    /**
     * 根据商品 ID 批量查询药品详情。
     * <p>
     * 返回商品的药品详细信息，包括适应症、用法用量、
     * 不良反应、注意事项、禁忌等药品说明书内容。
     *
     * @param productIds 商品 ID 列表，支持批量查询
     * @return 药品详情列表
     */
    @GetMapping("/drug/{productIds}")
    @Operation(summary = "获取药品详情", description = "根据商品ID获取药品详细信息")
    @PreAuthorize("hasAuthority('mall:product:query') or hasRole('super_admin')")
    public AjaxResult<List<AgentDrugDetailVo>> getDrugDetail(
            @Parameter(description = "商品ID")
            @PathVariable List<Long> productIds
    ) {
        return success(agentProductService.getDrugDetail(productIds));
    }

    /**
     * 将商品分页数据转换为管理端智能体表格结果。
     *
     * @param page 商品分页数据
     * @return 表格分页结果
     */
    private AjaxResult<TableDataResult> buildProductTableResult(Page<MallProductDetailDto> page) {
        List<AgentProductListVo> mallProductListVos = page.getRecords().stream()
                .map(product -> {
                    AgentProductListVo productListVo = copyProperties(product, AgentProductListVo.class);
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        productListVo.setCoverImage(product.getImages().getFirst());
                    }
                    return productListVo;
                })
                .toList();
        return getTableData(page, mallProductListVos);
    }
}
