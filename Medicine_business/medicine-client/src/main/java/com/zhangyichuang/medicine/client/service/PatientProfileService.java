package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.PatientProfileAddRequest;
import com.zhangyichuang.medicine.client.model.request.PatientProfileUpdateRequest;
import com.zhangyichuang.medicine.model.entity.PatientProfile;

import java.util.List;

/**
 * @author Chuang
 */
public interface PatientProfileService extends IService<PatientProfile> {

    /**
     * 查询就诊人信息列表
     *
     * @return 当前用户的就诊人信息列表
     */
    List<PatientProfile> listPatientPro();

    /**
     * 删除就诊人信息
     *
     * @param id 就诊人信息id
     * @return 是否删除成功
     */
    boolean deletePatientPro(Long id);

    /**
     * 新增就诊人信息
     *
     * @param request 就诊人信息
     * @return 是否新增成功
     */
    boolean addPatientPro(PatientProfileAddRequest request);

    /**
     * 修改就诊人信息
     *
     * @param request 就诊人信息
     * @return 是否修改成功
     */
    boolean updatePatientPro(PatientProfileUpdateRequest request);

    /**
     * 查询就诊人信息
     *
     * @param id 就诊人信息id
     * @return 就诊人信息
     */
    PatientProfile getPatientPro(Long id);

    /**
     * 设置默认就诊人信息
     *
     * @param id 就诊人信息id
     * @return 是否设置成功
     */
    boolean setDefaultPatientPro(Long id);
}
