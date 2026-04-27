package com.zhangyichuang.medicine.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预设大模型厂商模板。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmPresetProviderTemplateDto {

    /**
     * 预设厂商英文键。
     */
    private String providerKey;

    /**
     * 提供商显示名称。
     */
    private String providerName;

    /**
     * 提供商类型。
     */
    private String providerType;

    /**
     * 默认基础地址。
     */
    private String baseUrl;

    /**
     * 描述。
     */
    private String description;

    /**
     * 预设模型列表。
     */
    private List<Model> models;

    /**
     * 预设模型模板。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Model {

        /**
         * 模型名称。
         */
        private String modelName;

        /**
         * 模型类型。
         */
        private String modelType;

        /**
         * 是否支持深度思考：0否 1是。
         */
        private Integer supportReasoning;

        /**
         * 是否支持图片识别：0否 1是。
         */
        private Integer supportVision;

        /**
         * 模型描述。
         */
        private String description;

        /**
         * 状态。
         */
        private Integer enabled;

        /**
         * 排序值。
         */
        private Integer sort;
    }
}
