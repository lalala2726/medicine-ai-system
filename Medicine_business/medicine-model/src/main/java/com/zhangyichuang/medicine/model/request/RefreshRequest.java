package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
@Schema(description = "刷新令牌请求参数")
@Data
public class RefreshRequest {

    /**
     * 刷新令牌。
     */
    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY5MjE5M")
    private String refreshToken;
}
