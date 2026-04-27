package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批次激活码分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批次激活码分页查询请求")
public class ActivationBatchCodeListRequest extends PageRequest {
}
