package com.zhangyichuang.medicine.admin.rpc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.admin.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import com.zhangyichuang.medicine.rpc.admin.AdminAgentUserRpcService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 Agent 用户 RPC Provider。
 */
@DubboService(interfaceClass = AdminAgentUserRpcService.class, group = "medicine-admin", version = "1.0.0")
@RequiredArgsConstructor
public class AdminAgentUserRpcServiceImpl implements AdminAgentUserRpcService {

    /**
     * AI Context 工具单次批量查询上限。
     */
    private static final int CONTEXT_BATCH_LIMIT = 20;

    /**
     * 用户账号正常状态编码。
     */
    private static final int USER_STATUS_NORMAL = 0;

    /**
     * 用户钱包冻结状态编码。
     */
    private static final int WALLET_STATUS_FROZEN = 1;

    private final UserService userService;
    private final UserWalletService userWalletService;

    @Override
    public Page<UserListDto> listUsers(UserListQueryRequest query) {
        return userService.listUser(query);
    }

    /**
     * 根据用户 ID 列表批量查询用户详情。
     *
     * @param userIds 用户 ID 列表
     * @return 用户详情列表
     */
    @Override
    public List<UserDetailDto> getUserDetailsByIds(List<Long> userIds) {
        Assert.notEmpty(userIds, "用户ID不能为空");
        return userIds.stream()
                .map(userService::getUserDetailById)
                .toList();
    }

    /**
     * 根据用户 ID 列表批量查询智能体用户上下文。
     *
     * @param userIds 用户 ID 列表
     * @return 按用户 ID 分组的用户上下文
     */
    @Override
    public Map<Long, UserContextDto> getUserContextsByIds(List<Long> userIds) {
        validateContextUserIds(userIds);
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        Assert.notEmpty(normalizedUserIds, "用户ID不能为空");
        Map<Long, User> userMap = loadUserMap(normalizedUserIds);
        Map<Long, UserWalletDto> walletMap = loadWalletMap(normalizedUserIds);
        Map<Long, UserContextDto> result = new LinkedHashMap<>();
        for (Long userId : normalizedUserIds) {
            User user = userMap.get(userId);
            Assert.notNull(user, "用户不存在: " + userId);
            result.put(userId, buildUserContext(user, walletMap.get(userId)));
        }
        return result;
    }

    /**
     * 根据用户 ID 列表批量查询钱包信息。
     *
     * @param userIds 用户 ID 列表
     * @return 钱包信息列表
     */
    @Override
    public List<UserWalletDto> getUserWalletsByUserIds(List<Long> userIds) {
        Assert.notEmpty(userIds, "用户ID不能为空");
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        Assert.notEmpty(normalizedUserIds, "用户ID不能为空");
        Map<Long, UserWalletDto> walletMap = loadWalletMap(normalizedUserIds);
        return normalizedUserIds.stream()
                .map(userId -> {
                    UserWalletDto wallet = walletMap.get(userId);
                    Assert.notNull(wallet, "用户钱包不存在: " + userId);
                    return wallet;
                })
                .toList();
    }

    @Override
    public Page<UserWalletFlowDto> getUserWalletFlow(Long userId, PageRequest request) {
        PageRequest safeRequest = request == null ? new PageRequest() : request;
        return userService.getUserWalletFlow(userId, safeRequest);
    }

    @Override
    public Page<UserConsumeInfoDto> getConsumeInfo(Long userId, PageRequest request) {
        PageRequest safeRequest = request == null ? new PageRequest() : request;
        return userService.getConsumeInfo(userId, safeRequest);
    }

    /**
     * 校验用户 Context 批量查询参数。
     *
     * @param userIds 用户 ID 列表
     */
    private void validateContextUserIds(List<Long> userIds) {
        Assert.notEmpty(userIds, "用户ID不能为空");
        Assert.isParamTrue(userIds.size() <= CONTEXT_BATCH_LIMIT, "用户ID最多支持20个");
    }

