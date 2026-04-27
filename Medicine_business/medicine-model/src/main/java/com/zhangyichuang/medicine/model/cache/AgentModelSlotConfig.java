package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * Agent 模型槽位配置。
 * <p>
 * 用于表达某个业务场景槽位最终使用的模型及运行参数。
 */
@Data
public class AgentModelSlotConfig implements Serializable {

    /**
     * 当前业务槽位绑定的模型名称。
     */
    private String modelName;

    /**
     * 当前业务槽位是否开启深度思考
     */
    private Boolean reasoningEnabled;

    /**
     * 当前业务槽位绑定模型是否支持深度思考。
     */
    private Boolean supportReasoning;

    /**
     * 当前业务槽位绑定模型是否支持图片理解。
     */
    private Boolean supportVision;

}
