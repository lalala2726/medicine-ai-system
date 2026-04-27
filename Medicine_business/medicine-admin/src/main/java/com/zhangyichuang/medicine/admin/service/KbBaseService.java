package com.zhangyichuang.medicine.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseListDto;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseAddRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseListRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseSearchRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.KnowledgeBaseSearchVo;
import com.zhangyichuang.medicine.model.entity.KbBase;

import java.util.List;

/**
 * @author Chuang
 */
public interface KbBaseService extends IService<KbBase> {

    /**
     * 分页查询知识库
     *
     * @param request 查询参数
     * @return 分页结果
     */
    Page<KnowledgeBaseListDto> listKnowledgeBase(KnowledgeBaseListRequest request);

    /**
     * 根据ID查询知识库
     *
     * @param id 主键ID
     * @return 知识库信息
     */
    KbBase getKnowledgeBaseById(Long id);

    /**
     * 查询全部启用中的知识库。
     *
     * @return 启用中的知识库列表
     */
    List<KbBase> listEnabledKnowledgeBases();

    /**
     * 按业务名称查询启用中的知识库。
     *
     * @param knowledgeNames 业务名称列表
     * @return 匹配到的启用知识库列表
     */
    List<KbBase> listEnabledKnowledgeBasesByNames(List<String> knowledgeNames);

    /**
     * 执行知识库结构化检索。
     *
     * @param request 检索请求
     * @return 检索结果
     */
    KnowledgeBaseSearchVo searchKnowledgeBase(KnowledgeBaseSearchRequest request);

    /**
     * 添加知识库
     *
     * @param request 添加参数（包含业务名称 knowledgeName）
     * @return 添加结果
     */
    boolean addKnowledgeBase(KnowledgeBaseAddRequest request);

    /**
     * 修改知识库
     *
     * @param request 修改参数
     * @return 修改结果
     */
    boolean updateKnowledgeBase(KnowledgeBaseUpdateRequest request);

    /**
     * 删除知识库
     *
     * @param id 主键ID
     * @return 删除结果
     */
    boolean deleteKnowledgeBase(Long id);

}
