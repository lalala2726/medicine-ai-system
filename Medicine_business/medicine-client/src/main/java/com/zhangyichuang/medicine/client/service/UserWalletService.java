package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.UserWalletBillRequest;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;

import java.math.BigDecimal;

/**
 * @author Chuang
 */
public interface UserWalletService extends IService<UserWallet> {

    /**
     * 获取用户钱包余额
     *
     * @return 钱包余额
     */
    BigDecimal getUserWalletBalance();

    /**
     * 获取用户钱包流水
     *
     * @param request 查询参数
     * @return 流水列表
     */
    Page<UserWalletLog> getBillList(UserWalletBillRequest request);

    /**
     * 获取当前用户钱包流水详情
     *
     * @param billId 流水ID
     * @return 流水详情
     */
    UserWalletLog getBillDetail(Long billId);

    /**
     * 扣除用户钱包余额
     *
     * @param userId 用户ID
     * @param amount 扣除金额
     * @param reason 扣除原因
     * @return 是否扣除成功
     */
    boolean deductBalance(Long userId, BigDecimal amount, String reason);

}
