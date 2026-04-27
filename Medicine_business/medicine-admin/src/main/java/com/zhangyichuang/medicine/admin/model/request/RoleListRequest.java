package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(description = "角色列表查询参数")
@EqualsAndHashCode(callSuper = true)
public class RoleListRequest extends PageRequest {

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "角色标识", example = "admin")
    private String roleCode;

    @Schema(description = "状态：0启用 1禁用", example = "0")
    private Integer status;
}
