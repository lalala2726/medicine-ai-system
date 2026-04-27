package com.zhangyichuang.medicine.common.redis.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 防重复提交参数指纹构建器。
 * <p>
 * 负责将控制器方法参数转换为稳定 JSON，再计算 SHA-256 指纹，
 * 避免同一业务参数因字段顺序不同导致重复判定失效。
 * </p>
 */
@Component
public class PreventDuplicateSubmitFingerprintBuilder {

    /**
     * 十六进制编码器，统一输出小写十六进制。
     */
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * 基于方法参数构建参数指纹。
     *
     * @param parameterNames 方法参数名数组
     * @param args           方法参数值数组
     * @return 参数指纹
     */
    public String buildFingerprint(String[] parameterNames, Object[] args) {
        JsonObject parameterObject = new JsonObject();
        if (args != null) {
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                if (isFilteredObject(arg)) {
                    continue;
                }
                String parameterName = resolveParameterName(parameterNames, index);
                parameterObject.add(parameterName, toCanonicalJsonElement(arg));
            }
        }
        return sha256Hex(JSONUtils.toJson(parameterObject));
    }

    /**
     * 解析参数名。
     *
     * @param parameterNames 方法参数名数组
     * @param index          当前参数下标
     * @return 归一化后的参数名
     */
    private String resolveParameterName(String[] parameterNames, int index) {
        if (parameterNames != null && parameterNames.length > index && parameterNames[index] != null) {
            String parameterName = parameterNames[index].trim();
            if (!parameterName.isEmpty()) {
                return parameterName;
            }
        }
        return "arg" + index;
    }

    /**
     * 将任意参数对象转换为稳定 JSON 元素。
     *
     * @param value 原始参数值
     * @return 稳定 JSON 元素
     */
    private JsonElement toCanonicalJsonElement(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof MultipartFile multipartFile) {
            return buildMultipartFileElement(multipartFile);
        }
        if (value instanceof Collection<?> collection) {
            JsonArray array = new JsonArray();
            for (Object item : collection) {
                array.add(toCanonicalJsonElement(item));
            }
            return array;
        }
        if (value instanceof Map<?, ?> map) {
            return buildSortedMapElement(map);
        }
        if (value.getClass().isArray()) {
            JsonArray array = new JsonArray();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                array.add(toCanonicalJsonElement(Array.get(value, index)));
            }
            return array;
        }
        return sortJsonElement(JSONUtils.toJsonTree(value));
    }

    /**
     * 构建文件参数的稳定 JSON 表达。
     *
     * @param multipartFile 上传文件
     * @return 文件 JSON 表达
     */
    private JsonElement buildMultipartFileElement(MultipartFile multipartFile) {
        JsonObject fileObject = new JsonObject();
        fileObject.addProperty("name", multipartFile.getName());
        fileObject.addProperty("originalFilename", multipartFile.getOriginalFilename());
        fileObject.addProperty("size", multipartFile.getSize());
        fileObject.addProperty("contentType", multipartFile.getContentType());
        return fileObject;
    }

    /**
     * 将 Map 转换为按 key 排序后的 JSON 对象。
     *
     * @param map 原始 Map
     * @return 排序后的 JSON 对象
     */
    private JsonElement buildSortedMapElement(Map<?, ?> map) {
        JsonObject mapObject = new JsonObject();
        List<String> sortedKeys = new ArrayList<>();
        for (Object key : map.keySet()) {
            sortedKeys.add(String.valueOf(key));
        }
        Collections.sort(sortedKeys);
        for (String sortedKey : sortedKeys) {
            Object mapValue = map.get(sortedKey);
            if (mapValue == null && !map.containsKey(sortedKey)) {
                mapValue = findOriginalMapValue(map, sortedKey);
            }
            mapObject.add(sortedKey, toCanonicalJsonElement(mapValue));
        }
        return mapObject;
    }

    /**
     * 根据排序后的 key 查找原始 Map 中的值。
     *
     * @param map       原始 Map
     * @param sortedKey 排序后的 key 字符串
     * @return 原始 Map 中的值
     */
    private Object findOriginalMapValue(Map<?, ?> map, String sortedKey) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (Objects.equals(String.valueOf(entry.getKey()), sortedKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 对 JSON 元素中的对象字段按字典序排序。
     *
     * @param element 原始 JSON 元素
     * @return 排序后的 JSON 元素
     */
    private JsonElement sortJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonArray()) {
            JsonArray sourceArray = element.getAsJsonArray();
            JsonArray targetArray = new JsonArray();
            for (JsonElement item : sourceArray) {
                targetArray.add(sortJsonElement(item));
            }
            return targetArray;
        }
        if (!element.isJsonObject()) {
            return element;
        }
        JsonObject sourceObject = element.getAsJsonObject();
        JsonObject targetObject = new JsonObject();
        List<String> sortedKeys = new ArrayList<>(sourceObject.keySet());
        Collections.sort(sortedKeys);
        for (String sortedKey : sortedKeys) {
            targetObject.add(sortedKey, sortJsonElement(sourceObject.get(sortedKey)));
        }
        return targetObject;
    }

    /**
     * 判断对象是否应从指纹构建中排除。
     *
     * @param value 待判断对象
     * @return true 表示应排除
     */
    private boolean isFilteredObject(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof ServletRequest) {
            return true;
        }
        if (value instanceof ServletResponse) {
            return true;
        }
        if (value instanceof BindingResult) {
            return true;
        }
        if (value instanceof InputStream || value instanceof OutputStream) {
            return true;
        }
        return value instanceof File;
    }

    /**
     * 计算字符串内容的 SHA-256 指纹。
     *
     * @param source 原始字符串
     * @return SHA-256 指纹
     */
    private String sha256Hex(String source) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
