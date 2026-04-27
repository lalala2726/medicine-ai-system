package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "权限树")
public class PermissionTreeVo {

    @Schema(description = "权限ID", example = "1")
    private long id;

    @Schema(description = "权限标识", example = "system:user:add")
    private String permissionCode;

    @Schema(description = "权限名称", example = "用户新增")
    private String permissionName;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态（0启用 1禁用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "系统用户新增权限")
    private String remark;

    @Schema(description = "创建时间", example = "2023-05-09 10:10:10")
    private Date createTime;

    @Schema(description = "子节点")
    private List<PermissionTreeVo> children;
}
