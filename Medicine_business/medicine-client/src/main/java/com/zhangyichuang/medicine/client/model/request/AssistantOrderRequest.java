package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/1
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "助手订单请求参数")
public class AssistantOrderRequest extends PageRequest {

    @Schema(description = "搜索关键词")
    private String keyword;
}
