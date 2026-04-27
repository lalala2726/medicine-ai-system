package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色修改参数")
public class RoleUpdateRequest extends RoleAddRequest {

    @Schema(description = "角色ID", example = "1")
    @NotNull(message = "角色ID不能为空")
    @Min(value = 1L, message = "角色ID必须大于0")
    @Max(value = Long.MAX_VALUE, message = "角色ID过大")
    private Long id;
}
