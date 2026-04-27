package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.ActivationCodeRedeemRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.ActivationCodeRedeemVo;
import com.zhangyichuang.medicine.client.service.CouponActivationCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户端激活码控制器测试。
 */
@ExtendWith(MockitoExtension.class)
class CouponActivationCodeControllerTests {

    /**
     * 客户端激活码服务。
     */
    @Mock
    private CouponActivationCodeService couponActivationCodeService;

    /**
     * 被测控制器。
     */
    @InjectMocks
    private CouponActivationCodeController controller;

    /**
     * 验证兑换接口会返回统一响应结构。
     */
    @Test
    void redeemCurrentUserCode_ShouldReturnUnifiedResult() {
        ActivationCodeRedeemRequest request = new ActivationCodeRedeemRequest();
        request.setCode("ABCD1234EFGH5678");
        ActivationCodeRedeemVo resultVo = ActivationCodeRedeemVo.builder()
                .couponId(7001L)
                .couponName("新人100元券")
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(couponActivationCodeService.redeemCurrentUserCode(request)).thenReturn(resultVo);

        var result = controller.redeemCurrentUserCode(request);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(7001L, result.getData().getCouponId());
        verify(couponActivationCodeService).redeemCurrentUserCode(request);
    }
}
