package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/6
 */
@Data
@Schema(description = "知识库文档切片列表")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentSliceListVo {

    @Schema(description = "id", example = "1")
    private Long id;

    @Schema(description = "uuid", example = "496df75e-7f5d-46a8-b5d6-6a931d2f31c8")
    private String uuid;

    @Schema(description = "文档id", example = "1")
    private Long documentId;

    @Schema(description = "文档切片内容", example = "这是文档切片内容")
    private String context;

    @Schema(description = "创建时间", example = "2025-12-06 00:00:00")
    private Date createTime;

    @Schema(description = "更新时间", example = "2025-12-06 00:00:00")
    private Date updateTime;

    @Schema(description = "创建人", example = "张三")
    private String createBy;

    @Schema(description = "更新人", example = "张三")
    private String updateBy;
}
