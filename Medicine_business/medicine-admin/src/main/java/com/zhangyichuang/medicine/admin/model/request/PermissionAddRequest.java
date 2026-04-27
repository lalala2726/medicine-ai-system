package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "权限添加参数")
public class PermissionAddRequest {

    @Schema(description = "父权限ID", example = "0")
    private Long parentId;

    @Schema(description = "权限标识", example = "system:user:add")
    @NotBlank(message = "权限标识不能为空")
    private String permissionCode;

    @Schema(description = "权限名称", example = "用户新增")
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态（0启用 1禁用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "系统用户新增权限")
    private String remark;
}
