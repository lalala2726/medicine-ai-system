package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.request.PermissionAddRequest;
import com.zhangyichuang.medicine.admin.model.request.PermissionUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.PermissionTreeVo;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.model.entity.Permission;

import java.util.List;
import java.util.Set;

public interface PermissionService extends IService<Permission> {

    /**
     * 获取权限树
     */
    List<PermissionTreeVo> permissionTree();

    /**
     * 根据ID获取权限详情
     */
    Permission getPermissionById(Long id);

    /**
     * 添加权限
     */
    boolean addPermission(PermissionAddRequest request);

    /**
     * 修改权限
     */
    boolean updatePermissionById(PermissionUpdateRequest request);

    /**
     * 批量删除权限
     */
    boolean deletePermissionByIds(List<Long> ids);

    /**
     * 获取权限选项列表
     */
    List<Option<Long>> permissionOption();

    /**
     * 根据用户ID获取权限列表
     */
    Set<String> getPermissionCodesByUserId(Long userId);
}
