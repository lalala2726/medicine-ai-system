package com.zhangyichuang.medicine.common.core.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson 工具类，统一 JSON 读写配置。
 */
public final class JSONUtils {

    /**
     * 默认日期格式，保证序列化输出一致。
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE_ONLY_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_TIME_ONLY_FORMAT = "HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_ONLY_FORMAT);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_ONLY_FORMAT);

    /**
     * 全局 Gson 实例（线程安全）。
     */
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .setDateFormat(DEFAULT_DATE_FORMAT)
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.format(DATE_TIME_FORMATTER)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.format(DATE_FORMATTER)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : LocalDate.parse(json.getAsString(), DATE_FORMATTER))
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.format(TIME_FORMATTER)))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : LocalTime.parse(json.getAsString(), TIME_FORMATTER))
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : Instant.parse(json.getAsString()))
            .registerTypeAdapter(OffsetDateTime.class, (JsonSerializer<OffsetDateTime>) (src, typeOfSrc, context) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>) (json, typeOfT, context) ->
                    json == null || json.isJsonNull() ? null : OffsetDateTime.parse(json.getAsString()))
            .create();
    /**
     * Map<String, Object> 反序列化类型。
     */
    private static final Type MAP_STRING_OBJECT_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    /**
     * List<String> 反序列化类型。
     */
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private JSONUtils() {
    }

    public static Gson getGson() {
        return GSON;
    }

    /**
     * 对象转 JSON 字符串。
     */
    public static String toJson(Object source) {
        return GSON.toJson(source);
    }

    /**
     * JsonElement 转 JSON 字符串。
     */
    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }

    /**
     * 对象转 JsonElement。
     */
    public static JsonElement toJsonTree(Object source) {
        return GSON.toJsonTree(source);
    }

    /**
     * JSON 字符串反序列化。
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * JSON 字符串按指定泛型反序列化。
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * 以宽松模式反序列化 JSON 字符串。
     */
    public static <T> T fromJsonLenient(String json, Class<T> clazz) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return GSON.fromJson(reader, clazz);
    }

    /**
     * JsonElement 反序列化。
     */
    public static <T> T fromJson(JsonElement element, Class<T> clazz) {
        return GSON.fromJson(element, clazz);
    }

    /**
     * JsonElement 按指定泛型反序列化。
     */
    public static <T> T fromJson(JsonElement element, Type type) {
        return GSON.fromJson(element, type);
    }

    /**
     * 解析 JSON 字符串为 JsonElement。
     */
    public static JsonElement parse(String json) {
        return JsonParser.parseString(json);
    }

    /**
     * 以宽松模式解析 JSON 字符串，兼容非标准格式。
     */
    public static JsonElement parseLenient(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return JsonParser.parseReader(reader);
    }

    /**
     * 解析 JSON 字符串为 JsonObject。
     */
    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * 解析 JSON 数组为字符串列表。
     */
    public static List<String> parseStringList(String json) {
        return fromJson(json, STRING_LIST_TYPE);
    }

    /**
     * 将对象转换为 Map 结构，便于 query 参数拼装。
     */
    public static Map<String, Object> toMap(Object source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        if (source instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        JsonElement element = toJsonTree(source);
        if (!element.isJsonObject()) {
            return new LinkedHashMap<>();
        }
        return fromJson(element, MAP_STRING_OBJECT_TYPE);
    }
}
