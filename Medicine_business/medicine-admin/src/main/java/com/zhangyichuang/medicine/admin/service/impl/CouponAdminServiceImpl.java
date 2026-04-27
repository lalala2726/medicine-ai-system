package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.CouponLogMapper;
import com.zhangyichuang.medicine.admin.mapper.CouponTemplateMapper;
import com.zhangyichuang.medicine.admin.mapper.UserCouponMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.AdminUserCouponVo;
import com.zhangyichuang.medicine.admin.model.vo.CouponLogVo;
import com.zhangyichuang.medicine.admin.model.vo.CouponTemplateVo;
import com.zhangyichuang.medicine.admin.publisher.CouponBatchIssueMessagePublisher;
import com.zhangyichuang.medicine.admin.publisher.CouponIssueMessagePublisher;
import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.entity.*;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.model.mq.CouponBatchIssueMessage;
import com.zhangyichuang.medicine.model.mq.CouponIssueMessage;
import com.zhangyichuang.medicine.shared.service.CouponGrantCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理端优惠券服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CouponAdminServiceImpl implements CouponAdminService, BaseService {

    /**
     * 商品允许使用优惠券的标记值。
     */
    private static final int COUPON_ENABLED_FLAG = 1;

    /**
     * 金额小数位数。
     */
    private static final int MONEY_SCALE = 2;

    /**
     * 启用状态值。
     */
    private static final int USER_ENABLED_STATUS = 0;

    /**
     * 全量发券分页大小。
     */
    private static final int BATCH_ISSUE_PAGE_SIZE = 500;
    /**
     * 优惠券过期扫描批量大小。
     */
    private static final int COUPON_EXPIRE_BATCH_SIZE = 500;

    /**
     * 管理端发券批次号前缀。
     */
    private static final String ADMIN_ISSUE_SOURCE_BIZ_NO_PREFIX = "ADMIN_COUPON";

    /**
     * 管理端发券批次号时间格式。
     */
    private static final DateTimeFormatter ADMIN_ISSUE_SOURCE_BIZ_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 模板删除导致失效时的备注。
     */
    private static final String TEMPLATE_DELETE_EXPIRE_REMARK = "模板删除导致失效";

    /**
     * 模板删除来源业务号前缀。
     */
    private static final String TEMPLATE_DELETE_SOURCE_BIZ_NO_PREFIX = "DELETE_TEMPLATE";

    /**
     * 系统操作人标识。
     */
    private static final String SYSTEM_OPERATOR_ID = "system";

    /**
     * 系统过期优惠券日志备注。
     */
    private static final String SYSTEM_EXPIRE_REMARK = "系统过期优惠券";

    /**
     * 优惠券模板 Mapper。
     */
    private final CouponTemplateMapper couponTemplateMapper;

    /**
     * 用户优惠券 Mapper。
     */
    private final UserCouponMapper userCouponMapper;

    /**
     * 优惠券日志 Mapper。
     */
    private final CouponLogMapper couponLogMapper;

    /**
     * 用户 Mapper。
     */
    private final UserMapper userMapper;

    /**
     * 批量发券消息发布器。
     */
    private final CouponBatchIssueMessagePublisher couponBatchIssueMessagePublisher;

    /**
     * 发券消息发布器。
     */
    private final CouponIssueMessagePublisher couponIssueMessagePublisher;

    /**
     * 分布式锁执行器。
     */
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 优惠券共享发放核心服务。
     */
    private final CouponGrantCoreService couponGrantCoreService;

    /**
     * 验证码服务。
     */
    private final CaptchaService captchaService;

    /**
     * 查询优惠券模板列表。
     *
     * @param request 查询请求
     * @return 优惠券模板分页结果
     */
    @Override
    public Page<CouponTemplateVo> listTemplates(CouponTemplateListRequest request) {
        LambdaQueryWrapper<CouponTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(request.getName()), CouponTemplate::getName, request.getName())
                .eq(StringUtils.hasText(request.getStatus()), CouponTemplate::getStatus, request.getStatus())
                .orderByDesc(CouponTemplate::getCreateTime);
        Page<CouponTemplate> templatePage = couponTemplateMapper.selectPage(request.toPage(), queryWrapper);
        Page<CouponTemplateVo> result = new Page<>(templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());
        result.setRecords(templatePage.getRecords().stream().map(this::toTemplateVo).toList());
        return result;
    }

    /**
     * 查询优惠券模板详情。
     *
     * @param id 模板ID
     * @return 优惠券模板详情
     */
    @Override
    public CouponTemplateVo getTemplate(Long id) {
        CouponTemplate template = getTemplateEntity(id);
        return toTemplateVo(template);
    }

    /**
     * 新增优惠券模板。
     *
     * @param request 新增请求
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTemplate(CouponTemplateAddRequest request) {
        validateTemplateRequest(request);
        assertTemplateNameUnique(null, request.getName());
        Date now = new Date();
        CouponTemplate template = CouponTemplate.builder()
                .couponType(CouponTypeEnum.FULL_REDUCTION.getType())
                .name(request.getName().trim())
                .thresholdAmount(normalizeAmount(request.getThresholdAmount()))
                .faceAmount(normalizeAmount(request.getFaceAmount()))
                .continueUseEnabled(request.getContinueUseEnabled())
                .stackableEnabled(request.getStackableEnabled())
                .status(request.getStatus())
                .remark(request.getRemark())
                .createTime(now)
                .updateTime(now)
                .createBy(getUsername())
                .updateBy(getUsername())
                .isDeleted(0)
                .build();
        return couponTemplateMapper.insert(template) > 0;
    }

    /**
     * 修改优惠券模板。
     *
     * @param request 修改请求
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(CouponTemplateUpdateRequest request) {
        validateTemplateRequest(request);
        CouponTemplate template = getTemplateEntity(request.getId());
        assertTemplateNameUnique(template.getId(), request.getName());
        template.setName(request.getName().trim());
        template.setThresholdAmount(normalizeAmount(request.getThresholdAmount()));
        template.setFaceAmount(normalizeAmount(request.getFaceAmount()));
        template.setContinueUseEnabled(request.getContinueUseEnabled());
        template.setStackableEnabled(request.getStackableEnabled());
        template.setStatus(request.getStatus());
        template.setRemark(request.getRemark());
        template.setUpdateTime(new Date());
        template.setUpdateBy(getUsername());
        return couponTemplateMapper.updateById(template) > 0;
    }

    /**
     * 删除优惠券模板。
     *
     * @param id         模板ID
     * @param deleteMode 删除模式
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id, CouponTemplateDeleteModeEnum deleteMode) {
        CouponTemplate template = getTemplateEntity(id);
        Assert.notNull(deleteMode, "模板删除模式不能为空");
        Date now = new Date();
        String operatorId = getUsername();
        boolean hidden = hideTemplate(template, now, operatorId);
        if (!hidden) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "删除优惠券模板失败");
        }
        if (deleteMode == CouponTemplateDeleteModeEnum.HIDE_AND_EXPIRE_ISSUED) {
            expireAvailableCouponsByTemplate(template, now, operatorId);
        }
        return true;
    }

    /**
     * 管理端发券。
     *
     * @param request 发券请求
     * @return 是否提交成功
     */
    @Override
    public boolean issueCoupon(CouponIssueRequest request) {
        captchaService.validateLoginCaptcha(request == null ? null : request.getCaptchaVerificationId());
        CouponIssueTargetTypeEnum issueTargetType = validateIssueRequest(request);
        CouponTemplate template = getTemplateEntity(request.getTemplateId());
        validateTemplateStatusForIssue(template);
        List<Long> normalizedUserIds = issueTargetType == CouponIssueTargetTypeEnum.SPECIFIED
                ? validateSpecifiedUserIds(request.getUserIds())
                : null;
        String sourceBizNo = generateIssueSourceBizNo();
        couponBatchIssueMessagePublisher.publish(CouponBatchIssueMessage.builder()
                .templateId(request.getTemplateId())
                .issueTargetType(issueTargetType.getType())
                .userIds(normalizedUserIds)
                .effectiveTime(request.getEffectiveTime())
                .expireTime(request.getExpireTime())
                .sourceBizNo(sourceBizNo)
                .remark(request.getRemark())
                .operatorId(getUsername())
                .build());
        return true;
    }

    /**
     * 消费批量发券消息并拆分单用户发券消息。
     *
     * @param message 批量发券消息
     */
    @Override
    public void consumeBatchIssueCoupon(CouponBatchIssueMessage message) {
        String lockName = buildCouponBatchIssueLockName(message);
        distributedLockExecutor.tryExecuteOrElse(lockName,
                0L,
                -1L,
                () -> {
                    doConsumeBatchIssueCoupon(message);
                    return null;
                },
                () -> {
                    log.info("批量发券消息正在处理中，跳过重复消费，sourceBizNo={}",
                            message == null ? null : message.getSourceBizNo());
                    return null;
                });
    }

    /**
     * 消费发券消息并完成用户优惠券落库。
     *
     * @param message 发券消息
     */
    @Override
    public void consumeIssueCoupon(CouponIssueMessage message) {
        String lockName = buildCouponIssueLockName(message);
        distributedLockExecutor.tryExecuteOrElse(lockName,
                0L,
                -1L,
                () -> {
                    transactionTemplate.execute(status -> {
                        doConsumeIssueCoupon(message);
                        return null;
                    });
                    return null;
                },
                () -> {
                    log.info("单用户发券消息正在处理中，跳过重复消费，sourceBizNo={}，userId={}",
                            message == null ? null : message.getSourceBizNo(),
                            message == null ? null : message.getUserId());
                    return null;
                });
    }

    /**
     * 实际执行批量发券拆单逻辑。
     *
     * @param message 批量发券消息
     */
    private void doConsumeBatchIssueCoupon(CouponBatchIssueMessage message) {
        CouponIssueTargetTypeEnum issueTargetType = validateBatchIssueMessage(message);
        CouponTemplate template = getTemplateEntity(message.getTemplateId());
        validateTemplateStatusForIssue(template);
        if (issueTargetType == CouponIssueTargetTypeEnum.ALL) {
            publishIssueMessagesForAllUsers(message);
            return;
        }
        List<Long> normalizedUserIds = validateSpecifiedUserIds(message.getUserIds());
        publishSingleIssueMessages(normalizedUserIds, message);
    }

    /**
     * 实际执行单用户发券入库逻辑。
     *
     * @param message 发券消息
     */
    private void doConsumeIssueCoupon(CouponIssueMessage message) {
        validateIssueMessage(message);
        couponGrantCoreService.grantCoupon(CouponGrantCommand.builder()
                .templateId(message.getTemplateId())
                .userId(message.getUserId())
                .effectiveTime(message.getEffectiveTime())
                .expireTime(message.getExpireTime())
                .sourceType(CouponSourceTypeEnum.ADMIN_GRANT.getType())
                .sourceBizNo(message.getSourceBizNo())
                .remark(message.getRemark())
                .operatorId(message.getOperatorId())
                .build());
    }

    /**
     * 查询用户优惠券列表。
     *
     * @param request 查询请求
     * @return 用户优惠券分页结果
     */
    @Override
    public Page<AdminUserCouponVo> listUserCoupons(AdminUserCouponListRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        Assert.notNull(request.getUserId(), "用户ID不能为空");
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(request.getUserId() != null, UserCoupon::getUserId, request.getUserId())
                .eq(StringUtils.hasText(request.getCouponStatus()), UserCoupon::getCouponStatus, request.getCouponStatus())
                .like(StringUtils.hasText(request.getCouponName()), UserCoupon::getCouponNameSnapshot, request.getCouponName())
                .like(StringUtils.hasText(request.getLockOrderNo()), UserCoupon::getLockOrderNo, request.getLockOrderNo())
                .orderByDesc(UserCoupon::getCreateTime);
        Page<UserCoupon> couponPage = userCouponMapper.selectPage(request.toPage(), queryWrapper);
        Map<Long, User> userMap = listUserMap(couponPage.getRecords().stream().map(UserCoupon::getUserId).distinct().toList());
        Page<AdminUserCouponVo> result = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        result.setRecords(couponPage.getRecords().stream()
                .map(coupon -> toAdminUserCouponVo(coupon, userMap.get(coupon.getUserId())))
                .toList());
        return result;
    }

    /**
     * 删除指定用户优惠券。
     *
     * @param userId     用户ID
     * @param couponId   用户优惠券ID
     * @param operatorId 操作人标识
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserCoupon(Long userId, Long couponId, String operatorId) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(couponId, "用户优惠券ID不能为空");

        UserCoupon userCoupon = userCouponMapper.selectById(couponId);
        if (userCoupon == null || !Objects.equals(userCoupon.getUserId(), userId)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "用户优惠券不存在");
        }
        if (Objects.equals(userCoupon.getCouponStatus(), UserCouponStatusEnum.LOCKED.getType())) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "已锁定优惠券不允许删除");
        }

        BigDecimal beforeAvailableAmount = normalizeAmount(userCoupon.getAvailableAmount());
        userCoupon.setAvailableAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        userCoupon.setLockedConsumeAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        userCoupon.setIsDeleted(1);
        userCoupon.setUpdateTime(new Date());
        userCoupon.setUpdateBy(operatorId);
        int updated = userCouponMapper.updateById(userCoupon);
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "删除用户优惠券失败");
        }

        writeCouponLog(CouponLog.builder()
                .couponId(userCoupon.getId())
                .userId(userCoupon.getUserId())
                .changeType(CouponChangeTypeEnum.MANUAL_ADJUST.getType())
                .changeAmount(beforeAvailableAmount)
                .deductAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .wasteAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .beforeAvailableAmount(beforeAvailableAmount)
                .afterAvailableAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .sourceType(CouponSourceTypeEnum.MANUAL.getType())
                .sourceBizNo(String.valueOf(userCoupon.getId()))
                .remark("管理员删除用户优惠券")
                .operatorId(operatorId)
                .build());
        return true;
    }

    /**
     * 查询指定用户优惠券日志列表。
     *
     * @param userId  用户ID
     * @param request 查询请求
     * @return 用户优惠券日志分页结果
     */
    @Override
    public Page<CouponLogVo> listUserCouponLogs(Long userId, CouponLogListRequest request) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(request, "查询参数不能为空");
        LambdaQueryWrapper<CouponLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CouponLog::getUserId, userId)
                .eq(request.getCouponId() != null, CouponLog::getCouponId, request.getCouponId())
                .eq(StringUtils.hasText(request.getChangeType()), CouponLog::getChangeType, request.getChangeType())
                .like(StringUtils.hasText(request.getOrderNo()), CouponLog::getOrderNo, request.getOrderNo())
                .orderByDesc(CouponLog::getCreateTime);
        Page<CouponLog> logPage = couponLogMapper.selectPage(request.toPage(), queryWrapper);
        Map<Long, String> couponNameMap = listCouponNameMap(logPage.getRecords().stream().map(CouponLog::getCouponId).distinct().toList());
        Map<Long, User> userMap = listUserMap(logPage.getRecords().stream().map(CouponLog::getUserId).distinct().toList());
        Map<Long, User> operatorUserMap = listOperatorUserMap(logPage.getRecords().stream().map(CouponLog::getOperatorId).toList());
        Page<CouponLogVo> result = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        result.setRecords(logPage.getRecords().stream()
                .map(log -> toCouponLogVo(
                        log,
                        couponNameMap.get(log.getCouponId()),
                        userMap.get(log.getUserId()),
                        resolveOperatorUser(operatorUserMap, log.getOperatorId())
                ))
                .toList());
        return result;
    }

    /**
     * 查询优惠券日志列表。
     *
     * @param request 查询请求
     * @return 优惠券日志分页结果
     */
    @Override
    public Page<CouponLogVo> listCouponLogs(CouponLogListRequest request) {
        LambdaQueryWrapper<CouponLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(request.getCouponId() != null, CouponLog::getCouponId, request.getCouponId())
                .eq(request.getUserId() != null, CouponLog::getUserId, request.getUserId())
                .eq(StringUtils.hasText(request.getChangeType()), CouponLog::getChangeType, request.getChangeType())
                .like(StringUtils.hasText(request.getOrderNo()), CouponLog::getOrderNo, request.getOrderNo())
                .orderByDesc(CouponLog::getCreateTime);
        Page<CouponLog> logPage = couponLogMapper.selectPage(request.toPage(), queryWrapper);
        Map<Long, String> couponNameMap = listCouponNameMap(logPage.getRecords().stream().map(CouponLog::getCouponId).distinct().toList());
        Map<Long, User> userMap = listUserMap(logPage.getRecords().stream().map(CouponLog::getUserId).distinct().toList());
        Map<Long, User> operatorUserMap = listOperatorUserMap(logPage.getRecords().stream().map(CouponLog::getOperatorId).toList());
        Page<CouponLogVo> result = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        result.setRecords(logPage.getRecords().stream()
                .map(log -> toCouponLogVo(
                        log,
                        couponNameMap.get(log.getCouponId()),
                        userMap.get(log.getUserId()),
                        resolveOperatorUser(operatorUserMap, log.getOperatorId())
                ))
                .toList());
        return result;
    }

    /**
     * 扫描并过期可用优惠券。
     *
     * @return 本次过期数量
     */
    @Override
    public int expireAvailableCoupons() {
        Date now = truncateToSecond(new Date());
        int successCount = 0;
        Long lastId = 0L;
        while (true) {
            List<UserCoupon> expiredCoupons = listExpiredAvailableCouponsBatch(now, lastId);
            if (expiredCoupons.isEmpty()) {
                break;
            }
            List<Long> couponIds = expiredCoupons.stream()
                    .map(UserCoupon::getId)
                    .filter(Objects::nonNull)
                    .toList();
            successCount += expireCouponBatchInTransaction(couponIds, now);
            lastId = expiredCoupons.getLast().getId();
        }
        return successCount;
    }

    /**
     * 在独立事务中处理一批可过期用户券。
     *
     * @param couponIds 本批用户券ID集合
     * @param now       统一处理时间
     * @return 本批成功过期数量
     */
    private int expireCouponBatchInTransaction(List<Long> couponIds, Date now) {
        Integer batchSuccessCount = transactionTemplate.execute(status -> {
            int updatedCount = userCouponMapper.batchExpireCouponsByIds(
                    couponIds,
                    UserCouponStatusEnum.AVAILABLE.getType(),
                    UserCouponStatusEnum.EXPIRED.getType(),
                    now,
                    SYSTEM_OPERATOR_ID
            );
            if (updatedCount <= 0) {
                return 0;
            }
            int insertedLogCount = couponLogMapper.batchInsertExpireLogsByCouponIds(
                    couponIds,
                    UserCouponStatusEnum.EXPIRED.getType(),
                    SYSTEM_OPERATOR_ID,
                    CouponChangeTypeEnum.EXPIRE.getType(),
                    CouponSourceTypeEnum.SYSTEM_EXPIRE.getType(),
                    SYSTEM_EXPIRE_REMARK,
                    now
            );
            if (insertedLogCount != updatedCount) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "系统过期优惠券日志写入失败");
            }
            return updatedCount;
        });
        return batchSuccessCount == null ? 0 : batchSuccessCount;
    }

    /**
     * 将时间精度截断到秒，避免数据库时间精度差异导致的匹配失败。
     *
     * @param date 原始时间
     * @return 截断到秒的时间
     */
    private Date truncateToSecond(Date date) {
        if (date == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "时间不能为空");
        }
        return new Date((date.getTime() / 1000) * 1000);
    }

    /**
     * 按游标分批查询可过期用户券。
     *
     * @param now    当前时间
     * @param lastId 上一批最后一条用户券ID
     * @return 本批可过期用户券列表
     */
    private List<UserCoupon> listExpiredAvailableCouponsBatch(Date now, Long lastId) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .lt(UserCoupon::getExpireTime, now)
                .isNull(UserCoupon::getLockOrderNo)
                .gt(lastId != null && lastId > 0, UserCoupon::getId, lastId)
                .orderByAsc(UserCoupon::getId)
                .last("LIMIT " + COUPON_EXPIRE_BATCH_SIZE));
    }

    /**
     * 释放订单已锁定优惠券。
     *
     * @param order      订单实体
     * @param operatorId 操作人标识
     * @param reason     操作原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseLockedCouponForOrder(MallOrder order, String operatorId, String reason) {
        if (order == null || order.getCouponId() == null) {
            return;
        }
        OrderCouponSelectionSnapshotDto couponSelectionSnapshot = parseCouponSelectionSnapshot(order.getCouponSnapshotJson());
        List<CouponAppliedDetailDto> appliedCoupons = couponSelectionSnapshot.getAppliedCoupons();
        if (appliedCoupons == null || appliedCoupons.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (CouponAppliedDetailDto appliedDetail : appliedCoupons) {
            if (appliedDetail == null || appliedDetail.getCouponId() == null) {
                continue;
            }
            UserCoupon userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, appliedDetail.getCouponId())
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .eq(UserCoupon::getLockOrderNo, order.getOrderNo()));
            if (userCoupon == null) {
                continue;
            }
            userCoupon.setCouponStatus(now.after(userCoupon.getExpireTime())
                    ? UserCouponStatusEnum.EXPIRED.getType()
                    : UserCouponStatusEnum.AVAILABLE.getType());
            userCoupon.setLockedConsumeAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            userCoupon.setLockOrderNo(null);
            userCoupon.setLockTime(null);
            userCoupon.setUpdateTime(now);
            int updated = userCouponMapper.updateById(userCoupon);
            if (updated <= 0) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "释放锁定优惠券失败");
            }

            writeCouponLog(CouponLog.builder()
                    .couponId(userCoupon.getId())
                    .userId(userCoupon.getUserId())
                    .orderNo(order.getOrderNo())
                    .changeType(CouponChangeTypeEnum.RELEASE.getType())
                    .changeAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                    .deductAmount(normalizeAmount(appliedDetail.getCouponDeductAmount()))
                    .wasteAmount(normalizeAmount(appliedDetail.getCouponWasteAmount()))
                    .beforeAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .afterAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .sourceType(CouponSourceTypeEnum.ORDER.getType())
                    .sourceBizNo(order.getOrderNo())
                    .remark(StringUtils.hasText(reason) ? reason : "后台释放锁定优惠券")
                    .operatorId(operatorId)
                    .build());
        }
    }

    /**
     * 为待支付订单重算已锁定优惠券。
     *
     * @param order      订单实体
     * @param orderItems 订单项列表
     * @return 优惠券重算结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponSettlementResultDto recalculateLockedCoupon(MallOrder order, List<MallOrderItem> orderItems) {
        List<CouponSettlementItemDto> settlementItems = buildSettlementItems(orderItems, List.of());
        if (order == null || order.getCouponId() == null) {
            CouponAutoSelectResultDto emptyResult = CouponAutoSelectCalculator.applyCouponsInOrder(List.of(), settlementItems);
            return buildSettlementResult(emptyResult, settlementItems);
        }
        OrderCouponSelectionSnapshotDto couponSelectionSnapshot = parseCouponSelectionSnapshot(order.getCouponSnapshotJson());
        List<OrderCouponSnapshotDto> selectedCoupons = normalizeSelectedCoupons(couponSelectionSnapshot.getSelectedCoupons());
        settlementItems = buildSettlementItems(orderItems, selectedCoupons);
        if (selectedCoupons.isEmpty()) {
            CouponAutoSelectResultDto emptyResult = CouponAutoSelectCalculator.applyCouponsInOrder(List.of(), settlementItems);
            return buildSettlementResult(emptyResult, settlementItems);
        }

        Map<Long, UserCoupon> lockedCouponMap = new LinkedHashMap<>();
        for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
            Long couponId = selectedCoupon.getCouponId();
            UserCoupon lockedCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, couponId)
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .eq(UserCoupon::getLockOrderNo, order.getOrderNo()));
            if (lockedCoupon == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单锁定优惠券状态异常");
            }
            lockedCouponMap.put(couponId, lockedCoupon);
        }

        CouponAutoSelectResultDto settlementResult = CouponAutoSelectCalculator.applyCouponsInOrder(selectedCoupons, settlementItems);
        List<CouponAppliedDetailDto> appliedCoupons = settlementResult.getAppliedCoupons() == null
                ? List.of()
                : settlementResult.getAppliedCoupons();
        if (appliedCoupons.size() != selectedCoupons.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "改价后订单已不满足优惠券使用门槛");
        }
        if (normalizeAmount(settlementResult.getCouponDeductAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "改价后订单已不满足优惠券使用门槛");
        }

        BigDecimal itemsAmount = settlementItems.stream()
                .map(CouponSettlementItemDto::getTotalAmount)
                .map(this::normalizeAmount)
                .reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal payableAmount = normalizeAmount(itemsAmount)
                .add(normalizeAmount(order.getFreightAmount()))
                .subtract(normalizeAmount(settlementResult.getCouponDeductAmount()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠后订单实付金额必须大于0");
        }

        Map<Long, CouponAppliedDetailDto> appliedDetailMap = new LinkedHashMap<>();
        for (CouponAppliedDetailDto appliedDetail : appliedCoupons) {
            if (appliedDetail == null || appliedDetail.getCouponId() == null) {
                continue;
            }
            appliedDetailMap.put(appliedDetail.getCouponId(), appliedDetail);
        }

        Date now = new Date();
        for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
            Long couponId = selectedCoupon.getCouponId();
            UserCoupon lockedCoupon = lockedCouponMap.get(couponId);
            CouponAppliedDetailDto appliedDetail = appliedDetailMap.get(couponId);
            if (lockedCoupon == null || appliedDetail == null) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券重算结果异常");
            }
            LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, couponId)
                    .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.LOCKED.getType())
                    .eq(UserCoupon::getLockOrderNo, order.getOrderNo())
                    .set(UserCoupon::getLockedConsumeAmount, normalizeAmount(appliedDetail.getCouponConsumeAmount()))
                    .set(UserCoupon::getUpdateTime, now);
            int updated = userCouponMapper.update(null, updateWrapper);
            if (updated <= 0) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "更新锁券金额失败");
            }
        }
        return buildSettlementResult(settlementResult, settlementItems);
    }

    /**
     * 解析订单多券快照。
     *
     * @param couponSnapshotJson 优惠券快照JSON
     * @return 订单多券快照
     */
    private OrderCouponSelectionSnapshotDto parseCouponSelectionSnapshot(String couponSnapshotJson) {
        if (!StringUtils.hasText(couponSnapshotJson)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券快照缺失");
        }
        OrderCouponSelectionSnapshotDto couponSelectionSnapshot = JSONUtils.fromJson(couponSnapshotJson, OrderCouponSelectionSnapshotDto.class);
        if (couponSelectionSnapshot == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "订单优惠券快照解析失败");
        }
        return couponSelectionSnapshot;
    }

    /**
     * 规范化订单中选中的优惠券快照集合。
     *
     * @param selectedCoupons 原始选中券集合
     * @return 规范化后的选中券集合
     */
    private List<OrderCouponSnapshotDto> normalizeSelectedCoupons(List<OrderCouponSnapshotDto> selectedCoupons) {
        if (selectedCoupons == null || selectedCoupons.isEmpty()) {
            return List.of();
        }
        List<OrderCouponSnapshotDto> normalizedCoupons = new ArrayList<>();
        Set<Long> couponIdSet = new LinkedHashSet<>();
        for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
            if (selectedCoupon == null || selectedCoupon.getCouponId() == null) {
                continue;
            }
            if (couponIdSet.add(selectedCoupon.getCouponId())) {
                normalizedCoupons.add(selectedCoupon);
            }
        }
        return normalizedCoupons;
    }

    /**
     * 将自动选券结果转换为管理端重算返回结构。
     *
     * @param autoSelectResult 自动选券结果
     * @param settlementItems  结算商品项列表
     * @return 优惠券结算结果
     */
    private CouponSettlementResultDto buildSettlementResult(CouponAutoSelectResultDto autoSelectResult,
                                                            List<CouponSettlementItemDto> settlementItems) {
        BigDecimal itemsAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal eligibleItemsAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (settlementItems != null) {
            for (CouponSettlementItemDto settlementItem : settlementItems) {
                if (settlementItem == null) {
                    continue;
                }
                BigDecimal totalAmount = normalizeAmount(settlementItem.getTotalAmount());
                itemsAmount = normalizeAmount(itemsAmount.add(totalAmount));
                if (Objects.equals(settlementItem.getCouponEnabled(), COUPON_ENABLED_FLAG)) {
                    eligibleItemsAmount = normalizeAmount(eligibleItemsAmount.add(totalAmount));
                }
            }
        }
        return CouponSettlementResultDto.builder()
                .itemsAmount(itemsAmount)
                .eligibleItemsAmount(eligibleItemsAmount)
                .couponDeductAmount(normalizeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponDeductAmount()))
                .couponConsumeAmount(normalizeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponConsumeAmount()))
                .couponWasteAmount(normalizeAmount(autoSelectResult == null ? null : autoSelectResult.getCouponWasteAmount()))
                .allocations(autoSelectResult == null || autoSelectResult.getAllocations() == null
                        ? List.of()
                        : autoSelectResult.getAllocations())
                .build();
    }

    /**
     * 构建结算商品项列表。
     *
     * @param orderItems      订单项列表
     * @param selectedCoupons 订单中选中的优惠券快照集合
     * @return 结算商品项列表
     */
    private List<CouponSettlementItemDto> buildSettlementItems(List<MallOrderItem> orderItems,
                                                               List<OrderCouponSnapshotDto> selectedCoupons) {
        Set<Long> eligibleProductIdSet = new LinkedHashSet<>();
        boolean allProductsEligible = false;
        if (selectedCoupons != null) {
            for (OrderCouponSnapshotDto selectedCoupon : selectedCoupons) {
                if (selectedCoupon == null) {
                    continue;
                }
                List<Long> eligibleProductIds = selectedCoupon.getEligibleProductIds();
                if (eligibleProductIds == null || eligibleProductIds.isEmpty()) {
                    allProductsEligible = true;
                    break;
                }
                for (Long eligibleProductId : eligibleProductIds) {
                    if (eligibleProductId != null) {
                        eligibleProductIdSet.add(eligibleProductId);
                    }
                }
            }
        }
        boolean allProductsEligibleFinal = allProductsEligible;
        return orderItems == null ? List.of() : orderItems.stream()
                .map(item -> CouponSettlementItemDto.builder()
                        .itemKey(String.valueOf(item.getId()))
                        .productId(item.getProductId())
                        .totalAmount(normalizeAmount(item.getTotalPrice()))
                        .couponEnabled(allProductsEligibleFinal || eligibleProductIdSet.contains(item.getProductId())
                                ? COUPON_ENABLED_FLAG : 0)
                        .build())
                .toList();
    }

    /**
     * 校验模板请求参数。
     *
     * @param request 模板请求
     */
    private void validateTemplateRequest(CouponTemplateAddRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "模板参数不能为空");
        }
        if (normalizeAmount(request.getFaceAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠券面额必须大于0");
        }
        if (normalizeAmount(request.getThresholdAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "使用门槛金额不能小于0");
        }
        if (CouponTemplateStatusEnum.fromCode(request.getStatus()) == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "模板状态不正确");
        }
    }

    /**
     * 校验发券请求参数。
     *
     * @param request 发券请求
     */
    private CouponIssueTargetTypeEnum validateIssueRequest(CouponIssueRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券参数不能为空");
        }
        validateEffectiveWindow(request.getEffectiveTime(), request.getExpireTime());
        CouponIssueTargetTypeEnum issueTargetType = CouponIssueTargetTypeEnum.fromCode(request.getIssueTargetType());
        if (issueTargetType == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券目标类型不正确");
        }
        if (issueTargetType == CouponIssueTargetTypeEnum.SPECIFIED && (request.getUserIds() == null || request.getUserIds().isEmpty())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "指定用户发券时用户列表不能为空");
        }
        return issueTargetType;
    }

    /**
     * 校验批量发券消息参数。
     *
     * @param message 批量发券消息
     * @return 发券目标类型枚举
     */
    private CouponIssueTargetTypeEnum validateBatchIssueMessage(CouponBatchIssueMessage message) {
        if (message == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券消息不能为空");
        }
        validateEffectiveWindow(message.getEffectiveTime(), message.getExpireTime());
        CouponIssueTargetTypeEnum issueTargetType = CouponIssueTargetTypeEnum.fromCode(message.getIssueTargetType());
        if (issueTargetType == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券目标类型不正确");
        }
        if (!StringUtils.hasText(message.getOperatorId())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券消息缺少操作人信息");
        }
        if (!StringUtils.hasText(message.getSourceBizNo())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券消息缺少业务批次号");
        }
        return issueTargetType;
    }

    /**
     * 校验发券消息参数。
     *
     * @param message 发券消息
     */
    private void validateIssueMessage(CouponIssueMessage message) {
        if (message == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息不能为空");
        }
        if (message.getTemplateId() == null || message.getUserId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息缺少模板或用户信息");
        }
        if (!StringUtils.hasText(message.getOperatorId())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息缺少操作人信息");
        }
        if (!StringUtils.hasText(message.getSourceBizNo())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息缺少业务批次号");
        }
    }

    /**
     * 构建批量发券消息分布式锁名称。
     *
     * @param message 批量发券消息
     * @return 分布式锁名称
     */
    private String buildCouponBatchIssueLockName(CouponBatchIssueMessage message) {
        if (message == null || !StringUtils.hasText(message.getSourceBizNo())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券消息缺少业务批次号");
        }
        return String.format(RedisConstants.Lock.COUPON_BATCH_ISSUE_KEY, message.getSourceBizNo().trim());
    }

    /**
     * 构建单用户发券消息分布式锁名称。
     *
     * @param message 发券消息
     * @return 分布式锁名称
     */
    private String buildCouponIssueLockName(CouponIssueMessage message) {
        if (message == null || !StringUtils.hasText(message.getSourceBizNo()) || message.getUserId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息缺少业务批次号或用户信息");
        }
        String businessKey = message.getSourceBizNo().trim() + ":" + message.getUserId();
        return String.format(RedisConstants.Lock.COUPON_ISSUE_KEY, businessKey);
    }

    /**
     * 隐藏模板。
     *
     * @param template   模板实体
     * @param now        当前时间
     * @param operatorId 操作人标识
     * @return 是否隐藏成功
     */
    private boolean hideTemplate(CouponTemplate template, Date now, String operatorId) {
        template.setIsDeleted(1);
        template.setUpdateTime(now);
        template.setUpdateBy(operatorId);
        return couponTemplateMapper.updateById(template) > 0;
    }

    /**
     * 将模板下已发可用券批量失效。
     *
     * @param template   模板实体
     * @param now        当前时间
     * @param operatorId 操作人标识
     */
    private void expireAvailableCouponsByTemplate(CouponTemplate template, Date now, String operatorId) {
        List<UserCoupon> availableCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, template.getId())
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .orderByAsc(UserCoupon::getId));
        String sourceBizNo = buildTemplateDeleteSourceBizNo(template.getId());
        for (UserCoupon userCoupon : availableCoupons) {
            boolean updated = expireUserCouponByTemplateDelete(userCoupon, now, operatorId);
            if (!updated) {
                continue;
            }
            writeCouponLog(CouponLog.builder()
                    .couponId(userCoupon.getId())
                    .userId(userCoupon.getUserId())
                    .changeType(CouponChangeTypeEnum.EXPIRE.getType())
                    .changeAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                    .deductAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                    .wasteAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                    .beforeAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .afterAvailableAmount(normalizeAmount(userCoupon.getAvailableAmount()))
                    .sourceType(CouponSourceTypeEnum.MANUAL.getType())
                    .sourceBizNo(sourceBizNo)
                    .remark(TEMPLATE_DELETE_EXPIRE_REMARK)
                    .operatorId(operatorId)
                    .build());
        }
    }

    /**
     * 将单张用户券更新为模板删除导致的失效状态。
     *
     * @param userCoupon 用户优惠券实体
     * @param now        当前时间
     * @param operatorId 操作人标识
     * @return 是否更新成功
     */
    private boolean expireUserCouponByTemplateDelete(UserCoupon userCoupon, Date now, String operatorId) {
        if (userCoupon == null || userCoupon.getId() == null) {
            return false;
        }
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getCouponStatus, UserCouponStatusEnum.AVAILABLE.getType())
                .set(UserCoupon::getCouponStatus, UserCouponStatusEnum.EXPIRED.getType())
                .set(UserCoupon::getUpdateTime, now)
                .set(UserCoupon::getUpdateBy, operatorId);
        return userCouponMapper.update(null, updateWrapper) > 0;
    }

    /**
     * 校验指定用户ID列表。
     *
     * @param userIds 用户ID列表
     * @return 规范化后的用户ID列表
     */
    private List<Long> validateSpecifiedUserIds(List<Long> userIds) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "指定用户发券时用户列表不能为空");
        }
        List<User> enabledUsers = listEnabledUsersByIds(normalizedUserIds);
        if (enabledUsers.size() != normalizedUserIds.size()) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "指定用户中存在禁用或不存在的用户");
        }
        return normalizedUserIds;
    }

    /**
     * 发布全部用户发券消息。
     *
     * @param message 批量发券消息
     */
    private void publishIssueMessagesForAllUsers(CouponBatchIssueMessage message) {
        long currentPage = 1L;
        while (true) {
            Page<User> page = new Page<>(currentPage, BATCH_ISSUE_PAGE_SIZE);
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getStatus, USER_ENABLED_STATUS)
                    .eq(User::getIsDelete, 0)
                    .orderByAsc(User::getId);
            Page<User> userPage = userMapper.selectPage(page, queryWrapper);
            List<Long> userIds = userPage.getRecords().stream()
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .toList();
            publishSingleIssueMessages(userIds, message);
            if (currentPage >= userPage.getPages()) {
                return;
            }
            currentPage++;
        }
    }

    /**
     * 批量发布单用户发券消息。
     *
     * @param userIds 用户ID列表
     * @param message 批量发券消息
     */
    private void publishSingleIssueMessages(List<Long> userIds, CouponBatchIssueMessage message) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            couponIssueMessagePublisher.publish(CouponIssueMessage.builder()
                    .templateId(message.getTemplateId())
                    .userId(userId)
                    .effectiveTime(message.getEffectiveTime())
                    .expireTime(message.getExpireTime())
                    .sourceBizNo(message.getSourceBizNo())
                    .remark(message.getRemark())
                    .operatorId(message.getOperatorId())
                    .build());
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
     * 校验模板状态是否允许发券。
     *
     * @param template 优惠券模板
     */
    private void validateTemplateStatusForIssue(CouponTemplate template) {
        if (!Objects.equals(template.getStatus(), CouponTemplateStatusEnum.ACTIVE.getType())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "当前模板状态不允许发券");
        }
    }

    /**
     * 校验模板名称是否重复。
     *
     * @param id   模板ID
     * @param name 模板名称
     */
    private void assertTemplateNameUnique(Long id, String name) {
        Long duplicateCount = couponTemplateMapper.selectCount(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getName, name == null ? null : name.trim())
                .ne(id != null, CouponTemplate::getId, id));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "优惠券模板名称已存在");
        }
    }

    /**
     * 查询模板实体。
     *
     * @param id 模板ID
     * @return 模板实体
     */
    private CouponTemplate getTemplateEntity(Long id) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "优惠券模板不存在");
        }
        return template;
    }

    /**
     * 根据用户ID列表查询启用用户。
     *
     * @param userIds 用户ID列表
     * @return 启用用户列表
     */
    private List<User> listEnabledUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .filter(Objects::nonNull)
                .filter(user -> Objects.equals(user.getStatus(), USER_ENABLED_STATUS))
                .filter(user -> !Objects.equals(user.getIsDelete(), 1))
                .toList();
    }

    /**
     * 构建用户信息映射。
     *
     * @param userIds 用户ID列表
     * @return 用户映射
     */
    private Map<Long, User> listUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(normalizedUserIds);
        Map<Long, User> userMap = new HashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                userMap.put(user.getId(), user);
            }
        }
        return userMap;
    }

    /**
     * 构建优惠券名称映射。
     *
     * @param couponIds 用户优惠券ID列表
     * @return 优惠券名称映射
     */
    private Map<Long, String> listCouponNameMap(List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return Map.of();
        }
        List<UserCoupon> userCoupons = userCouponMapper.selectBatchIds(couponIds);
        Map<Long, String> couponNameMap = new HashMap<>();
        for (UserCoupon userCoupon : userCoupons) {
            couponNameMap.put(userCoupon.getId(), userCoupon.getCouponNameSnapshot());
        }
        return couponNameMap;
    }

    /**
     * 写入优惠券日志。
     *
     * @param couponLog 优惠券日志
     */
    private void writeCouponLog(CouponLog couponLog) {
        couponLog.setCreateTime(new Date());
        couponLog.setIsDeleted(0);
        couponLogMapper.insert(couponLog);
    }

    /**
     * 转换为模板视图对象。
     *
     * @param template 模板实体
     * @return 模板视图对象
     */
    private CouponTemplateVo toTemplateVo(CouponTemplate template) {
        return CouponTemplateVo.builder()
                .id(template.getId())
                .couponType(template.getCouponType())
                .name(template.getName())
                .thresholdAmount(template.getThresholdAmount())
                .faceAmount(template.getFaceAmount())
                .continueUseEnabled(template.getContinueUseEnabled())
                .stackableEnabled(template.getStackableEnabled())
                .status(template.getStatus())
                .remark(template.getRemark())
                .createTime(template.getCreateTime())
                .build();
    }

    /**
     * 转换为管理端用户优惠券视图对象。
     *
     * @param userCoupon 用户优惠券实体
     * @param user       用户实体
     * @return 管理端用户优惠券视图对象
     */
    private AdminUserCouponVo toAdminUserCouponVo(UserCoupon userCoupon, User user) {
        return AdminUserCouponVo.builder()
                .couponId(userCoupon.getId())
                .templateId(userCoupon.getTemplateId())
                .userId(userCoupon.getUserId())
                .userNickname(user == null ? null : user.getNickname())
                .userPhoneNumber(user == null ? null : user.getPhoneNumber())
                .couponName(userCoupon.getCouponNameSnapshot())
                .thresholdAmount(userCoupon.getThresholdAmount())
                .totalAmount(userCoupon.getTotalAmount())
                .availableAmount(userCoupon.getAvailableAmount())
                .lockedConsumeAmount(userCoupon.getLockedConsumeAmount())
                .couponStatus(userCoupon.getCouponStatus())
                .continueUseEnabled(userCoupon.getContinueUseEnabled())
                .effectiveTime(userCoupon.getEffectiveTime())
                .expireTime(userCoupon.getExpireTime())
                .lockOrderNo(userCoupon.getLockOrderNo())
                .lockTime(userCoupon.getLockTime())
                .sourceType(userCoupon.getSourceType())
                .createTime(userCoupon.getCreateTime())
                .build();
    }

    /**
     * 转换为优惠券日志视图对象。
     *
     * @param couponLog    优惠券日志实体
     * @param couponName   优惠券名称
     * @param user         用户实体
     * @param operatorUser 操作人用户实体
     * @return 优惠券日志视图对象
     */
    private CouponLogVo toCouponLogVo(CouponLog couponLog,
                                      String couponName,
                                      User user,
                                      User operatorUser) {
        return CouponLogVo.builder()
                .id(couponLog.getId())
                .couponId(couponLog.getCouponId())
                .couponName(couponName)
                .userId(couponLog.getUserId())
                .userName(user == null ? null : user.getUsername())
                .orderNo(couponLog.getOrderNo())
                .changeType(couponLog.getChangeType())
                .changeAmount(couponLog.getChangeAmount())
                .deductAmount(couponLog.getDeductAmount())
                .wasteAmount(couponLog.getWasteAmount())
                .beforeAvailableAmount(couponLog.getBeforeAvailableAmount())
                .afterAvailableAmount(couponLog.getAfterAvailableAmount())
                .sourceType(couponLog.getSourceType())
                .sourceBizNo(couponLog.getSourceBizNo())
                .remark(couponLog.getRemark())
                .operatorId(couponLog.getOperatorId())
                .operatorName(resolveOperatorName(couponLog.getOperatorId(), operatorUser))
                .createTime(couponLog.getCreateTime())
                .build();
    }

    /**
     * 查询操作人用户映射。
     *
     * @param operatorIds 操作人标识列表
     * @return 操作人用户映射
     */
    private Map<Long, User> listOperatorUserMap(List<String> operatorIds) {
        if (operatorIds == null || operatorIds.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = operatorIds.stream()
                .map(this::parseOperatorUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return listUserMap(userIds);
    }

    /**
     * 解析操作人用户实体。
     *
     * @param operatorUserMap 操作人用户映射
     * @param operatorId      操作人标识
     * @return 操作人用户实体
     */
    private User resolveOperatorUser(Map<Long, User> operatorUserMap, String operatorId) {
        Long operatorUserId = parseOperatorUserId(operatorId);
        if (operatorUserId == null || operatorUserMap == null || operatorUserMap.isEmpty()) {
            return null;
        }
        return operatorUserMap.get(operatorUserId);
    }

    /**
     * 解析操作人展示名称。
     *
     * @param operatorId   操作人标识
     * @param operatorUser 操作人用户实体
     * @return 操作人展示名称
     */
    private String resolveOperatorName(String operatorId, User operatorUser) {
        if (operatorUser != null && StringUtils.hasText(operatorUser.getUsername())) {
            return operatorUser.getUsername();
        }
        return operatorId;
    }

    /**
     * 解析操作人用户ID。
     *
     * @param operatorId 操作人标识
     * @return 操作人用户ID
     */
    private Long parseOperatorUserId(String operatorId) {
        if (!StringUtils.hasText(operatorId)) {
            return null;
        }
        try {
            return Long.parseLong(operatorId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 规范化金额字段。
     *
     * @param amount 原始金额
     * @return 规范化后的金额
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 查询用户实体。
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    private User getExistingUserEntity(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "用户不存在");
        }
        return user;
    }

    /**
     * 规范化用户ID列表。
     *
     * @param userIds 原始用户ID列表
     * @return 规范化后的用户ID列表
     */
    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> userIdSet = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null && userId > 0) {
                userIdSet.add(userId);
            }
        }
        return new ArrayList<>(userIdSet);
    }

    /**
     * 生成管理端发券来源业务号。
     *
     * @return 来源业务号
     */
    private String generateIssueSourceBizNo() {
        String timeText = LocalDateTime.now().format(ADMIN_ISSUE_SOURCE_BIZ_NO_TIME_FORMATTER);
        String randomText = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return ADMIN_ISSUE_SOURCE_BIZ_NO_PREFIX + "_" + timeText + "_" + randomText;
    }

    /**
     * 构建模板删除来源业务号。
     *
     * @param templateId 模板ID
     * @return 来源业务号
     */
    private String buildTemplateDeleteSourceBizNo(Long templateId) {
        return TEMPLATE_DELETE_SOURCE_BIZ_NO_PREFIX + "_" + templateId;
    }
}
