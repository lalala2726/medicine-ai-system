package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量删除文档请求参数。
 */
@Data
@Schema(description = "批量删除文档请求参数")
public class DocumentDeleteRequest {

    @NotEmpty(message = "文档ID不能为空")
    @ArraySchema(schema = @Schema(description = "文档ID", example = "1001"))
    private List<@NotNull(message = "文档ID不能为空") @Min(value = 1, message = "文档ID必须大于0") Long> documentIds;
}
