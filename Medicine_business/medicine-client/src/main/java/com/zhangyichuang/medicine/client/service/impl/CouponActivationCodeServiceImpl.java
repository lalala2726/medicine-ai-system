package com.zhangyichuang.medicine.client.service.impl;

import com.zhangyichuang.medicine.client.model.request.ActivationCodeRedeemRequest;
import com.zhangyichuang.medicine.client.model.vo.coupon.ActivationCodeRedeemVo;
import com.zhangyichuang.medicine.client.service.CouponActivationCodeService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.IpAddressUtils;
import com.zhangyichuang.medicine.common.core.utils.UUIDUtils;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.coupon.ActivationRedeemCodeDto;
import com.zhangyichuang.medicine.model.coupon.CouponGrantCommand;
import com.zhangyichuang.medicine.model.coupon.CouponGrantResultDto;
import com.zhangyichuang.medicine.model.entity.CouponActivationGrantLog;
import com.zhangyichuang.medicine.model.entity.CouponActivationLog;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationBatchMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationCodeMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationGrantLogMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationLogMapper;
import com.zhangyichuang.medicine.shared.service.CouponGrantCoreService;
import com.zhangyichuang.medicine.shared.utils.CouponActivationCodeUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 客户端激活码服务实现。
 */
@Service
@RequiredArgsConstructor
public class CouponActivationCodeServiceImpl implements CouponActivationCodeService, BaseService {

    /**
     * 共享码重复兑换提示文案。
     */
    private static final String SHARED_RULE_ALREADY_USED_MESSAGE = "您已使用过该激活码";

    /**
     * 唯一码已使用提示文案。
     */
    private static final String UNIQUE_RULE_ALREADY_USED_MESSAGE = "激活码已被使用";

    /**
     * 激活码不存在提示文案。
     */
    private static final String ACTIVATION_CODE_NOT_FOUND_MESSAGE = "激活码不存在";

    /**
     * 激活码已停用提示文案。
     */
    private static final String ACTIVATION_CODE_DISABLED_MESSAGE = "激活码已停用";

    /**
     * 激活码已过期提示文案。
     */
    private static final String ACTIVATION_CODE_EXPIRED_MESSAGE = "激活码已过期";

    /**
     * 激活码处理中提示文案。
     */
    private static final String ACTIVATION_CODE_LOCK_MESSAGE = "激活码处理中，请勿重复提交";

    /**
     * 发券失败提示文案。
     */
    private static final String GRANT_COUPON_FAIL_MESSAGE = "发券失败，请稍后重试";

    /**
     * 发券方式编码。
     */
    private static final String GRANT_MODE = ActivationGrantModeEnum.COUPON_GRANT_CORE.getType();

    /**
     * 激活码不存在失败编码。
     */
    private static final String FAIL_CODE_CODE_NOT_FOUND = "ACTIVATION_CODE_NOT_FOUND";

    /**
     * 规则冲突失败编码。
     */
    private static final String FAIL_CODE_RULE_CONFLICT = "ACTIVATION_RULE_CONFLICT";

    /**
     * 业务失败编码。
     */
    private static final String FAIL_CODE_BIZ_ERROR = "ACTIVATION_BIZ_ERROR";

    /**
     * 发券失败编码。
     */
    private static final String FAIL_CODE_GRANT_ERROR = "ACTIVATION_GRANT_ERROR";

    /**
     * 系统异常失败编码。
     */
    private static final String FAIL_CODE_SYSTEM_ERROR = "ACTIVATION_SYSTEM_ERROR";

    /**
     * 全局唯一占位后缀。
     */
    private static final String UNIQUE_GLOBAL_LOCK_SUFFIX = "GLOBAL";

    /**
     * 基础激活码批次 Mapper。
     */
    private final BasicCouponActivationBatchMapper activationBatchMapper;

    /**
     * 基础激活码 Mapper。
     */
    private final BasicCouponActivationCodeMapper activationCodeMapper;

    /**
     * 基础激活码日志 Mapper。
     */
    private final BasicCouponActivationLogMapper activationLogMapper;

