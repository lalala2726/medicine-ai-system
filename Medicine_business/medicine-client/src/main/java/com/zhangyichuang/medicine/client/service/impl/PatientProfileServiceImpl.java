package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.PatientProfileMapper;
import com.zhangyichuang.medicine.client.model.request.PatientProfileAddRequest;
import com.zhangyichuang.medicine.client.model.request.PatientProfileUpdateRequest;
import com.zhangyichuang.medicine.client.service.PatientProfileService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.PatientProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Chuang
 */
@Service
public class PatientProfileServiceImpl extends ServiceImpl<PatientProfileMapper, PatientProfile>
        implements PatientProfileService, BaseService {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PATIENT_LIMIT = 8L;

    @Override
    public List<PatientProfile> listPatientPro() {
        Long userId = getUserId();
        return lambdaQuery()
                .eq(PatientProfile::getUserId, userId)
                .orderByDesc(PatientProfile::getIsDefault)
                .orderByDesc(PatientProfile::getUpdatedAt)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePatientPro(Long id) {
        Assert.notNull(id, "就诊人信息ID不能为空");
        Long userId = getUserId();

        PatientProfile patientProfile = getById(id);
        Assert.notNull(patientProfile, "就诊人信息不存在");
        Assert.isTrue(Objects.equals(patientProfile.getUserId(), userId), "无权操作该就诊人信息");

        boolean removed = removeById(id);
        if (removed && Objects.equals(patientProfile.getIsDefault(), 1)) {
            assignDefaultIfNecessary(userId);
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addPatientPro(PatientProfileAddRequest request) {
        Long userId = getUserId();
        Date now = new Date();

        long existingCount = lambdaQuery()
                .eq(PatientProfile::getUserId, userId)
                .count();

        // 检查是否超过最大就诊人数量限制
        Assert.isTrue(existingCount < MAX_PATIENT_LIMIT, "就诊人数量不能超过" + MAX_PATIENT_LIMIT + "人");

        PatientProfile patientProfile = new PatientProfile();
        BeanUtils.copyProperties(request, patientProfile);
        patientProfile.setUserId(userId);
        patientProfile.setCreatedAt(now);
        patientProfile.setUpdatedAt(now);

        // 第一次添加自动设为默认，否则遵循用户显式选择
        if (existingCount == 0) {
            patientProfile.setIsDefault(1);
        } else if (isDefaultFlag(request.getIsDefault())) {
            clearDefaultPatientProfile(userId);
            patientProfile.setIsDefault(1);
        } else {
            patientProfile.setIsDefault(0);
        }

        return save(patientProfile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePatientPro(PatientProfileUpdateRequest request) {
        Assert.notNull(request.getId(), "就诊人信息ID不能为空");
        Long userId = getUserId();

        PatientProfile patientProfile = getById(request.getId());
        Assert.notNull(patientProfile, "就诊人信息不存在");
        Assert.isTrue(Objects.equals(patientProfile.getUserId(), userId), "无权操作该就诊人信息");

        BeanUtils.copyProperties(request, patientProfile, "id", "userId", "createdAt", "updatedAt");
        patientProfile.setUpdatedAt(new Date());

        if (request.getIsDefault() != null) {
            if (isDefaultFlag(request.getIsDefault())) {
                clearDefaultPatientProfile(userId);
                patientProfile.setIsDefault(1);
            } else {
                patientProfile.setIsDefault(0);
            }
        }

        return updateById(patientProfile);
    }

    @Override
    public PatientProfile getPatientPro(Long id) {
        Assert.notNull(id, "就诊人信息ID不能为空");
        Long userId = getUserId();

        PatientProfile patientProfile = getById(id);
        Assert.notNull(patientProfile, "就诊人信息不存在");
        Assert.isTrue(Objects.equals(patientProfile.getUserId(), userId), "无权查看该就诊人信息");
        return patientProfile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultPatientPro(Long id) {
        Assert.notNull(id, "就诊人信息ID不能为空");
        Long userId = getUserId();

        PatientProfile patientProfile = getById(id);
        Assert.notNull(patientProfile, "就诊人信息不存在");
        Assert.isTrue(Objects.equals(patientProfile.getUserId(), userId), "无权操作该就诊人信息");

        clearDefaultPatientProfile(userId);
        patientProfile.setIsDefault(1);
        patientProfile.setUpdatedAt(new Date());
        return updateById(patientProfile);
    }

    private Page<PatientProfile> buildPageRequest() {
        long current = DEFAULT_PAGE_NUM;
        long size = DEFAULT_PAGE_SIZE;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            current = parsePositiveLong(request.getParameter("pageNum"), DEFAULT_PAGE_NUM);
            size = parsePositiveLong(request.getParameter("pageSize"), DEFAULT_PAGE_SIZE);
        }
        return new Page<>(current, size);
    }

    private long parsePositiveLong(String rawValue, long defaultValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(rawValue);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean isDefaultFlag(Integer flag) {
        return Objects.equals(flag, 1);
    }

    /**
     * 保证同一用户仅有一个默认就诊人
     */
    private void clearDefaultPatientProfile(Long userId) {
        lambdaUpdate()
                .eq(PatientProfile::getUserId, userId)
                .eq(PatientProfile::getIsDefault, 1)
                .set(PatientProfile::getIsDefault, 0)
                .update();
    }

    /**
     * 当默认就诊人被删除时，为用户挑选最近更新的一条作为新的默认
     */
    private void assignDefaultIfNecessary(Long userId) {
        PatientProfile existsDefault = lambdaQuery()
                .eq(PatientProfile::getUserId, userId)
                .eq(PatientProfile::getIsDefault, 1)
                .one();
        if (existsDefault != null) {
            return;
        }

        PatientProfile latestProfile = lambdaQuery()
                .eq(PatientProfile::getUserId, userId)
                .orderByDesc(PatientProfile::getUpdatedAt)
                .last("limit 1")
                .one();
        if (latestProfile != null) {
            latestProfile.setIsDefault(1);
            latestProfile.setUpdatedAt(new Date());
            updateById(latestProfile);
        }
    }
}




