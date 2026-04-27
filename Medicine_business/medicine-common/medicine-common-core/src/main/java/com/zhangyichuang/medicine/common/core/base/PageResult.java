package com.zhangyichuang.medicine.common.core.base;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Chuang
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    public Long pageNum;

    /**
     * 每页记录数
     */
    public Long pageSize;

    /**
     * 总记录数
     */
    public Long total;

    /**
     * 列表数据
     */
    public List<T> rows;

    public PageResult() {
    }

    public PageResult(Long pageNum, Long pageSize, Long total, List<T> rows) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.rows = rows;
    }
}
