package com.zhangyichuang.medicine.agent.json;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.lang.reflect.Field;

/**
 * Agent 编码中文标签序列化器。
 */
public class AgentCodeLabelSerializer extends ValueSerializer<Object> {

    private final AgentCodeLabel annotation;

    public AgentCodeLabelSerializer() {
        this.annotation = null;
    }

    public AgentCodeLabelSerializer(AgentCodeLabel annotation) {
        this.annotation = annotation;
    }

    /**
     * 将指定值序列化为带有编码值与中文说明的结构。
     * <p>
     * 该方法会先解析当前字段上的 AgentCodeLabel 注解配置；如果未找到注解，则直接按原值输出。
     * 如果存在注解，则根据 source 指定的来源字段或当前字段值解析原始编码，再结合字典配置查询对应说明。
     * 当原始编码为空时输出 null；当未命中字典且允许回退时，说明字段使用原编码值。
     * 最终输出内容包含 value 与 description 两部分。
     *
     * @param value       当前待序列化的字段值
     * @param gen         JSON 生成器，用于写出序列化结果
     * @param serializers 序列化提供器，用于处理默认值序列化
     * @throws IOException 在写出 JSON 过程中发生输入输出异常时抛出
     */
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext serializers) {
        AgentCodeLabel codeLabel = resolveAnnotation(gen);
        if (codeLabel == null) {
            gen.writePOJO(value);
            return;
        }

        Object rawValue = resolveRawValue(codeLabel, value, gen.currentValue());
        if (rawValue == null) {
            gen.writeNull();
            return;
        }

        String code = String.valueOf(rawValue);
        String description = findLabel(code, codeLabel);
        if (description == null && codeLabel.fallbackToSource()) {
            description = code;
        }
        writeCodeObject(gen, serializers, rawValue, description);
    }

    /**
     * 根据当前属性上的 AgentCodeLabel 注解决定返回的序列化器实例。
     * 如果属性或上下文中存在该注解，则使用对应注解创建新的序列化器；
     * 否则返回当前序列化器实例。
     *
     * @param prov     序列化提供者，用于提供当前序列化上下文信息
     * @param property 当前正在处理的属性，可能为 null
     * @return 绑定了属性注解信息的新序列化器；如果未找到相关注解或属性为空，则返回当前实例
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        if (property != null) {
            AgentCodeLabel current = property.getAnnotation(AgentCodeLabel.class);
            if (current == null) {
                current = property.getContextAnnotation(AgentCodeLabel.class);
            }
            if (current != null) {
                return new AgentCodeLabelSerializer(current);
            }
        }
        return this;
    }

    /**
     * 根据编码值和字典配置信息查询对应的标签文本。
     *
     * @param code      编码值
     * @param codeLabel 编码标签注解配置，提供字典注册表键等查询所需信息
     * @return 查询到的标签文本；如果未命中映射，则返回结果取决于底层注册表处理逻辑
     */
    private String findLabel(String code, AgentCodeLabel codeLabel) {
        return AgentCodeLabelRegistry.getLabel(codeLabel.dictKey(), code);
    }

    /**
     * 解析用于字典标签匹配的原始编码值。
     * <p>
     * 当注解未指定来源字段名时，直接返回当前字段值；当指定了来源字段名时，优先从所属对象中读取对应字段值。
     * 如果所属对象为空，则无法继续解析，返回 null。
     *
     * @param codeLabel         编码标签注解，用于提供来源字段配置
     * @param currentFieldValue 当前正在序列化的字段值，在未配置来源字段时作为默认返回值
     * @param bean              当前字段所属的对象实例，用于按来源字段名读取实际编码值
     * @return 用于后续字典查询的原始值；未配置来源字段时返回当前字段值，所属对象为空或读取失败时返回 null
     */
    private Object resolveRawValue(AgentCodeLabel codeLabel, Object currentFieldValue, Object bean) {
        String sourceField = codeLabel.source();
        if (sourceField == null || sourceField.isBlank()) {
            return currentFieldValue;
        }
        if (bean == null) {
            return null;
        }
        return readSourceValue(bean, sourceField);
    }

    /**
     * 将编码原值及其对应说明写出为一个 JSON 对象。
     * 生成的对象固定包含 value 和 description 两个字段，其中 value 使用默认序列化方式输出，
     * description 在传入值为 null 时写出为 JSON null，否则写出对应的字符串说明。
     *
     * @param gen         用于写出 JSON 内容的生成器
     * @param serializers 提供默认值序列化能力的序列化器提供者
     * @param rawValue    需要写出的原始编码值
     * @param description 原始编码值对应的说明文本，可以为 null
     * @throws IOException 在写出 JSON 过程中发生输入输出异常时抛出
     */
    private void writeCodeObject(JsonGenerator gen,
                                 SerializationContext serializers,
                                 Object rawValue,
                                 String description) {
        gen.writeStartObject();
        gen.writePOJOProperty("value", rawValue);
        gen.writeName("description");
        if (description == null) {
            gen.writeNull();
        } else {
            gen.writeString(description);
        }
        gen.writeEndObject();
    }

    /**
     * 解析当前序列化字段对应的 {@link AgentCodeLabel} 注解配置。
     * <p>
     * 优先返回构造时注入的注解；若未注入，则基于当前输出上下文定位字段名称，
     * 再从当前序列化对象及其父类中查找字段上的注解。
     *
     * @param gen JSON 生成器，用于获取当前序列化上下文
     * @return 字段对应的注解配置；若无法解析到目标字段或未标注注解则返回 {@code null}
     */
    private AgentCodeLabel resolveAnnotation(JsonGenerator gen) {
        if (annotation != null) {
            return annotation;
        }
        Object bean = gen.currentValue();
        if (bean == null) {
            return null;
        }
        String propertyName = gen.streamWriteContext() == null ? null : gen.streamWriteContext().currentName();
        if (propertyName == null || propertyName.isBlank()) {
            return null;
        }
        return findAnnotation(bean.getClass(), propertyName);
    }

    /**
     * 在指定类及其父类中按字段名查找 AgentCodeLabel 注解。
     * 该方法会沿继承层级向上遍历，直到找到目标字段并返回其上的注解；
     * 如果当前层级不存在同名字段，则继续在父类中查找；
     * 若始终未找到字段或字段上未声明该注解，则返回 null。
     *
     * @param beanClass    要查找的起始类
     * @param propertyName 目标字段名称
     * @return 找到的 AgentCodeLabel 注解；如果未找到对应字段或字段上未标注该注解，则返回 null
     */
    private AgentCodeLabel findAnnotation(Class<?> beanClass, String propertyName) {
        Class<?> current = beanClass;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(propertyName);
                return field.getAnnotation(AgentCodeLabel.class);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 按字段名从指定对象中读取字段值。
     * 该方法会从当前对象的运行时类型开始，沿继承层级逐级向父类查找同名字段，
     * 找到后尝试开启访问权限并返回对应的字段值。
     * 如果字段不存在、无法访问，或遍历完整个继承体系仍未找到目标字段，则返回 null。
     *
     * @param bean        要读取字段值的对象实例
     * @param sourceField 要查找并读取的字段名称
     * @return 目标字段的值；当字段不存在、无法访问或未找到时返回 null
     */
    private Object readSourceValue(Object bean, String sourceField) {
        Class<?> current = bean.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(sourceField);
                field.trySetAccessible();
                return field.get(bean);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }
}
