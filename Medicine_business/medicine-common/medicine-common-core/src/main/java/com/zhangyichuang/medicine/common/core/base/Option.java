package com.zhangyichuang.medicine.common.core.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/4/19
 */
@Schema(description = "下拉选项对象")
@Data
@NoArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class Option<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -2430627728459706234L;

    @Schema(description = "选项的值")
    private T value;

    @Schema(description = "选项的标签")
    private String label;

    @Schema(description = "子选项列表")
    private List<Option<T>> children;

    public Option(T value) {
        this.value = value;
    }

    public Option(T value, String label) {
        this.value = value;
        this.label = label;
    }


    public Option(T value, String label, List<Option<T>> children) {
        this.value = value;
        this.label = label;
        this.children = children;
    }

}
