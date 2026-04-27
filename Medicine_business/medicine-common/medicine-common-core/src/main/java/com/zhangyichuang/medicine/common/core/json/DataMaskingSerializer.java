package com.zhangyichuang.medicine.common.core.json;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.utils.DataMaskingUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 数据脱敏 JSON 序列化器
 *
 * @author Chuang
 */
public class DataMaskingSerializer extends ValueSerializer<String> {

    private DataMasking dataMasking;

    public DataMaskingSerializer() {
    }

    public DataMaskingSerializer(DataMasking dataMasking) {
        this.dataMasking = dataMasking;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (dataMasking != null) {
            String maskedValue = DataMaskingUtils.mask(value, dataMasking);
            gen.writeString(maskedValue);
        } else {
            gen.writeString(value);
        }
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        if (property != null) {
            DataMasking annotation = property.getAnnotation(DataMasking.class);
            if (annotation == null) {
                annotation = property.getContextAnnotation(DataMasking.class);
            }
            if (annotation != null) {
                return new DataMaskingSerializer(annotation);
            }
        }
        return this;
    }
}
