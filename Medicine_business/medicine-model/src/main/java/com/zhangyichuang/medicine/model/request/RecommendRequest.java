package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/16
 */
@Data
@Schema(description = "商品推荐请求参数")
public class RecommendRequest {

    @Schema(description = "分页大小", example = "10")
    private Integer size;

    @Schema(description = "热门游标", example = "0")
    private Integer hotCursor;

}
