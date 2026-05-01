package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.AgentTraceRunListRequest;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.repository.AgentTraceMongoRepository;
import com.zhangyichuang.medicine.admin.service.AgentTraceService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Agent Trace 管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentTraceServiceImpl implements AgentTraceService {

    /**
     * 运行中状态值。
     */
    private static final String STATUS_RUNNING = "running";

    /**
     * Trace 不存在提示文案。
     */
    private static final String TRACE_NOT_FOUND_MESSAGE = "Trace 不存在";

    /**
     * 运行中 Trace 删除提示文案。
     */
    private static final String RUNNING_TRACE_DELETE_MESSAGE = "运行中的 Trace 不允许删除";

    /**
     * 用户消息类型值。
     */
    private static final String MESSAGE_TYPE_HUMAN = "human";

    /**
     * AI 消息类型值。
     */
    private static final String MESSAGE_TYPE_AI = "ai";

    /**
     * 工具消息类型值。
     */
    private static final String MESSAGE_TYPE_TOOL = "tool";

    /**
     * 可读消息用户角色。
     */
    private static final String TRACE_MESSAGE_ROLE_USER = "user";

    /**
     * 可读消息 AI 角色。
     */
    private static final String TRACE_MESSAGE_ROLE_AI = "ai";

    /**
     * Root span 输入载荷中的消息视图字段名。
     */
    private static final String TRACE_MESSAGE_VIEW_FIELD = "message_view";

    /**
     * 消息视图中的消息列表字段名。
     */
    private static final String TRACE_MESSAGE_VIEW_MESSAGES_FIELD = "messages";

    /**
     * 消息视图中的消息角色字段名。
     */
    private static final String TRACE_MESSAGE_VIEW_ROLE_FIELD = "role";

    /**
     * 消息视图中的消息内容字段名。
     */
    private static final String TRACE_MESSAGE_VIEW_CONTENT_FIELD = "content";

    /**
     * 消息视图中的消息顺序字段名。
     */
    private static final String TRACE_MESSAGE_VIEW_INDEX_FIELD = "index";

    /**
     * 模型自然结束原因值。
     */
    private static final String FINISH_REASON_STOP = "stop";

    /**
     * Markdown 渲染模式值。
     */
    private static final String RENDER_MODE_MARKDOWN = "markdown";

    /**
     * 模型 span 类型值。
     */
    private static final String SPAN_TYPE_MODEL = "model";

    /**
     * Graph span 类型值。
     */
    private static final String SPAN_TYPE_GRAPH = "graph";

    /**
     * 节点 span 类型值。
     */
    private static final String SPAN_TYPE_NODE = "node";

    /**
     * 中间件 span 类型值。
     */
    private static final String SPAN_TYPE_MIDDLEWARE = "middleware";

    /**
     * 工具 span 类型值。
     */
    private static final String SPAN_TYPE_TOOL = "tool";

    /**
     * 异常状态值。
     */
    private static final String STATUS_ERROR = "error";

    /**
     * 工具分组 span 名称。
     */
    private static final String TOOLS_GROUP_SPAN_NAME = "tools";

    /**
     * 模型分组展示名称。
     */
    private static final String MODEL_GROUP_DISPLAY_NAME = "模型";

    /**
     * 工具分组展示名称。
     */
    private static final String TOOLS_GROUP_DISPLAY_NAME = "工具";

    /**
     * 分组 span 树节点类型值。
     */
    private static final String TREE_NODE_TYPE_SPAN_GROUP = "span_group";

    /**
     * 普通 span 树节点类型值。
     */
    private static final String TREE_NODE_TYPE_SPAN = "span";

    /**
     * 模型调用展示树节点类型值。
     */
    private static final String TREE_NODE_TYPE_MODEL_CALL = "model_call";

    /**
     * 模型调用展示节点 ID 后缀。
     */
    private static final String MODEL_CALL_NODE_ID_SUFFIX = ":model_call";

    /**
     * Agent Trace Mongo 仓储。
     */
    private final AgentTraceMongoRepository agentTraceMongoRepository;

    /**
     * 分页查询 Agent Trace run 列表。
     *
     * @param request 查询参数。
     * @return Agent Trace run 分页列表。
     */
    @Override
    public Page<AgentTraceRunListVo> listRuns(AgentTraceRunListRequest request) {
        Page<Document> documentPage = agentTraceMongoRepository.listRuns(request);
        Page<AgentTraceRunListVo> page = new Page<>(documentPage.getCurrent(), documentPage.getSize());
        page.setTotal(documentPage.getTotal());
        List<AgentTraceRunListVo> records = documentPage.getRecords().stream().map(this::toRunListVo).toList();
        fillRunInputOutputFields(records);
        page.setRecords(records);
        return page;
    }

    /**
     * 查询 Agent Trace 详情。
     *
     * @param traceId Trace 唯一标识。
     * @return Agent Trace 详情。
     */
    @Override
    public AgentTraceDetailVo getTraceDetail(String traceId) {
        String normalizedTraceId = normalizeTraceId(traceId);
        Document runDocument = getExistingRunDocument(normalizedTraceId);
        List<AgentTraceSpanVo> spans = agentTraceMongoRepository.listSpansByTraceId(normalizedTraceId)
                .stream()
                .map(this::toSpanVo)
                .toList();
        fillModelDetails(spans);
        fillNodeDetails(spans);
        AgentTraceDetailVo detailVo = toDetailVo(runDocument);
        detailVo.setSpans(spans);
        detailVo.setSpanTree(buildSpanTree(spans));
        detailVo.setOverviewDetail(buildOverviewDetail(detailVo, spans));
        return detailVo;
    }

    /**
     * 删除 Agent Trace。
     *
     * @param traceId Trace 唯一标识。
     * @return 删除成功返回 true。
     */
    @Override
    public boolean deleteTrace(String traceId) {
        String normalizedTraceId = normalizeTraceId(traceId);
        Document runDocument = getExistingRunDocument(normalizedTraceId);
        if (STATUS_RUNNING.equals(runDocument.getString("status"))) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, RUNNING_TRACE_DELETE_MESSAGE);
        }
        return agentTraceMongoRepository.deleteRunAndSpans(normalizedTraceId) > 0;
    }

    /**
     * 读取已存在的 run 文档。
     *
     * @param traceId Trace 唯一标识。
     * @return run 文档。
     */
    private Document getExistingRunDocument(String traceId) {
        Document runDocument = agentTraceMongoRepository.findRunByTraceId(traceId);
        if (runDocument == null) {
            throw new ServiceException(ResponseCode.DATA_NOT_FOUND, TRACE_NOT_FOUND_MESSAGE);
        }
        return runDocument;
    }

    /**
     * 规范化 trace ID。
     *
     * @param traceId 原始 trace ID。
     * @return 去除首尾空白后的 trace ID。
     */
    private String normalizeTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "Trace ID 不能为空");
        }
        return traceId.trim();
    }

    /**
     * 转换 run 列表视图对象。
     *
     * @param document Mongo run 文档。
     * @return run 列表视图对象。
     */
    private AgentTraceRunListVo toRunListVo(Document document) {
        AgentTraceRunListVo vo = new AgentTraceRunListVo();
        fillRunBaseFields(vo, document);
        return vo;
    }

    /**
     * 转换 trace 详情视图对象。
     *
     * @param document Mongo run 文档。
     * @return trace 详情视图对象。
     */
    private AgentTraceDetailVo toDetailVo(Document document) {
        AgentTraceDetailVo vo = new AgentTraceDetailVo();
        fillRunBaseFields(vo, document);
        vo.setRootSpanId(document.getString("root_span_id"));
        vo.setErrorPayload(document.get("error_payload"));
        return vo;
    }

    /**
     * 填充 run 基础字段。
     *
     * @param vo       run 视图对象。
     * @param document Mongo run 文档。
     * @return 无返回值。
     */
    private void fillRunBaseFields(AgentTraceRunListVo vo, Document document) {
        vo.setTraceId(document.getString("trace_id"));
        vo.setConversationUuid(document.getString("conversation_uuid"));
        vo.setAssistantMessageUuid(document.getString("assistant_message_uuid"));
        vo.setUserId(toLong(document.get("user_id")));
        vo.setConversationType(document.getString("conversation_type"));
        vo.setGraphName(document.getString("graph_name"));
        vo.setEntrypoint(document.getString("entrypoint"));
        vo.setStatus(document.getString("status"));
        vo.setStartedAt(document.getDate("started_at"));
        vo.setEndedAt(document.getDate("ended_at"));
        vo.setDurationMs(toLong(document.get("duration_ms")));
        vo.setInputTokens(toLong(document.get("input_tokens")));
        vo.setOutputTokens(toLong(document.get("output_tokens")));
        vo.setTotalTokens(toLong(document.get("total_tokens")));
    }

    /**
     * 填充 run 列表的输入输出预览字段。
     *
     * @param records run 列表视图对象。
     * @return 无返回值。
     */
    private void fillRunInputOutputFields(List<AgentTraceRunListVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> traceIds = records.stream()
                .map(AgentTraceRunListVo::getTraceId)
                .filter(StringUtils::hasText)
                .toList();
        if (traceIds.isEmpty()) {
            return;
        }

        Map<String, List<Document>> modelSpansByTraceId = new HashMap<>();
        for (Document spanDocument : agentTraceMongoRepository.listModelSpansByTraceIds(traceIds)) {
            String traceId = spanDocument.getString("trace_id");
            if (!StringUtils.hasText(traceId)) {
                continue;
            }
            modelSpansByTraceId.computeIfAbsent(traceId, ignored -> new ArrayList<>()).add(spanDocument);
        }

        for (AgentTraceRunListVo record : records) {
            List<Document> modelSpans = modelSpansByTraceId.get(record.getTraceId());
            if (modelSpans == null || modelSpans.isEmpty()) {
                continue;
            }
            record.setInputText(extractLatestHumanInput(modelSpans));
            record.setOutputText(extractFinalAiOutput(modelSpans));
        }
    }

    /**
     * 从模型 span 列表提取用户最新输入。
     *
     * @param modelSpans 按 sequence 升序排列的模型 span 列表。
     * @return 用户最新输入文本；无法提取时返回 null。
     */
    private String extractLatestHumanInput(List<Document> modelSpans) {
        Document firstModelSpan = modelSpans.getFirst();
        Document inputPayload = toDocument(firstModelSpan.get("input_payload"));
        return findLastMessageContent(getMessages(inputPayload), MESSAGE_TYPE_HUMAN);
    }

    /**
     * 从模型 span 列表提取 AI 最终输出。
     *
     * @param modelSpans 按 sequence 升序排列的模型 span 列表。
     * @return AI 最终输出文本；无法提取时返回 null。
     */
    private String extractFinalAiOutput(List<Document> modelSpans) {
        for (int index = modelSpans.size() - 1; index >= 0; index--) {
            Document outputPayload = toDocument(modelSpans.get(index).get("output_payload"));
            if (FINISH_REASON_STOP.equals(outputPayload.getString("finish_reason"))) {
                String content = findLastMessageContent(getMessages(outputPayload), MESSAGE_TYPE_AI);
                if (StringUtils.hasText(content)) {
                    return content;
                }
            }
        }
        for (int index = modelSpans.size() - 1; index >= 0; index--) {
            Document outputPayload = toDocument(modelSpans.get(index).get("output_payload"));
            String content = findLastMessageContent(getMessages(outputPayload), MESSAGE_TYPE_AI);
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        return null;
    }

    /**
     * 构建顶层 Trace 概览详情。
     *
     * @param detailVo 当前 Trace 运行详情。
     * @param spans    Span 明细列表。
     * @return 顶层 Trace 概览详情。
     */
    private AgentTraceOverviewDetailVo buildOverviewDetail(AgentTraceDetailVo detailVo, List<AgentTraceSpanVo> spans) {
        AgentTraceSpanVo rootSpan = findRootSpan(detailVo, spans);
        AgentTraceSpanVo firstModelSpan = findFirstModelSpanWithDetail(spans);
        AgentTraceSpanVo outputModelSpan = findLastOutputModelSpan(spans);
        if (!hasOverviewContract(rootSpan, firstModelSpan)) {
            return null;
        }

        AgentTraceOverviewDetailVo overviewDetail = new AgentTraceOverviewDetailVo();
        overviewDetail.setSpanId(rootSpan.getSpanId());
        overviewDetail.setName(rootSpan.getName());
        overviewDetail.setStatus(rootSpan.getStatus());
        overviewDetail.setStartedAt(rootSpan.getStartedAt());
        overviewDetail.setEndedAt(rootSpan.getEndedAt());
        overviewDetail.setDurationMs(rootSpan.getDurationMs());
        overviewDetail.setTokenUsage(rootSpan.getTokenUsage() != null
                ? rootSpan.getTokenUsage()
                : buildRunTokenUsage(detailVo));
        overviewDetail.setInput(buildOverviewInput(firstModelSpan));
        overviewDetail.setOutput(buildOverviewOutput(rootSpan, outputModelSpan, spans));
        overviewDetail.setAttributes(buildOverviewAttributes(detailVo, rootSpan));
        overviewDetail.setErrorPayload(rootSpan != null && rootSpan.getErrorPayload() != null
                ? rootSpan.getErrorPayload()
                : detailVo.getErrorPayload());
        overviewDetail.setMessageView(buildOverviewMessageView(rootSpan));
        return overviewDetail;
    }

    /**
     * 判断当前 Trace 是否具备新版顶层概览合同。
     *
     * @param rootSpan       root graph span。
     * @param firstModelSpan 最早结构化模型 span。
     * @return 满足新版顶层概览合同返回 true；旧数据或缺关键字段返回 false。
     */
    private boolean hasOverviewContract(AgentTraceSpanVo rootSpan, AgentTraceSpanVo firstModelSpan) {
        if (rootSpan == null || firstModelSpan == null || firstModelSpan.getModelDetail() == null) {
            return false;
        }
        Document rootOutputPayload = toDocument(rootSpan.getOutputPayload());
        return rootOutputPayload.containsKey("final_text");
    }

    /**
     * 查找 root graph span。
     *
     * @param detailVo 当前 Trace 运行详情。
     * @param spans    Span 明细列表。
     * @return root graph span；不存在时返回 null。
     */
    private AgentTraceSpanVo findRootSpan(AgentTraceDetailVo detailVo, List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(detailVo.getRootSpanId())) {
            for (AgentTraceSpanVo span : spans) {
                if (detailVo.getRootSpanId().equals(span.getSpanId())) {
                    return span;
                }
            }
        }
        for (AgentTraceSpanVo span : spans) {
            if (SPAN_TYPE_GRAPH.equals(span.getSpanType()) && !StringUtils.hasText(span.getParentSpanId())) {
                return span;
            }
        }
        return null;
    }

    /**
     * 查找最早的结构化模型 span。
     *
     * @param spans Span 明细列表。
     * @return 最早的结构化模型 span；不存在时返回 null。
     */
    private AgentTraceSpanVo findFirstModelSpanWithDetail(List<AgentTraceSpanVo> spans) {
        if (spans == null) {
            return null;
        }
        for (AgentTraceSpanVo span : spans) {
            if (SPAN_TYPE_MODEL.equals(span.getSpanType()) && span.getModelDetail() != null) {
                return span;
            }
        }
        return null;
    }

    /**
     * 查找最适合作为顶层输出来源的模型 span。
     *
     * @param spans Span 明细列表。
     * @return 输出模型 span；不存在时返回 null。
     */
    private AgentTraceSpanVo findLastOutputModelSpan(List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }
        for (int index = spans.size() - 1; index >= 0; index--) {
            AgentTraceSpanVo span = spans.get(index);
            AgentTraceModelDetailVo modelDetail = span.getModelDetail();
            if (SPAN_TYPE_MODEL.equals(span.getSpanType())
                    && modelDetail != null
                    && StringUtils.hasText(modelDetail.getFinalText())) {
                return span;
            }
        }
        for (int index = spans.size() - 1; index >= 0; index--) {
            AgentTraceSpanVo span = spans.get(index);
            if (SPAN_TYPE_MODEL.equals(span.getSpanType()) && span.getModelDetail() != null) {
                return span;
            }
        }
        return null;
    }

    /**
     * 构建顶层输入详情。
     *
     * @param modelSpan 最早模型 span。
     * @return 顶层输入详情。
     */
    private AgentTraceOverviewDetailVo.InputVo buildOverviewInput(AgentTraceSpanVo modelSpan) {
        AgentTraceOverviewDetailVo.InputVo inputVo = new AgentTraceOverviewDetailVo.InputVo();
        if (modelSpan == null || modelSpan.getModelDetail() == null) {
            return inputVo;
        }
        inputVo.setSystemPrompt(modelSpan.getModelDetail().getSystemPrompt());
        inputVo.setMessages(filterOverviewInputMessages(modelSpan.getModelDetail().getInputMessages()));
        return inputVo;
    }

    /**
     * 过滤顶层概览可读输入消息。
     *
     * @param messages 模型真实输入消息列表。
     * @return 只包含用户输入的消息列表。
     */
    private List<AgentTraceModelDetailVo.MessageVo> filterOverviewInputMessages(
            List<AgentTraceModelDetailVo.MessageVo> messages
    ) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<AgentTraceModelDetailVo.MessageVo> filteredMessages = new ArrayList<>();
        for (AgentTraceModelDetailVo.MessageVo message : messages) {
            if (message == null) {
                continue;
            }
            String messageType = message.getType();
            if (MESSAGE_TYPE_HUMAN.equals(messageType)) {
                filteredMessages.add(message);
            }
        }
        return filteredMessages;
    }

    /**
     * 构建顶层输出详情。
     *
     * @param rootSpan        root graph span。
     * @param outputModelSpan 最终输出模型 span。
     * @param spans           Span 明细列表。
     * @return 顶层输出详情。
     */
    private AgentTraceOverviewDetailVo.OutputVo buildOverviewOutput(
            AgentTraceSpanVo rootSpan,
            AgentTraceSpanVo outputModelSpan,
            List<AgentTraceSpanVo> spans
    ) {
        AgentTraceOverviewDetailVo.OutputVo outputVo = new AgentTraceOverviewDetailVo.OutputVo();
        Document rootOutputPayload = rootSpan == null ? new Document() : toDocument(rootSpan.getOutputPayload());
        String finalText = readDocumentString(rootOutputPayload, "final_text");
        if (!StringUtils.hasText(finalText)) {
            finalText = outputModelSpan == null || outputModelSpan.getModelDetail() == null
                    ? null
                    : outputModelSpan.getModelDetail().getFinalText();
        }
        outputVo.setFinalText(finalText);
        outputVo.setToolCalls(buildOverviewToolCalls(spans));
        return outputVo;
    }

    /**
     * 构建顶层 Trace 可读消息视图。
     *
     * @param rootSpan root graph span。
     * @return 可读消息视图。
     */
    private AgentTraceMessageViewVo buildOverviewMessageView(AgentTraceSpanVo rootSpan) {
        List<AgentTraceMessageVo> messages = new ArrayList<>();
        appendRootMessageViewMessages(messages, rootSpan);
        String finalText = rootSpan == null
                ? null
                : readDocumentString(toDocument(rootSpan.getOutputPayload()), "final_text");
        appendOverviewFinalMessage(messages, rootSpan, finalText);
        return buildTraceMessageView(rootSpan == null ? "Trace" : rootSpan.getName(), messages);
    }

    /**
     * 构建节点可读消息视图。
     *
     * @param nodeSpan        节点 Span。
     * @param descendantSpans 节点后代 Span 列表。
     * @return 节点可读消息视图。
     */
    private AgentTraceMessageViewVo buildNodeMessageView(
            AgentTraceSpanVo nodeSpan,
            List<AgentTraceSpanVo> descendantSpans
    ) {
        AgentTraceSpanVo firstModelSpan = findFirstModelSpanWithDetail(descendantSpans);
        AgentTraceSpanVo outputModelSpan = findLastOutputModelSpan(descendantSpans);
        List<AgentTraceMessageVo> messages = new ArrayList<>();
        appendConversationMessages(messages, firstModelSpan);
        appendAiFinalMessage(messages, outputModelSpan, null);
        return buildTraceMessageView(nodeSpan.getName(), messages);
    }

    /**
     * 构建模型可读消息视图。
     *
     * @param modelSpan   模型 Span。
     * @param modelDetail 模型结构化详情。
     * @return 模型可读消息视图。
     */
    private AgentTraceMessageViewVo buildModelMessageView(
            AgentTraceSpanVo modelSpan,
            AgentTraceModelDetailVo modelDetail
    ) {
        List<AgentTraceMessageVo> messages = new ArrayList<>();
        appendConversationMessages(messages, modelSpan);
        if (StringUtils.hasText(modelDetail.getFinalText())) {
            addTraceMessageIfAbsent(messages, modelSpan, TRACE_MESSAGE_ROLE_AI, modelDetail.getFinalText(), "final");
        } else {
            appendReadableMessages(messages, modelSpan, modelDetail.getOutputMessages(), TRACE_MESSAGE_ROLE_AI);
        }
        return buildTraceMessageView(resolveModelName(modelSpan), messages);
    }

    /**
     * 添加模型输入中的历史可读对话消息。
     *
     * @param messages  消息输出列表。
     * @param modelSpan 模型 Span。
     * @return 无返回值。
     */
    private void appendConversationMessages(List<AgentTraceMessageVo> messages, AgentTraceSpanVo modelSpan) {
        if (modelSpan == null || modelSpan.getModelDetail() == null) {
            return;
        }
        appendReadableConversationMessages(messages, modelSpan, modelSpan.getModelDetail().getInputMessages());
    }

    /**
     * 添加 root span 中由 Python Trace 采集层保存的完整历史消息快照。
     *
     * @param messages 消息输出列表。
     * @param rootSpan root graph span。
     * @return 无返回值。
     */
    private void appendRootMessageViewMessages(List<AgentTraceMessageVo> messages, AgentTraceSpanVo rootSpan) {
        if (rootSpan == null) {
            return;
        }
        Document inputPayload = toDocument(rootSpan.getInputPayload());
        Document messageView = toDocument(inputPayload.get(TRACE_MESSAGE_VIEW_FIELD));
        List<?> rawMessages = getListValue(messageView, TRACE_MESSAGE_VIEW_MESSAGES_FIELD);
        for (int index = 0; index < rawMessages.size(); index++) {
            Document rawMessage = toDocument(rawMessages.get(index));
            String role = readDocumentString(rawMessage, TRACE_MESSAGE_VIEW_ROLE_FIELD);
            if (!TRACE_MESSAGE_ROLE_USER.equals(role) && !TRACE_MESSAGE_ROLE_AI.equals(role)) {
                continue;
            }
            String content = readDocumentString(rawMessage, TRACE_MESSAGE_VIEW_CONTENT_FIELD);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String suffix = readMessageViewIndex(rawMessage, index);
            addTraceMessage(messages, rootSpan, role, content, suffix);
        }
    }

    /**
     * 读取消息视图中的稳定顺序后缀。
     *
     * @param rawMessage 原始消息文档。
     * @param index      当前遍历顺序。
     * @return 消息 ID 后缀。
     */
    private String readMessageViewIndex(Document rawMessage, int index) {
        Object rawIndex = rawMessage.get(TRACE_MESSAGE_VIEW_INDEX_FIELD);
        if (rawIndex instanceof Number number) {
            return String.valueOf(number.longValue());
        }
        String indexText = rawIndex == null ? "" : String.valueOf(rawIndex).trim();
        return StringUtils.hasText(indexText) ? indexText : String.valueOf(index);
    }

    /**
     * 添加顶层 Trace 最终 AI 回复。
     *
     * @param messages  消息输出列表。
     * @param rootSpan  root graph span。
     * @param finalText root span 保存的最终回复文本。
     * @return 无返回值。
     */
    private void appendOverviewFinalMessage(
            List<AgentTraceMessageVo> messages,
            AgentTraceSpanVo rootSpan,
            String finalText
    ) {
        if (rootSpan == null || !StringUtils.hasText(finalText)) {
            return;
        }
        String normalizedFinalText = finalText.trim();
        if (!messages.isEmpty()) {
            AgentTraceMessageVo lastMessage = messages.get(messages.size() - 1);
            if (TRACE_MESSAGE_ROLE_AI.equals(lastMessage.getRole())
                    && normalizedFinalText.equals(lastMessage.getContent())) {
                return;
            }
        }
        addTraceMessage(messages, rootSpan, TRACE_MESSAGE_ROLE_AI, normalizedFinalText, "final");
    }

    /**
     * 添加模型最终 AI 消息。
     *
     * @param messages       消息输出列表。
     * @param outputModelSpan 输出模型 Span。
     * @param preferredText  优先使用的最终回复文本。
     * @return 无返回值。
     */
    private void appendAiFinalMessage(
            List<AgentTraceMessageVo> messages,
            AgentTraceSpanVo outputModelSpan,
            String preferredText
    ) {
        if (outputModelSpan == null || outputModelSpan.getModelDetail() == null) {
            return;
        }
        String finalText = StringUtils.hasText(preferredText)
                ? preferredText
                : outputModelSpan.getModelDetail().getFinalText();
        if (StringUtils.hasText(finalText)) {
            addTraceMessageIfAbsent(messages, outputModelSpan, TRACE_MESSAGE_ROLE_AI, finalText, "final");
            return;
        }
        appendReadableMessages(
                messages,
                outputModelSpan,
                outputModelSpan.getModelDetail().getOutputMessages(),
                TRACE_MESSAGE_ROLE_AI
        );
    }

    /**
     * 添加指定角色的可读消息。
     *
     * @param outputMessages 消息输出列表。
     * @param sourceSpan     消息来源 Span。
     * @param sourceMessages 原始模型消息列表。
     * @param role           目标角色。
     * @return 无返回值。
     */
    private void appendReadableMessages(
            List<AgentTraceMessageVo> outputMessages,
            AgentTraceSpanVo sourceSpan,
            List<AgentTraceModelDetailVo.MessageVo> sourceMessages,
            String role
    ) {
        if (sourceSpan == null || sourceMessages == null || sourceMessages.isEmpty()) {
            return;
        }
        for (int index = 0; index < sourceMessages.size(); index++) {
            AgentTraceModelDetailVo.MessageVo sourceMessage = sourceMessages.get(index);
            if (!shouldUseMessageForRole(sourceMessage, role)) {
                continue;
            }
            String content = readMessageContentText(sourceMessage.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            addTraceMessage(outputMessages, sourceSpan, role, content, String.valueOf(index));
        }
    }

    /**
     * 判断消息是否属于目标可读角色。
     *
     * @param message 模型消息。
     * @param role    目标角色。
     * @return 属于目标角色返回 true。
     */
    private boolean shouldUseMessageForRole(AgentTraceModelDetailVo.MessageVo message, String role) {
        if (message == null) {
            return false;
        }
        String messageType = message.getType();
        if (TRACE_MESSAGE_ROLE_USER.equals(role)) {
            return MESSAGE_TYPE_HUMAN.equals(messageType);
        }
        if (TRACE_MESSAGE_ROLE_AI.equals(role)) {
            return MESSAGE_TYPE_AI.equals(messageType);
        }
        return false;
    }

    /**
     * 读取消息文本内容。
     *
     * @param content 原始消息内容。
     * @return 文本内容；非文本或空文本返回 null。
     */
    private String readMessageContentText(Object content) {
        if (content instanceof String text && StringUtils.hasText(text)) {
            return text.trim();
        }
        return null;
    }

    /**
     * 添加单条可读消息。
     *
     * @param messages 消息输出列表。
     * @param span     消息来源 Span。
     * @param role     消息角色。
     * @param content  消息内容。
     * @param suffix   消息 ID 后缀。
     * @return 无返回值。
     */
    private void addTraceMessage(
            List<AgentTraceMessageVo> messages,
            AgentTraceSpanVo span,
            String role,
            String content,
            String suffix
    ) {
        if (span == null || !StringUtils.hasText(role) || !StringUtils.hasText(content)) {
            return;
        }
        String normalizedContent = content.trim();
        AgentTraceMessageVo messageVo = new AgentTraceMessageVo();
        messageVo.setId(span.getSpanId() + ":" + role + ":" + suffix);
        messageVo.setRole(role);
        messageVo.setContent(normalizedContent);
        messageVo.setSourceSpanId(span.getSpanId());
        messageVo.setSequence(span.getSequence());
        messages.add(messageVo);
    }

    /**
     * 在不存在相同角色与内容时添加消息。
     *
     * @param messages 消息输出列表。
     * @param span     消息来源 Span。
     * @param role     消息角色。
     * @param content  消息内容。
     * @param suffix   消息 ID 后缀。
     * @return 无返回值。
     */
    private void addTraceMessageIfAbsent(
            List<AgentTraceMessageVo> messages,
            AgentTraceSpanVo span,
            String role,
            String content,
            String suffix
    ) {
        if (!StringUtils.hasText(role) || !StringUtils.hasText(content)) {
            return;
        }
        String normalizedContent = content.trim();
        if (containsTraceMessage(messages, role, normalizedContent)) {
            return;
        }
        addTraceMessage(messages, span, role, normalizedContent, suffix);
    }

    /**
     * 添加用户与 AI 的可读历史消息。
     *
     * @param outputMessages 消息输出列表。
     * @param sourceSpan     消息来源 Span。
     * @param sourceMessages 原始模型消息列表。
     * @return 无返回值。
     */
    private void appendReadableConversationMessages(
            List<AgentTraceMessageVo> outputMessages,
            AgentTraceSpanVo sourceSpan,
            List<AgentTraceModelDetailVo.MessageVo> sourceMessages
    ) {
        if (sourceSpan == null || sourceMessages == null || sourceMessages.isEmpty()) {
            return;
        }
        for (int index = 0; index < sourceMessages.size(); index++) {
            AgentTraceModelDetailVo.MessageVo sourceMessage = sourceMessages.get(index);
            String role = resolveTraceMessageRole(sourceMessage);
            if (!StringUtils.hasText(role)) {
                continue;
            }
            String content = readMessageContentText(sourceMessage.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            addTraceMessage(outputMessages, sourceSpan, role, content, String.valueOf(index));
        }
    }

    /**
     * 解析 Trace 消息视图角色。
     *
     * @param message 模型消息。
     * @return `user` 或 `ai`；非可读对话消息返回 null。
     */
    private String resolveTraceMessageRole(AgentTraceModelDetailVo.MessageVo message) {
        if (message == null) {
            return null;
        }
        String messageType = message.getType();
        if (MESSAGE_TYPE_HUMAN.equals(messageType)) {
            return TRACE_MESSAGE_ROLE_USER;
        }
        if (MESSAGE_TYPE_AI.equals(messageType)) {
            return TRACE_MESSAGE_ROLE_AI;
        }
        return null;
    }

    /**
     * 判断消息列表中是否已经存在相同角色和内容的消息。
     *
     * @param messages 消息列表。
     * @param role     消息角色。
     * @param content  消息内容。
     * @return 存在相同消息时返回 true。
     */
    private boolean containsTraceMessage(List<AgentTraceMessageVo> messages, String role, String content) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (AgentTraceMessageVo message : messages) {
            if (role.equals(message.getRole()) && content.equals(message.getContent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建可读消息视图容器。
     *
     * @param title    消息视图标题。
     * @param messages 可读消息列表。
     * @return 可读消息视图容器。
     */
    private AgentTraceMessageViewVo buildTraceMessageView(String title, List<AgentTraceMessageVo> messages) {
        AgentTraceMessageViewVo messageView = new AgentTraceMessageViewVo();
        messageView.setTitle(StringUtils.hasText(title) ? title : "Trace");
        messageView.setMessages(messages == null ? List.of() : messages);
        return messageView;
    }

    /**
     * 构建顶层概览工具调用列表。
     *
     * @param spans Span 明细列表。
     * @return 按工具执行顺序排列的工具调用列表。
     */
    private List<AgentTraceModelDetailVo.ToolCallVo> buildOverviewToolCalls(List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        Map<String, AgentTraceModelDetailVo.ToolCallVo> modelToolCallById = new LinkedHashMap<>();
        for (AgentTraceSpanVo span : spans) {
            AgentTraceModelDetailVo modelDetail = span.getModelDetail();
            if (!SPAN_TYPE_MODEL.equals(span.getSpanType())
                    || modelDetail == null
                    || modelDetail.getToolCalls() == null) {
                continue;
            }
            for (AgentTraceModelDetailVo.ToolCallVo toolCall : modelDetail.getToolCalls()) {
                if (toolCall == null || !StringUtils.hasText(toolCall.getId())) {
                    continue;
                }
                modelToolCallById.putIfAbsent(toolCall.getId(), toolCall);
            }
        }

        Set<String> appendedToolCallIds = new HashSet<>();
        List<AgentTraceModelDetailVo.ToolCallVo> overviewToolCalls = new ArrayList<>();
        for (AgentTraceSpanVo span : spans) {
            if (!isRealToolSpan(span)) {
                continue;
            }
            String toolCallId = readDocumentString(toDocument(span.getInputPayload()), "tool_call_id");
            if (!StringUtils.hasText(toolCallId) || appendedToolCallIds.contains(toolCallId)) {
                continue;
            }
            AgentTraceModelDetailVo.ToolCallVo toolCall = modelToolCallById.get(toolCallId);
            if (toolCall == null) {
                continue;
            }
            overviewToolCalls.add(toolCall);
            appendedToolCallIds.add(toolCallId);
        }
        return overviewToolCalls;
    }

    /**
     * 构建顶层属性详情。
     *
     * @param detailVo 当前 Trace 运行详情。
     * @param rootSpan root graph span。
     * @return 顶层属性详情。
     */
    private Map<String, Object> buildOverviewAttributes(AgentTraceDetailVo detailVo, AgentTraceSpanVo rootSpan) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("trace_id", detailVo.getTraceId());
        attributes.put("conversation_uuid", detailVo.getConversationUuid());
        attributes.put("assistant_message_uuid", detailVo.getAssistantMessageUuid());
        attributes.put("conversation_type", detailVo.getConversationType());
        attributes.put("user_id", detailVo.getUserId());
        attributes.put("graph_name", detailVo.getGraphName());
        attributes.put("entrypoint", detailVo.getEntrypoint());
        attributes.put("root_span_id", detailVo.getRootSpanId());
        if (rootSpan != null && rootSpan.getAttributes() != null) {
            attributes.put("root_attributes", rootSpan.getAttributes());
        }
        return attributes;
    }

    /**
     * 构建 run 级 Token 用量。
     *
     * @param detailVo 当前 Trace 运行详情。
     * @return Token 用量 Map。
     */
    private Map<String, Object> buildRunTokenUsage(AgentTraceDetailVo detailVo) {
        Map<String, Object> tokenUsage = new LinkedHashMap<>();
        tokenUsage.put("input_tokens", detailVo.getInputTokens());
        tokenUsage.put("output_tokens", detailVo.getOutputTokens());
        tokenUsage.put("total_tokens", detailVo.getTotalTokens());
        return tokenUsage;
    }

    /**
     * 填充节点 span 的结构化详情。
     *
     * @param spans Span 明细列表。
     * @return 无返回值。
     */
    private void fillNodeDetails(List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return;
        }
        Map<String, List<AgentTraceSpanVo>> childSpansByParentId = buildChildSpansByParentId(spans);
        for (AgentTraceSpanVo span : spans) {
            if (!SPAN_TYPE_NODE.equals(span.getSpanType())) {
                continue;
            }
            span.setNodeDetail(buildNodeDetail(span, childSpansByParentId));
        }
    }

    /**
     * 按父 Span ID 建立子 Span 索引。
     *
     * @param spans Span 明细列表。
     * @return 父 Span ID 到子 Span 列表的映射。
     */
    private Map<String, List<AgentTraceSpanVo>> buildChildSpansByParentId(List<AgentTraceSpanVo> spans) {
        Map<String, List<AgentTraceSpanVo>> childSpansByParentId = new HashMap<>();
        for (AgentTraceSpanVo span : spans) {
            if (!StringUtils.hasText(span.getParentSpanId())) {
                continue;
            }
            childSpansByParentId.computeIfAbsent(span.getParentSpanId(), ignored -> new ArrayList<>()).add(span);
        }
        return childSpansByParentId;
    }

    /**
     * 构建节点执行详情。
     *
     * @param nodeSpan             节点 Span。
     * @param childSpansByParentId 父 Span ID 到子 Span 列表的映射。
     * @return 节点执行详情。
     */
    private AgentTraceNodeDetailVo buildNodeDetail(
            AgentTraceSpanVo nodeSpan,
            Map<String, List<AgentTraceSpanVo>> childSpansByParentId
    ) {
        List<AgentTraceSpanVo> descendantSpans = collectDescendantSpans(nodeSpan.getSpanId(), childSpansByParentId);
        AgentTraceNodeDetailVo nodeDetail = new AgentTraceNodeDetailVo();
        nodeDetail.setNodeName(nodeSpan.getName());
        nodeDetail.setStatus(nodeSpan.getStatus());
        nodeDetail.setStartedAt(nodeSpan.getStartedAt());
        nodeDetail.setEndedAt(nodeSpan.getEndedAt());
        nodeDetail.setDurationMs(nodeSpan.getDurationMs());
        nodeDetail.setTokenUsage(buildNodeTokenUsage(descendantSpans));
        nodeDetail.setInputPayload(nodeSpan.getInputPayload());
        nodeDetail.setOutputPayload(nodeSpan.getOutputPayload());
        nodeDetail.setChildSummary(buildNodeChildSummary(descendantSpans));
        nodeDetail.setExecutionSteps(buildNodeExecutionSteps(descendantSpans));
        nodeDetail.setErrorPayload(buildNodeErrorPayload(nodeSpan, descendantSpans));
        nodeDetail.setMessageView(buildNodeMessageView(nodeSpan, descendantSpans));
        return nodeDetail;
    }

    /**
     * 递归采集指定 Span 下的全部后代 Span。
     *
     * @param spanId               当前 Span ID。
     * @param childSpansByParentId 父 Span ID 到子 Span 列表的映射。
     * @return 后代 Span 列表，按 sequence 升序排列。
     */
    private List<AgentTraceSpanVo> collectDescendantSpans(
            String spanId,
            Map<String, List<AgentTraceSpanVo>> childSpansByParentId
    ) {
        if (!StringUtils.hasText(spanId)) {
            return List.of();
        }
        List<AgentTraceSpanVo> descendantSpans = new ArrayList<>();
        collectDescendantSpans(spanId, childSpansByParentId, descendantSpans);
        descendantSpans.sort((left, right) -> Long.compare(
                left.getSequence() == null ? 0L : left.getSequence(),
                right.getSequence() == null ? 0L : right.getSequence()
        ));
        return descendantSpans;
    }

    /**
     * 递归写入后代 Span 列表。
     *
     * @param spanId               当前 Span ID。
     * @param childSpansByParentId 父 Span ID 到子 Span 列表的映射。
     * @param descendantSpans      后代 Span 输出列表。
     * @return 无返回值。
     */
    private void collectDescendantSpans(
            String spanId,
            Map<String, List<AgentTraceSpanVo>> childSpansByParentId,
            List<AgentTraceSpanVo> descendantSpans
    ) {
        List<AgentTraceSpanVo> childSpans = childSpansByParentId.getOrDefault(spanId, List.of());
        for (AgentTraceSpanVo childSpan : childSpans) {
            descendantSpans.add(childSpan);
            collectDescendantSpans(childSpan.getSpanId(), childSpansByParentId, descendantSpans);
        }
    }

    /**
     * 汇总节点子树模型 Token。
     *
     * @param descendantSpans 节点后代 Span 列表。
     * @return Token 汇总；没有模型 Token 时返回 null。
     */
    private Map<String, Object> buildNodeTokenUsage(List<AgentTraceSpanVo> descendantSpans) {
        long inputTokens = 0L;
        long outputTokens = 0L;
        long totalTokens = 0L;
        boolean hasTokenUsage = false;
        for (AgentTraceSpanVo span : descendantSpans) {
            if (!SPAN_TYPE_MODEL.equals(span.getSpanType()) || span.getTokenUsage() == null) {
                continue;
            }
            Long inputTokenValue = readMapLong(span.getTokenUsage(), "input_tokens");
            Long outputTokenValue = readMapLong(span.getTokenUsage(), "output_tokens");
            Long totalTokenValue = readMapLong(span.getTokenUsage(), "total_tokens");
            if (inputTokenValue == null && outputTokenValue == null && totalTokenValue == null) {
                continue;
            }
            long resolvedInputTokens = inputTokenValue == null ? 0L : inputTokenValue;
            long resolvedOutputTokens = outputTokenValue == null ? 0L : outputTokenValue;
            inputTokens += resolvedInputTokens;
            outputTokens += resolvedOutputTokens;
            totalTokens += totalTokenValue == null ? resolvedInputTokens + resolvedOutputTokens : totalTokenValue;
            hasTokenUsage = true;
        }
        if (!hasTokenUsage) {
            return null;
        }
        Map<String, Object> tokenUsage = new LinkedHashMap<>();
        tokenUsage.put("input_tokens", inputTokens);
        tokenUsage.put("output_tokens", outputTokens);
        tokenUsage.put("total_tokens", totalTokens);
        return tokenUsage;
    }

    /**
     * 构建节点子树摘要。
     *
     * @param descendantSpans 节点后代 Span 列表。
     * @return 节点子树摘要。
     */
    private AgentTraceNodeDetailVo.ChildSummaryVo buildNodeChildSummary(List<AgentTraceSpanVo> descendantSpans) {
        int modelCount = 0;
        int toolCount = 0;
        int middlewareCount = 0;
        int errorCount = 0;
        for (AgentTraceSpanVo span : descendantSpans) {
            if (SPAN_TYPE_MODEL.equals(span.getSpanType())) {
                modelCount++;
            } else if (isRealToolSpan(span)) {
                toolCount++;
            } else if (SPAN_TYPE_MIDDLEWARE.equals(span.getSpanType())) {
                middlewareCount++;
            }
            if (isErrorSpan(span)) {
                errorCount++;
            }
        }
        AgentTraceNodeDetailVo.ChildSummaryVo childSummary = new AgentTraceNodeDetailVo.ChildSummaryVo();
        childSummary.setModelCount(modelCount);
        childSummary.setToolCount(toolCount);
        childSummary.setMiddlewareCount(middlewareCount);
        childSummary.setErrorCount(errorCount);
        return childSummary;
    }

    /**
     * 构建节点内部执行步骤。
     *
     * @param descendantSpans 节点后代 Span 列表。
     * @return 节点内部执行步骤列表。
     */
    private List<AgentTraceNodeDetailVo.ExecutionStepVo> buildNodeExecutionSteps(
            List<AgentTraceSpanVo> descendantSpans
    ) {
        List<AgentTraceNodeDetailVo.ExecutionStepVo> executionSteps = new ArrayList<>();
        for (AgentTraceSpanVo span : descendantSpans) {
            if (!shouldShowNodeExecutionStep(span)) {
                continue;
            }
            AgentTraceNodeDetailVo.ExecutionStepVo stepVo = new AgentTraceNodeDetailVo.ExecutionStepVo();
            stepVo.setSpanId(span.getSpanId());
            stepVo.setParentSpanId(span.getParentSpanId());
            stepVo.setSpanType(resolveTreeNodeSpanType(span));
            stepVo.setName(span.getName());
            stepVo.setDisplayName(resolveExecutionStepDisplayName(span));
            stepVo.setStatus(span.getStatus());
            stepVo.setDurationMs(span.getDurationMs());
            stepVo.setTokenText(formatTokenText(span.getTokenUsage()));
            stepVo.setTokenUsage(span.getTokenUsage());
            stepVo.setSequence(span.getSequence());
            executionSteps.add(stepVo);
        }
        return executionSteps;
    }

    /**
     * 构建节点错误载荷。
     *
     * @param nodeSpan        节点 Span。
     * @param descendantSpans 节点后代 Span 列表。
     * @return 当前节点错误或子节点错误摘要；没有错误时返回 null。
     */
    private Object buildNodeErrorPayload(AgentTraceSpanVo nodeSpan, List<AgentTraceSpanVo> descendantSpans) {
        if (hasPayload(nodeSpan.getErrorPayload())) {
            return nodeSpan.getErrorPayload();
        }
        List<Map<String, Object>> childErrors = new ArrayList<>();
        for (AgentTraceSpanVo span : descendantSpans) {
            if (!isErrorSpan(span)) {
                continue;
            }
            Map<String, Object> errorSummary = new LinkedHashMap<>();
            errorSummary.put("span_id", span.getSpanId());
            errorSummary.put("span_type", resolveTreeNodeSpanType(span));
            errorSummary.put("name", resolveExecutionStepDisplayName(span));
            errorSummary.put("status", span.getStatus());
            errorSummary.put("duration_ms", span.getDurationMs());
            errorSummary.put("error_payload", span.getErrorPayload());
            childErrors.add(errorSummary);
        }
        return childErrors.isEmpty() ? null : childErrors;
    }

    /**
     * 判断 Span 是否是有效工具调用。
     *
     * @param span Span 明细。
     * @return 是真实工具调用返回 true。
     */
    private boolean isRealToolSpan(AgentTraceSpanVo span) {
        return SPAN_TYPE_TOOL.equals(span.getSpanType()) && !TOOLS_GROUP_SPAN_NAME.equals(span.getName());
    }

    /**
     * 判断 Span 是否处于异常状态。
     *
     * @param span Span 明细。
     * @return 异常状态或存在错误载荷时返回 true。
     */
    private boolean isErrorSpan(AgentTraceSpanVo span) {
        return STATUS_ERROR.equals(span.getStatus()) || hasPayload(span.getErrorPayload());
    }

    /**
     * 判断节点执行流是否展示当前 Span。
     *
     * @param span Span 明细。
     * @return 需要展示返回 true。
     */
    private boolean shouldShowNodeExecutionStep(AgentTraceSpanVo span) {
        return !SPAN_TYPE_GRAPH.equals(span.getSpanType()) && !TOOLS_GROUP_SPAN_NAME.equals(span.getName());
    }

    /**
     * 解析执行步骤展示名称。
     *
     * @param span Span 明细。
     * @return 执行步骤展示名称。
     */
    private String resolveExecutionStepDisplayName(AgentTraceSpanVo span) {
        if (SPAN_TYPE_MODEL.equals(span.getSpanType())) {
            String modelName = resolveModelName(span);
            return StringUtils.hasText(modelName) ? modelName : resolveSpanDisplayName(span);
        }
        return resolveSpanDisplayName(span);
    }

    /**
     * 判断载荷是否存在可展示内容。
     *
     * @param value 原始载荷。
     * @return 有内容时返回 true。
     */
    private boolean hasPayload(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.hasText(text);
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }

    /**
     * 填充模型 span 的结构化详情。
     *
     * @param spans Span 明细列表。
     * @return 无返回值。
     */
    private void fillModelDetails(List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return;
        }
        Map<String, AgentTraceSpanVo> toolSpanByCallId = buildToolSpanByCallId(spans);
        for (AgentTraceSpanVo span : spans) {
            if (!SPAN_TYPE_MODEL.equals(span.getSpanType())) {
                continue;
            }
            AgentTraceModelDetailVo modelDetail = buildModelDetail(span, toolSpanByCallId);
            if (modelDetail != null) {
                span.setModelDetail(modelDetail);
            }
        }
    }

    /**
     * 按 tool_call_id 建立工具 span 索引。
     *
     * @param spans Span 明细列表。
     * @return tool_call_id 到工具 span 的映射。
     */
    private Map<String, AgentTraceSpanVo> buildToolSpanByCallId(List<AgentTraceSpanVo> spans) {
        Map<String, AgentTraceSpanVo> toolSpanByCallId = new HashMap<>();
        for (AgentTraceSpanVo span : spans) {
            if (!SPAN_TYPE_TOOL.equals(span.getSpanType())) {
                continue;
            }
            Document inputPayload = toDocument(span.getInputPayload());
            String toolCallId = readDocumentString(inputPayload, "tool_call_id");
            if (StringUtils.hasText(toolCallId)) {
                toolSpanByCallId.put(toolCallId, span);
            }
        }
        return toolSpanByCallId;
    }

    /**
     * 构建模型调用结构化详情。
     *
     * @param span             模型 Span。
     * @param toolSpanByCallId 工具调用 ID 到工具 Span 的映射。
     * @return 模型调用详情；非新结构模型 payload 返回 null。
     */
    private AgentTraceModelDetailVo buildModelDetail(
            AgentTraceSpanVo span,
            Map<String, AgentTraceSpanVo> toolSpanByCallId
    ) {
        Document inputPayload = toDocument(span.getInputPayload());
        Document outputPayload = toDocument(span.getOutputPayload());
        if (!hasStructuredModelPayload(inputPayload, outputPayload)) {
            return null;
        }

        List<AgentTraceModelDetailVo.ToolCallVo> toolCalls = buildModelToolCalls(
                getListValue(outputPayload, "tool_calls"),
                toolSpanByCallId
        );
        AgentTraceModelDetailVo modelDetail = new AgentTraceModelDetailVo();
        modelDetail.setModelName(resolveModelName(span));
        modelDetail.setModelClass(readMapString(span.getAttributes(), "model_class"));
        modelDetail.setSlot(readMapString(span.getAttributes(), "slot"));
        modelDetail.setSettings(span.getAttributes() == null ? null : span.getAttributes().get("model_settings"));
        modelDetail.setFinishReason(readDocumentString(outputPayload, "finish_reason"));
        modelDetail.setTokenUsage(span.getTokenUsage());
        modelDetail.setSystemPrompt(buildModelSystemPrompt(inputPayload));
        modelDetail.setAvailableTools(buildAvailableTools(inputPayload, toolCalls));
        modelDetail.setInputMessages(buildModelMessages(getMessages(inputPayload), toolSpanByCallId));
        modelDetail.setOutputMessages(buildModelMessages(getMessages(outputPayload), toolSpanByCallId));
        modelDetail.setToolCalls(toolCalls);
        modelDetail.setFinalText(readDocumentString(outputPayload, "final_text"));
        modelDetail.setMessageView(buildModelMessageView(span, modelDetail));
        return modelDetail;
    }

    /**
     * 判断模型 payload 是否已经升级为结构化详情合同。
     *
     * @param inputPayload  模型输入载荷。
     * @param outputPayload 模型输出载荷。
     * @return 新结构返回 true。
     */
    private boolean hasStructuredModelPayload(Document inputPayload, Document outputPayload) {
        return inputPayload.containsKey("system_prompt")
                || inputPayload.containsKey("available_tools")
                || outputPayload.containsKey("final_text");
    }

    /**
     * 构建模型系统提示词视图。
     *
     * @param inputPayload 模型输入载荷。
     * @return 系统提示词视图；没有提示词时返回 null。
     */
    private AgentTraceModelDetailVo.SystemPromptVo buildModelSystemPrompt(Document inputPayload) {
        Document systemPromptDocument = toDocument(inputPayload.get("system_prompt"));
        String content = readDocumentString(systemPromptDocument, "content");
        if (!StringUtils.hasText(content)) {
            content = readDocumentString(inputPayload, "system_message");
        }
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String renderMode = readDocumentString(systemPromptDocument, "render_mode");
        AgentTraceModelDetailVo.SystemPromptVo systemPromptVo = new AgentTraceModelDetailVo.SystemPromptVo();
        systemPromptVo.setContent(content);
        systemPromptVo.setRenderMode(StringUtils.hasText(renderMode) ? renderMode : RENDER_MODE_MARKDOWN);
        return systemPromptVo;
    }

    /**
     * 构建模型可见工具列表。
     *
     * @param inputPayload 模型输入载荷。
     * @param toolCalls    模型工具调用列表。
     * @return 模型可见工具列表。
     */
    private List<AgentTraceModelDetailVo.ToolVo> buildAvailableTools(
            Document inputPayload,
            List<AgentTraceModelDetailVo.ToolCallVo> toolCalls
    ) {
        Map<String, List<AgentTraceModelDetailVo.ToolCallVo>> callsByToolName = groupToolCallsByName(toolCalls);
        List<AgentTraceModelDetailVo.ToolVo> availableTools = new ArrayList<>();
        for (Object item : getModelAvailableToolItems(inputPayload)) {
            Document toolDocument = toDocument(item);
            String name = readDocumentString(toolDocument, "name");
            if (!StringUtils.hasText(name)) {
                continue;
            }
            List<AgentTraceModelDetailVo.ToolCallVo> calls = callsByToolName.getOrDefault(name, List.of());
            AgentTraceModelDetailVo.ToolVo toolVo = new AgentTraceModelDetailVo.ToolVo();
            toolVo.setName(name);
            toolVo.setDisplayName(readDocumentString(toolDocument, "display_name"));
            toolVo.setDescription(readDocumentString(toolDocument, "description"));
            toolVo.setArgsSchema(toolDocument.get("args_schema"));
            toolVo.setCalled(!calls.isEmpty());
            toolVo.setCalls(calls);
            availableTools.add(toolVo);
        }
        return availableTools;
    }

    /**
     * 读取模型可见工具原始列表。
     *
     * @param inputPayload 模型输入载荷。
     * @return 工具原始列表。
     */
    private List<?> getModelAvailableToolItems(Document inputPayload) {
        List<?> availableTools = getListValue(inputPayload, "available_tools");
        if (!availableTools.isEmpty()) {
            return availableTools;
        }
        return getListValue(inputPayload, "tools");
    }

    /**
     * 按工具名称分组工具调用。
     *
     * @param toolCalls 工具调用列表。
     * @return 工具名称到调用列表的映射。
     */
    private Map<String, List<AgentTraceModelDetailVo.ToolCallVo>> groupToolCallsByName(
            List<AgentTraceModelDetailVo.ToolCallVo> toolCalls
    ) {
        Map<String, List<AgentTraceModelDetailVo.ToolCallVo>> callsByToolName = new LinkedHashMap<>();
        for (AgentTraceModelDetailVo.ToolCallVo toolCall : toolCalls) {
            if (!StringUtils.hasText(toolCall.getName())) {
                continue;
            }
            callsByToolName.computeIfAbsent(toolCall.getName(), ignored -> new ArrayList<>()).add(toolCall);
        }
        return callsByToolName;
    }

    /**
     * 构建模型消息列表。
     *
     * @param messages         原始消息列表。
     * @param toolSpanByCallId 工具调用 ID 到工具 Span 的映射。
     * @return 模型消息视图列表。
     */
    private List<AgentTraceModelDetailVo.MessageVo> buildModelMessages(
            List<?> messages,
            Map<String, AgentTraceSpanVo> toolSpanByCallId
    ) {
        List<AgentTraceModelDetailVo.MessageVo> messageVos = new ArrayList<>();
        for (Object message : messages) {
            Document messageDocument = toDocument(message);
            AgentTraceModelDetailVo.MessageVo messageVo = new AgentTraceModelDetailVo.MessageVo();
            messageVo.setType(readDocumentString(messageDocument, "type"));
            messageVo.setContent(messageDocument.get("content"));
            messageVo.setName(readDocumentString(messageDocument, "name"));
            messageVo.setToolCallId(readDocumentString(messageDocument, "tool_call_id"));
            messageVo.setResponseMetadata(messageDocument.get("response_metadata"));
            messageVo.setToolCalls(buildModelToolCalls(getListValue(messageDocument, "tool_calls"), toolSpanByCallId));
            messageVos.add(messageVo);
        }
        return messageVos;
    }

    /**
     * 构建模型工具调用列表。
     *
     * @param toolCallItems    原始工具调用列表。
     * @param toolSpanByCallId 工具调用 ID 到工具 Span 的映射。
     * @return 工具调用视图列表。
     */
    private List<AgentTraceModelDetailVo.ToolCallVo> buildModelToolCalls(
            List<?> toolCallItems,
            Map<String, AgentTraceSpanVo> toolSpanByCallId
    ) {
        List<AgentTraceModelDetailVo.ToolCallVo> toolCalls = new ArrayList<>();
        for (Object item : toolCallItems) {
            Document toolCallDocument = toDocument(item);
            AgentTraceModelDetailVo.ToolCallVo toolCallVo = buildModelToolCall(toolCallDocument, toolSpanByCallId);
            if (StringUtils.hasText(toolCallVo.getId()) || StringUtils.hasText(toolCallVo.getName())) {
                toolCalls.add(toolCallVo);
            }
        }
        return toolCalls;
    }

    /**
     * 构建单个模型工具调用。
     *
     * @param toolCallDocument 原始工具调用文档。
     * @param toolSpanByCallId 工具调用 ID 到工具 Span 的映射。
     * @return 工具调用视图对象。
     */
    private AgentTraceModelDetailVo.ToolCallVo buildModelToolCall(
            Document toolCallDocument,
            Map<String, AgentTraceSpanVo> toolSpanByCallId
    ) {
        String toolCallId = readDocumentString(toolCallDocument, "id");
        if (!StringUtils.hasText(toolCallId)) {
            toolCallId = readDocumentString(toolCallDocument, "tool_call_id");
        }
        AgentTraceSpanVo toolSpan = StringUtils.hasText(toolCallId) ? toolSpanByCallId.get(toolCallId) : null;
        String toolName = readDocumentString(toolCallDocument, "name");
        if (!StringUtils.hasText(toolName) && toolSpan != null) {
            toolName = toolSpan.getName();
        }

        AgentTraceModelDetailVo.ToolCallVo toolCallVo = new AgentTraceModelDetailVo.ToolCallVo();
        toolCallVo.setId(toolCallId);
        toolCallVo.setName(toolName);
        toolCallVo.setDisplayName(toolSpan == null ? null : readMapString(toolSpan.getAttributes(), "display_name"));
        toolCallVo.setArguments(readToolCallArguments(toolCallDocument));
        if (toolSpan != null) {
            toolCallVo.setStatus(toolSpan.getStatus());
            toolCallVo.setDurationMs(toolSpan.getDurationMs());
            toolCallVo.setOutputPayload(toolSpan.getOutputPayload());
            toolCallVo.setErrorPayload(toolSpan.getErrorPayload());
        }
        return toolCallVo;
    }

    /**
     * 读取工具调用参数。
     *
     * @param toolCallDocument 原始工具调用文档。
     * @return 工具调用参数。
     */
    private Object readToolCallArguments(Document toolCallDocument) {
        if (toolCallDocument.containsKey("arguments")) {
            return toolCallDocument.get("arguments");
        }
        return toolCallDocument.get("args");
    }

    /**
     * 构建 Span 树形展示节点。
     *
     * @param spans Span 明细列表。
     * @return Span 树形展示节点列表。
     */
    private List<AgentTraceSpanTreeNodeVo> buildSpanTree(List<AgentTraceSpanVo> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        Map<String, AgentTraceSpanTreeNodeVo> nodeMap = new HashMap<>();
        List<AgentTraceSpanTreeNodeVo> rootNodes = new ArrayList<>();

        for (AgentTraceSpanVo span : spans) {
            if (!StringUtils.hasText(span.getSpanId())) {
                continue;
            }
            nodeMap.put(span.getSpanId(), toSpanTreeNode(span));
        }

        for (AgentTraceSpanVo span : spans) {
            if (!StringUtils.hasText(span.getSpanId())) {
                continue;
            }
            AgentTraceSpanTreeNodeVo node = nodeMap.get(span.getSpanId());
            if (node == null) {
                continue;
            }
            AgentTraceSpanTreeNodeVo parentNode = StringUtils.hasText(span.getParentSpanId())
                    ? nodeMap.get(span.getParentSpanId())
                    : null;
            if (parentNode == null || span.getSpanId().equals(parentNode.getNodeId())) {
                rootNodes.add(node);
            } else {
                addChildTreeNode(parentNode, node);
            }
            if (SPAN_TYPE_MODEL.equals(span.getSpanType())) {
                addChildTreeNode(node, toModelCallTreeNode(span));
            }
        }

        sortSpanTreeNodes(rootNodes);
        pruneEmptyTreeChildren(rootNodes);
        return rootNodes;
    }

    /**
     * 转换真实 Span 为树形展示节点。
     *
     * @param span Span 明细。
     * @return 树形展示节点。
     */
    private AgentTraceSpanTreeNodeVo toSpanTreeNode(AgentTraceSpanVo span) {
        AgentTraceSpanTreeNodeVo node = new AgentTraceSpanTreeNodeVo();
        node.setNodeId(span.getSpanId());
        node.setSourceSpanId(span.getSpanId());
        node.setParentNodeId(span.getParentSpanId());
        node.setNodeType(isGroupSpan(span) ? TREE_NODE_TYPE_SPAN_GROUP : TREE_NODE_TYPE_SPAN);
        node.setSpanType(resolveTreeNodeSpanType(span));
        node.setName(span.getName());
        node.setDisplayName(resolveSpanDisplayName(span));
        node.setModelName(isGroupSpan(span) ? null : resolveModelName(span));
        node.setStatus(span.getStatus());
        node.setDurationMs(isGroupSpan(span) ? null : span.getDurationMs());
        node.setTokenText(isGroupSpan(span) ? null : formatTokenText(span.getTokenUsage()));
        node.setSequence(span.getSequence());
        return node;
    }

    /**
     * 转换模型真实 Span 为模型调用展示子节点。
     *
     * @param span 模型 Span 明细。
     * @return 模型调用展示子节点。
     */
    private AgentTraceSpanTreeNodeVo toModelCallTreeNode(AgentTraceSpanVo span) {
        AgentTraceSpanTreeNodeVo node = new AgentTraceSpanTreeNodeVo();
        String modelName = resolveModelName(span);
        node.setNodeId(span.getSpanId() + MODEL_CALL_NODE_ID_SUFFIX);
        node.setSourceSpanId(span.getSpanId());
        node.setParentNodeId(span.getSpanId());
        node.setNodeType(TREE_NODE_TYPE_MODEL_CALL);
        node.setSpanType(span.getSpanType());
        node.setName(StringUtils.hasText(modelName) ? modelName : "-");
        node.setDisplayName(StringUtils.hasText(modelName) ? modelName : "-");
        node.setModelName(modelName);
        node.setStatus(span.getStatus());
        node.setDurationMs(span.getDurationMs());
        node.setTokenText(formatTokenText(span.getTokenUsage()));
        node.setSequence(span.getSequence());
        return node;
    }

    /**
     * 解析 Span 展示名称。
     *
     * @param span Span 明细。
     * @return Span 展示名称。
     */
    private String resolveSpanDisplayName(AgentTraceSpanVo span) {
        if (SPAN_TYPE_MODEL.equals(span.getSpanType())) {
            return MODEL_GROUP_DISPLAY_NAME;
        }
        if (TOOLS_GROUP_SPAN_NAME.equals(span.getName())) {
            return TOOLS_GROUP_DISPLAY_NAME;
        }
        return StringUtils.hasText(span.getName()) ? span.getName() : "-";
    }

    /**
     * 判断 Span 是否只是树形分组节点。
     *
     * @param span Span 明细。
     * @return 是分组节点返回 true。
     */
    private boolean isGroupSpan(AgentTraceSpanVo span) {
        return SPAN_TYPE_MODEL.equals(span.getSpanType()) || TOOLS_GROUP_SPAN_NAME.equals(span.getName());
    }

    /**
     * 解析树节点展示使用的 Span 类型。
     *
     * @param span Span 明细。
     * @return 树节点展示 Span 类型。
     */
    private String resolveTreeNodeSpanType(AgentTraceSpanVo span) {
        if (TOOLS_GROUP_SPAN_NAME.equals(span.getName())) {
            return SPAN_TYPE_TOOL;
        }
        return span.getSpanType();
    }

    /**
     * 解析模型名称。
     *
     * @param span Span 明细。
     * @return 模型名称；不存在时返回 null。
     */
    private String resolveModelName(AgentTraceSpanVo span) {
        if (!SPAN_TYPE_MODEL.equals(span.getSpanType())) {
            return null;
        }
        String modelName = readMapString(span.getAttributes(), "model_name");
        if (StringUtils.hasText(modelName)) {
            return modelName;
        }
        modelName = readMapString(span.getAttributes(), "model");
        if (StringUtils.hasText(modelName)) {
            return modelName;
        }
        return readMapString(span.getAttributes(), "model_id");
    }

    /**
     * 从 Map 中读取字符串字段。
     *
     * @param map Map 数据。
     * @param key 字段名。
     * @return 字符串字段；不存在时返回 null。
     */
    private String readMapString(Map<String, Object> map, String key) {
        if (map == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 格式化 Token 展示文本。
     *
     * @param tokenUsage Token 用量。
     * @return Token 展示文本；没有 token 时返回 null。
     */
    private String formatTokenText(Map<String, Object> tokenUsage) {
        Long totalTokens = readMapLong(tokenUsage, "total_tokens");
        if (totalTokens == null) {
            totalTokens = readMapLong(tokenUsage, "totalTokens");
        }
        if (totalTokens == null || totalTokens <= 0) {
            return null;
        }
        if (totalTokens >= 1000) {
            double kiloTokens = totalTokens / 1000.0;
            return String.format("%.1fK", kiloTokens);
        }
        return String.valueOf(totalTokens);
    }

    /**
     * 从 Map 中读取 Long 字段。
     *
     * @param map Map 数据。
     * @param key 字段名。
     * @return Long 字段；不存在或无法转换时返回 null。
     */
    private Long readMapLong(Map<String, Object> map, String key) {
        if (map == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 添加树形子节点。
     *
     * @param parentNode 父节点。
     * @param childNode  子节点。
     * @return 无返回值。
     */
    private void addChildTreeNode(AgentTraceSpanTreeNodeVo parentNode, AgentTraceSpanTreeNodeVo childNode) {
        if (parentNode.getChildren() == null) {
            parentNode.setChildren(new ArrayList<>());
        }
        parentNode.getChildren().add(childNode);
    }

    /**
     * 按 sequence 递归排序树节点。
     *
     * @param nodes 树节点列表。
     * @return 无返回值。
     */
    private void sortSpanTreeNodes(List<AgentTraceSpanTreeNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort((left, right) -> Long.compare(
                left.getSequence() == null ? 0L : left.getSequence(),
                right.getSequence() == null ? 0L : right.getSequence()
        ));
        for (AgentTraceSpanTreeNodeVo node : nodes) {
            sortSpanTreeNodes(node.getChildren());
        }
    }

    /**
     * 移除空 children 字段，避免前端出现无意义展开图标。
     *
     * @param nodes 树节点列表。
     * @return 无返回值。
     */
    private void pruneEmptyTreeChildren(List<AgentTraceSpanTreeNodeVo> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (AgentTraceSpanTreeNodeVo node : nodes) {
            if (node.getChildren() == null || node.getChildren().isEmpty()) {
                node.setChildren(null);
            } else {
                pruneEmptyTreeChildren(node.getChildren());
            }
        }
    }

    /**
     * 从 Document 中读取列表字段。
     *
     * @param document Mongo 文档。
     * @param key      字段名。
     * @return 列表字段；不存在时返回空列表。
     */
    private List<?> getListValue(Document document, String key) {
        Object value = document.get(key);
        if (value instanceof List<?> listValue) {
            return listValue;
        }
        return List.of();
    }

    /**
     * 读取 payload 中的 messages 列表。
     *
     * @param payload 输入或输出 payload 文档。
     * @return messages 列表；不存在时返回空列表。
     */
    private List<?> getMessages(Document payload) {
        return getListValue(payload, "messages");
    }

    /**
     * 从消息列表中倒序查找指定类型的内容。
     *
     * @param messages    消息列表。
     * @param messageType 目标消息类型。
     * @return 消息内容文本；找不到时返回 null。
     */
    private String findLastMessageContent(List<?> messages, String messageType) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            Document messageDocument = toDocument(messages.get(index));
            if (!messageType.equals(messageDocument.getString("type"))) {
                continue;
            }
            String content = extractContentText(messageDocument.get("content"));
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        return null;
    }

    /**
     * 提取消息 content 文本。
     *
     * @param content 原始 content 字段。
     * @return 文本内容；非文本内容返回 null。
     */
    private String extractContentText(Object content) {
        if (content instanceof String text) {
            String normalizedText = text.trim();
            return normalizedText.isEmpty() ? null : normalizedText;
        }
        return null;
    }

    /**
     * 从 Document 中读取字符串字段。
     *
     * @param document Mongo 文档。
     * @param key      字段名。
     * @return 字符串字段；不存在时返回 null。
     */
    private String readDocumentString(Document document, String key) {
        if (document == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = document.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 将 Mongo 子文档转换成 Document。
     *
     * @param value 原始字段值。
     * @return Document；字段不是文档时返回空 Document。
     */
    private Document toDocument(Object value) {
        if (value instanceof Document document) {
            return document;
        }
        if (value instanceof Map<?, ?> map) {
            return new Document(toMap(map));
        }
        return new Document();
    }

    /**
     * 转换 span 视图对象。
     *
     * @param document Mongo span 文档。
     * @return span 视图对象。
     */
    private AgentTraceSpanVo toSpanVo(Document document) {
        AgentTraceSpanVo vo = new AgentTraceSpanVo();
        vo.setSpanId(document.getString("span_id"));
        vo.setParentSpanId(document.getString("parent_span_id"));
        vo.setSpanType(document.getString("span_type"));
        vo.setName(document.getString("name"));
        vo.setStatus(document.getString("status"));
        vo.setStartedAt(document.getDate("started_at"));
        vo.setEndedAt(document.getDate("ended_at"));
        vo.setDurationMs(toLong(document.get("duration_ms")));
        vo.setInputPayload(document.get("input_payload"));
        vo.setOutputPayload(document.get("output_payload"));
        vo.setAttributes(toMap(document.get("attributes")));
        vo.setTokenUsage(toMap(document.get("token_usage")));
        vo.setErrorPayload(document.get("error_payload"));
        vo.setSequence(toLong(document.get("sequence")));
        return vo;
    }

    /**
     * 将 Mongo 数值字段转换成 Long。
     *
     * @param value 原始字段值。
     * @return Long 值；字段为空时返回 null。
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * 将 Mongo 文档字段转换成 Map。
     *
     * @param value 原始字段值。
     * @return Map 结构；字段不是 Map 时返回 null。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return null;
    }

}
