package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.RolePermissionUpdateRequest;
import com.zhangyichuang.medicine.model.entity.RolePermission;

import java.util.List;

public interface RolePermissionService extends IService<RolePermission> {

    /**
     * 获取角色拥有的权限ID
     */
    List<Long> getRolePermission(Long id);

    /**
     * 批量获取角色拥有的权限ID
     */
    List<Long> getRolePermission(List<Long> ids);

    /**
     * 获取全部权限ID
     */
    List<Long> getAllPermissionIds();

    /**
     * 更新角色权限
     */
    boolean updateRolePermission(RolePermissionUpdateRequest request);
}
