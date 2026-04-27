package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/12 13:33
 */
@Data
@Schema(description = "用户简略信息")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBriefVo {

    @Schema(description = "头像", example = "https://picsum.photos/200/300")
    private String avatarUrl;

    @Schema(description = "昵称", example = "张三")
    private String nickName;

    @Schema(description = "手机号", example = "13800000000")
    @DataMasking(type = MaskingType.MOBILE_PHONE)
    private String phoneNumber;

    @Schema(description = "账户余额", example = "100.00")
    private BigDecimal balance;

    @Schema(description = "优惠券数量", example = "10")
    private Integer couponCount;

    @Schema(description = "待支付订单数量", example = "10")
    private Integer payOrderCount;

    @Schema(description = "待发货订单数量", example = "10")
    private Integer deliverOrderCount;

    @Schema(description = "待收货订单数量", example = "10")
    private Integer receiveOrderCount;

    @Schema(description = "已完成订单数量", example = "10")
    private Integer completeOrderCount;

    @Schema(description = "退货/售后订单数量", example = "10")
    private Integer afterSaleOrderCount;

}
