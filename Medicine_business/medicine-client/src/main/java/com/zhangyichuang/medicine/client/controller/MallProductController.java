package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.enums.ProductViewPeriod;
import com.zhangyichuang.medicine.client.model.dto.MallProductSearchResultDto;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductVo;
import com.zhangyichuang.medicine.client.service.MallProductService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.RecommendListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商城商品前台接口
 *
 * <p>聚焦商品详情、浏览统计等读操作。</p>
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/product")
@RequiredArgsConstructor
@Validated
@Tag(name = "商城商品前台接口")
public class MallProductController extends BaseController {

    private final MallProductService mallProductService;


    /**
     * 商品推荐
     *
     * @return 商品列表
     */
    @GetMapping("/recommend")
    @Operation(summary = "商品推荐")
    @Anonymous
    public AjaxResult<List<RecommendListVo>> recommend() {
        List<RecommendListVo> mallProducts = mallProductService.recommend();
        return success(mallProducts);
    }

    /**
     * 商品搜索
     */
    @GetMapping("/search")
    @Operation(summary = "搜索商品")
    @Anonymous
    public AjaxResult<TableDataResult> search(@Validated MallProductSearchRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            request.setKeyword(request.getKeyword().trim());
        }
        MallProductSearchResultDto result = mallProductService.searchWithTagFilters(request);
        return success(new TableDataResult(
                result.getPageResult().getRows(),
                result.getPageResult().getTotal(),
                result.getPageResult().getPageSize(),
                result.getPageResult().getPageNum(),
                Map.of("tagFilters", result.getTagFilters())
        ));
    }

    /**
     * 查询搜索筛选弹窗使用的全量商品标签。
     *
     * @return 按标签类型分组后的商品标签列表
     */
    @GetMapping("/search/tag-filters")
    @Operation(summary = "查询搜索筛选弹窗商品标签")
    @Anonymous
    public AjaxResult<List<MallProductSearchTagFilterVo>> listSearchTagFilters() {
        return success(mallProductService.listAllEnabledSearchTagFilters());
    }


    /**
     * 商品搜索建议
     *
     * @param keyword 关键字
     * @return 建议
     */
    @GetMapping("/search/suggest")
    @Operation(summary = "商品搜索建议")
    @Anonymous
    public AjaxResult<List<String>> suggest(@RequestParam("keyword") String keyword) {
        List<String> suggests = mallProductService.suggest(StringUtils.hasText(keyword) ? keyword.trim() : keyword);
        return success(suggests);
    }

    /**
     * 获取商品详情（包含图片和药品详情）
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取商品详情")
    public AjaxResult<MallProductVo> getMallProductById(@PathVariable @Min(value = 1, message = "商品ID不能小于1") Long id) {
        // 查询商品详情（包含图片和药品详情）
        MallProductVo mallProductVo = mallProductService.getMallProductDetail(id);
        // 记录商品浏览量
        mallProductService.recordView(id);

        return success(mallProductVo);
    }


    /**
     * 查询商品浏览量，可按小时/天/周/月等维度统计
     *
     * @param id     商品ID
     * @param period 统计周期（hour、day、week、month、total）
     * @return 浏览次数
     */
    @GetMapping("/{id}/views")
    @Operation(summary = "查询商品浏览量")
    @Anonymous
    public AjaxResult<Long> getProductViews(@PathVariable @Min(value = 1, message = "商品ID不能小于1") Long id,
                                            @Parameter(description = "统计周期（hour、day、week、month、total）")
                                            @RequestParam(value = "period", required = false, defaultValue = "total")
                                            String period) {
        ProductViewPeriod viewPeriod = ProductViewPeriod.fromCode(period);
        long count = mallProductService.getViewCount(id, viewPeriod);
        return success(count);
    }
}
