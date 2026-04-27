package com.zhangyichuang.medicine.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.util.StdDateFormat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Chuang
 * <p>
 * created on 2025/12/21
 */
@Configuration
public class JacksonConfig {

    /**
     * 默认日期时间格式。
     */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式。
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 应用默认时区。
     */
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 默认日期时间格式化器。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 默认日期格式化器。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * Jackson Mapper 配置。
     *
     * @return JsonMapper Jackson Mapper
     */
    @Bean
    public JsonMapper jsonMapper() {
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
        );
        timeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DATE_TIME_FORMATTER)
        );
        timeModule.addSerializer(
                LocalDate.class,
                new LocalDateSerializer(DATE_FORMATTER)
        );
        timeModule.addDeserializer(
                LocalDate.class,
                new LocalDateDeserializer(DATE_FORMATTER)
        );
        timeModule.addSerializer(Date.class, new DateJsonSerializer());
        timeModule.addDeserializer(Date.class, new DateJsonDeserializer());

        /**
         * 数值转字符串模块。
         */
        SimpleModule numberToStringModule = new SimpleModule();
        numberToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
        numberToStringModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        numberToStringModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);

        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(timeModule)
                .addModule(numberToStringModule)
                // 忽略未知字段
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // 时区
                .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
                .defaultDateFormat(new StdDateFormat().withLocale(Locale.CHINA).withTimeZone(TimeZone.getTimeZone("GMT+8")))
                .build();
    }

    /**
     * Date 序列化器。
     */
    private static class DateJsonSerializer extends ValueSerializer<Date> {

        /**
         * 序列化 Date。
         *
         * @param value         Date 值
         * @param jsonGenerator JSON 生成器
         */
        @Override
        public void serialize(Date value, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
            if (value == null) {
                jsonGenerator.writeNull();
                return;
            }
            String formattedText = LocalDateTime.ofInstant(value.toInstant(), DEFAULT_ZONE_ID).format(DATE_TIME_FORMATTER);
            jsonGenerator.writeString(formattedText);
        }
    }

    /**
     * Date 反序列化器。
     */
    private static class DateJsonDeserializer extends ValueDeserializer<Date> {

        /**
         * 反序列化 Date。
         *
         * @param jsonParser             JSON 解析器
         * @param deserializationContext 反序列化上下文
         * @return Date 日期对象
         */
        @Override
        public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            String rawText = jsonParser.getValueAsString();
            if (rawText == null || rawText.isBlank()) {
                return null;
            }
            String normalizedText = rawText.trim();
            Date localDateTimeDate = tryParseLocalDateTime(normalizedText);
            if (localDateTimeDate != null) {
                return localDateTimeDate;
            }
            Date isoDate = tryParseIsoDateTime(normalizedText);
            if (isoDate != null) {
                return isoDate;
            }
            return (Date) deserializationContext.handleWeirdStringValue(
                    Date.class,
                    normalizedText,
                    "不支持的日期格式，支持 yyyy-MM-dd HH:mm:ss 或 ISO-8601"
            );
        }

        /**
         * 尝试解析普通日期时间格式。
         *
         * @param text 原始文本
         * @return 解析后的 Date
         */
        private Date tryParseLocalDateTime(String text) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(text, DATE_TIME_FORMATTER);
                return Date.from(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant());
            } catch (DateTimeParseException exception) {
                return null;
            }
        }

        /**
         * 尝试解析 ISO-8601 日期时间格式。
         *
         * @param text 原始文本
         * @return 解析后的 Date
         */
        private Date tryParseIsoDateTime(String text) {
            try {
                return Date.from(OffsetDateTime.parse(text).toInstant());
            } catch (DateTimeParseException ignoredOffsetException) {
                try {
                    return Date.from(Instant.parse(text));
                } catch (DateTimeParseException ignoredInstantException) {
                    return null;
                }
            }
        }
    }
}
