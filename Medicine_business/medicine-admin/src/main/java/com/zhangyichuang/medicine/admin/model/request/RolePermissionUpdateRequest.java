package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色权限更新参数")
public class RolePermissionUpdateRequest {

    @Schema(description = "角色ID", example = "1")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "权限ID列表", example = "[1,2,3]")
    private List<Long> permissionIds;
}
