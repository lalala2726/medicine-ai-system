package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/5
 */
@Schema(description = "知识库文档搜索查询对象")
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentListRequest extends PageRequest {

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "文件名称", example = "文件名称")
    private String fileName;
}
