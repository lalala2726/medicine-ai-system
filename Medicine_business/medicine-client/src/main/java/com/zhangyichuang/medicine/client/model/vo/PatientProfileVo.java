package com.zhangyichuang.medicine.client.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 就诊人信息
 */
@Data
@Schema(description = "就诊人信息")
public class PatientProfileVo {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "所属用户ID", example = "1001")
    private Long userId;

    @Schema(description = "就诊人姓名", example = "张三")
    private String name;

    @Schema(description = "性别：1男 2女", example = "1")
    private Integer gender;

    @Schema(description = "出生日期", example = "2000-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Schema(description = "过敏史", example = "青霉素过敏")
    private String allergy;

    @Schema(description = "既往病史", example = "高血压")
    private String pastMedicalHistory;

    @Schema(description = "慢性病信息", example = "糖尿病")
    private String chronicDisease;

    @Schema(description = "长期用药", example = "降压药")
    private String longTermMedications;

    @Schema(description = "与账户关系", example = "本人")
    private String relationship;

    @Schema(description = "是否默认就诊人：1是 0否", example = "1")
    private Integer isDefault;
}
