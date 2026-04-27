package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.MallProductTagType;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeAddRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeListQueryRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeStatusUpdateRequest;
import com.zhangyichuang.medicine.model.request.MallProductTagTypeUpdateRequest;
import com.zhangyichuang.medicine.model.vo.MallProductTagTypeAdminVo;
import com.zhangyichuang.medicine.model.vo.MallProductTagTypeVo;

import java.util.List;
import java.util.Map;

/**
 * 商品标签类型服务。
 *
 * @author Chuang
 */
public interface MallProductTagTypeService extends IService<MallProductTagType> {

    /**
     * 分页查询标签类型列表。
     *
     * @param request 查询参数
     * @return 标签类型分页列表
     */
    Page<MallProductTagTypeAdminVo> listTypes(MallProductTagTypeListQueryRequest request);

    /**
     * 查询标签类型详情。
     *
     * @param id 标签类型ID
     * @return 标签类型详情
     */
    MallProductTagTypeAdminVo getTypeById(Long id);

    /**
     * 查询标签类型实体。
     *
     * @param id 标签类型ID
     * @return 标签类型实体
     */
    MallProductTagType getTypeEntityById(Long id);

    /**
     * 根据编码查询标签类型实体。
     *
     * @param code 标签类型编码
     * @return 标签类型实体
     */
    MallProductTagType getTypeEntityByCode(String code);

    /**
     * 按ID列表查询标签类型映射。
     *
     * @param typeIds 标签类型ID列表
     * @return 标签类型映射
     */
    Map<Long, MallProductTagType> listTypeEntityMapByIds(List<Long> typeIds);

    /**
     * 查询启用标签类型下拉列表。
     *
     * @return 启用标签类型列表
     */
    List<MallProductTagTypeVo> option();

    /**
     * 新增标签类型。
     *
     * @param request 新增请求
     * @return 是否成功
     */
    boolean addType(MallProductTagTypeAddRequest request);

    /**
     * 修改标签类型。
     *
     * @param request 修改请求
     * @return 是否成功
     */
    boolean updateType(MallProductTagTypeUpdateRequest request);

    /**
     * 修改标签类型状态。
     *
     * @param request 状态请求
     * @return 是否成功
     */
    boolean updateTypeStatus(MallProductTagTypeStatusUpdateRequest request);

    /**
     * 删除标签类型。
     *
     * @param id 标签类型ID
     * @return 是否成功
     */
    boolean deleteType(Long id);
}
