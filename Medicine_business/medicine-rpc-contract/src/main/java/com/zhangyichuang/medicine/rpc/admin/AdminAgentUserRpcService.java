package com.zhangyichuang.medicine.rpc.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 管理端 Agent 用户只读 RPC。
 */
public interface AdminAgentUserRpcService {

    /**
     * 分页查询用户列表。
     *
     * @param query 用户查询参数
     * @return 用户分页结果
     */
    Page<UserListDto> listUsers(UserListQueryRequest query);

    /**
     * 根据用户 ID 列表批量查询用户详情。
     *
     * @param userIds 用户 ID 列表
     * @return 用户详情列表
     */
    List<UserDetailDto> getUserDetailsByIds(List<Long> userIds);

    /**
     * 根据用户 ID 列表批量查询智能体用户上下文。
     *
     * @param userIds 用户 ID 列表
     * @return 按用户 ID 分组的用户上下文
     */
    Map<Long, UserContextDto> getUserContextsByIds(List<Long> userIds);

    /**
     * 根据用户 ID 列表批量查询钱包信息。
     *
     * @param userIds 用户 ID 列表
     * @return 钱包信息列表
     */
    List<UserWalletDto> getUserWalletsByUserIds(List<Long> userIds);

    /**
     * 分页查询用户钱包流水。
     *
     * @param userId  用户 ID
     * @param request 分页参数
     * @return 钱包流水分页结果
     */
    Page<UserWalletFlowDto> getUserWalletFlow(Long userId, PageRequest request);

    /**
     * 分页查询用户消费信息。
     *
     * @param userId  用户 ID
     * @param request 分页参数
     * @return 用户消费分页结果
     */
    Page<UserConsumeInfoDto> getConsumeInfo(Long userId, PageRequest request);
}
