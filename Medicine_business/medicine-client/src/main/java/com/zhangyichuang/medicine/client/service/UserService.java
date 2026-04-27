package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.dto.UserProfileDto;
import com.zhangyichuang.medicine.client.model.vo.UserBriefVo;
import com.zhangyichuang.medicine.model.entity.User;

import java.util.Set;

/**
 * @author Chuang
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据用户ID查询用户角色集合
     *
     * @param userId 用户ID
     * @return 用户角色集合
     */
    Set<String> getUserRolesByUserId(Long userId);

    /**
     * 根据用户名查询用户角色集合
     *
     * @param username 用户名
     * @return 用户角色集合
     */
    Set<String> getUserRolesByUserName(String username);

    /**
     * 更新用户登录信息
     *
     * @param userId 用户ID
     * @param ip     IP地址
     */
    void updateLoginInfo(Long userId, String ip);

    /**
     * 获取当前用户界面简述信息
     *
     * @return 当前用户界面简述
     */
    UserBriefVo getUserBriefInfo();

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    UserProfileDto getUserProfile();

    /**
     * 更新当前用户信息
     *
     * @param userProfileDto 用户信息
     * @return 是否更新成功
     */
    boolean updateUserProfile(UserProfileDto userProfileDto);
}
