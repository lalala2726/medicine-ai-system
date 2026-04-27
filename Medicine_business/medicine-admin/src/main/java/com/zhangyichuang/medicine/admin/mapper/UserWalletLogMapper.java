package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
public interface UserWalletLogMapper extends BaseMapper<UserWalletLog> {

    /**
     * 获取用户钱包流水
     *
     * @param userId 用户ID
     * @param page   分页参数
     * @return 用户钱包流水
     */
    Page<UserWalletLog> getBillPageByUserId(@Param("userId") Long userId, Page<UserWalletLog> page);

}




