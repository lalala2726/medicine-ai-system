package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.service.AnalyticsService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.vo.analytics.AnalyticsDashboardVo;
import com.zhangyichuang.medicine.model.vo.analytics.AnalyticsTrendVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营分析控制器
 * 提供电商运营看板相关接口
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/mall/analytics")
@Tag(name = "运营分析")
@PreAuthorize("hasAuthority('mall:analytics:query') or hasRole('super_admin')")
@RequiredArgsConstructor
@Validated
public class AnalyticsController extends BaseController {

    private final AnalyticsService analyticsService;


    /**
     * 获取运营看板
     *
     * @param days 最近天数
     * @return 运营看板
     */
    @GetMapping("/dashboard")
    @Operation(summary = "运营看板", description = "days 表示最近天数，默认30，范围1-730")
    public AjaxResult<AnalyticsDashboardVo> dashboard(
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(analyticsService.dashboard(days));
    }

    /**
     * 获取运营趋势
     *
     * @param days 最近天数
     * @return 趋势数据
     */
    @GetMapping("/trend")
    @Operation(summary = "运营趋势", description = "days 表示最近天数，默认30，范围1-730")
    public AjaxResult<AnalyticsTrendVo> trend(
            @RequestParam(defaultValue = "30") @Min(1) @Max(730) Integer days
    ) {
        return success(analyticsService.trend(days));
    }
}
