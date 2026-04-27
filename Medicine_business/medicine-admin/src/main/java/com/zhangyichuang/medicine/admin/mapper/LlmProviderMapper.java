package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.LlmProviderListDto;
import com.zhangyichuang.medicine.admin.model.request.LlmProviderListRequest;
import com.zhangyichuang.medicine.model.entity.LlmProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
@Mapper
public interface LlmProviderMapper extends BaseMapper<LlmProvider> {

    /**
     * 提供商分页列表。
     *
     * @param page    分页对象
     * @param request 查询参数
     * @return 提供商分页结果
     */
    Page<LlmProviderListDto> listProviders(Page<LlmProviderListDto> page,
                                           @Param("request") LlmProviderListRequest request);
}




