package com.zhangyichuang.medicine.common.core.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Chuang
 */
@Data
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", type = "integer", format = "int32", defaultValue = "1", requiredMode = Schema.RequiredMode.AUTO)
    @Min(value = 1, message = "当前页码不能小于1")
    @Max(value = Integer.MAX_VALUE, message = "当前页码不能超过" + Integer.MAX_VALUE)
    private int pageNum = 1;

    /**
     * 每页数量
     */
    @Schema(description = "当前页码", type = "integer", format = "int32", defaultValue = "10", requiredMode = Schema.RequiredMode.AUTO)
    @Max(value = 200, message = "每页数量不能超过200")
    private int pageSize = 10;


    public <T> Page<T> toPage() {
        long current = this.pageNum;
        long size = Math.max(this.pageSize, 1);
        return new Page<>(current, size);
    }
}
