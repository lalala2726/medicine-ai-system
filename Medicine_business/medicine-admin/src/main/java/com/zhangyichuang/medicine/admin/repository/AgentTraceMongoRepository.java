package com.zhangyichuang.medicine.admin.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongodb.client.result.DeleteResult;
import com.zhangyichuang.medicine.admin.model.request.AgentTraceMonitorRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentTraceRunListRequest;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Trace Mongo 读取仓储。
 */
@Repository
@RequiredArgsConstructor
public class AgentTraceMongoRepository {

    /**
     * Agent Trace run 集合名。
     */
    private static final String RUNS_COLLECTION = "agent_trace_runs";

    /**
     * Agent Trace span 集合名。
     */
    private static final String SPANS_COLLECTION = "agent_trace_spans";

    /**
     * Agent 模型 Token 用量明细集合名。
     */
    private static final String TOKEN_USAGE_COLLECTION = "agent_model_token_usage";

    /**
     * Trace ID 字段名。
     */
    private static final String FIELD_TRACE_ID = "trace_id";

    /**
     * 开始时间字段名。
     */
    private static final String FIELD_STARTED_AT = "started_at";

    /**
     * Span 顺序字段名。
     */
    private static final String FIELD_SEQUENCE = "sequence";

    /**
     * Span 类型字段名。
     */
    private static final String FIELD_SPAN_TYPE = "span_type";

    /**
     * 模型 span 类型值。
     */
    private static final String SPAN_TYPE_MODEL = "model";

    /**
     * Spring Mongo 操作入口。
     */
    private final MongoTemplate mongoTemplate;

    /**
     * 分页查询 Agent Trace run 文档。
     *
     * @param request 查询参数。
     * @return Mongo run 文档分页。
     */
    public Page<Document> listRuns(AgentTraceRunListRequest request) {
        Query countQuery = buildRunQuery(request);
        long total = mongoTemplate.count(countQuery, RUNS_COLLECTION);
        Page<Document> page = request.toPage();
        Query pageQuery = buildRunQuery(request)
                .with(Sort.by(Sort.Direction.DESC, FIELD_STARTED_AT))
                .skip((page.getCurrent() - 1) * page.getSize())
                .limit(Math.toIntExact(page.getSize()));
        List<Document> documents = mongoTemplate.find(pageQuery, Document.class, RUNS_COLLECTION);
        page.setTotal(total);
        page.setRecords(documents);
        return page;
    }

    /**
     * 根据 trace ID 查询 run 文档。
     *
     * @param traceId Trace 唯一标识。
     * @return run 文档；不存在时返回 null。
     */
    public Document findRunByTraceId(String traceId) {
        Query query = Query.query(Criteria.where(FIELD_TRACE_ID).is(traceId)).limit(1);
        return mongoTemplate.findOne(query, Document.class, RUNS_COLLECTION);
    }

    /**
     * 根据 trace ID 查询 span 文档列表。
     *
     * @param traceId Trace 唯一标识。
     * @return span 文档列表，按 sequence 升序排列。
     */
    public List<Document> listSpansByTraceId(String traceId) {
        Query query = Query.query(Criteria.where(FIELD_TRACE_ID).is(traceId))
                .with(Sort.by(Sort.Direction.ASC, FIELD_SEQUENCE));
        return mongoTemplate.find(query, Document.class, SPANS_COLLECTION);
    }

    /**
     * 批量查询指定 trace 的模型 span 文档。
     *
     * @param traceIds Trace 唯一标识列表。
     * @return 模型 span 文档列表，按 trace_id 与 sequence 升序排列。
     */
    public List<Document> listModelSpansByTraceIds(List<String> traceIds) {
        if (traceIds == null || traceIds.isEmpty()) {
            return List.of();
        }
        Query query = Query.query(
                        Criteria.where(FIELD_TRACE_ID).in(traceIds)
                                .and(FIELD_SPAN_TYPE).is(SPAN_TYPE_MODEL)
                )
                .with(Sort.by(
                        Sort.Order.asc(FIELD_TRACE_ID),
                        Sort.Order.asc(FIELD_SEQUENCE)
                ));
        query.fields()
                .include(FIELD_TRACE_ID)
                .include(FIELD_SEQUENCE)
                .include("input_payload.messages")
                .include("output_payload.messages")
                .include("output_payload.finish_reason");
        return mongoTemplate.find(query, Document.class, SPANS_COLLECTION);
    }

