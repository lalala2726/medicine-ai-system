package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.UserWalletMapper;
import com.zhangyichuang.medicine.admin.service.UserWalletLogService;
import com.zhangyichuang.medicine.admin.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.UUIDUtils;
import com.zhangyichuang.medicine.model.dto.UserWalletLogRecordDto;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包业务逻辑实现类（管理端）。
 * 提供钱包开通、余额充值（退款）、余额扣减、冻结与解冻等核心资金管理功能。
 * <p>
 * 安全特性：
 * 1. 余额变动采用数据库级原子更新（Atomic Update），杜绝并发覆盖和余额超扣风险。
 * 2. 核心扣款/充值逻辑受事务（@Transactional）保护，确保余额变动与账务流水的一致性。
 *
 * @author Chuang
 */
@Service
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet>
        implements UserWalletService {

    /**
     * 钱包状态：正常
     */
    private static final int WALLET_STATUS_NORMAL = 0;

    /**
     * 钱包状态：已冻结
     */
    private static final int WALLET_STATUS_FROZEN = 1;

    /**
     * 钱包账务流水服务
     */
    private final UserWalletLogService userWalletLogService;

    public UserWalletServiceImpl(UserWalletLogService userWalletLogService) {
        this.userWalletLogService = userWalletLogService;
    }

    /**
     * 为指定用户开通电子钱包。
     * 初始化余额、冻结金额为 0，并生成唯一的钱包编号。
     *
     * @param userId 用户唯一标识
     * @return 是否开通成功
     * @throws ServiceException 若用户已开通钱包
     */
    @Override
    public boolean openWallet(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        boolean exists = lambdaQuery()
                .eq(UserWallet::getUserId, userId)
                .count() > 0;
        if (exists) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "用户已开通钱包");
        }
        UserWallet userWallet = UserWallet.builder()
                .userId(userId)
                .walletNo(UUIDUtils.complex())
                .balance(BigDecimal.ZERO)
                .frozenBalance(BigDecimal.ZERO)
                .totalIncome(BigDecimal.ZERO)
                .totalExpend(BigDecimal.ZERO)
                .currency("CNY")
                .status(WALLET_STATUS_NORMAL)
                .remark("用户钱包开通成功")
                .build();
        return save(userWallet);
    }

    /**
     * 钱包余额充值（由管理员发起）。
     * 采用原子加操作，确保并发场景下余额增加的准确性。
     *
     * @param userId 用户唯一标识
     * @param amount 充值金额，必须大于 0
     * @param reason 充值原因/备注
     * @return 是否操作成功
     * @throws ServiceException 若账户状态异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rechargeWallet(Long userId, BigDecimal amount, String reason) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(amount, "充值金额不能为空");
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "充值金额必须大于0");
        Assert.notEmpty(reason, "充值原因不能为空");

        // 1. 获取操作前余额快照用于记录流水（不用于计算新余额，仅用于记录）
        UserWallet userWallet = getWalletByUserIdForUpdateOrThrow(userId);
        ensureWalletNotFrozen(userWallet);
        BigDecimal beforeBalance = safeAmount(userWallet.getBalance());

        // 2. 执行数据库级原子增加操作
        int rows = getBaseMapper().addBalanceAtomic(userId, amount);
        if (rows <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "充值失败，账户状态异常");
        }

        // 3. 计算操作后余额并异步/同步记录账务流水
        BigDecimal newBalance = beforeBalance.add(amount);
        recordWalletLog(userWallet, amount, beforeBalance, newBalance, reason, 1, "管理员操作-" + reason);
        return true;
    }

    /**
     * 钱包余额扣减（由管理员发起）。
     * 采用原子减操作，利用数据库行级锁实现余额充足性校验。
     *
     * @param userId 用户唯一标识
     * @param amount 扣减金额，必须大于 0
     * @param reason 扣减原因/备注
     * @return 是否操作成功
     * @throws ServiceException 若余额不足或账户被冻结
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount, String reason) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(amount, "扣减金额不能为空");
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "扣减金额必须大于0");
        Assert.notEmpty(reason, "扣减原因不能为空");

        // 1. 获取操作前快照
        UserWallet userWallet = getWalletByUserIdForUpdateOrThrow(userId);
        ensureWalletNotFrozen(userWallet);
        BigDecimal beforeBalance = safeAmount(userWallet.getBalance());

        // 2. 执行数据库原子扣减（关键：在 SQL 中判断 balance >= amount）
        int rows = getBaseMapper().deductBalanceAtomic(userId, amount);
        if (rows <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "扣款失败，余额不足或账户状态异常");
        }

        // 3. 记录变更流水
        BigDecimal afterBalance = beforeBalance.subtract(amount);
        recordWalletLog(userWallet, amount, beforeBalance, afterBalance, reason, 2, "管理员操作-" + reason);
        return true;
    }

    /**
     * 冻结钱包账户。
     * 冻结后账户将无法进行充值和扣款操作。
     *
     * @param userId 用户唯一标识
     * @param reason 冻结原因
     * @return 是否成功
     */
    @Override
    public boolean freezeWallet(Long userId, String reason) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(reason, "冻结原因不能为空");

        UserWallet userWallet = getWalletOrThrow(userId);
        if (isWalletFrozen(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包已冻结");
        }

        userWallet.setStatus(WALLET_STATUS_FROZEN);
        userWallet.setFreezeReason(reason);
        userWallet.setFreezeTime(new Date());
        userWallet.setRemark(reason);

        if (!updateById(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "冻结失败, 请稍后重试");
        }
        BigDecimal balance = safeAmount(userWallet.getBalance());
        recordWalletLog(userWallet, BigDecimal.ZERO, balance, balance, reason, 3, reason);
        return true;
    }

    /**
     * 解冻钱包账户。
     *
     * @param userId 用户唯一标识
     * @param reason 解冻原因/操作备注
     * @return 是否成功
     */
    @Override
    public boolean unfreezeWallet(Long userId, String reason) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(reason, "解冻原因不能为空");

        UserWallet userWallet = getWalletOrThrow(userId);
        if (!isWalletFrozen(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包未冻结");
        }

        userWallet.setStatus(WALLET_STATUS_NORMAL);
        userWallet.setFreezeReason(null);
        userWallet.setFreezeTime(null);
        userWallet.setRemark(reason);

        if (!updateById(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "解冻失败, 请稍后重试");
        }
        BigDecimal balance = safeAmount(userWallet.getBalance());
        recordWalletLog(userWallet, BigDecimal.ZERO, balance, balance, reason, 4, reason);
        return true;
    }

    /**
     * 根据用户 ID 查询钱包详情。
     *
     * @param userId 用户唯一标识
     * @return 钱包实体信息
     * @throws ServiceException 若钱包不存在
     */
    @Override
    public UserWallet getUserWalletByUserId(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        UserWallet userWallet = lambdaQuery()
                .eq(UserWallet::getUserId, userId)
                .one();
        if (userWallet == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "用户钱包不存在");
        }
        return userWallet;
    }

    /**
     * 获取钱包实体，不存在则抛出异常。
     */
    private UserWallet getWalletOrThrow(Long userId) {
        UserWallet userWallet = lambdaQuery()
                .eq(UserWallet::getUserId, userId)
                .one();
        if (userWallet == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "用户钱包不存在");
        }
        return userWallet;
    }

    /**
     * 按用户 ID 查询钱包并加写锁，不存在则抛出异常。
     *
     * @param userId 用户唯一标识
     * @return 加锁后的钱包实体
     */
    private UserWallet getWalletByUserIdForUpdateOrThrow(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        UserWallet userWallet = getBaseMapper().selectWalletByUserIdForUpdate(userId);
        if (userWallet == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "用户钱包不存在");
        }
        return userWallet;
    }

    /**
     * 校验钱包是否被冻结，若冻结则抛出业务异常。
     */
    private void ensureWalletNotFrozen(UserWallet userWallet) {
        if (isWalletFrozen(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包已冻结, 暂不可操作");
        }
    }

    /**
     * 判断钱包状态是否为冻结。
     */
    private boolean isWalletFrozen(UserWallet userWallet) {
        return Integer.valueOf(WALLET_STATUS_FROZEN).equals(userWallet.getStatus());
    }

    /**
     * 金额空安全处理：若为 null 则返回 BigDecimal.ZERO。
     */
    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 记录账务变更流水。
     *
     * @param wallet        钱包对象
     * @param amount        变动金额
     * @param beforeBalance 变动前余额
     * @param afterBalance  变动后余额
     * @param reason        业务原因
     * @param changeType    变更类型（1-收入，2-支出，3-冻结，4-解冻等）
     * @param remark        备注说明
     */
    private void recordWalletLog(UserWallet wallet, BigDecimal amount, BigDecimal beforeBalance,
                                 BigDecimal afterBalance, String reason, Integer changeType, String remark) {
        UserWalletLogRecordDto recordDto = UserWalletLogRecordDto.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUserId())
                .amount(amount)
                .beforeBalance(beforeBalance)
                .afterBalance(afterBalance)
                .reason(reason)
                .changeType(changeType)
                .remark(remark)
                .build();
        userWalletLogService.recordWalletLog(recordDto);
    }
}
