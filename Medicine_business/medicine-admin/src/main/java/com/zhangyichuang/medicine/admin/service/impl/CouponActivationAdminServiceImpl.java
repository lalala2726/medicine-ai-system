package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.mapper.CouponTemplateMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.CouponActivationAdminService;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.excel.support.ExcelExportSupport;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.coupon.*;
import com.zhangyichuang.medicine.model.entity.CouponActivationBatch;
import com.zhangyichuang.medicine.model.entity.CouponActivationCode;
import com.zhangyichuang.medicine.model.entity.CouponTemplate;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.*;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationBatchMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationCodeMapper;
import com.zhangyichuang.medicine.shared.mapper.BasicCouponActivationLogMapper;
import com.zhangyichuang.medicine.shared.utils.CouponActivationCodeUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 管理端激活码服务实现。
 */
@Service
@RequiredArgsConstructor
public class CouponActivationAdminServiceImpl implements CouponActivationAdminService, BaseService {

    /**
     * 共享码固定生成数量。
     */
    private static final int SHARED_PER_USER_ONCE_CODE_GENERATE_COUNT = 1;

    /**
     * 单批唯一码最大生成数量。
     */
    private static final int UNIQUE_SINGLE_USE_CODE_GENERATE_MAX_COUNT = 1000;

    /**
     * 激活码批量生成最大重试次数。
     */
    private static final int MAX_BATCH_GENERATE_RETRY_COUNT = 3;

    /**
     * 激活码导出工作表名称。
     */
    private static final String ACTIVATION_CODE_EXPORT_SHEET_NAME = "激活码列表";

    /**
     * 激活码导出文件名前缀。
     */
    private static final String ACTIVATION_CODE_EXPORT_FILE_NAME_PREFIX = "激活码列表-";

    /**
     * Excel 导出文件后缀。
     */
    private static final String EXCEL_FILE_SUFFIX = ".xlsx";

    /**
     * 激活码导出列数量。
     */
    private static final int ACTIVATION_CODE_EXPORT_COLUMN_COUNT = 6;

    /**
     * 导出说明头时间格式。
     */
    private static final String EXPORT_HEADER_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 用户批量查询分片大小。
     */
    private static final int USER_QUERY_BATCH_SIZE = 500;

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
     * 优惠券模板 Mapper。
     */
    private final CouponTemplateMapper couponTemplateMapper;

    /**
     * 用户 Mapper。
     */
    private final UserMapper userMapper;

    /**
     * 验证码服务。
     */
    private final CaptchaService captchaService;

    /**
     * 生成激活码批次。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivationCodeGenerateResultVo generateActivationCodes(ActivationCodeGenerateRequest request) {
        if (request == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码批次生成参数不能为空");
        }
        captchaService.validateLoginCaptcha(request.getCaptchaVerificationId());
        ActivationRedeemRuleTypeEnum redeemRuleType = validateRedeemRuleType(request.getRedeemRuleType());
        ActivationCodeValidityTypeEnum validityType = validateValidityType(request.getValidityType());
        CouponTemplate template = getActiveTemplate(request.getTemplateId());
        validateGenerateRequest(request, redeemRuleType, validityType);

        int generateCount = redeemRuleType == ActivationRedeemRuleTypeEnum.SHARED_PER_USER_ONCE
                ? SHARED_PER_USER_ONCE_CODE_GENERATE_COUNT
                : request.getGenerateCount();
        String batchNo = CouponActivationCodeUtils.generateBatchNo();
        Date now = new Date();
        String operatorId = getUsername();

        CouponActivationBatch activationBatch = CouponActivationBatch.builder()
                .batchNo(batchNo)
                .templateId(template.getId())
                .redeemRuleType(redeemRuleType.getType())
                .validityType(validityType.getType())
                .fixedEffectiveTime(request.getFixedEffectiveTime())
                .fixedExpireTime(request.getFixedExpireTime())
                .relativeValidDays(request.getRelativeValidDays())
                .status(ActivationCodeStatusEnum.ACTIVE.getType())
                .generateCount(generateCount)
                .successUseCount(0)
                .remark(request.getRemark())
                .version(0)
                .createTime(now)
                .updateTime(now)
                .createBy(operatorId)
                .updateBy(operatorId)
                .isDeleted(0)
                .build();
        boolean inserted = activationBatchMapper.insert(activationBatch) > 0;
        if (!inserted) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码批次创建失败");
        }

        List<ActivationCodeGeneratedItemVo> generatedCodes =
                generateBatchCodes(activationBatch.getId(), generateCount, operatorId, now);
        return ActivationCodeGenerateResultVo.builder()
                .batchNo(batchNo)
                .templateId(template.getId())
                .templateName(template.getName())
                .redeemRuleType(redeemRuleType.getType())
                .validityType(validityType.getType())
                .generatedCount(generatedCodes.size())
                .codes(generatedCodes)
                .build();
    }

    /**
     * 查询激活码批次详情。
     *
     * @param id 激活码批次ID
     * @return 激活码批次详情
     */
    @Override
    public ActivationCodeDetailVo getActivationCodeDetail(Long id) {
        if (id == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码批次ID不能为空");
        }
        ActivationBatchRowDto batchRow = activationBatchMapper.selectBatchDetail(id);
        if (batchRow == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码批次不存在");
        }
        return toActivationCodeDetailVo(batchRow);
    }

