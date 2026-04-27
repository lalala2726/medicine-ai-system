package com.zhangyichuang.medicine.model.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * 豆包语音 Agent 配置。
 */
@Data
public class SpeechAgentConfig implements Serializable {

    /**
     * 语音服务提供商，固定为 volcengine。
     */
    private String provider;

    /**
     * 火山引擎语音应用ID。
     */
    private String appId;

    /**
     * 火山引擎语音访问令牌。
     */
    private String accessToken;

    /**
     * 语音识别配置。
     */
    private SpeechRecognitionAgentConfig speechRecognition;

    /**
     * 语音合成配置。
     */
    private TextToSpeechAgentConfig textToSpeech;
}