    /**
     * 查询模型 Token 用量明细。
     *
     * @param request 监控查询参数。
     * @return 模型 Token 用量明细列表，按 started_at 升序排列。
     */
    public List<Document> listModelTokenUsage(AgentTraceMonitorRequest request) {
        Query query = buildModelTokenUsageQuery(request)
                .with(Sort.by(Sort.Direction.ASC, FIELD_STARTED_AT));
        return mongoTemplate.find(query, Document.class, TOKEN_USAGE_COLLECTION);
    }

    /**
     * 删除指定 trace 的 run 和 span 文档。
     *
     * @param traceId Trace 唯一标识。
     * @return 删除的文档总数。
     */
    public long deleteRunAndSpans(String traceId) {
        Query query = Query.query(Criteria.where(FIELD_TRACE_ID).is(traceId));
        DeleteResult spanDeleteResult = mongoTemplate.remove(query, SPANS_COLLECTION);
        DeleteResult runDeleteResult = mongoTemplate.remove(query, RUNS_COLLECTION);
        return spanDeleteResult.getDeletedCount() + runDeleteResult.getDeletedCount();
    }

    /**
     * 构造 run 查询条件。
     *
     * @param request 查询参数。
     * @return Mongo 查询对象。
     */
    private Query buildRunQuery(AgentTraceRunListRequest request) {
        List<Criteria> criteriaList = new ArrayList<>();
        addTextEquals(criteriaList, FIELD_TRACE_ID, request.getTraceId());
        addTextEquals(criteriaList, "conversation_uuid", request.getConversationUuid());
        addTextEquals(criteriaList, "assistant_message_uuid", request.getAssistantMessageUuid());
        addTextEquals(criteriaList, "conversation_type", request.getConversationType());
        addTextEquals(criteriaList, "status", request.getStatus());
        addTextEquals(criteriaList, "graph_name", request.getGraphName());
        if (request.getUserId() != null) {
            criteriaList.add(Criteria.where("user_id").is(request.getUserId()));
        }
        if (request.getStartTime() != null || request.getEndTime() != null) {
            Criteria startedAtCriteria = Criteria.where(FIELD_STARTED_AT);
            if (request.getStartTime() != null) {
                startedAtCriteria.gte(request.getStartTime());
            }
            if (request.getEndTime() != null) {
                startedAtCriteria.lte(request.getEndTime());
            }
            criteriaList.add(startedAtCriteria);
        }
        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(Criteria[]::new)));
        }
        return query;
    }

    /**
     * 构造模型 Token 用量明细查询条件。
     *
     * @param request 监控查询参数。
     * @return Mongo 查询对象。
     */
    private Query buildModelTokenUsageQuery(AgentTraceMonitorRequest request) {
        List<Criteria> criteriaList = new ArrayList<>();
        addTextEquals(criteriaList, "conversation_type", request.getConversationType());
        addTextEquals(criteriaList, "provider", request.getProvider());
        addTextEquals(criteriaList, "model_name", request.getModelName());
        addTextEquals(criteriaList, "slot", request.getSlot());
        addTextEquals(criteriaList, "status", request.getStatus());
        if (request.getStartTime() != null || request.getEndTime() != null) {
            Criteria startedAtCriteria = Criteria.where(FIELD_STARTED_AT);
            if (request.getStartTime() != null) {
                startedAtCriteria.gte(request.getStartTime());
            }
            if (request.getEndTime() != null) {
                startedAtCriteria.lte(request.getEndTime());
            }
            criteriaList.add(startedAtCriteria);
        }
        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(Criteria[]::new)));
        }
        return query;
    }

    /**
     * 添加文本精确匹配条件。
     *
     * @param criteriaList 查询条件列表。
     * @param fieldName    Mongo 字段名。
     * @param value        查询值。
     * @return 无返回值。
     */
    private void addTextEquals(List<Criteria> criteriaList, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            criteriaList.add(Criteria.where(fieldName).is(value.trim()));
        }
    }
}
