package com.zhangyichuang.medicine.common.core.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 表格分页数据结果
 * 用于封装所有分页相关的数据返回
 *
 * @author Chuang
 * <p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Tag(name = "表格分页数据结果", description = "表格分页数据结果")
public class TableDataResult implements Serializable {

    @Serial
    private static final long serialVersionUID = -8909765534058591928L;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    private Long pageNum;

    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数")
    private Long pageSize;

    /**
     * 列表数据
     */
    @Schema(description = "列表数据")
    private List<?> rows;

    /**
     * 其他参数
     */
    @Schema(description = "其他参数")
    private Map<String, Object> extra;

    /**
     * 默认构造函数，初始化基本属性
     */
    public TableDataResult(List<?> rows, Long total, Long pageSize, Long pageNum, Map<String, Object> extra) {
        this.rows = rows;
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
        this.extra = extra;
    }

    public TableDataResult(List<?> rows, Long total, Long pageSize, Long pageNum) {
        this.rows = rows;
        this.total = total;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
    }


    public static AjaxResult<TableDataResult> build(PageResult<?> result) {
        return AjaxResult.success(new TableDataResult(
                result.getRows(),
                result.getTotal(),
                result.getPageSize(),
                result.getPageNum()
        ));
    }


    /**
     * 从 Page 对象构建 TableDataResult
     *
     * @param page 分页对象
     * @return TableDataResult 实例
     */
    public static AjaxResult<TableDataResult> build(Page<?> page) {
        return AjaxResult.success(new TableDataResult(
                page.getRecords(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        ));
    }


    /**
     * 从 Page 对象和自定义行数据构建 TableDataResult
     *
     * @param page 分页对象
     * @param rows 自定义行数据
     * @return TableDataResult 实例
     */
    public static AjaxResult<TableDataResult> build(Page<?> page, List<?> rows) {
        return AjaxResult.success(
                new TableDataResult(
                        rows,
                        page.getTotal(),
                        page.getSize(),
                        page.getCurrent()
                )
        );
    }

    /**
     * 从 Page 、自定义行数据、其他参数构建 TableDataResult
     *
     * @param page  分页对象
     * @param rows  自定义行数据
     * @param extra 其他参数
     * @return TableDataResult 实例
     */
    public static AjaxResult<TableDataResult> build(Page<?> page, List<?> rows, Map<String, Object> extra) {
        return AjaxResult.success(
                new TableDataResult(
                        rows,
                        page.getTotal(),
                        page.getSize(),
                        page.getCurrent(),
                        extra
                )
        );
    }

}

