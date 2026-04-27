package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.UserWallet;

import java.math.BigDecimal;

/**
 * @author Chuang
 */
public interface UserWalletService extends IService<UserWallet> {

    /**
     * 开通钱包
     *
     * @param userId 用户ID
     * @return 是否开通成功
     */
    boolean openWallet(Long userId);

    /**
     * 充值钱包
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @param reason 充值原因
     * @return 是否充值成功
     */
    boolean rechargeWallet(Long userId, BigDecimal amount, String reason);

    /**
     * 扣减钱包余额
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     * @param reason 扣减原因
     * @return 是否扣减成功
     */
    boolean deductBalance(Long userId, BigDecimal amount, String reason);

    /**
     * 冻结钱包
     *
     * @param userId 用户ID
     * @param reason 冻结原因
     * @return 是否冻结成功
     */
    boolean freezeWallet(Long userId, String reason);

    /**
     * 解冻钱包
     *
     * @param userId 用户ID
     * @param reason 解冻原因
     * @return 是否解冻成功
     */
    boolean unfreezeWallet(Long userId, String reason);

    /**
     * 获取用户钱包信息
     *
     * @param userId 用户ID
     * @return 用户钱包信息
     */
    UserWallet getUserWalletByUserId(Long userId);

}
