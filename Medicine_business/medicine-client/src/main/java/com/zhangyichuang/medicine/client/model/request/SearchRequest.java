package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/26
 */
@Data
@Schema(description = "搜索请求")
@EqualsAndHashCode(callSuper = true)
public class SearchRequest extends PageRequest {

    @Schema(description = "搜索关键字", requiredMode = Schema.RequiredMode.REQUIRED, example = "感冒药")
    private String keyword;

}
