package com.zhangyichuang.medicine.agent.advice;

import com.zhangyichuang.medicine.agent.support.AgentVoDescriptionResolver;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 为标注 {@code @FieldDescription}（类级）的响应数据自动追加字段语义说明。
 */
@RestControllerAdvice(basePackages = "com.zhangyichuang.medicine.agent.controller")
@RequiredArgsConstructor
public class AgentResponseDescriptionAdvice implements ResponseBodyAdvice<Object> {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    /**
     * 列表响应转换为结构化行数据时使用的类型定义。
     */
    private static final TypeReference<List<LinkedHashMap<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
    };

    private final AgentVoDescriptionResolver descriptionResolver;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(@NonNull MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        if (!(body instanceof AjaxResult<?> ajaxResult)) {
            return body;
        }

        Object data = ajaxResult.getData();
        if (data == null) {
            return body;
        }

        if (data instanceof TableDataResult tableDataResult) {
            enrichTableData(ajaxResult, tableDataResult);
            return ajaxResult;
        }

        if (data instanceof List<?> listData) {
            enrichListData(ajaxResult, listData);
            return ajaxResult;
        }

        if (data instanceof Map<?, ?> mapData && mapData.containsKey("meta")) {
            return ajaxResult;
        }

        enrichObjectData(ajaxResult, data);
        return ajaxResult;
    }

    private void enrichObjectData(AjaxResult<?> ajaxResult, Object data) {
        Optional<AgentVoDescriptionResolver.Meta> metaOptional = descriptionResolver.resolve(data.getClass());
        if (metaOptional.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Object> objectData = objectMapper.convertValue(data, MAP_TYPE);
        objectData.put("meta", buildMeta(metaOptional.get()));
        setData(ajaxResult, objectData);
    }

    private void enrichListData(AjaxResult<?> ajaxResult, List<?> listData) {
        if (listData == null || listData.isEmpty()) {
            return;
        }

        Object firstElement = listData.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (firstElement == null) {
            return;
        }

        Optional<AgentVoDescriptionResolver.Meta> metaOptional = descriptionResolver.resolve(firstElement.getClass());
        if (metaOptional.isEmpty()) {
            return;
        }

        List<LinkedHashMap<String, Object>> rowData = objectMapper.convertValue(listData, LIST_MAP_TYPE);
        LinkedHashMap<String, Object> wrappedListData = new LinkedHashMap<>();
        wrappedListData.put("rows", rowData);
        wrappedListData.put("meta", buildMeta(metaOptional.get()));
        setData(ajaxResult, wrappedListData);
    }

    private void enrichTableData(AjaxResult<?> ajaxResult, TableDataResult tableDataResult) {
        List<?> rows = tableDataResult.getRows();
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Object firstElement = rows.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (firstElement == null) {
            return;
        }

        Optional<AgentVoDescriptionResolver.Meta> metaOptional = descriptionResolver.resolve(firstElement.getClass());
        if (metaOptional.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Object> tableData = objectMapper.convertValue(tableDataResult, MAP_TYPE);
        tableData.put("meta", buildMeta(metaOptional.get()));
        setData(ajaxResult, tableData);
    }

    private Map<String, Object> buildMeta(AgentVoDescriptionResolver.Meta meta) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("entityDescription", meta.entityDescription());
        value.put("fieldDescriptions", meta.fieldDescriptions());
        return value;
    }

    @SuppressWarnings("unchecked")
    private void setData(AjaxResult<?> ajaxResult, Object data) {
        ((AjaxResult<Object>) ajaxResult).setData(data);
    }
}
