package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.UserCouponListRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.UserCouponVo;
import com.zhangyichuang.medicine.client.service.UserCouponService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端优惠券控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupon")
@Tag(name = "优惠券管理", description = "客户端优惠券接口")
@PreventDuplicateSubmit
public class CouponController extends BaseController {

    /**
     * 用户优惠券服务。
     */
    private final UserCouponService userCouponService;

    /**
     * 查询当前用户优惠券列表。
     *
     * @param request 查询请求
     * @return 当前用户优惠券列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询当前用户优惠券列表")
    public AjaxResult<TableDataResult> list(UserCouponListRequest request) {
        Page<UserCouponVo> page = userCouponService.listCurrentUserCoupons(request);
        return getTableData(page, page.getRecords());
    }

    /**
     * 删除当前用户优惠券。
     *
     * @param couponId 用户优惠券ID
     * @return 删除结果
     */
    @DeleteMapping("/{couponId}")
    @Operation(summary = "删除当前用户优惠券")
    public AjaxResult<Void> delete(@PathVariable Long couponId) {
        return toAjax(userCouponService.deleteCurrentUserCoupon(couponId));
    }
}
