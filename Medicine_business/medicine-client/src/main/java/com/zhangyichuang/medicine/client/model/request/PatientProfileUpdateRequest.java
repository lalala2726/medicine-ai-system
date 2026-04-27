package com.zhangyichuang.medicine.client.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "添加就诊人修改请求对象")
public class PatientProfileUpdateRequest {

    @Schema(description = "ID", example = "1")
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "就诊人姓名", example = "张三")
    @NotNull(message = "就诊人姓名不能为空")
    private String name;

    @Schema(description = "性别", example = "1")
    @NotNull(message = "性别不能为空")
    private Integer gender;

    @Schema(description = "出生日期", example = "2000-01-01")
    @NotNull(message = "出生日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
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

    @Schema(description = "是否默认就诊人", example = "1")
    private Integer isDefault;
}
