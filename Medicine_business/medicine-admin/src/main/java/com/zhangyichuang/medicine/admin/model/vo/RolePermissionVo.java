package com.zhangyichuang.medicine.admin.model.vo;

import com.zhangyichuang.medicine.common.core.base.Option;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "角色权限信息")
public class RolePermissionVo {

    @Schema(description = "权限选项")
    private List<Option<Long>> permissionOption;

    @Schema(description = "角色已有权限ID")
    private List<Long> rolePermission;
}
