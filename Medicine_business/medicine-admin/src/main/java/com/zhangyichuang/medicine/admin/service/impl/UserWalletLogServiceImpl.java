package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.mapper.UserWalletLogMapper;
import com.zhangyichuang.medicine.admin.service.UserWalletLogService;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.UUIDUtils;
import com.zhangyichuang.medicine.model.dto.UserWalletLogRecordDto;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author Chuang
 */
@Service
public class UserWalletLogServiceImpl extends ServiceImpl<UserWalletLogMapper, UserWalletLog>
        implements UserWalletLogService {

    private final UserWalletLogMapper userWalletLogMapper;

    public UserWalletLogServiceImpl(UserWalletLogMapper userWalletLogMapper) {
        this.userWalletLogMapper = userWalletLogMapper;
    }

    @Override
    public Page<UserWalletLog> getUserWalletFlow(Long userId, PageRequest request) {
        Page<UserWalletLog> page = request.toPage();
        return userWalletLogMapper.getBillPageByUserId(userId, page);
    }

    @Override
    public void recordWalletLog(UserWalletLogRecordDto recordDto) {
        Assert.notNull(recordDto, "钱包流水不能为空");
        Assert.notNull(recordDto.getWalletId(), "钱包ID不能为空");
        Assert.notNull(recordDto.getUserId(), "钱包用户不能为空");
        Assert.notNull(recordDto.getAmount(), "变动金额不能为空");
        Assert.notNull(recordDto.getReason(), "业务类型不能为空");
        Assert.notNull(recordDto.getChangeType(), "变动类型不能为空");

        UserWalletLog walletLog = UserWalletLog.builder()
                .walletId(recordDto.getWalletId())
                .userId(recordDto.getUserId())
                .flowNo(Optional.ofNullable(recordDto.getFlowNo()).orElse(UUIDUtils.complex()))
                .reason(recordDto.getReason())
                .bizId(recordDto.getBizId())
                .changeType(recordDto.getChangeType())
                .amount(recordDto.getAmount())
                .beforeBalance(Optional.ofNullable(recordDto.getBeforeBalance()).orElse(BigDecimal.ZERO))
                .afterBalance(Optional.ofNullable(recordDto.getAfterBalance()).orElse(BigDecimal.ZERO))
                .remark(recordDto.getRemark())
                .build();
        save(walletLog);
    }
}




