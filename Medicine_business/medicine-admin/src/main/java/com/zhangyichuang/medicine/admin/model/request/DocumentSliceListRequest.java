package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/6
 */
@Data
@Schema(description = "文档切片列表分页请求参数")
@EqualsAndHashCode(callSuper = true)
public class DocumentSliceListRequest extends PageRequest {

    @Schema(description = "文档名称", type = "string", requiredMode = Schema.RequiredMode.AUTO)
    private String name;
}
