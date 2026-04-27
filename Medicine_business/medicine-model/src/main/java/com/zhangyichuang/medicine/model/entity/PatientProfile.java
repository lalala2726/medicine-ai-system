package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

/**
 * 就诊人信息
 */
@TableName(value = "patient_profile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientProfile {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 就诊人姓名
     */
    private String name;

    /**
     * 性别：1男 2女
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 过敏史
     */
    private String allergy;

    /**
     * 既往病史
     */
    private String pastMedicalHistory;

    /**
     * 慢性病信息
     */
    private String chronicDisease;

    /**
     * 长期用药
     */
    private String longTermMedications;

    /**
     * 与账户关系
     */
    private String relationship;

    /**
     * 是否默认就诊人：1是 0否
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
