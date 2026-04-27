package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.AfterSaleListRequest;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.vo.AfterSaleListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 售后申请Mapper
 *
 * @author Chuang
 * created 2025/11/08
 */
@Mapper
public interface MallAfterSaleMapper extends BaseMapper<MallAfterSale> {

    /**
     * 分页查询售后列表
     *
     * @param page    分页对象
     * @param request 查询条件
     * @param userId  用户ID
     * @return 售后列表
     */
    Page<AfterSaleListVo> selectAfterSaleList(@Param("page") Page<AfterSaleListVo> page,
                                              @Param("request") AfterSaleListRequest request,
                                              @Param("userId") Long userId);
}

