package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 语音识别 Agent 配置。
 */
@Data
public class SpeechRecognitionAgentConfig implements Serializable {

    /**
     * 火山引擎语音识别资源ID，例如 volc.seedasr.sauc.duration。
     */
    private String resourceId;
}
