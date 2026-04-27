package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户收货地址视图对象
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户收货地址信息")
public class UserAddressVo {

    @Schema(description = "地址ID", example = "1")
    private Long id;

    @Schema(description = "收货人姓名", example = "张三")
    private String receiverName;

    @Schema(description = "收货人手机号", example = "13800138000")
    @DataMasking(type = MaskingType.MOBILE_PHONE)
    private String receiverPhone;

    @Schema(description = "地址(省市区县街道等)", example = "广东省深圳市南山区科技园街道")
    private String address;

    @Schema(description = "详细地址(如小区名、栋号、门牌)", example = "科技南路XX号XX栋XX室")
    private String detailAddress;

    @Schema(description = "是否默认地址 1是 0否", example = "1")
    private Integer isDefault;

    @Schema(description = "创建时间", example = "2025-11-13 10:00:00")
    private Date createTime;

    @Schema(description = "更新时间", example = "2025-11-13 10:00:00")
    private Date updateTime;
}

