package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.UserWalletMapper;
import com.zhangyichuang.medicine.client.model.request.UserWalletBillRequest;
import com.zhangyichuang.medicine.client.service.UserWalletLogService;
import com.zhangyichuang.medicine.client.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.dto.UserWalletLogRecordDto;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @author Chuang
 */
@Service
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet>
        implements UserWalletService, BaseService {

    /**
     * 钱包流水不存在提示语。
     */
    private static final String WALLET_BILL_NOT_FOUND_MESSAGE = "账单记录不存在";

    /**
     * 钱包状态：已冻结。
     */
    private static final int WALLET_STATUS_FROZEN = 1;

    /**
     * 钱包账务流水服务。
     */
    private final UserWalletLogService userWalletLogService;

    public UserWalletServiceImpl(UserWalletLogService userWalletLogService) {
        this.userWalletLogService = userWalletLogService;
    }


    @Override
    public BigDecimal getUserWalletBalance() {
        Long userId = getUserId();
        UserWallet userWallet = getWalletOrThrow(userId);
        return userWallet.getBalance();
    }

    @Override
    public Page<UserWalletLog> getBillList(UserWalletBillRequest request) {
        Page<UserWalletLog> walletLogPage = request.toPage();
        Long userId = getUserId();
        return userWalletLogService.getBillPageByUserId(userId, request, walletLogPage);
    }

    /**
     * 获取当前用户钱包流水详情。
     *
     * @param billId 流水ID
     * @return 流水详情
     */
    @Override
    public UserWalletLog getBillDetail(Long billId) {
        Assert.notNull(billId, "流水ID不能为空");
        Long userId = getUserId();
        UserWalletLog walletLog = userWalletLogService.getBillDetailByUserId(userId, billId);
        if (walletLog == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, WALLET_BILL_NOT_FOUND_MESSAGE);
        }
        return walletLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount, String reason) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(amount, "扣减金额不能为空");
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "扣减金额必须大于0");
        Assert.notEmpty(reason, "原因不能为空");

        // 1. 获取扣减前快照用于记录流水
        UserWallet userWallet = getWalletByUserIdForUpdateOrThrow(userId);
        ensureWalletNotFrozen(userWallet);
        BigDecimal beforeBalance = safeAmount(userWallet.getBalance());

        // 2. 执行数据库原子扣减（关键安全点：此操作是线程排队的，且自带余额充足校验）
        int rows = getBaseMapper().deductBalanceAtomic(userId, amount);
        if (rows <= 0) {
            // 如果返回0，有两种可能：1.余额不足；2.钱包状态不是0（已冻结）
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "扣减失败，余额不足或账户状态异常");
        }

        // 3. 记录流水
        BigDecimal afterBalance = beforeBalance.subtract(amount);
        UserWalletLogRecordDto recordDto = UserWalletLogRecordDto.builder()
                .walletId(userWallet.getId())
                .userId(userId)
                .amount(amount)
                .beforeBalance(beforeBalance)
                .afterBalance(afterBalance)
                .reason(reason)
                .changeType(2)
                .remark("余额扣减-" + reason)
                .build();
        userWalletLogService.recordWalletLog(recordDto);
        return true;
    }


    /**
     * 根据用户 ID 查询钱包，不存在则抛出异常。
     *
     * @param userId 用户ID
     * @return 钱包实体
     */
    private UserWallet getWalletOrThrow(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");
        UserWallet userWallet = lambdaQuery()
                .eq(UserWallet::getUserId, userId)
                .one();
        if (userWallet == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "您的钱包还未开通, 请先开通钱包");
        }
        return userWallet;
    }

    /**
     * 根据用户 ID 查询钱包并加写锁，不存在则抛出异常。
     *
     * @param userId 用户ID
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
     * 校验钱包是否被冻结。
     *
     * @param userWallet 钱包实体
     */
    private void ensureWalletNotFrozen(UserWallet userWallet) {
        if (isWalletFrozen(userWallet)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "钱包已冻结, 暂不可操作");
        }
    }

    /**
     * 判断钱包是否为冻结状态。
     *
     * @param userWallet 钱包实体
     * @return 是否冻结
     */
    private boolean isWalletFrozen(UserWallet userWallet) {
        return Integer.valueOf(WALLET_STATUS_FROZEN).equals(userWallet.getStatus());
    }

    /**
     * 处理金额空值。
     *
     * @param value 原始金额
     * @return 非空金额
     */
    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
