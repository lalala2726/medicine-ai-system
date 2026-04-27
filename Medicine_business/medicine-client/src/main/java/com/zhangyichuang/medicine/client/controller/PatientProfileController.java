package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.PatientProfileAddRequest;
import com.zhangyichuang.medicine.client.model.request.PatientProfileUpdateRequest;
import com.zhangyichuang.medicine.client.model.vo.PatientProfileListVo;
import com.zhangyichuang.medicine.client.model.vo.PatientProfileVo;
import com.zhangyichuang.medicine.client.service.PatientProfileService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.entity.PatientProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/20
 */
@RestController
@RequestMapping("/patient_profile")
@Tag(name = "就诊人管理", description = "就诊人管理")
@PreventDuplicateSubmit
public class PatientProfileController extends BaseController {

    private final PatientProfileService patientProfileService;

    public PatientProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }


    /**
     * 查询就诊人信息列表
     *
     * @return 当前用户的就诊人信息列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询就诊人信息列表")
    public AjaxResult<List<PatientProfileListVo>> listPatientPro() {
        List<PatientProfile> patientProfilePage = patientProfileService.listPatientPro();
        List<PatientProfileListVo> patientProfileListVos = copyListProperties(patientProfilePage, PatientProfileListVo.class);
        return success(patientProfileListVos);
    }

    /**
     * 删除就诊人信息
     *
     * @param id ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除就诊人信息")
    public AjaxResult<Void> deletePatientPro(@PathVariable("id") Long id) {
        boolean result = patientProfileService.deletePatientPro(id);
        return toAjax(result);
    }

    /**
     * 新增就诊人信息
     *
     * @param request 请求
     * @return 添加结果
     */
    @PostMapping
    @Operation(summary = "新增就诊人信息")
    public AjaxResult<Void> addPatientPro(@RequestBody PatientProfileAddRequest request) {
        boolean result = patientProfileService.addPatientPro(request);
        return toAjax(result);
    }

    /**
     * 修改就诊人信息
     *
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改就诊人信息")
    public AjaxResult<Void> updatePatientPro(@RequestBody PatientProfileUpdateRequest request) {
        boolean result = patientProfileService.updatePatientPro(request);
        return toAjax(result);
    }

    /**
     * 查询就诊人信息
     *
     * @param id ID
     * @return 查询结果
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询就诊人信息")
    public AjaxResult<PatientProfileVo> getPatientPro(@PathVariable("id") Long id) {
        PatientProfile patientProfile = patientProfileService.getPatientPro(id);
        PatientProfileVo patientProfileVo = copyProperties(patientProfile, PatientProfileVo.class);
        return success(patientProfileVo);
    }

    /**
     * 设置默认就诊人
     *
     * @param id ID
     * @return 设置结果
     */
    @PutMapping("/default/{id}")
    @Operation(summary = "设置默认就诊人")
    public AjaxResult<Void> setDefaultPatientPro(@PathVariable("id") Long id) {
        boolean result = patientProfileService.setDefaultPatientPro(id);
        return toAjax(result);
    }

}
