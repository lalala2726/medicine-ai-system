package com.zhangyichuang.medicine.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 知识库列表展示对象
 */
@Data
@Schema(description = "知识库列表展示对象")
public class KnowledgeBaseListVo {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "知识库唯一名称（业务键）", example = "common_medicine_kb")
    private String knowledgeName;

    @Schema(description = "知识库展示名称", example = "常见用药知识库")
    private String displayName;

    @Schema(description = "知识库封面", example = "https://example.com/kb-cover.png")
    private String cover;

    @Schema(description = "知识库描述", example = "覆盖常见用药相关问答内容")
    private String description;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    private Integer status;

    @Schema(description = "知识库详情")
    private Detail detail;


    @Data
    @Schema(description = "知识库详情")
    public static class Detail {

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @Schema(description = "更新时间", example = "2023-07-01 12:00")
        private Date updateTime;

        @Schema(description = "切片数量", example = "10")
        private Long chunkCount;

        @Schema(description = "知识库文件数量", example = "5")
        private Long fileCount;
    }

}
