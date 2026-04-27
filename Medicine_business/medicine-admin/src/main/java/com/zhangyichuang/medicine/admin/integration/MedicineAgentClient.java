package com.zhangyichuang.medicine.admin.integration;

import com.google.gson.reflect.TypeToken;
import com.zhangyichuang.medicine.admin.config.KnowledgeBaseAiProperties;
import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import com.zhangyichuang.medicine.common.http.exception.HttpClientException;
import com.zhangyichuang.medicine.common.http.model.BaseResponse;
import com.zhangyichuang.medicine.common.http.model.ClientRequest;
import com.zhangyichuang.medicine.common.http.model.HttpMethod;
import com.zhangyichuang.medicine.common.http.model.HttpResult;
import com.zhangyichuang.medicine.common.systemauth.client.SystemAuthRequestClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 知识库 Agent 服务调用客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineAgentClient {

    /**
     * 创建知识库接口路径。
     */
    private static final String CREATE_PATH = "/knowledge_base";

    /**
     * 启用知识库接口路径。
     */
    private static final String LOAD_PATH = "/knowledge_base/load";

    /**
     * 关闭知识库接口路径。
     */
    private static final String RELEASE_PATH = "/knowledge_base/release";

    /**
     * 批量删除文档接口路径。
     */
    private static final String DOCUMENT_DELETE_PATH = "/knowledge_base/document";

    /**
     * 文档切片列表接口路径。
     */
    private static final String DOCUMENT_CHUNK_LIST_PATH = "/knowledge_base/document/chunks/list";

    /**
     * 按向量主键更新切片状态接口路径。
     */
    private static final String DOCUMENT_CHUNK_STATUS_PATH = "/knowledge_base/document/chunk/status";

    /**
     * 结构化知识检索接口路径。
     */
    private static final String KNOWLEDGE_SEARCH_PATH = "/knowledge_base/search";

    /**
     * 切片启用状态：0。
     */
    private static final int CHUNK_STATUS_ENABLED = 0;

    /**
     * 文档切片分页查询默认每页条数。
     */
    private static final int CHUNK_PAGE_SIZE = 50;

    /**
     * 文档切片分页查询允许的最大页码。
     */
    private static final int MAX_CHUNK_PAGE = 10_000;

    /**
     * 文档切片分页响应类型。
     */
    private static final Type DOCUMENT_CHUNK_PAGE_DATA_TYPE = new TypeToken<DocumentChunkPageData>() {
    }.getType();

    /**
     * 结构化知识检索响应类型。
     */
    private static final Type KNOWLEDGE_SEARCH_RESPONSE_DATA_TYPE = new TypeToken<KnowledgeSearchResponseData>() {
    }.getType();

    private final KnowledgeBaseAiProperties properties;
    private final SystemAuthRequestClient systemAuthRequestClient;

    /**
     * 调用 Agent 服务创建知识库。
     */
    public void createKnowledgeBase(String knowledgeName, Integer embeddingDim, String description) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        validateEmbeddingDim(embeddingDim);

        String url = buildUrl(CREATE_PATH);
        CreateKnowledgeBasePayload payload = new CreateKnowledgeBasePayload(knowledgeName, embeddingDim, description);
        String requestBody = JSONUtils.toJson(payload);
        doPostWithValidation(url, requestBody, "调用Agent服务创建知识库失败: ");
    }

    /**
     * 调用 Agent 服务启用（Load）知识库集合。
     */
    public void loadKnowledgeBase(String knowledgeName) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        String url = buildUrl(LOAD_PATH);
        String requestBody = JSONUtils.toJson(new CollectionPayload(knowledgeName));
        doPostWithValidation(url, requestBody, "调用Agent服务启用知识库失败: ");
    }

    /**
     * 调用 Agent 服务关闭（Release）知识库集合。
     */
    public void releaseKnowledgeBase(String knowledgeName) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        String url = buildUrl(RELEASE_PATH);
        String requestBody = JSONUtils.toJson(new CollectionPayload(knowledgeName));
        doPostWithValidation(url, requestBody, "调用Agent服务关闭知识库失败: ");
    }

    /**
     * 调用 Agent 服务删除知识库集合。
     */
    public void deleteKnowledgeBase(String knowledgeName) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        String url = buildUrl(CREATE_PATH);
        String requestBody = JSONUtils.toJson(new KnowledgeBaseDeletePayload(knowledgeName));
        doRequestWithValidation(HttpMethod.DELETE, url, requestBody, "调用Agent服务删除知识库失败: ");
    }

    /**
     * 调用 Agent 服务批量删除文档。
     */
    public void deleteDocuments(String knowledgeName, List<Long> documentIds) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        List<Long> normalizedDocumentIds = normalizeDocumentIds(documentIds);

        String url = buildUrl(DOCUMENT_DELETE_PATH);
        String requestBody = JSONUtils.toJson(new DocumentDeletePayload(knowledgeName, normalizedDocumentIds));
        doRequestWithValidation(HttpMethod.DELETE, url, requestBody, "调用Agent服务批量删除文档失败: ");
    }

    /**
     * 分页拉取文档切片列表。
     *
     * @param knowledgeName 知识库名称
     * @param documentId    文档ID
     * @return 全量切片行
     */
    public List<DocumentChunkRow> listDocumentChunks(String knowledgeName, Long documentId) {
        Assert.notEmpty(knowledgeName, "知识库名称不能为空");
        Assert.isPositive(documentId, "文档ID必须大于0");

        String url = buildUrl(DOCUMENT_CHUNK_LIST_PATH);
        List<DocumentChunkRow> rows = new ArrayList<>();
        int page = 1;
        log.info("开始拉取 AI 文档切片: knowledge_name={}, document_id={}", knowledgeName, documentId);
        while (true) {
            DocumentChunkPageData pageData = fetchDocumentChunkPage(url, knowledgeName, documentId, page);
            if (pageData != null && pageData.getRows() != null && !pageData.getRows().isEmpty()) {
                rows.addAll(pageData.getRows());
            }
            if (pageData == null || !Boolean.TRUE.equals(pageData.getHas_next())) {
                log.info("完成拉取 AI 文档切片: knowledge_name={}, document_id={}, page_count={}, chunk_count={}",
                        knowledgeName, documentId, page, rows.size());
                return rows;
            }
            page++;
            if (page > MAX_CHUNK_PAGE) {
                throw new ServiceException(ResponseCode.OPERATION_ERROR, "文档切片分页超过上限");
            }
        }
    }

    /**
     * 调用 Agent 服务按向量主键修改切片状态。
     *
     * @param vectorId 向量ID
     * @param status   切片状态，0启用，1禁用
     */
    public void updateDocumentChunkStatus(Long vectorId, Integer status) {
        Assert.isPositive(vectorId, "向量ID必须大于0");
        Assert.notNull(status, "切片状态不能为空");

        String url = buildUrl(DOCUMENT_CHUNK_STATUS_PATH);
        String requestBody = JSONUtils.toJson(new DocumentChunkStatusPayload(vectorId, status));
        doRequestWithValidation(HttpMethod.PUT, url, requestBody, "调用Agent服务修改切片状态失败: ");
    }

    /**
     * 调用 Agent 服务执行知识库结构化检索。
     *
     * @param question       检索问题
     * @param knowledgeNames 参与检索的知识库名称列表
     * @param embeddingModel 当前检索使用的向量模型名称
     * @param embeddingDim   当前检索使用的向量维度
     * @param rankingEnabled 当前检索是否启用重排
     * @param rankingModel   当前检索使用的重排模型名称
     * @param topK           当前检索最终返回条数
     * @return 结构化检索结果
     */
    public KnowledgeSearchResponseData searchKnowledge(String question,
                                                       List<String> knowledgeNames,
                                                       String embeddingModel,
                                                       Integer embeddingDim,
                                                       Boolean rankingEnabled,
                                                       String rankingModel,
                                                       Integer topK) {
        Assert.notEmpty(question, "检索问题不能为空");
        Assert.notEmpty(knowledgeNames, "知识库名称列表不能为空");
        Assert.notEmpty(embeddingModel, "向量模型不能为空");
        Assert.notNull(embeddingDim, "向量维度不能为空");
        Assert.notNull(rankingEnabled, "是否开启重排不能为空");
        Assert.notNull(topK, "检索返回条数不能为空");

        String url = buildUrl(KNOWLEDGE_SEARCH_PATH);
        KnowledgeSearchPayload payload = new KnowledgeSearchPayload();
        payload.setQuestion(question);
        payload.setKnowledge_names(knowledgeNames);
        payload.setEmbedding_model(embeddingModel);
        payload.setEmbedding_dim(embeddingDim);
        payload.setRanking_enabled(rankingEnabled);
        payload.setRanking_model(rankingModel);
        payload.setTop_k(topK);
        String requestBody = JSONUtils.toJson(payload);

        HttpResult<String> result = null;
        try {
            result = executePostRequest(url, requestBody);
            return BaseResponse.extractData(result, KNOWLEDGE_SEARCH_RESPONSE_DATA_TYPE);
        } catch (HttpClientException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    "调用Agent服务执行知识库检索失败: " + resolveDetailedErrorMessage(result, ex.getMessage()));
        }
    }

    /**
     * 发送 POST 请求，并验证响应结果。
     *
     * @param url         Agent 服务完整请求地址
     * @param body        JSON 请求体
     * @param errorPrefix 错误信息前缀
     */
    private void doPostWithValidation(String url, String body, String errorPrefix) {
        doRequestWithValidation(HttpMethod.POST, url, body, errorPrefix);
    }

    /**
     * 发送 JSON 请求，并验证响应结果。
     *
     * @param method      HTTP 方法
     * @param url         Agent 服务完整请求地址
     * @param body        JSON 请求体
     * @param errorPrefix 错误信息前缀
     */
    private void doRequestWithValidation(HttpMethod method, String url, String body, String errorPrefix) {
        HttpResult<String> result = null;
        try {
            result = method == HttpMethod.POST
                    ? executePostRequest(url, body)
                    : executeJsonRequest(method, url, body);
            BaseResponse.extractData(result);
        } catch (HttpClientException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    errorPrefix + resolveDetailedErrorMessage(result, ex.getMessage()));
        }
    }

    /**
     * 拉取指定页码的文档切片数据。
     *
     * @param url           Agent 服务完整请求地址
     * @param knowledgeName 知识库名称
     * @param documentId    文档ID
     * @param page          当前页码
     * @return 单页文档切片数据
     */
    private DocumentChunkPageData fetchDocumentChunkPage(String url, String knowledgeName,
                                                         Long documentId, int page) {
        HttpResult<String> result = null;
        try {
            ClientRequest request = buildChunkListRequest(url, knowledgeName, documentId, page, MedicineAgentClient.CHUNK_PAGE_SIZE);
            result = executeGetRequest(request);
            return BaseResponse.extractData(result, DOCUMENT_CHUNK_PAGE_DATA_TYPE);
        } catch (HttpClientException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    "调用Agent服务拉取文档切片失败: " + resolveDetailedErrorMessage(result, ex.getMessage()));
        }
    }

    /**
     * 优先提取 AI 响应体中的业务 message，便于上层直接写入 lastError。
     * 若响应体中没有有效 message，再退回到业务 code、HTTP 状态码或底层异常信息。
     */
    private String resolveDetailedErrorMessage(HttpResult<String> result, String fallbackMessage) {
        ErrorResponseSummary errorSummary = parseErrorResponse(result == null ? null : result.getBody());
        if (StringUtils.hasText(errorSummary.message())) {
            return errorSummary.message().trim();
        }
        if (result != null && !result.isSuccessful()) {
            return "请求失败，HTTP 状态码: " + result.getStatusCode();
        }
        if (errorSummary.code() != null && errorSummary.code() != 200) {
            return "业务失败，code=" + errorSummary.code();
        }
        return fallbackMessage;
    }

    /**
     * 尝试从标准 BaseResponse 结构中提取 code/message。
     */
    private ErrorResponseSummary parseErrorResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return ErrorResponseSummary.empty();
        }
        try {
            BaseResponse<Object> response = BaseResponse.fromJson(responseBody, Object.class);
            if (response == null) {
                return ErrorResponseSummary.empty();
            }
            return new ErrorResponseSummary(response.getCode(), response.getMessage());
        } catch (RuntimeException ex) {
            return ErrorResponseSummary.empty();
        }
    }

    /**
     * 使用系统签名客户端发送 Agent POST 请求。
     *
     * @param url  Agent 服务完整请求地址
     * @param body JSON 请求体
     * @return HTTP 响应结果
     */
    HttpResult<String> executePostRequest(String url, String body) {
        ClientRequest request = ClientRequest.builder()
                .method(HttpMethod.POST)
                .url(url)
                .body(body)
                .build();
        return systemAuthRequestClient.post(request);
    }

    /**
     * 使用系统签名客户端发送 Agent JSON 请求。
     *
     * @param method HTTP 方法
     * @param url    Agent 服务完整请求地址
     * @param body   JSON 请求体
     * @return HTTP 响应结果
     */
    HttpResult<String> executeJsonRequest(HttpMethod method, String url, String body) {
        ClientRequest request = ClientRequest.builder()
                .method(method)
                .url(url)
                .body(body)
                .build();
        return systemAuthRequestClient.execute(request, String.class);
    }

    /**
     * 构造文档切片分页查询请求。
     *
     * @param url           Agent 服务完整请求地址
     * @param knowledgeName 知识库名称
     * @param documentId    文档ID
     * @param page          当前页码
     * @param pageSize      每页条数
     * @return GET 请求对象
     */
    ClientRequest buildChunkListRequest(String url, String knowledgeName, Long documentId, int page, int pageSize) {
        return ClientRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .addQueryParameter("knowledge_name", knowledgeName)
                .addQueryParameter("document_id", String.valueOf(documentId))
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("page_size", String.valueOf(pageSize))
                .build();
    }

    /**
     * 使用系统签名客户端发送 Agent GET 请求。
     *
     * @param request GET 请求对象
     * @return HTTP 响应结果
     */
    HttpResult<String> executeGetRequest(ClientRequest request) {
        return systemAuthRequestClient.get(request);
    }

    /**
     * 构建 Agent 接口完整 URL。
     *
     * @param path 接口路径
     * @return 形如 <a href="http://host:port/knowledge_base">...</a> 的完整地址
     */
    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        Assert.notEmpty(baseUrl, "knowledge-base.ai.base-url 未配置");
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    /**
     * 校验知识库向量维度是否属于支持集合。
     *
     * @param embeddingDim 向量维度
     */
    private void validateEmbeddingDim(Integer embeddingDim) {
        Assert.notNull(embeddingDim, "向量维度不能为空");
        Assert.isParamTrue(KnowledgeBaseEmbeddingDimSupport.isSupported(embeddingDim),
                KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE);
    }

    /**
     * 归一化文档 ID 列表并去重。
     *
     * @param documentIds 原始文档ID列表
     * @return 去重后的文档ID列表
     */
    private List<Long> normalizeDocumentIds(List<Long> documentIds) {
        Assert.notEmpty(documentIds, "文档ID不能为空");
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>();
        for (Long documentId : documentIds) {
            Assert.isPositive(documentId, "文档ID必须大于0");
            distinctIds.add(documentId);
        }
        return new ArrayList<>(distinctIds);
    }

    private record CreateKnowledgeBasePayload(String knowledge_name, Integer embedding_dim, String description) {
    }

    private record CollectionPayload(String collection_name) {
    }

    private record KnowledgeBaseDeletePayload(String knowledge_name) {
    }

    private record DocumentDeletePayload(String knowledge_name, List<Long> document_ids) {
    }

    private record DocumentChunkStatusPayload(Long vector_id, Integer status) {
    }

    /**
     * 结构化知识检索请求载荷。
     */
    @Data
    private static class KnowledgeSearchPayload {

        /**
         * 检索问题。
         */
        private String question;

        /**
         * 参与检索的知识库名称列表。
         */
        private List<String> knowledge_names;

        /**
         * 当前检索使用的向量模型名称。
         */
        private String embedding_model;

        /**
         * 当前检索使用的向量维度。
         */
        private Integer embedding_dim;

        /**
         * 当前检索是否启用重排。
         */
        private Boolean ranking_enabled;

        /**
         * 当前检索使用的重排模型名称。
         */
        private String ranking_model;

        /**
         * 当前检索最终返回条数。
         */
        private Integer top_k;
    }

    /**
     * 错误响应摘要。
     */
    private record ErrorResponseSummary(Integer code, String message) {
        /**
         * 构造空错误摘要。
         *
         * @return 空错误摘要实例
         */
        private static ErrorResponseSummary empty() {
            return new ErrorResponseSummary(null, null);
        }
    }

    @Data
    public static class DocumentChunkPageData {

        /**
         * 当前页切片列表。
         */
        private List<DocumentChunkRow> rows;

        /**
         * 切片总数。
         */
        private Integer total;

        /**
         * 当前页码，从 1 开始。
         */
        private Integer page_num;

        /**
         * 当前页大小。
         */
        private Integer page_size;

        /**
         * 是否存在下一页。
         */
        private Boolean has_next;
    }

    @Data
    public static class DocumentChunkRow {
        /**
         * 上游向量记录主键。
         */
        private Long id;

        /**
         * 所属文档 ID。
         */
        private Long document_id;

        /**
         * 切片序号。
         */
        private Integer chunk_index;

        /**
         * 切片文本内容。
         */
        private String content;

        /**
         * 切片字符数。
         */
        private Integer char_count;

        /**
         * 切片大小。
         */
        private Integer chunk_size;

        /**
         * 切片重叠大小。
         */
        private Integer chunk_overlap;

        /**
         * 内容来源哈希。
         */
        private String source_hash;

        /**
         * 切片状态：0启用，1禁用。
         */
        private Integer status;

        /**
         * 创建时间戳，毫秒。
         */
        private Long created_at_ts;
    }

    @Data
    public static class KnowledgeSearchResponseData {

        /**
         * 结构化命中结果列表。
         */
        private List<KnowledgeSearchHitRow> hits;
    }

    @Data
    public static class KnowledgeSearchHitRow {

        /**
         * 命中结果所在的知识库名称。
         */
        private String knowledge_name;

        /**
         * 命中的知识文本内容。
         */
        private String content;

        /**
         * 命中的相似度分数。
         */
        private Double score;

        /**
         * 命中的业务文档ID。
         */
        private Long document_id;

        /**
         * 命中的切片序号。
         */
        private Integer chunk_index;

        /**
         * 命中的切片字符数。
         */
        private Integer char_count;
    }
}
