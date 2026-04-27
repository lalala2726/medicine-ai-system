package com.zhangyichuang.medicine.agent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.service.UserService;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentUserRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 用户服务 Dubbo Consumer 实现。
 */
@Service
public class UserServiceImpl implements UserService {

    @DubboReference(group = "medicine-admin", version = "1.0.0", check = false, timeout = 10000, retries = 0,
            url = "${dubbo.references.medicine-admin.url:}")
    private AdminAgentUserRpcService adminAgentUserRpcService;

    @Override
    public Page<UserListDto> listUsers(UserListQueryRequest request) {
        return adminAgentUserRpcService.listUsers(request);
    }

    /**
     * 根据用户 ID 列表批量查询用户详情。
     *
     * @param userIds 用户 ID 列表
     * @return 用户详情列表
     */
    @Override
    public List<UserDetailDto> getUserDetailsByIds(List<Long> userIds) {
        return adminAgentUserRpcService.getUserDetailsByIds(userIds);
    }

    /**
     * 根据用户 ID 列表批量查询智能体用户上下文。
     *
     * @param userIds 用户 ID 列表
     * @return 按用户 ID 分组的用户上下文
     */
    @Override
    public Map<Long, UserContextDto> getUserContextsByIds(List<Long> userIds) {
        return adminAgentUserRpcService.getUserContextsByIds(userIds);
    }

    /**
     * 根据用户 ID 列表批量查询用户钱包信息。
     *
     * @param userIds 用户 ID 列表
     * @return 用户钱包信息列表
     */
    @Override
    public List<UserWalletDto> getUserWalletsByUserIds(List<Long> userIds) {
        return adminAgentUserRpcService.getUserWalletsByUserIds(userIds);
    }

    @Override
    public Page<UserWalletFlowDto> getUserWalletFlow(Long userId, PageRequest request) {
        PageRequest safeRequest = request == null ? new PageRequest() : request;
        return adminAgentUserRpcService.getUserWalletFlow(userId, safeRequest);
    }

    @Override
    public Page<UserConsumeInfoDto> getConsumeInfo(Long userId, PageRequest request) {
        PageRequest safeRequest = request == null ? new PageRequest() : request;
        return adminAgentUserRpcService.getConsumeInfo(userId, safeRequest);
    }
}
