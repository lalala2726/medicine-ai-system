package com.zhangyichuang.medicine.admin.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 知识库列表数据传输对象。
 */
@Data
public class KnowledgeBaseListDto {

    /**
     * 知识库主键ID。
     */
    private Long id;

    /**
     * 知识库业务唯一名称。
     */
    private String knowledgeName;

    /**
     * 知识库展示名称。
     */
    private String displayName;

    /**
     * 知识库封面地址。
     */
    private String cover;

    /**
     * 知识库描述。
     */
    private String description;

    /**
     * 知识库状态：0启用，1停用。
     */
    private Integer status;

    /**
     * 列表页附加统计信息。
     */
    private Detail detail;

    /**
     * 知识库列表页详情统计。
     */
    @Data
    public static class Detail {

        /**
         * 当前知识库下最新切片更新时间。
         */
        private Date updateTime;

        /**
         * 当前知识库下的切片数量。
         */
        private Long chunkCount;

        /**
         * 当前知识库下的文件数量。
         */
        private Long fileCount;
    }
}
