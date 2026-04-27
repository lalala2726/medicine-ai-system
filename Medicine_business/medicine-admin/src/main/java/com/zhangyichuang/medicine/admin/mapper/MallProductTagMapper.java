package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.model.entity.MallProductTag;
import com.zhangyichuang.medicine.model.request.MallProductTagListQueryRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagAdminVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 商品标签 Mapper。
 *
 * @author Chuang
 */
public interface MallProductTagMapper extends BaseMapper<MallProductTag> {

    /**
     * 分页查询商品标签列表。
     *
     * @param page    分页参数
     * @param request 查询参数
     * @return 商品标签分页结果
     */
    Page<MallProductTagAdminVo> listTags(Page<MallProductTagAdminVo> page,
                                         @Param("request") MallProductTagListQueryRequest request);

    /**
     * 物理删除未绑定的标签。
     *
     * @param id 标签ID
     * @return 影响行数
     */
    @Delete("DELETE FROM mall_product_tag WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
