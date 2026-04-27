package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.UserRole;

import java.util.Set;

public interface UserRoleService extends IService<UserRole> {

    /**
     * 获取用户角色ID。
     *
     * @param id 用户ID
     * @return 角色ID集合
     */
    Set<Long> getUserRoleByUserId(Long id);

    /**
     * 更新用户角色。
     *
     * @param id    用户ID
     * @param roles 角色ID集合
     */
    void updateUserRole(Long id, Set<Long> roles);

    /**
     * 根据角色ID查询绑定用户ID集合。
     *
     * @param roleId 角色ID
     * @return 用户ID集合
     */
    Set<Long> getUserIdsByRoleId(Long roleId);
}
