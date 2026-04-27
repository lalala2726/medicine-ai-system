package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.entity.MallProductTagType;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeListQueryRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagTypeAdminVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 商品标签类型 Mapper。
 *
 * @author Chuang
 */
public interface MallProductTagTypeMapper extends BaseMapper<MallProductTagType> {

    /**
     * 分页查询商品标签类型列表。
     *
     * @param page    分页参数
     * @param request 查询参数
     * @return 商品标签类型分页结果
     */
    Page<MallProductTagTypeAdminVo> listTypes(Page<MallProductTagTypeAdminVo> page,
                                              @Param("request") MallProductTagTypeListQueryRequest request);

    /**
     * 物理删除标签类型。
     *
     * @param id 标签类型ID
     * @return 影响行数
     */
    @Delete("DELETE FROM mall_product_tag_type WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
