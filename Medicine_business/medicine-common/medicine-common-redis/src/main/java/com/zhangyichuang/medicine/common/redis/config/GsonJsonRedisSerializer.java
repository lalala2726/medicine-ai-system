package com.zhangyichuang.medicine.common.redis.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.zhangyichuang.medicine.common.core.constants.Constants;
import com.zhangyichuang.medicine.common.core.utils.JSONUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis 使用 Gson 序列化，包含类型信息以便反序列化。
 *
 * @author Chuang
 */
public class GsonJsonRedisSerializer<T> implements RedisSerializer<T> {
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String CLASS_FIELD = "@class";
    private static final String LEGACY_CLASS_FIELD = "@type";
    private static final String DATA_FIELD = "data";

    private final Class<T> clazz;

    public GsonJsonRedisSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        String className = value.getClass().getName();
        String json;
        if (isWhitelisted(className)) {
            Wrapper wrapper = new Wrapper(className, value);
            json = JSONUtils.toJson(wrapper);
        } else {
            json = JSONUtils.toJson(value);
        }
        return json.getBytes(DEFAULT_CHARSET);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String json = new String(bytes, DEFAULT_CHARSET);
        try {
            JsonElement element = JSONUtils.parseLenient(json);
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                String className = readClassName(jsonObject);
                if (className != null) {
                    if (isWhitelisted(className)) {
                        Class<?> targetClass = Class.forName(className);
                        JsonElement dataElement = jsonObject.get(DATA_FIELD);
                        if (dataElement != null && !dataElement.isJsonNull()) {
                            return (T) JSONUtils.fromJson(dataElement, targetClass);
                        }
                        return (T) JSONUtils.fromJson(jsonObject, targetClass);
                    }
                }
            }
            return JSONUtils.fromJsonLenient(json, clazz);
        } catch (ClassNotFoundException ex) {
            throw new SerializationException("Redis 反序列化失败，类不存在: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readClassName(JsonObject jsonObject) {
        JsonElement classElement = jsonObject.get(CLASS_FIELD);
        if (classElement == null || classElement.isJsonNull()) {
            classElement = jsonObject.get(LEGACY_CLASS_FIELD);
        }
        if (classElement == null || classElement.isJsonNull()) {
            return null;
        }
        return classElement.getAsString();
    }

    private boolean isWhitelisted(String className) {
        for (String prefix : Constants.JSON_WHITELIST_STR) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private record Wrapper(@SerializedName(CLASS_FIELD) String className, Object data) {

    }
}
