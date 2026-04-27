package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/1
 */
@Data
@Schema(description = "地址更新参数")
public class AddressUpdateRequest {

    @Schema(description = "订单ID", example = "123456")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "收货人姓名", example = "张三")
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800138000")
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    @Schema(description = "收货人地址", example = "北京市朝阳区某某街道某某号")
    @NotBlank(message = "收货人地址不能为空")
    private String receiverAddress;

    @Schema(description = "配送方式", example = "快递配送")
    @NotBlank(message = "配送方式不能为空")
    private String deliveryType;


}
