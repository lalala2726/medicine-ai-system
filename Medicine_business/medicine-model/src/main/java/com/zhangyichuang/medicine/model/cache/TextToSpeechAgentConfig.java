package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 语音合成 Agent 配置。
 */
@Data
public class TextToSpeechAgentConfig implements Serializable {

    /**
     * 火山引擎语音合成资源 ID，例如 seed-tts-2.0。
     */
    private String resourceId;

    /**
     * 火山引擎语音合成音色类型。
     */
    private String voiceType;

    /**
     * 单次语音合成最大文本长度。
     */
    private Integer maxTextChars;
}
