package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.ViewHistoryRequest;
import com.zhangyichuang.medicine.client.model.vo.ViewHistoryVo;
import com.zhangyichuang.medicine.client.service.MallProductViewHistoryService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品浏览历史接口
 *
 * <p>提供浏览记录的查询、记录与清理能力，方便用户查看自己的浏览足迹。</p>
 */
@RestController
@RequestMapping("/mall/product/history")
@Tag(name = "商品浏览历史", description = "商品浏览记录相关接口")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class ViewHistoryController extends BaseController {

    private final MallProductViewHistoryService viewHistoryService;

    /**
     * 分页查询当前用户的商品浏览历史
     *
     * @param request 分页参数
     * @return 浏览历史分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "查询浏览历史")
    public AjaxResult<TableDataResult> list(ViewHistoryRequest request) {
        Long userId = getUserId();
        Page<ViewHistoryVo> page = viewHistoryService.listViewHistory(userId, request);
        return getTableData(page);
    }

    /**
     * 记录一次商品浏览
     *
     * @param productId 商品ID
     * @return 操作结果
     */
    @PostMapping("/{productId}")
    @Operation(summary = "记录商品浏览")
    public AjaxResult<Void> record(@PathVariable("productId") Long productId) {
        Long userId = getUserId();
        viewHistoryService.recordViewHistory(userId, productId);
        return success();
    }

    /**
     * 删除指定商品的浏览记录
     *
     * @param productId 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/{productId}")
    @Operation(summary = "删除单条浏览记录")
    public AjaxResult<Void> delete(@PathVariable("productId") Long productId) {
        Long userId = getUserId();
        viewHistoryService.deleteViewHistory(userId, productId);
        return success();
    }

    /**
     * 清空当前用户的浏览记录
     *
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清空浏览记录")
    public AjaxResult<Void> clear() {
        Long userId = getUserId();
        viewHistoryService.deleteAllViewHistory(userId);
        return success();
    }
}
