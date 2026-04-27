package com.zhangyichuang.medicine.client.model.dto;

import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchVo;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import lombok.Data;

import java.util.List;

/**
 * 商品搜索结果对象。
 *
 * @author Chuang
 */
@Data
public class MallProductSearchResultDto {

    /**
     * 商品分页结果。
     */
    private PageResult<MallProductSearchVo> pageResult;

    /**
     * 动态标签筛选列表。
     */
    private List<MallProductSearchTagFilterVo> tagFilters;
}
