package com.zhangyichuang.medicine.common.log.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志工具类。
 */
public final class OperationLogUtils {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd", "token", "authorization", "secret",
            "accesstoken", "refreshtoken",
            "phone", "mobile", "phonenumber", "receiverphone",
            "email", "mail",
            "idcard", "id_card", "idno", "identityno"
    );

    private OperationLogUtils() {
    }

    /**
     * 对对象执行敏感字段脱敏后转 JSON。
     */
    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            JsonElement element = JSONUtils.toJsonTree(value);
            maskSensitive(element);
            return JSONUtils.toJson(element);
        } catch (Exception ex) {
            // 序列化异常时避免回退到对象 toString() 导致明文敏感数据泄露。
            return "\"[MASKED_SERIALIZATION_ERROR]\"";
        }
    }

    /**
     * 是否应从日志中过滤该对象。
     */
    public static boolean isFilterObject(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof ServletRequest) {
            return true;
        }
        if (value instanceof ServletResponse) {
            return true;
        }
        if (value instanceof MultipartFile) {
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

    private static void maskSensitive(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                maskSensitive(item);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (isSensitiveKey(key)) {
                object.addProperty(key, "***");
                continue;
            }
            maskSensitive(entry.getValue());
        }
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }
}
