package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.ActivationCodeRedeemRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.ActivationCodeRedeemVo;
import com.zhangyichuang.medicine.client.service.CouponActivationCodeService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端激活码控制器。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/coupon/activation-code")
@Tag(name = "激活码管理", description = "客户端激活码接口")
@PreventDuplicateSubmit
public class CouponActivationCodeController extends BaseController {

    /**
     * 客户端激活码服务。
     */
    private final CouponActivationCodeService couponActivationCodeService;

    /**
     * 兑换当前用户激活码。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    @PostMapping("/redeem")
    @Operation(summary = "兑换当前用户激活码")
    public AjaxResult<ActivationCodeRedeemVo> redeemCurrentUserCode(
            @Validated @RequestBody ActivationCodeRedeemRequest request) {
        return success(couponActivationCodeService.redeemCurrentUserCode(request));
    }
}