    /**
     * 查询激活码批次列表。
     *
     * @param request 查询请求
     * @return 激活码批次分页结果
     */
    @Override
    public Page<ActivationCodeVo> listActivationCodes(ActivationCodeListRequest request) {
        return mapBatchPage(selectBatchPage(request));
    }

    /**
     * 查询批次下的全部激活码。
     *
     * @param batchId 批次ID
     * @param request 分页查询请求
     * @return 激活码明细分页结果
     */
    @Override
    public Page<ActivationCodeGeneratedItemVo> listActivationBatchCodes(Long batchId,
                                                                        ActivationBatchCodeListRequest request) {
        if (batchId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批次ID不能为空");
        }
        ActivationBatchRowDto batchRow = activationBatchMapper.selectBatchDetail(batchId);
        if (batchRow == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码批次不存在");
        }
        return mapCodeItemPage(selectCodeItemPage(batchId, request));
    }

    /**
     * 导出批次下的全部激活码。
     *
     * @param batchId  批次ID
     * @param response Http 响应对象
     * @return 无返回值
     * @throws IOException IO 异常
     */
    @Override
    public void exportActivationBatchCodes(Long batchId, HttpServletResponse response) throws IOException {
        if (batchId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批次ID不能为空");
        }
        ActivationBatchRowDto batchRow = activationBatchMapper.selectBatchDetail(batchId);
        if (batchRow == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码批次不存在");
        }
        List<ActivationCodeRowDto> codeRows = activationCodeMapper.selectCodesByBatchId(batchId);
        List<ActivationCodeExportVo> exportRows = mapCodeRowsToExportVos(codeRows);
        ExcelExportSupport.export(
                response,
                buildActivationCodeExportFileName(batchRow.getBatchNo()),
                ACTIVATION_CODE_EXPORT_SHEET_NAME,
                ActivationCodeExportVo.class,
                exportRows,
                buildActivationCodeExportHeaderLines(batchRow),
                ACTIVATION_CODE_EXPORT_COLUMN_COUNT
        );
    }

