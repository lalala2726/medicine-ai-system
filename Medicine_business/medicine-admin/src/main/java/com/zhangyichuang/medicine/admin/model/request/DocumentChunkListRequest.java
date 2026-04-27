package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片分页查询请求。
 */
@Data
@Schema(description = "文档切片分页查询请求")
@EqualsAndHashCode(callSuper = true)
public class DocumentChunkListRequest extends PageRequest {
}
