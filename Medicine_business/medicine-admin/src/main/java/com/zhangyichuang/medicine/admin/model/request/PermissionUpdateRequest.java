package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限修改参数")
public class PermissionUpdateRequest extends PermissionAddRequest {

    @Schema(description = "权限ID", example = "1")
    @NotNull(message = "权限ID不能为空")
    @Min(value = 1L, message = "权限ID必须大于0")
    @Max(value = Long.MAX_VALUE, message = "权限ID过大")
    private Long id;
}