    /**
     * 更新激活码批次状态。
     *
     * @param request 状态更新请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActivationCodeStatus(ActivationCodeStatusUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码批次ID不能为空");
        }
        ActivationCodeStatusEnum statusEnum = validateBatchStatus(request.getStatus());
        CouponActivationBatch activationBatch = activationBatchMapper.selectById(request.getId());
        if (activationBatch == null || Objects.equals(activationBatch.getIsDeleted(), 1)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码批次不存在");
        }
        Date now = new Date();
        String operatorId = getUsername();
        int updated = activationBatchMapper.updateBatchStatus(request.getId(), statusEnum.getType(), operatorId, now);
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码批次状态更新失败");
        }
        if (ActivationCodeStatusEnum.DISABLED == statusEnum) {
            activationCodeMapper.updateCodeStatusByBatchId(
                    request.getId(),
                    ActivationCodeItemStatusEnum.ACTIVE.getType(),
                    ActivationCodeItemStatusEnum.DISABLED.getType(),
                    operatorId,
                    now
            );
        } else {
            activationCodeMapper.updateCodeStatusByBatchId(
                    request.getId(),
                    ActivationCodeItemStatusEnum.DISABLED.getType(),
                    ActivationCodeItemStatusEnum.ACTIVE.getType(),
                    operatorId,
                    now
            );
        }
        return true;
    }

    /**
     * 删除激活码批次。
     *
     * @param id 激活码批次ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteActivationBatch(Long id) {
        if (id == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码批次ID不能为空");
        }
        CouponActivationBatch activationBatch = activationBatchMapper.selectById(id);
        if (activationBatch == null || Objects.equals(activationBatch.getIsDeleted(), 1)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码批次不存在");
        }
        Date now = new Date();
        String operatorId = getUsername();
        int batchUpdated = activationBatchMapper.softDeleteBatchById(id, operatorId, now);
        if (batchUpdated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码批次删除失败");
        }
        activationCodeMapper.softDeleteCodeByBatchId(id, operatorId, now);
        return true;
    }

    /**
     * 更新激活码单码状态。
     *
     * @param request 状态更新请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActivationCodeItemStatus(ActivationCodeItemStatusUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码ID不能为空");
        }
        ActivationCodeItemStatusEnum statusEnum = validateCodeItemStatus(request.getStatus());
        CouponActivationCode activationCode = activationCodeMapper.selectById(request.getId());
        if (activationCode == null || Objects.equals(activationCode.getIsDeleted(), 1)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码不存在");
        }
        ActivationCodeItemStatusEnum currentStatus = ActivationCodeItemStatusEnum.fromCode(activationCode.getStatus());
        if (currentStatus == ActivationCodeItemStatusEnum.USED) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "已使用激活码不允许变更状态");
        }
        if (currentStatus == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码当前状态不正确");
        }
        if (Objects.equals(activationCode.getStatus(), statusEnum.getType())) {
            return true;
        }
        Date now = new Date();
        String operatorId = getUsername();
        int updated = activationCodeMapper.updateCodeStatusById(
                request.getId(),
                activationCode.getStatus(),
                statusEnum.getType(),
                operatorId,
                now
        );
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码状态更新失败");
        }
        return true;
    }

    /**
     * 删除激活码单码。
     *
     * @param id 激活码ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteActivationCodeItem(Long id) {
        if (id == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码ID不能为空");
        }
        CouponActivationCode activationCode = activationCodeMapper.selectById(id);
        if (activationCode == null || Objects.equals(activationCode.getIsDeleted(), 1)) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "激活码不存在");
        }
        Date now = new Date();
        String operatorId = getUsername();
        int updated = activationCodeMapper.softDeleteCodeById(id, operatorId, now);
        if (updated <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "激活码删除失败");
        }
        return true;
    }

    /**
     * 查询激活码兑换日志列表。
     *
     * @param request 查询请求
     * @return 激活码兑换日志分页结果
     */
    @Override
    public Page<ActivationLogVo> listActivationLogs(ActivationLogListRequest request) {
        return mapRedeemLogPage(selectRedeemLogPage(buildRedeemLogQuery(request), request));
    }