    /**
     * 基础激活码发券日志 Mapper。
     */
    private final BasicCouponActivationGrantLogMapper activationGrantLogMapper;

    /**
     * 优惠券共享发放核心服务。
     */
    private final CouponGrantCoreService couponGrantCoreService;

    /**
     * 验证码服务。
     */
    private final CaptchaService captchaService;

    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 兑换当前用户激活码。
     *
     * @param request 兑换请求
     * @return 兑换结果
     */
    @Override
    @DistributedLock(
            prefix = RedisConstants.Lock.COUPON_ACTIVATION_KEY,
            key = "@couponActivationLockKeyResolver.normalizeCode(#request.code)",
            failMessage = ACTIVATION_CODE_LOCK_MESSAGE
    )
    public ActivationCodeRedeemVo redeemCurrentUserCode(ActivationCodeRedeemRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "兑换请求不能为空");
        }
        captchaService.validateLoginCaptcha(request.getCaptchaVerificationId());
        Long userId = getUserId();
        String operatorId = String.valueOf(userId);
        String normalizedCode = CouponActivationCodeUtils.normalizeCode(request.getCode());
        String codeHash = CouponActivationCodeUtils.hashCode(normalizedCode);
        String clientIp = resolveClientIp();
        String requestId = UUIDUtils.simple();
        ActivationRedeemCodeDto redeemCode = activationCodeMapper.selectRedeemCodeByHash(codeHash);
        if (redeemCode == null) {
            writeFailureLog(
                    null,
                    null,
                    normalizedCode,
                    null,
                    userId,
                    clientIp,
                    requestId,
                    FAIL_CODE_CODE_NOT_FOUND,
                    ACTIVATION_CODE_NOT_FOUND_MESSAGE
            );
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, ACTIVATION_CODE_NOT_FOUND_MESSAGE);
        }

        try {
            Date now = new Date();
            validateCodeForRedeem(redeemCode, now);
            RedeemTransactionResult transactionResult = transactionTemplate.execute(status ->
                    executeRedeemTransaction(redeemCode, userId, operatorId, clientIp, requestId)
            );
            if (transactionResult == null || transactionResult.grantResult() == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, GRANT_COUPON_FAIL_MESSAGE);
            }
            return toActivationCodeRedeemVo(transactionResult.grantResult(), transactionResult.redeemLogId());
        } catch (DuplicateKeyException duplicateKeyException) {
            String conflictMessage = resolveRuleConflictMessage(redeemCode.getRedeemRuleType());
            writeFailureLog(
                    redeemCode.getBatchId(),
                    redeemCode.getCodeId(),
                    redeemCode.getPlainCode(),
                    redeemCode.getRedeemRuleType(),
                    userId,
                    clientIp,
                    requestId,
                    FAIL_CODE_RULE_CONFLICT,
                    conflictMessage
            );
            throw new ServiceException(ResponseCode.OPERATION_ERROR, conflictMessage);
        } catch (GrantCouponFailedException grantCouponFailedException) {
            writeFailureLog(
                    redeemCode.getBatchId(),
                    redeemCode.getCodeId(),
                    redeemCode.getPlainCode(),
                    redeemCode.getRedeemRuleType(),
                    userId,
                    clientIp,
                    requestId,
                    FAIL_CODE_GRANT_ERROR,
                    grantCouponFailedException.getMessage()
            );
            writeGrantFailureLog(
                    redeemCode,
                    userId,
                    requestId,
                    FAIL_CODE_GRANT_ERROR,
                    grantCouponFailedException.getMessage()
            );
            throw new ServiceException(ResponseCode.OPERATION_ERROR, grantCouponFailedException.getMessage());
        } catch (ServiceException serviceException) {
            writeFailureLog(
                    redeemCode.getBatchId(),
                    redeemCode.getCodeId(),
                    redeemCode.getPlainCode(),
                    redeemCode.getRedeemRuleType(),
                    userId,
                    clientIp,
                    requestId,
                    FAIL_CODE_BIZ_ERROR,
                    serviceException.getMessage()
            );
            throw serviceException;
        } catch (Exception exception) {
            writeFailureLog(
                    redeemCode.getBatchId(),
                    redeemCode.getCodeId(),
                    redeemCode.getPlainCode(),
                    redeemCode.getRedeemRuleType(),
                    userId,
                    clientIp,
                    requestId,
                    FAIL_CODE_SYSTEM_ERROR,
                    GRANT_COUPON_FAIL_MESSAGE
            );
            throw new ServiceException(ResponseCode.OPERATION_ERROR, GRANT_COUPON_FAIL_MESSAGE);
        }
    }

    /**
     * 执行事务内兑换逻辑。
     *
     * @param redeemCode 兑换用激活码信息
     * @param userId     用户ID
     * @param operatorId 操作人标识
     * @param clientIp   客户端IP
     * @param requestId  兑换请求ID
     * @return 事务兑换结果
     */
    private RedeemTransactionResult executeRedeemTransaction(ActivationRedeemCodeDto redeemCode,
                                                             Long userId,
                                                             String operatorId,
                                                             String clientIp,
                                                             String requestId) {
        Date now = new Date();
        String successLockKey = buildSuccessLockKey(redeemCode, userId);
        Long redeemLogId = insertSuccessLog(redeemCode, userId, clientIp, requestId, successLockKey, now);
        try {
            CouponGrantResultDto grantResult = grantCoupon(redeemCode, userId, operatorId, now, requestId);
            updateCodeAfterGrantSuccess(redeemCode, operatorId, now);
            increaseBatchSuccessUseCount(redeemCode.getBatchId(), operatorId, now);
            updateSuccessLogWithCouponInfo(redeemLogId, grantResult.getCouponId());
            writeGrantSuccessLog(redeemLogId, redeemCode, userId, grantResult.getCouponId(), requestId, now);
            return new RedeemTransactionResult(redeemLogId, grantResult);
        } catch (ServiceException serviceException) {
            throw new GrantCouponFailedException(serviceException.getMessage(), serviceException);
        } catch (RuntimeException runtimeException) {
            throw new GrantCouponFailedException(GRANT_COUPON_FAIL_MESSAGE, runtimeException);
        }
    }

    /**
     * 校验激活码是否允许兑换。
     *
     * @param redeemCode 兑换用激活码信息
     * @param now        当前时间
     * @return 无返回值
     */
    private void validateCodeForRedeem(ActivationRedeemCodeDto redeemCode, Date now) {
        if (!Objects.equals(redeemCode.getBatchStatus(), ActivationCodeStatusEnum.ACTIVE.getType())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, ACTIVATION_CODE_DISABLED_MESSAGE);
        }
        if (Objects.equals(redeemCode.getCodeStatus(), ActivationCodeItemStatusEnum.USED.getType())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, UNIQUE_RULE_ALREADY_USED_MESSAGE);
        }
        if (!Objects.equals(redeemCode.getCodeStatus(), ActivationCodeItemStatusEnum.ACTIVE.getType())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, ACTIVATION_CODE_DISABLED_MESSAGE);
        }
        if (Objects.equals(redeemCode.getValidityType(), ActivationCodeValidityTypeEnum.ONCE.getType())
                && redeemCode.getFixedExpireTime() != null
                && !redeemCode.getFixedExpireTime().after(now)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, ACTIVATION_CODE_EXPIRED_MESSAGE);
        }
    }

    /**
     * 写入成功占位日志。
     *
     * @param redeemCode     兑换用激活码信息
     * @param userId         用户ID
     * @param clientIp       客户端IP
     * @param requestId      兑换请求ID
     * @param successLockKey 成功占位键
     * @param now            当前时间
     * @return 日志ID
     */
    private Long insertSuccessLog(ActivationRedeemCodeDto redeemCode,
                                  Long userId,
                                  String clientIp,
                                  String requestId,
                                  String successLockKey,
                                  Date now) {
        CouponActivationLog activationLog = CouponActivationLog.builder()
                .requestId(requestId)
                .batchId(redeemCode.getBatchId())
                .activationCodeId(redeemCode.getCodeId())
                .plainCodeSnapshot(redeemCode.getPlainCode())
                .redeemRuleType(redeemCode.getRedeemRuleType())
                .resultStatus(ActivationLogResultStatusEnum.SUCCESS.getType())
                .userId(userId)
                .clientIp(clientIp)
                .successLockKey(successLockKey)
                .grantMode(GRANT_MODE)
                .grantStatus(ActivationGrantStatusEnum.SUCCESS.getType())
                .createTime(now)
                .isDeleted(0)
                .build();
        activationLogMapper.insert(activationLog);
        return activationLog.getId();
    }

    /**
     * 按规则更新激活码状态与成功次数。
     *
     * @param redeemCode 兑换用激活码信息
     * @param operatorId 操作人标识
     * @param now        当前时间
     * @return 无返回值
     */
    private void updateCodeAfterGrantSuccess(ActivationRedeemCodeDto redeemCode,
                                             String operatorId,
                                             Date now) {
        ActivationRedeemRuleTypeEnum redeemRuleTypeEnum =
                ActivationRedeemRuleTypeEnum.fromCode(redeemCode.getRedeemRuleType());
        if (redeemRuleTypeEnum == ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE) {
            int updated = activationCodeMapper.markUniqueCodeUsed(redeemCode.getCodeId(), operatorId, now);
            if (updated <= 0) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, UNIQUE_RULE_ALREADY_USED_MESSAGE);
            }
            return;
        }
        int updated = activationCodeMapper.increaseSuccessUseCount(redeemCode.getCodeId(), operatorId, now);
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码状态更新失败，请稍后重试");
        }
    }

    /**
     * 累加批次成功使用次数。
     *
     * @param batchId    批次ID
     * @param operatorId 操作人标识
     * @param now        当前时间
     * @return 无返回值
     */
    private void increaseBatchSuccessUseCount(Long batchId, String operatorId, Date now) {
        int updated = activationBatchMapper.increaseSuccessUseCount(batchId, operatorId, now);
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码批次状态更新失败，请稍后重试");
        }
    }

    /**
     * 更新成功日志的发券信息。
     *
     * @param redeemLogId 兑换日志ID
     * @param couponId    用户优惠券ID
     * @return 无返回值
     */
    private void updateSuccessLogWithCouponInfo(Long redeemLogId, Long couponId) {
        CouponActivationLog updateLog = new CouponActivationLog();
        updateLog.setId(redeemLogId);
        updateLog.setCouponId(couponId);
        activationLogMapper.updateById(updateLog);
    }

    /**
     * 按激活码配置发放优惠券。
     *
     * @param redeemCode 兑换用激活码信息
     * @param userId     用户ID
     * @param operatorId 操作人标识
     * @param now        当前时间
     * @param requestId  兑换请求ID
     * @return 发券结果
     */
    private CouponGrantResultDto grantCoupon(ActivationRedeemCodeDto redeemCode,
                                             Long userId,
                                             String operatorId,
                                             Date now,
                                             String requestId) {
        CouponGrantCommand command = CouponGrantCommand.builder()
                .templateId(redeemCode.getTemplateId())
                .userId(userId)
                .effectiveTime(resolveCouponEffectiveTime(redeemCode, now))
                .expireTime(resolveCouponExpireTime(redeemCode, now))
                .sourceType(CouponSourceTypeEnum.ACTIVATION_CODE.getType())
                .sourceBizNo(buildSourceBizNo(redeemCode, userId, requestId))
                .remark("激活码兑换发券")
                .operatorId(operatorId)
                .build();
        return couponGrantCoreService.grantCoupon(command);
    }

    /**
     * 写入发券成功日志。
     *
     * @param redeemLogId 兑换日志ID
     * @param redeemCode  兑换用激活码信息
     * @param userId      用户ID
     * @param couponId    用户优惠券ID
     * @param requestId   兑换请求ID
     * @param now         当前时间
     * @return 无返回值
     */
    private void writeGrantSuccessLog(Long redeemLogId,
                                      ActivationRedeemCodeDto redeemCode,
                                      Long userId,
                                      Long couponId,
                                      String requestId,
                                      Date now) {
        activationGrantLogMapper.insert(CouponActivationGrantLog.builder()
                .redeemLogId(redeemLogId)
                .batchId(redeemCode.getBatchId())
                .activationCodeId(redeemCode.getCodeId())
                .templateId(redeemCode.getTemplateId())
                .userId(userId)
                .couponId(couponId)
                .sourceType(CouponSourceTypeEnum.ACTIVATION_CODE.getType())
                .sourceBizNo(buildSourceBizNo(redeemCode, userId, requestId))
                .grantMode(GRANT_MODE)
                .grantStatus(ActivationGrantStatusEnum.SUCCESS.getType())
                .createTime(now)
                .isDeleted(0)
                .build());
    }

    /**
     * 写入发券失败日志。
     *
     * @param redeemCode        兑换用激活码信息
     * @param userId            用户ID
     * @param requestId         兑换请求ID
     * @param grantErrorCode    发券错误编码
     * @param grantErrorMessage 发券错误信息
     * @return 无返回值
     */
    private void writeGrantFailureLog(ActivationRedeemCodeDto redeemCode,
                                      Long userId,
                                      String requestId,
                                      String grantErrorCode,
                                      String grantErrorMessage) {
        activationGrantLogMapper.insert(CouponActivationGrantLog.builder()
                .redeemLogId(null)
                .batchId(redeemCode.getBatchId())
                .activationCodeId(redeemCode.getCodeId())
                .templateId(redeemCode.getTemplateId())
                .userId(userId)
                .couponId(null)
                .sourceType(CouponSourceTypeEnum.ACTIVATION_CODE.getType())
                .sourceBizNo(buildSourceBizNo(redeemCode, userId, requestId))
                .grantMode(GRANT_MODE)
                .grantStatus(ActivationGrantStatusEnum.FAIL.getType())
                .grantErrorCode(grantErrorCode)
                .grantErrorMessage(grantErrorMessage)
                .createTime(new Date())
                .isDeleted(0)
                .build());
    }

    /**
     * 解析优惠券生效时间。
     *
     * @param redeemCode 兑换用激活码信息
     * @param now        当前时间
     * @return 优惠券生效时间
     */
    private Date resolveCouponEffectiveTime(ActivationRedeemCodeDto redeemCode, Date now) {
        if (Objects.equals(redeemCode.getValidityType(), ActivationCodeValidityTypeEnum.ONCE.getType())) {
            return redeemCode.getFixedEffectiveTime();
        }
        return now;
    }

    /**
     * 解析优惠券失效时间。
     *
     * @param redeemCode 兑换用激活码信息
     * @param now        当前时间
     * @return 优惠券失效时间
     */
    private Date resolveCouponExpireTime(ActivationRedeemCodeDto redeemCode, Date now) {
        if (Objects.equals(redeemCode.getValidityType(), ActivationCodeValidityTypeEnum.ONCE.getType())) {
            return redeemCode.getFixedExpireTime();
        }
        if (redeemCode.getRelativeValidDays() == null || redeemCode.getRelativeValidDays() <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码有效期配置不正确");
        }
        return new Date(now.getTime() + TimeUnit.DAYS.toMillis(redeemCode.getRelativeValidDays().longValue()));
    }

    /**
     * 构建激活码发券来源业务号。
     *
     * @param redeemCode 兑换用激活码信息
     * @param userId     用户ID
     * @param requestId  兑换请求ID
     * @return 来源业务号
     */
    private String buildSourceBizNo(ActivationRedeemCodeDto redeemCode, Long userId, String requestId) {
        return redeemCode.getBatchNo() + ":" + redeemCode.getCodeId() + ":" + userId + ":" + requestId;
    }

    /**
     * 构建成功占位键。
     *
     * @param redeemCode 兑换用激活码信息
     * @param userId     用户ID
     * @return 成功占位键
     */
    private String buildSuccessLockKey(ActivationRedeemCodeDto redeemCode, Long userId) {
        ActivationRedeemRuleTypeEnum redeemRuleTypeEnum =
                ActivationRedeemRuleTypeEnum.fromCode(redeemCode.getRedeemRuleType());
        if (redeemRuleTypeEnum == ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE) {
            return redeemCode.getCodeId() + ":" + UNIQUE_GLOBAL_LOCK_SUFFIX;
        }
        if (redeemRuleTypeEnum == ActivationRedeemRuleTypeEnum.SHARED_PER_USER_ONCE) {
            return redeemCode.getCodeId() + ":" + userId;
        }
        throw new ServiceException(ResponseCode.PARAM_ERROR, "兑换规则类型不正确");
    }

    /**
     * 解析规则冲突提示文案。
     *
     * @param redeemRuleType 兑换规则类型
     * @return 冲突提示文案
     */
    private String resolveRuleConflictMessage(String redeemRuleType) {
        ActivationRedeemRuleTypeEnum redeemRuleTypeEnum = ActivationRedeemRuleTypeEnum.fromCode(redeemRuleType);
        if (redeemRuleTypeEnum == ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE) {
            return UNIQUE_RULE_ALREADY_USED_MESSAGE;
        }
        return SHARED_RULE_ALREADY_USED_MESSAGE;
    }

    /**
     * 写入兑换失败日志。
     *
     * @param batchId           批次ID
     * @param codeId            激活码ID
     * @param plainCodeSnapshot 激活码明文快照
     * @param redeemRuleType    兑换规则类型
     * @param userId            用户ID
     * @param clientIp          客户端IP
     * @param requestId         兑换请求ID
     * @param failCode          失败编码
     * @param failMessage       失败信息
     * @return 无返回值
     */
    private void writeFailureLog(Long batchId,
                                 Long codeId,
                                 String plainCodeSnapshot,
                                 String redeemRuleType,
                                 Long userId,
                                 String clientIp,
                                 String requestId,
                                 String failCode,
                                 String failMessage) {
        activationLogMapper.insert(CouponActivationLog.builder()
                .requestId(requestId)
                .batchId(batchId)
                .activationCodeId(codeId)
                .plainCodeSnapshot(plainCodeSnapshot)
                .redeemRuleType(redeemRuleType)
                .resultStatus(ActivationLogResultStatusEnum.FAIL.getType())
                .userId(userId)
                .couponId(null)
                .clientIp(clientIp)
                .failCode(failCode)
                .failMessage(failMessage)
                .successLockKey(null)
                .grantMode(GRANT_MODE)
                .grantStatus(ActivationGrantStatusEnum.FAIL.getType())
                .createTime(new Date())
                .isDeleted(0)
                .build());
    }

    /**
     * 转换为激活码兑换结果视图对象。
     *
     * @param grantResult 发券结果
     * @param redeemLogId 兑换日志ID
     * @return 激活码兑换结果视图对象
     */
    private ActivationCodeRedeemVo toActivationCodeRedeemVo(CouponGrantResultDto grantResult, Long redeemLogId) {
        return ActivationCodeRedeemVo.builder()
                .couponId(grantResult.getCouponId())
                .templateId(grantResult.getTemplateId())
                .couponName(grantResult.getCouponName())
                .thresholdAmount(grantResult.getThresholdAmount())
                .totalAmount(grantResult.getTotalAmount())
                .availableAmount(grantResult.getAvailableAmount())
                .effectiveTime(grantResult.getEffectiveTime())
                .expireTime(grantResult.getExpireTime())
                .couponStatus(grantResult.getCouponStatus())
                .redeemLogId(redeemLogId)
                .build();
    }

    /**
     * 解析客户端IP。
     *
     * @return 客户端IP
     */
    private String resolveClientIp() {
        HttpServletRequest request = SecurityUtils.getHttpServletRequest();
        return request == null ? null : IpAddressUtils.getIpAddress(request);
    }

    /**
     * 事务兑换结果。
     */
    private record RedeemTransactionResult(Long redeemLogId, CouponGrantResultDto grantResult) {
    }

    /**
     * 发券失败异常。
     */
    private static class GrantCouponFailedException extends RuntimeException {

        /**
         * 构造发券失败异常。
         *
         * @param message 异常消息
         * @param cause   异常原因
         */
        GrantCouponFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
