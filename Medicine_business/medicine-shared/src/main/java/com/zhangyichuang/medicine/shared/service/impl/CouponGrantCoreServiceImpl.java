package com.zhangyichuang.medicine.shared.service.impl;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.coupon.CouponGrantCommand;
import com.zhangyichuang.medicine.model.coupon.CouponGrantResultDto;
import com.zhangyichuang.medicine.model.entity.CouponLog;
import com.zhangyichuang.medicine.model.entity.CouponTemplate;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.entity.UserCoupon;
import com.zhangyichuang.medicine.model.enums.CouponChangeTypeEnum;
import com.zhangyichuang.medicine.model.enums.CouponTemplateStatusEnum;
import com.zhangyichuang.medicine.model.enums.UserCouponStatusEnum;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponLogMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponTemplateMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicUserCouponMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicUserMapper;
import com.zhangyichuang.medicine.shared.service.CouponGrantCoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;

/**
 * 优惠券共享发放核心服务实现。
 */
@Service
@RequiredArgsConstructor
public class CouponGrantCoreServiceImpl implements CouponGrantCoreService {

    /**
     * 金额保留位数。
     */
    private static final int MONEY_SCALE = 2;

    /**
     * 用户启用状态。
     */
    private static final int USER_ENABLED_STATUS = 0;

    /**
     * 用户未删除标记。
     */
    private static final int USER_NOT_DELETED_FLAG = 0;

    /**
     * 零金额。
     */
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    /**
     * 基础优惠券模板 Mapper。
     */
    private final BasicCouponTemplateMapper couponTemplateMapper;

    /**
     * 基础用户优惠券 Mapper。
     */
    private final BasicUserCouponMapper userCouponMapper;

    /**
     * 基础优惠券日志 Mapper。
     */
    private final BasicCouponLogMapper couponLogMapper;

    /**
     * 基础用户 Mapper。
     */
    private final BasicUserMapper userMapper;

    /**
     * 按模板发放用户优惠券。
     *
     * @param command 发券命令
     * @return 发券结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponGrantResultDto grantCoupon(CouponGrantCommand command) {
        validateGrantCommand(command);
        CouponTemplate template = getActiveTemplate(command.getTemplateId());
        validateGrantUser(command.getUserId());
        validateEffectiveWindow(command.getEffectiveTime(), command.getExpireTime());

        Date now = new Date();
        UserCoupon userCoupon = UserCoupon.builder()
                .templateId(template.getId())
                .userId(command.getUserId())
                .couponNameSnapshot(template.getName())
                .thresholdAmount(normalizeAmount(template.getThresholdAmount()))
                .totalAmount(normalizeAmount(template.getFaceAmount()))
                .availableAmount(normalizeAmount(template.getFaceAmount()))
                .lockedConsumeAmount(ZERO_AMOUNT)
                .continueUseEnabled(template.getContinueUseEnabled())
                .stackableEnabled(template.getStackableEnabled())
                .effectiveTime(command.getEffectiveTime())
                .expireTime(command.getExpireTime())
                .couponStatus(resolveCouponStatus(command.getExpireTime(), now))
                .sourceType(command.getSourceType())
                .sourceBizNo(command.getSourceBizNo())
                .createTime(now)
                .updateTime(now)
                .createBy(command.getOperatorId())
                .updateBy(command.getOperatorId())
                .isDeleted(0)
                .build();
        boolean issued = userCouponMapper.insert(userCoupon) > 0;
        if (!issued) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发券入库失败");
        }

        CouponLog couponLog = CouponLog.builder()
                .couponId(userCoupon.getId())
                .userId(userCoupon.getUserId())
                .changeType(CouponChangeTypeEnum.GRANT.getType())
                .changeAmount(normalizeAmount(userCoupon.getTotalAmount()))
                .deductAmount(ZERO_AMOUNT)
                .wasteAmount(ZERO_AMOUNT)
                .beforeAvailableAmount(ZERO_AMOUNT)
                .afterAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                .sourceType(command.getSourceType())
                .sourceBizNo(command.getSourceBizNo())
                .remark(command.getRemark())
                .operatorId(command.getOperatorId())
                .createTime(now)
                .isDeleted(0)
                .build();
        couponLogMapper.insert(couponLog);

        return CouponGrantResultDto.builder()
                .couponId(userCoupon.getId())
                .templateId(userCoupon.getTemplateId())
                .userId(userCoupon.getUserId())
                .couponName(userCoupon.getCouponNameSnapshot())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .totalAmount(userCoupon.getTotalAmount())
                .availableAmount(userCoupon.getAvailableAmount())
                .effectiveTime(userCoupon.getEffectiveTime())
                .expireTime(userCoupon.getExpireTime())
                .couponStatus(userCoupon.getCouponStatus())
                .build();
    }

    /**
     * 校验发券命令。
     *
     * @param command 发券命令
     * @return 无返回值
     */
    private void validateGrantCommand(CouponGrantCommand command) {
        if (command == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券命令不能为空");
        }
        if (command.getTemplateId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠券模板ID不能为空");
        }
        if (command.getUserId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (command.getOperatorId() == null || command.getOperatorId().trim().isEmpty()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "操作人标识不能为空");
        }
        if (command.getSourceType() == null || command.getSourceType().trim().isEmpty()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "来源类型不能为空");
        }
        if (command.getSourceBizNo() == null || command.getSourceBizNo().trim().isEmpty()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "来源业务号不能为空");
        }
    }

    /**
     * 查询启用中的优惠券模板。
     *
     * @param templateId 优惠券模板ID
     * @return 优惠券模板实体
     */
    private CouponTemplate getActiveTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "优惠券模板不存在");
        }
        if (!Objects.equals(template.getStatus(), CouponTemplateStatusEnum.ACTIVE.getType())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "当前模板状态不允许发券");
        }
        return template;
    }

    /**
     * 校验发券用户。
     *
     * @param userId 用户ID
     * @return 无返回值
     */
    private void validateGrantUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getIsDelete(), USER_NOT_DELETED_FLAG)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "用户不存在");
        }
        if (!Objects.equals(user.getStatus(), USER_ENABLED_STATUS)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前用户状态不允许领券");
        }
    }

    /**
     * 校验有效时间窗口。
     *
     * @param effectiveTime 生效时间
     * @param expireTime    失效时间
     */
    private void validateEffectiveWindow(Date effectiveTime, Date expireTime) {
        if (effectiveTime == null || expireTime == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "生效时间和失效时间不能为空");
        }
        if (!effectiveTime.before(expireTime)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "失效时间必须晚于生效时间");
        }
    }

    /**
     * 解析用户优惠券状态。
     *
     * @param expireTime 失效时间
     * @param now        当前时间
     * @return 用户优惠券状态
     */
    private String resolveCouponStatus(Date expireTime, Date now) {
        if (expireTime.before(now)) {
            return UserCouponStatusEnum.EXPIRED.getType();
        }
        return UserCouponStatusEnum.AVAILABLE.getType();
    }

    /**
     * 归一化金额。
     *
     * @param amount 原始金额
     * @return 归一化后的金额
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return ZERO_AMOUNT;
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