    /**
     * 查询指定用户激活码兑换日志列表。
     *
     * @param userId  用户ID
     * @param request 查询请求
     * @return 激活码兑换日志分页结果
     */
    @Override
    public Page<ActivationLogVo> listUserActivationLogs(Long userId, ActivationLogListRequest request) {
        if (userId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "用户ID不能为空");
        }
        ActivationLogListRequest safeRequest = request == null ? new ActivationLogListRequest() : request;
        safeRequest.setUserId(userId);
        return mapRedeemLogPage(selectRedeemLogPage(buildRedeemLogQuery(safeRequest), safeRequest));
    }

    /**
     * 批量生成激活码并写入数据库。
     *
     * @param batchId       激活码批次ID
     * @param generateCount 生成数量
     * @param operatorId    操作人标识
     * @param now           当前时间
     * @return 激活码明细视图对象列表
     */
    private List<ActivationCodeGeneratedItemVo> generateBatchCodes(Long batchId,
                                                                   int generateCount,
                                                                   String operatorId,
                                                                   Date now) {
        Set<String> generatedCodeHashSet = new HashSet<>(Math.max(generateCount * 2, 16));
        int remainingCount = generateCount;
        int retryCount = 0;
        while (remainingCount > 0 && retryCount < MAX_BATCH_GENERATE_RETRY_COUNT) {
            List<CouponActivationCode> candidateCodes = buildActivationCodeCandidates(
                    batchId,
                    remainingCount,
                    operatorId,
                    now,
                    generatedCodeHashSet
            );
            int insertedCount = activationCodeMapper.batchInsertIgnore(candidateCodes);
            remainingCount -= insertedCount;
            retryCount++;
        }
        if (remainingCount > 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "生成激活码失败，请稍后重试");
        }
        List<ActivationCodeRowDto> codeRows = activationCodeMapper.selectCodesByBatchId(batchId);
        return codeRows.stream()
                .map(row -> toActivationCodeGeneratedItemVo(row, null))
                .toList();
    }

    /**
     * 构建批量入库候选激活码列表。
     *
     * @param batchId              激活码批次ID
     * @param generateCount        本轮生成数量
     * @param operatorId           操作人标识
     * @param now                  当前时间
     * @param generatedCodeHashSet 本次批量生成过程已生成过的激活码哈希集合
     * @return 候选激活码实体列表
     */
    private List<CouponActivationCode> buildActivationCodeCandidates(Long batchId,
                                                                     int generateCount,
                                                                     String operatorId,
                                                                     Date now,
                                                                     Set<String> generatedCodeHashSet) {
        List<CouponActivationCode> candidateCodes = new ArrayList<>(generateCount);
        while (candidateCodes.size() < generateCount) {
            String plainCode = CouponActivationCodeUtils.generateCode();
            String codeHash = CouponActivationCodeUtils.hashCode(plainCode);
            if (!generatedCodeHashSet.add(codeHash)) {
                continue;
            }
            candidateCodes.add(CouponActivationCode.builder()
                    .batchId(batchId)
                    .codeHash(codeHash)
                    .plainCode(plainCode)
                    .status(ActivationCodeItemStatusEnum.ACTIVE.getType())
                    .successUseCount(0)
                    .version(0)
                    .createTime(now)
                    .updateTime(now)
                    .createBy(operatorId)
                    .updateBy(operatorId)
                    .isDeleted(0)
                    .build());
        }
        return candidateCodes;
    }

    /**
     * 查询激活码批次分页结果。
     *
     * @param request 查询请求
     * @return 激活码批次分页结果
     */
    private Page<ActivationBatchRowDto> selectBatchPage(ActivationCodeListRequest request) {
        ActivationCodeListRequest safeRequest = request == null ? new ActivationCodeListRequest() : request;
        Page<ActivationBatchRowDto> page = activationBatchMapper.selectBatchPage(
                safeRequest.toPage(),
                buildBatchQuery(safeRequest)
        );
        return page == null ? new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize(), 0) : page;
    }

    /**
     * 查询批次激活码分页结果。
     *
     * @param batchId 批次ID
     * @param request 分页查询请求
     * @return 批次激活码分页结果
     */
    private Page<ActivationCodeRowDto> selectCodeItemPage(Long batchId, ActivationBatchCodeListRequest request) {
        ActivationBatchCodeListRequest safeRequest = request == null ? new ActivationBatchCodeListRequest() : request;
        Page<ActivationCodeRowDto> page = activationCodeMapper.selectCodePageByBatchId(safeRequest.toPage(), batchId);
        return page == null ? new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize(), 0) : page;
    }

    /**
     * 查询激活码兑换日志分页结果。
     *
     * @param query   查询参数
     * @param request 原始请求
     * @return 激活码兑换日志分页结果
     */
    private Page<ActivationRedeemLogRowDto> selectRedeemLogPage(ActivationRedeemLogQueryDto query,
                                                                ActivationLogListRequest request) {
        ActivationLogListRequest safeRequest = request == null ? new ActivationLogListRequest() : request;
        Page<ActivationRedeemLogRowDto> page = activationLogMapper.selectRedeemLogPage(safeRequest.toPage(), query);
        return page == null ? new Page<>(safeRequest.getPageNum(), safeRequest.getPageSize(), 0) : page;
    }

    /**
     * 构建激活码批次查询参数。
     *
     * @param request 查询请求
     * @return 激活码批次查询参数
     */
    private ActivationBatchQueryDto buildBatchQuery(ActivationCodeListRequest request) {
        return ActivationBatchQueryDto.builder()
                .batchNo(normalizeNullableText(request.getBatchNo()))
                .templateId(request.getTemplateId())
                .templateName(normalizeNullableText(request.getTemplateName()))
                .redeemRuleType(normalizeNullableText(request.getRedeemRuleType()))
                .validityType(normalizeNullableText(request.getValidityType()))
                .status(normalizeNullableText(request.getStatus()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    /**
     * 构建激活码兑换日志查询参数。
     *
     * @param request 查询请求
     * @return 激活码兑换日志查询参数
     */
    private ActivationRedeemLogQueryDto buildRedeemLogQuery(ActivationLogListRequest request) {
        return ActivationRedeemLogQueryDto.builder()
                .batchId(request.getBatchId())
                .activationCodeId(request.getActivationCodeId())
                .batchNo(normalizeNullableText(request.getBatchNo()))
                .plainCode(normalizeNullableText(request.getPlainCode()))
                .userId(request.getUserId())
                .resultStatus(normalizeNullableText(request.getResultStatus()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    /**
     * 校验兑换规则类型。
     *
     * @param redeemRuleType 兑换规则类型编码
     * @return 兑换规则类型枚举
     */
    private ActivationRedeemRuleTypeEnum validateRedeemRuleType(String redeemRuleType) {
        ActivationRedeemRuleTypeEnum redeemRuleTypeEnum = ActivationRedeemRuleTypeEnum.fromCode(redeemRuleType);
        if (redeemRuleTypeEnum == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "兑换规则类型不正确");
        }
        return redeemRuleTypeEnum;
    }

    /**
     * 校验有效期类型。
     *
     * @param validityType 有效期类型编码
     * @return 有效期类型枚举
     */
    private ActivationCodeValidityTypeEnum validateValidityType(String validityType) {
        ActivationCodeValidityTypeEnum validityTypeEnum = ActivationCodeValidityTypeEnum.fromCode(validityType);
        if (validityTypeEnum == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码有效期类型不正确");
        }
        return validityTypeEnum;
    }

    /**
     * 校验批次状态。
     *
     * @param status 批次状态编码
     * @return 批次状态枚举
     */
    private ActivationCodeStatusEnum validateBatchStatus(String status) {
        ActivationCodeStatusEnum statusEnum = ActivationCodeStatusEnum.fromCode(status);
        if (statusEnum == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码批次状态不正确");
        }
        return statusEnum;
    }

    /**
     * 校验单码状态。
     *
     * @param status 单码状态编码
     * @return 单码状态枚举
     */
    private ActivationCodeItemStatusEnum validateCodeItemStatus(String status) {
        ActivationCodeItemStatusEnum statusEnum = ActivationCodeItemStatusEnum.fromCode(status);
        if (statusEnum == null || statusEnum == ActivationCodeItemStatusEnum.USED) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码状态不正确");
        }
        return statusEnum;
    }

    /**
     * 校验生成请求。
     *
     * @param request        生成请求
     * @param redeemRuleType 兑换规则类型
     * @param validityType   有效期类型
     * @return 无返回值
     */
    private void validateGenerateRequest(ActivationCodeGenerateRequest request,
                                         ActivationRedeemRuleTypeEnum redeemRuleType,
                                         ActivationCodeValidityTypeEnum validityType) {
        if (redeemRuleType == ActivationRedeemRuleTypeEnum.UNIQUE_SINGLE_USE) {
            if (request.getGenerateCount() == null || request.getGenerateCount() <= 0) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "批量唯一码生成数量必须大于0");
            }
            if (request.getGenerateCount() > UNIQUE_SINGLE_USE_CODE_GENERATE_MAX_COUNT) {
                throw new ServiceException(ResponseCode.PARAM_ERROR,
                        "批量唯一码单次最多生成" + UNIQUE_SINGLE_USE_CODE_GENERATE_MAX_COUNT + "个");
            }
        }
        if (validityType == ActivationCodeValidityTypeEnum.ONCE) {
            if (request.getFixedEffectiveTime() == null || request.getFixedExpireTime() == null) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "一次性类型必须填写生效时间和失效时间");
            }
            if (!request.getFixedEffectiveTime().before(request.getFixedExpireTime())) {
                throw new ServiceException(ResponseCode.PARAM_ERROR, "固定失效时间必须晚于生效时间");
            }
        } else if (request.getRelativeValidDays() == null || request.getRelativeValidDays() <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活后有效天数必须大于0");
        }
    }

    /**
     * 查询启用中的优惠券模板。
     *
     * @param templateId 优惠券模板ID
     * @return 优惠券模板实体
     */
    private CouponTemplate getActiveTemplate(Long templateId) {
        if (templateId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "优惠券模板ID不能为空");
        }
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "优惠券模板不存在");
        }
        if (!Objects.equals(template.getStatus(), CouponTemplateStatusEnum.ACTIVE.getType())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "当前模板状态不允许生成激活码");
        }
        return template;
    }

    /**
     * 转换为激活码批次分页结果。
     *
     * @param sourcePage 原始分页结果
     * @return 激活码批次分页结果
     */
    private Page<ActivationCodeVo> mapBatchPage(Page<ActivationBatchRowDto> sourcePage) {
        Page<ActivationCodeVo> result = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        result.setRecords(sourcePage.getRecords().stream().map(this::toActivationCodeVo).toList());
        return result;
    }

    /**
     * 转换为激活码明细分页结果。
     *
     * @param sourcePage 原始分页结果
     * @return 激活码明细分页结果
     */
    private Page<ActivationCodeGeneratedItemVo> mapCodeItemPage(Page<ActivationCodeRowDto> sourcePage) {
        Page<ActivationCodeGeneratedItemVo> result =
                new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        Map<Long, User> lastSuccessUserMap = listUserMap(sourcePage.getRecords().stream()
                .map(ActivationCodeRowDto::getLastSuccessUserId)
                .distinct()
                .toList());
        result.setRecords(sourcePage.getRecords().stream()
                .map(row -> {
                    Long lastSuccessUserId = row.getLastSuccessUserId();
                    User lastSuccessUser = lastSuccessUserId == null ? null : lastSuccessUserMap.get(lastSuccessUserId);
                    return toActivationCodeGeneratedItemVo(row, lastSuccessUser);
                })
                .toList());
        return result;
    }

    /**
     * 转换为激活码导出视图对象列表。
     *
     * @param codeRows 激活码明细列表
     * @return 激活码导出视图对象列表
     */
    private List<ActivationCodeExportVo> mapCodeRowsToExportVos(List<ActivationCodeRowDto> codeRows) {
        if (codeRows == null || codeRows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> lastSuccessUserMap = listUserMap(codeRows.stream()
                .map(ActivationCodeRowDto::getLastSuccessUserId)
                .distinct()
                .toList());
        return codeRows.stream()
                .map(row -> {
                    Long lastSuccessUserId = row.getLastSuccessUserId();
                    User lastSuccessUser = lastSuccessUserId == null ? null : lastSuccessUserMap.get(lastSuccessUserId);
                    return toActivationCodeExportVo(row, lastSuccessUser);
                })
                .toList();
    }

    /**
     * 转换为激活码兑换日志分页结果。
     *
     * @param sourcePage 原始分页结果
     * @return 激活码兑换日志分页结果
     */
    private Page<ActivationLogVo> mapRedeemLogPage(Page<ActivationRedeemLogRowDto> sourcePage) {
        Page<ActivationLogVo> result = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        Map<Long, User> userMap = listUserMap(sourcePage.getRecords().stream().map(ActivationRedeemLogRowDto::getUserId).distinct().toList());
        result.setRecords(sourcePage.getRecords().stream()
                .map(row -> {
                    Long userId = row.getUserId();
                    User user = userId == null ? null : userMap.get(userId);
                    return toActivationLogVo(row, user);
                })
                .toList());
        return result;
    }

    /**
     * 转换为激活码批次视图对象。
     *
     * @param row 激活码批次查询结果
     * @return 激活码批次视图对象
     */
    private ActivationCodeVo toActivationCodeVo(ActivationBatchRowDto row) {
        return ActivationCodeVo.builder()
                .id(row.getId())
                .batchNo(row.getBatchNo())
                .templateId(row.getTemplateId())
                .templateName(row.getTemplateName())
                .redeemRuleType(row.getRedeemRuleType())
                .validityType(row.getValidityType())
                .fixedEffectiveTime(row.getFixedEffectiveTime())
                .fixedExpireTime(row.getFixedExpireTime())
                .relativeValidDays(row.getRelativeValidDays())
                .status(row.getStatus())
                .generateCount(row.getGenerateCount())
                .successUseCount(row.getSuccessUseCount())
                .remark(row.getRemark())
                .createTime(row.getCreateTime())
                .createBy(row.getCreateBy())
                .build();
    }

    /**
     * 转换为激活码批次详情视图对象。
     *
     * @param row 激活码批次查询结果
     * @return 激活码批次详情视图对象
     */
    private ActivationCodeDetailVo toActivationCodeDetailVo(ActivationBatchRowDto row) {
        return ActivationCodeDetailVo.builder()
                .id(row.getId())
                .batchNo(row.getBatchNo())
                .templateId(row.getTemplateId())
                .templateName(row.getTemplateName())
                .redeemRuleType(row.getRedeemRuleType())
                .validityType(row.getValidityType())
                .fixedEffectiveTime(row.getFixedEffectiveTime())
                .fixedExpireTime(row.getFixedExpireTime())
                .relativeValidDays(row.getRelativeValidDays())
                .status(row.getStatus())
                .generateCount(row.getGenerateCount())
                .successUseCount(row.getSuccessUseCount())
                .remark(row.getRemark())
                .createTime(row.getCreateTime())
                .createBy(row.getCreateBy())
                .build();
    }

    /**
     * 转换为激活码明细视图对象。
     *
     * @param row 激活码明细查询结果
     * @return 激活码明细视图对象
     */
    private ActivationCodeGeneratedItemVo toActivationCodeGeneratedItemVo(ActivationCodeRowDto row, User lastSuccessUser) {
        return ActivationCodeGeneratedItemVo.builder()
                .id(row.getId())
                .plainCode(row.getPlainCode())
                .status(row.getStatus())
                .successUseCount(row.getSuccessUseCount())
                .createTime(row.getCreateTime())
                .lastSuccessTime(row.getLastSuccessTime())
                .lastSuccessClientIp(row.getLastSuccessClientIp())
                .lastSuccessUserId(row.getLastSuccessUserId())
                .lastSuccessUserName(resolveLastSuccessUserName(row.getLastSuccessUserId(), lastSuccessUser))
                .build();
    }

    /**
     * 转换为激活码导出视图对象。
     *
     * @param row             激活码明细查询结果
     * @param lastSuccessUser 最近一次成功激活用户实体
     * @return 激活码导出视图对象
     */
    private ActivationCodeExportVo toActivationCodeExportVo(ActivationCodeRowDto row, User lastSuccessUser) {
        return ActivationCodeExportVo.builder()
                .plainCode(row.getPlainCode())
                .statusText(resolveActivationCodeStatusText(row.getStatus()))
                .successUseCount(row.getSuccessUseCount())
                .lastSuccessTime(row.getLastSuccessTime())
                .lastSuccessClientIp(row.getLastSuccessClientIp())
                .lastSuccessUserName(resolveLastSuccessUserName(row.getLastSuccessUserId(), lastSuccessUser))
                .build();
    }

    /**
     * 解析最近一次成功激活用户名。
     *
     * @param lastSuccessUserId 最近一次成功激活用户ID
     * @param lastSuccessUser   最近一次成功激活用户实体
     * @return 最近一次成功激活用户名
     */
    private String resolveLastSuccessUserName(Long lastSuccessUserId, User lastSuccessUser) {
        if (lastSuccessUser != null && StringUtils.hasText(lastSuccessUser.getUsername())) {
            return lastSuccessUser.getUsername();
        }
        if (lastSuccessUserId == null) {
            return null;
        }
        return String.valueOf(lastSuccessUserId);
    }

    /**
     * 解析激活码状态中文文案。
     *
     * @param status 激活码状态编码
     * @return 激活码状态中文文案
     */
    private String resolveActivationCodeStatusText(String status) {
        ActivationCodeItemStatusEnum statusEnum = ActivationCodeItemStatusEnum.fromCode(status);
        if (statusEnum == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "激活码状态不正确");
        }
        return switch (statusEnum) {
            case ACTIVE -> "启用";
            case DISABLED -> "停用";
            case USED -> "已使用";
        };
    }

    /**
     * 构建激活码导出文件名。
     *
     * @param batchNo 批次号
     * @return 激活码导出文件名
     */
    private String buildActivationCodeExportFileName(String batchNo) {
        return ACTIVATION_CODE_EXPORT_FILE_NAME_PREFIX + batchNo + EXCEL_FILE_SUFFIX;
    }

    /**
     * 构建激活码导出说明头文本列表。
     *
     * @param batchRow 激活码批次查询结果
     * @return 激活码导出说明头文本列表
     */
    private List<String> buildActivationCodeExportHeaderLines(ActivationBatchRowDto batchRow) {
        List<String> headerLines = new ArrayList<>();
        headerLines.add("批次号：" + batchRow.getBatchNo() + " | 模板名称：" + defaultText(batchRow.getTemplateName()));
        headerLines.add("兑换规则：" + resolveRedeemRuleTypeText(batchRow.getRedeemRuleType())
                + " | 批次状态：" + resolveBatchStatusText(batchRow.getStatus()));
        headerLines.add("有效期类型：" + resolveValidityTypeText(batchRow.getValidityType())
                + " | 生效说明：" + buildEffectiveDescription(batchRow));
        headerLines.add("生成数量：" + defaultNumberText(batchRow.getGenerateCount())
                + " | 成功使用：" + defaultNumberText(batchRow.getSuccessUseCount())
                + " | 创建人：" + defaultText(batchRow.getCreateBy())
                + " | 创建时间：" + formatNullableDate(batchRow.getCreateTime()));
        if (StringUtils.hasText(batchRow.getRemark())) {
            headerLines.add("备注：" + batchRow.getRemark().trim());
        }
        return headerLines;
    }

    /**
     * 构建批次生效说明文本。
     *
     * @param batchRow 激活码批次查询结果
     * @return 批次生效说明文本
     */
    private String buildEffectiveDescription(ActivationBatchRowDto batchRow) {
        ActivationCodeValidityTypeEnum validityTypeEnum = ActivationCodeValidityTypeEnum.fromCode(batchRow.getValidityType());
        if (validityTypeEnum == null) {
            return defaultText(batchRow.getValidityType());
        }
        if (validityTypeEnum == ActivationCodeValidityTypeEnum.ONCE) {
            return "固定时间生效，生效时间：" + formatNullableDate(batchRow.getFixedEffectiveTime())
                    + "，失效时间：" + formatNullableDate(batchRow.getFixedExpireTime());
        }
        return "激活成功后立即生效，生效后 " + defaultNumberText(batchRow.getRelativeValidDays()) + " 天内有效";
    }

    /**
     * 解析兑换规则类型中文文案。
     *
     * @param redeemRuleType 兑换规则类型编码
     * @return 兑换规则类型中文文案
     */
    private String resolveRedeemRuleTypeText(String redeemRuleType) {
        ActivationRedeemRuleTypeEnum redeemRuleTypeEnum = ActivationRedeemRuleTypeEnum.fromCode(redeemRuleType);
        return redeemRuleTypeEnum == null ? defaultText(redeemRuleType) : redeemRuleTypeEnum.getName();
    }

    /**
     * 解析有效期类型中文文案。
     *
     * @param validityType 有效期类型编码
     * @return 有效期类型中文文案
     */
    private String resolveValidityTypeText(String validityType) {
        ActivationCodeValidityTypeEnum validityTypeEnum = ActivationCodeValidityTypeEnum.fromCode(validityType);
        return validityTypeEnum == null ? defaultText(validityType) : validityTypeEnum.getName();
    }

    /**
     * 解析批次状态中文文案。
     *
     * @param status 批次状态编码
     * @return 批次状态中文文案
     */
    private String resolveBatchStatusText(String status) {
        ActivationCodeStatusEnum statusEnum = ActivationCodeStatusEnum.fromCode(status);
        return statusEnum == null ? defaultText(status) : statusEnum.getName();
    }

    /**
     * 格式化可空日期文本。
     *
     * @param date 日期对象
     * @return 格式化后的日期文本
     */
    private String formatNullableDate(Date date) {
        if (date == null) {
            return "无";
        }
        return new SimpleDateFormat(EXPORT_HEADER_DATE_PATTERN).format(date);
    }

    /**
     * 解析默认文本。
     *
     * @param value 原始文本
     * @return 默认文本
     */
    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无";
    }

    /**
     * 解析默认数字文本。
     *
     * @param value 原始数字
     * @return 默认数字文本
     */
    private String defaultNumberText(Number value) {
        return value == null ? "0" : String.valueOf(value);
    }

    /**
     * 转换为激活码兑换日志视图对象。
     *
     * @param row  激活码兑换日志查询结果
     * @param user 用户实体
     * @return 激活码兑换日志视图对象
     */
    private ActivationLogVo toActivationLogVo(ActivationRedeemLogRowDto row, User user) {
        return ActivationLogVo.builder()
                .id(row.getId())
                .requestId(row.getRequestId())
                .batchId(row.getBatchId())
                .activationCodeId(row.getActivationCodeId())
                .batchNo(row.getBatchNo())
                .templateId(row.getTemplateId())
                .templateName(row.getTemplateName())
                .redeemRuleType(row.getRedeemRuleType())
                .plainCodeSnapshot(row.getPlainCodeSnapshot())
                .userId(row.getUserId())
                .userName(user == null ? null : user.getUsername())
                .couponId(row.getCouponId())
                .resultStatus(row.getResultStatus())
                .failCode(row.getFailCode())
                .failMessage(row.getFailMessage())
                .clientIp(row.getClientIp())
                .grantMode(row.getGrantMode())
                .grantStatus(row.getGrantStatus())
                .createTime(row.getCreateTime())
                .build();
    }

    /**
     * 查询用户映射。
     *
     * @param userIds 用户ID列表
     * @return 用户映射
     */
    private Map<Long, User> listUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedUserIds.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, User> userMap = new HashMap<>();
        for (int start = 0; start < normalizedUserIds.size(); start += USER_QUERY_BATCH_SIZE) {
            int end = Math.min(start + USER_QUERY_BATCH_SIZE, normalizedUserIds.size());
            List<Long> currentBatchUserIds = normalizedUserIds.subList(start, end);
            List<User> users = userMapper.selectBatchIds(currentBatchUserIds);
            for (User user : users) {
                if (user != null && user.getId() != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }
        return userMap;
    }

    /**
     * 规范化可空文本。
     *
     * @param value 原始文本
     * @return 规范化后的文本
     */
    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
