package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.UserWallet;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * @author Chuang
 */
public interface UserWalletMapper extends BaseMapper<UserWallet> {

    /**
     * 按用户 ID 查询钱包并加写锁。
     *
     * @param userId 用户ID
     * @return 钱包实体
     */
    UserWallet selectWalletByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 原子扣减钱包余额（防超扣，防并发覆盖）
     *
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 影响行数，为0表示余额不足或用户钱包不存在
     */
    int deductBalanceAtomic(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子增加钱包余额（充值/退款）
     *
     * @param userId 用户ID
     * @param amount 增加金额
     * @return 影响行数
     */
    int addBalanceAtomic(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
