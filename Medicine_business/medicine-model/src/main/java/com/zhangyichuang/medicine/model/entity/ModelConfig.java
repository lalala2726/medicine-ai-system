package com.zhangyichuang.medicine.model.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Chuang
 * <p>
 * created on 2025/9/9
 */
@Data
public class ModelConfig implements Serializable {

    /**
     * 厂商
     */
    @NotNull(message = "厂商不能为空")
    private String provider;

    /**
     * 模型名称
     */
    @NotNull(message = "模型名称不能为空")
    private String model;


    /**
     * API KEY
     */
    @NotNull(message = "API KEY不能为空")
    private String apiKey;

    /**
     * 基础URL
     */
    @NotNull(message = "基础URL不能为空")
    private String baseUrl;


    /**
     * 最大token数
     */
    @Min(value = 256, message = "最大token数不能小于256")
    private Integer maxTokens;

    /**
     * 模型温度
     */
    @Min(value = 0, message = "模型温度不能小于0")
    private Double temperature;
}
