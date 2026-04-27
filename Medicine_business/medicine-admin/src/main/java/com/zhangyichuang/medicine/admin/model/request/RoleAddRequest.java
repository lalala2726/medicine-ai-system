package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色添加参数")
public class RoleAddRequest {

    @Schema(description = "角色标识", example = "admin")
    @Size(min = 1, max = 20, message = "角色标识长度为1-20个字符")
    @NotBlank(message = "角色标识不能为空")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    @Size(min = 1, max = 20, message = "角色名称长度为1-20个字符")
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "备注", example = "系统管理员角色")
    private String remark;

    @Schema(description = "状态：0启用 1禁用", example = "0")
    private Integer status;
}