    /**
     * 归一化用户 ID 列表。
     *
     * @param userIds 原始用户 ID 列表
     * @return 去空、去重后的用户 ID 列表
     */
    private List<Long> normalizeUserIds(List<Long> userIds) {
        return userIds.stream()
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 批量加载用户基础信息并按用户 ID 索引。
     *
     * @param userIds 用户 ID 列表
     * @return 用户 ID 到用户实体的映射
     */
    private Map<Long, User> loadUserMap(List<Long> userIds) {
        List<User> users = userService.lambdaQuery()
                .in(User::getId, userIds)
                .list();
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载用户钱包基础信息并按用户 ID 索引。
     *
     * @param userIds 用户 ID 列表
     * @return 用户 ID 到钱包 DTO 的映射
     */
    private Map<Long, UserWalletDto> loadWalletMap(List<Long> userIds) {
        List<UserWallet> wallets = userWalletService.lambdaQuery()
                .in(UserWallet::getUserId, userIds)
                .list();
        if (CollectionUtils.isEmpty(wallets)) {
            return Map.of();
        }
        return wallets.stream()
                .filter(wallet -> wallet.getUserId() != null)
                .map(wallet -> BeanCotyUtils.copyProperties(wallet, UserWalletDto.class))
                .collect(Collectors.toMap(UserWalletDto::getUserId, wallet -> wallet,
                        (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 构建单个用户的智能体上下文。
     *
     * @param user   用户实体
     * @param wallet 用户钱包 DTO
     * @return 用户智能体上下文
     */
    private UserContextDto buildUserContext(User user, UserWalletDto wallet) {
        UserContextDto.BasicSummary basicSummary = buildBasicSummary(user);
        boolean accountDisabled = isAccountDisabled(basicSummary);
        boolean walletFrozen = wallet != null && Integer.valueOf(WALLET_STATUS_FROZEN).equals(wallet.getStatus());
        boolean missingBasicProfile = isMissingBasicProfile(basicSummary);
        return UserContextDto.builder()
                .userId(user.getId())
                .basicSummary(basicSummary)
                .orderSummary(null)
                .walletSummary(buildWalletSummary(wallet))
                .riskSummary(UserContextDto.RiskSummary.builder()
                        .accountDisabled(accountDisabled)
                        .walletFrozen(walletFrozen)
                        .missingBasicProfile(missingBasicProfile)
                        .build())
                .aiHints(UserContextDto.AiHints.builder()
                        .canQueryOrders(!accountDisabled)
                        .canOperateWallet(!accountDisabled && !walletFrozen)
                        .walletFrozen(walletFrozen)
                        .accountDisabled(accountDisabled)
                        .build())
                .build();
    }

    /**
     * 构建用户基础摘要。
     *
     * @param user 用户实体
     * @return 用户基础摘要
     */
    private UserContextDto.BasicSummary buildBasicSummary(User user) {
        return UserContextDto.BasicSummary.builder()
                .nickName(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .registerTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    /**
     * 构建用户钱包摘要。
     *
     * @param wallet 用户钱包 DTO
     * @return 用户钱包摘要
     */
    private UserContextDto.WalletSummary buildWalletSummary(UserWalletDto wallet) {
        if (wallet == null) {
            return null;
        }
        return UserContextDto.WalletSummary.builder()
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .freezeReason(wallet.getFreezeReason())
                .freezeTime(wallet.getFreezeTime())
                .totalIncome(wallet.getTotalIncome())
                .totalExpend(wallet.getTotalExpend())
                .build();
    }

    /**
     * 判断账号是否禁用。
     *
     * @param basicSummary 用户基础摘要
     * @return true 表示账号已禁用
     */
    private boolean isAccountDisabled(UserContextDto.BasicSummary basicSummary) {
        Integer status = basicSummary == null ? null : basicSummary.getStatus();
        return status != null && status != USER_STATUS_NORMAL;
    }

    /**
     * 判断是否缺少关键用户资料。
     *
     * @param basicSummary 用户基础摘要
     * @return true 表示缺少昵称或手机号
     */
    private boolean isMissingBasicProfile(UserContextDto.BasicSummary basicSummary) {
        if (basicSummary == null) {
            return true;
        }
        return !StringUtils.hasText(basicSummary.getNickName()) || !StringUtils.hasText(basicSummary.getPhoneNumber());
    }
}
