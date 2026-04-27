package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigRequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static AgentModelSelectionRequest buildSelection() {
        AgentModelSelectionRequest request = new AgentModelSelectionRequest();
        request.setModelName("gpt-4.1");
        request.setReasoningEnabled(false);
        return request;
    }

    @Test
    void knowledgeBaseRequest_ShouldFail_WhenEmbeddingDimIsNotPowerOfTwo() {
        KnowledgeBaseAgentConfigRequest request = new KnowledgeBaseAgentConfigRequest();
        request.setEnabled(true);
        request.setKnowledgeNames(java.util.List.of("common_medicine_kb"));
        request.setEmbeddingDim(130);
        request.setEmbeddingModel(buildSelection());
        request.setTopK(10);
        request.setRankingEnabled(false);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item ->
                KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE.equals(item.getMessage())));
    }

    @Test
    void knowledgeBaseRequest_ShouldFail_WhenKnowledgeNamesEmpty() {
        KnowledgeBaseAgentConfigRequest request = new KnowledgeBaseAgentConfigRequest();
        request.setEnabled(true);
        request.setKnowledgeNames(java.util.List.of());
        request.setEmbeddingDim(1024);
        request.setEmbeddingModel(buildSelection());
        request.setTopK(10);
        request.setRankingEnabled(false);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "知识库名称列表不能为空".equals(item.getMessage())));
    }

    @Test
    void knowledgeBaseRequest_ShouldFail_WhenKnowledgeNamesExceedMaxLimit() {
        KnowledgeBaseAgentConfigRequest request = new KnowledgeBaseAgentConfigRequest();
        request.setEnabled(true);
        request.setKnowledgeNames(java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(index -> "knowledge_" + index)
                .toList());
        request.setEmbeddingDim(1024);
        request.setEmbeddingModel(buildSelection());
        request.setTopK(10);
        request.setRankingEnabled(false);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "知识库最多支持5个".equals(item.getMessage())));
    }

    @Test
    void knowledgeBaseRequest_ShouldFail_WhenEnabledMissing() {
        KnowledgeBaseAgentConfigRequest request = new KnowledgeBaseAgentConfigRequest();
        request.setKnowledgeNames(java.util.List.of("common_medicine_kb"));
        request.setEmbeddingDim(1024);
        request.setEmbeddingModel(buildSelection());
        request.setTopK(10);
        request.setRankingEnabled(false);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "是否启用知识库不能为空".equals(item.getMessage())));
    }

    @Test
    void knowledgeBaseRequest_ShouldPass_WhenDisabledAndFieldsEmpty() {
        KnowledgeBaseAgentConfigRequest request = new KnowledgeBaseAgentConfigRequest();
        request.setEnabled(false);

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void modelSelectionRequest_ShouldFail_WhenReasoningEnabledMissing() {
        AgentModelSelectionRequest request = buildSelection();
        request.setReasoningEnabled(null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "是否开启深度思考不能为空".equals(item.getMessage())));
    }

    @Test
    void clientAssistantRequest_ShouldFail_WhenRouteModelMissing() {
        ClientAssistantAgentConfigRequest request = new ClientAssistantAgentConfigRequest();
        ClientAssistantModelSelectionRequest serviceNodeModel = new ClientAssistantModelSelectionRequest();
        serviceNodeModel.setModelName("gpt-4.1");
        request.setServiceNodeModel(serviceNodeModel);
        ClientAssistantModelSelectionRequest diagnosisNodeModel = new ClientAssistantModelSelectionRequest();
        diagnosisNodeModel.setModelName("gpt-4.1");
        request.setDiagnosisNodeModel(diagnosisNodeModel);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "路由模型槽位配置不能为空".equals(item.getMessage())));
    }

    @Test
    void speechRequest_ShouldFail_WhenMaxTextCharsIsGreaterThanThreeThousand() {
        SpeechAgentConfigRequest request = new SpeechAgentConfigRequest();
        request.setAppId("speech-app-id");
        request.setAccessToken("speech-token");
        TextToSpeechConfigRequest textToSpeech = new TextToSpeechConfigRequest();
        textToSpeech.setVoiceType("zh_female_xiaohe_uranus_bigtts");
        textToSpeech.setMaxTextChars(3001);
        request.setTextToSpeech(textToSpeech);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(item -> "语音合成最大文本长度不能大于3000".equals(item.getMessage())));
    }
}
