package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.dto.UserOrderStatistics;
import com.zhangyichuang.medicine.admin.model.request.FreezeOrUnUserWalletRequest;
import com.zhangyichuang.medicine.admin.model.request.OpenUserWalletRequest;
import com.zhangyichuang.medicine.admin.model.request.WalletChangeRequest;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.constants.RolesConstant;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.token.RedisTokenStore;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.WalletChangeTypeEnum;
import com.zhangyichuang.medicine.model.request.UserAddRequest;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import com.zhangyichuang.medicine.model.request.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService, BaseService {

    /**
     * 正常启用的角色状态编码。
     */
    private static final int ROLE_STATUS_NORMAL = 0;

    private final UserWalletLogService userWalletLogService;
    private final MallOrderService mallOrderService;
    private final UserWalletService userWalletService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final CaptchaService captchaService;
    private final RedisTokenStore redisTokenStore;

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public UserDetailDto getUserDetailById(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        User user = getById(userId);
        Assert.notNull(user, "用户不存在");
        UserOrderStatistics userOrderStatistics = mallOrderService.getOrderStatisticsByUserId(userId);
        UserWallet userWallet = userWalletService.lambdaQuery()
                .eq(UserWallet::getUserId, userId)
                .one();

        BigDecimal walletBalance = userWallet == null ? BigDecimal.ZERO : defaultBigDecimal(userWallet.getBalance());
        long totalOrderCount = userOrderStatistics == null || userOrderStatistics.getTotalOrderCount() == null
                ? 0L : userOrderStatistics.getTotalOrderCount();
        BigDecimal totalConsumption = userOrderStatistics == null ? BigDecimal.ZERO
                : defaultBigDecimal(userOrderStatistics.getTotalConsumption());

        UserDetailDto.BasicInfo basicInfo = new UserDetailDto.BasicInfo();
        basicInfo.setUserId(user.getId());
        basicInfo.setRealName(user.getRealName());
        basicInfo.setPhoneNumber(user.getPhoneNumber());
        basicInfo.setEmail(user.getEmail());
        basicInfo.setGender(user.getGender());
        basicInfo.setIdCard(user.getIdCard());

        UserDetailDto.SecurityInfo securityInfo = new UserDetailDto.SecurityInfo();
        securityInfo.setRegisterTime(user.getCreateTime());
        securityInfo.setLastLoginTime(user.getLastLoginTime());
        securityInfo.setLastLoginIp(user.getLastLoginIp());
        securityInfo.setStatus(user.getStatus());

        UserDetailDto detail = new UserDetailDto();
        detail.setAvatar(user.getAvatar());
        detail.setNickName(user.getNickname());
        detail.setWalletBalance(walletBalance);
        detail.setTotalOrders(safeOrderCount(totalOrderCount));
        detail.setTotalConsume(totalConsumption);
        detail.setRoles(roleService.getRoleIdByUserId(userId));
        detail.setBasicInfo(basicInfo);
        detail.setSecurityInfo(securityInfo);
        return detail;
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public User getUserByUsername(String username) {
        LambdaQueryChainWrapper<User> eq = lambdaQuery().eq(User::getUsername, username);
        return eq.one();
    }

    /**
     * 根据用户ID查询用户角色集合
     *
     * @param userId 用户ID
     * @return 用户角色集合
     */
    @Override
    public Set<String> getUserRolesByUserId(Long userId) {
        return roleService.getUserRoleByUserId(userId);
    }

    /**
     * 根据用户名查询用户角色集合
     *
     * @param username 用户名
     * @return 用户角色集合
     */
    @Override
    public Set<String> getUserRolesByUserName(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            return Set.of();
        }
        return roleService.getUserRoleByUserId(user.getId());
    }

    /**
     * 获取用户列表
     *
     * @param request 列表查询参数
     * @return 返回用户分页
     */
    @Override
    public Page<UserListDto> listUser(UserListQueryRequest request) {
        Page<User> userPage = request.toPage();
        Page<User> result = baseMapper.listUser(userPage, request);
        Map<Long, String> userRoleNamesMap = buildUserRoleNamesMap(result.getRecords());
        List<UserListDto> rows = result.getRecords().stream()
                .map(user -> {
                    UserListDto userListDto = BeanCotyUtils.copyProperties(user, UserListDto.class);
                    userListDto.setRoles(userRoleNamesMap.getOrDefault(user.getId(), ""));
                    return userListDto;
                })
                .toList();
        Page<UserListDto> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(rows);
        return dtoPage;
    }

    /**
     * 添加用户
     *
     * @param request 用户添加请求对象
     * @return 添加结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(UserAddRequest request) {
        // 参数校验
        Assert.notNull(request, "用户添加请求对象不能为空");

        // 解析并校验待绑定角色，避免未授权用户创建超级管理员账号
        Set<Long> roleIds = resolveRoleIdsForCreate(request.getRoles());
        validateSuperAdminRoleAssignment(roleIds);

        // 转换请求对象为实体对象
        User user = BeanCotyUtils.copyProperties(request, User.class);

        // 加密密码
        String encryptPassword = encryptPassword(request.getPassword());
        user.setPassword(encryptPassword);

        // 保存用户信息
        boolean result = save(user);
        if (!result) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "添加用户失败");
        }

        // 建立用户与角色关联，若前端未传角色则默认分配 user
        userRoleService.updateUserRole(user.getId(), roleIds);

        // 开通钱包
        userWalletService.openWallet(user.getId());

        return true;
    }

    /**
     * 修改用户
     *
     * @param request 用户修改请求对象
     * @return 修改结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(UserUpdateRequest request) {
        Assert.notNull(request, "用户修改请求对象不能为空");
        Assert.notNull(request.getId(), "用户ID不能为空");
        User existingUser = getById(request.getId());
        Assert.notNull(existingUser, "用户不存在");

        Set<Long> currentRoleIds = userRoleService.getUserRoleByUserId(request.getId());
        Set<Long> requestedRoleIds = normalizeRoleIds(request.getRoles());
        boolean roleChanged = requestedRoleIds != null && !Objects.equals(currentRoleIds, requestedRoleIds);
        validateUserRoleChangePermission(request.getId(), requestedRoleIds, roleChanged);

        boolean hasUserInfoUpdate = hasUserInfoUpdate(request);
        boolean userInfoUpdated = false;
        if (hasUserInfoUpdate) {
            User user = BeanCotyUtils.copyProperties(request, User.class);
            if (request.getPassword() != null) {
                String password = request.getPassword();
                String encryptPassword = encryptPassword(password);
                user.setPassword(encryptPassword);
            }
            userInfoUpdated = updateById(user);
        }

        if (roleChanged) {
            roleService.isRoleExistById(requestedRoleIds);
            userRoleService.updateUserRole(request.getId(), requestedRoleIds);
            clearUserSessionsAfterCommit(request.getId());
        }
        return userInfoUpdated || roleChanged;
    }

    /**
     * 删除用户
     *
     * @param userId 用户id
     * @return 删除结果
     */
    @Override
    public boolean deleteUser(List<Long> userId) {
        Assert.notEmpty(userId, "用户ID不能为空");
        if (userId.contains(RolesConstant.SUPER_ADMIN_USER_ID)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "超级管理员账号禁止删除");
        }
        userRoleService.remove(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userId));
        return removeByIds(userId);
    }

    /**
     * 获取用户钱包流水
     *
     * @param request 查询参数
     * @return 用户钱包流水
     */
    @Override
    public Page<UserWalletFlowDto> getUserWalletFlow(Long userId, PageRequest request) {
        Page<UserWalletLog> userWalletFlow = userWalletLogService.getUserWalletFlow(userId, request);
        List<UserWalletFlowDto> walletFlowDtos = new ArrayList<>();

        AtomicLong atomicLong = new AtomicLong(1);
        userWalletFlow.getRecords().forEach(userWalletLog -> {
            // 获取变动类型：1收入、2支出、3冻结、4解冻
            Integer changeType = userWalletLog.getChangeType();
            // 判断是否为收入（使用枚举类的工具方法）
            Boolean isIncome = WalletChangeTypeEnum.isIncome(changeType);

            UserWalletFlowDto walletFlowDto = new UserWalletFlowDto();
            walletFlowDto.setIndex(atomicLong.getAndIncrement());
            walletFlowDto.setAfterBalance(userWalletLog.getAfterBalance());
            walletFlowDto.setAmount(userWalletLog.getAmount());
            walletFlowDto.setBeforeBalance(userWalletLog.getBeforeBalance());
            walletFlowDto.setChangeTime(userWalletLog.getCreatedAt());
            walletFlowDto.setChangeType(userWalletLog.getReason());
            walletFlowDto.setAmountDirection(changeType);
            walletFlowDto.setIsIncome(isIncome);
            walletFlowDtos.add(walletFlowDto);
        });
        Page<UserWalletFlowDto> page = new Page<>(userWalletFlow.getCurrent(), userWalletFlow.getSize(), userWalletFlow.getTotal());
        page.setRecords(walletFlowDtos);
        return page;

    }

    /**
     * 获取用户消费信息
     *
     * @param userId  用户id
     * @param request 查询参数
     * @return 用户消费信息
     */
    @Override
    public Page<UserConsumeInfoDto> getConsumeInfo(Long userId, PageRequest request) {
        Page<MallOrder> mallOrderPage = mallOrderService.getPaidOrderPage(userId, request);
        AtomicLong atomicLong = new AtomicLong(1);
        List<UserConsumeInfoDto> consumeInfoDtos = mallOrderPage.getRecords().stream()
                .map(order -> {
                    UserConsumeInfoDto consumeInfoDto = new UserConsumeInfoDto();
                    consumeInfoDto.setIndex(atomicLong.getAndIncrement());
                    consumeInfoDto.setOrderNo(order.getOrderNo());
                    consumeInfoDto.setPayPrice(order.getPayAmount());
                    consumeInfoDto.setFinishTime(order.getFinishTime());
                    consumeInfoDto.setTotalPrice(order.getTotalAmount());
                    consumeInfoDto.setUserId(order.getUserId());
                    return consumeInfoDto;
                })
                .toList();
        Page<UserConsumeInfoDto> page = new Page<>(mallOrderPage.getCurrent(), mallOrderPage.getSize(), mallOrderPage.getTotal());
        page.setRecords(consumeInfoDtos);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean openUserWallet(OpenUserWalletRequest request) {
        Assert.notNull(request, "钱包开通请求不能为空");
        validateWalletCaptcha(request.getCaptchaVerificationId());
        return userWalletService.openWallet(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeUserWallet(FreezeOrUnUserWalletRequest request) {
        Assert.notNull(request, "钱包冻结请求不能为空");
        validateWalletCaptcha(request.getCaptchaVerificationId());
        return userWalletService.freezeWallet(request.getUserId(), request.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeUserWallet(FreezeOrUnUserWalletRequest request) {
        Assert.notNull(request, "钱包解冻请求不能为空");
        validateWalletCaptcha(request.getCaptchaVerificationId());
        return userWalletService.unfreezeWallet(request.getUserId(), request.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean walletAmountChange(WalletChangeRequest request) {
        Assert.notNull(request, "请求参数不能为空");
        validateWalletCaptcha(request.getCaptchaVerificationId());
        Assert.notNull(request.getUserId(), "用户ID不能为空");
        Assert.notNull(request.getAmount(), "金额不能为空");
        Assert.isTrue(request.getAmount().compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
        Assert.notNull(request.getOperationType(), "操作类型不能为空");
        Assert.notEmpty(request.getReason(), "操作原因不能为空");

        User user = getById(request.getUserId());
        if (user == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "用户不存在");
        }

        return switch (request.getOperationType()) {
            case 1 -> userWalletService.rechargeWallet(request.getUserId(), request.getAmount(), request.getReason());
            case 2 -> userWalletService.deductBalance(request.getUserId(), request.getAmount(), request.getReason());
            default ->
                    throw new ServiceException(ResponseCode.PARAM_ERROR, "不支持的操作类型: " + request.getOperationType());
        };
    }

    @Override
    public User getUserById(Long userId) {
        return getById(userId);
    }

    @Override
    public UserWalletDto getUserWallet(Long userId) {
        UserWallet wallet = userWalletService.getUserWalletByUserId(userId);
        return BeanCotyUtils.copyProperties(wallet, UserWalletDto.class);
    }

    @Override
    public List<Option<Long>> listUserOptionsByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = userIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<User> users = lambdaQuery()
                .select(User::getId, User::getUsername)
                .in(User::getId, normalized)
                .list();
        Map<Long, String> userMap = users.stream()
                .filter(user -> user.getId() != null && StringUtils.isNotBlank(user.getUsername()))
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
        List<Option<Long>> options = new ArrayList<>();
        for (Long userId : normalized) {
            String username = userMap.get(userId);
            if (username != null) {
                options.add(new Option<>(userId, username));
            }
        }
        return options;
    }

    /**
     * 校验钱包敏感操作前置滑块验证码。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    private void validateWalletCaptcha(String captchaVerificationId) {
        captchaService.validateRequiredCaptcha(captchaVerificationId);
    }

    /**
     * 注册用户会话清理动作，保证角色变更提交成功后旧权限立即失效。
     *
     * @param userId 需要清理在线会话的用户ID
     */
    private void clearUserSessionsAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTokenStore.deleteSessionsByUserIds(Set.of(userId));
                }
            });
            return;
        }
        redisTokenStore.deleteSessionsByUserIds(Set.of(userId));
    }

    /**
     * 校验用户角色变更权限。
     *
     * @param targetUserId     被修改的用户ID
     * @param requestedRoleIds 前端提交的角色ID集合
     * @param roleChanged      角色是否真实发生变化
     */
    private void validateUserRoleChangePermission(Long targetUserId, Set<Long> requestedRoleIds, boolean roleChanged) {
        if (!roleChanged) {
            return;
        }
        Long currentUserId = getUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "不能修改自己的角色");
        }
        validateSuperAdminRoleAssignment(requestedRoleIds);
    }

    /**
     * 校验当前登录用户是否允许绑定超级管理员角色。
     *
     * @param requestedRoleIds 前端提交的目标角色ID集合
     */
    private void validateSuperAdminRoleAssignment(Set<Long> requestedRoleIds) {
        if (requestedRoleIds == null || !requestedRoleIds.contains(RolesConstant.SUPER_ADMIN_ROLE_ID)) {
            return;
        }
        if (!isCurrentUserSuperAdminRole()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "您无权将此用户设置为超级管理员");
        }
    }

    /**
     * 判断当前登录用户是否绑定超级管理员角色。
     *
     * @return true 表示当前用户拥有角色ID 1，false 表示没有
     */
    private boolean isCurrentUserSuperAdminRole() {
        Long currentUserId = getUserId();
        Set<Long> currentRoleIds = userRoleService.getUserRoleByUserId(currentUserId);
        return currentRoleIds != null && currentRoleIds.contains(RolesConstant.SUPER_ADMIN_ROLE_ID);
    }

    /**
     * 归一化用户更新请求中的角色ID集合。
     *
     * @param roles 前端提交的角色ID集合，null 表示不修改角色
     * @return 归一化后的角色ID集合，null 表示未提交角色字段
     */
    private Set<Long> normalizeRoleIds(Set<Long> roles) {
        if (roles == null) {
            return null;
        }
        roles.forEach(roleId -> Assert.isPositive(roleId, "角色ID必须大于0"));
        return Set.copyOf(roles);
    }

    /**
     * 判断用户更新请求中是否包含用户基础信息字段。
     *
     * @param request 用户修改请求对象
     * @return true 表示需要写入用户表，false 表示只处理角色字段
     */
    private boolean hasUserInfoUpdate(UserUpdateRequest request) {
        return request.getUsername() != null
                || request.getNickname() != null
                || request.getAvatar() != null
                || request.getPassword() != null
                || request.getStatus() != null
                || request.getIdCard() != null
                || request.getPhoneNumber() != null
                || request.getRealName() != null
                || request.getEmail() != null
                || request.getGender() != null;
    }

    /**
     * 批量构建用户角色名称展示文本。
     *
     * @param users 当前分页用户列表
     * @return 用户ID到逗号分隔启用角色名称的映射
     */
    private Map<Long, String> buildUserRoleNamesMap(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserRole> userRoles = userRoleService.lambdaQuery()
                .in(UserRole::getUserId, userIds)
                .list();
        if (userRoles == null || userRoles.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> roleNameMap = roleService.lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, ROLE_STATUS_NORMAL)
                .list()
                .stream()
                .filter(role -> role.getId() != null)
                .filter(role -> StringUtils.isNotBlank(role.getRoleName()))
                .collect(Collectors.toMap(Role::getId, role -> role.getRoleName().trim(),
                        (left, right) -> left, LinkedHashMap::new));
        if (roleNameMap.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> namesByUserId = new LinkedHashMap<>();
        for (UserRole userRole : userRoles) {
            String roleName = roleNameMap.get(userRole.getRoleId());
            if (userRole.getUserId() != null && StringUtils.isNotBlank(roleName)) {
                namesByUserId.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>()).add(roleName);
            }
        }
        return namesByUserId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .distinct()
                                .sorted()
                                .collect(Collectors.joining(",")),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * 获取非空金额值。
     *
     * @param value 待处理金额
     * @return 非空金额，入参为空时返回0
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 将订单数量转换为安全的 int 值。
     *
     * @param count 原始订单数量
     * @return int 范围内的订单数量
     */
    private int safeOrderCount(long count) {
        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (count < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) count;
    }

    /**
     * 解析新建用户的角色ID集合。
     *
     * @param roles 前端提交的角色ID集合
     * @return 新建用户最终绑定的角色ID集合
     */
    private Set<Long> resolveRoleIdsForCreate(Set<Long> roles) {
        if (roles != null && !roles.isEmpty()) {
            roleService.isRoleExistById(roles);
            return roles;
        }

        Role userRole = roleService.lambdaQuery()
                .eq(Role::getRoleCode, RolesConstant.USER)
                .eq(Role::getStatus, ROLE_STATUS_NORMAL)
                .one();
        if (userRole == null || userRole.getId() == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "默认用户角色不存在，请先初始化RBAC数据");
        }
        return Set.of(userRole.getId());
    }

}
