package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "角色列表")
public class RoleListVo {

    @Schema(description = "角色ID", example = "1")
    private long id;

    @Schema(description = "角色标识", example = "admin")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "备注", example = "系统管理员角色")
    private String remark;

    @Schema(description = "状态：0启用 1禁用", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-01-01 10:00:00")
    private Date createTime;
}
