package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户收货地址请求参数
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户收货地址请求参数")
public class UserAddressRequest {

    @Schema(description = "地址ID，更新时必填", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "收货人手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String receiverPhone;

    @NotBlank(message = "地址不能为空")
    @Schema(description = "地址(省市区县街道等)", requiredMode = Schema.RequiredMode.REQUIRED, example = "广东省深圳市南山区科技园街道")
    private String address;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址(如小区名、栋号、门牌)", requiredMode = Schema.RequiredMode.REQUIRED, example = "科技南路XX号XX栋XX室")
    private String detailAddress;

    @Schema(description = "是否默认地址 1是 0否", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "0")
    private Integer isDefault;
}

