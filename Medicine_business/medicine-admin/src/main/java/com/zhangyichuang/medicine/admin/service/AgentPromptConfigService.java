package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.admin.model.request.AgentPromptKeyUpsertRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptRollbackRequest;
import com.zhangyichuang.medicine.admin.model.request.AgentPromptUpdateRequest;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptConfigVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptHistoryVo;
import com.zhangyichuang.medicine.admin.model.vo.AgentPromptKeyOptionVo;

import java.util.List;

/**
 * Agent 提示词配置服务。
 */
public interface AgentPromptConfigService {

    /**
     * 查询指定提示词当前配置。
     *
     * @param promptKey 提示词业务键
     * @return 提示词当前配置
     */
    AgentPromptConfigVo getPromptConfig(String promptKey);

    /**
     * 保存提示词配置并生成新版本。
     *
     * @param request 保存请求
     * @return 是否保存成功
     */
    boolean savePromptConfig(AgentPromptUpdateRequest request);

    /**
     * 查询指定提示词历史版本列表。
     *
     * @param promptKey 提示词业务键
     * @param limit     返回条数上限
     * @return 历史版本列表（按版本倒序）
     */
    List<AgentPromptHistoryVo> listPromptHistory(String promptKey, Integer limit);

    /**
     * 将提示词回滚到指定历史版本（回滚本身会生成新版本）。
     *
     * @param request 回滚请求
     * @return 是否回滚成功
     */
    boolean rollbackPromptConfig(AgentPromptRollbackRequest request);

    /**
     * 删除指定提示词当前配置与历史版本。
     *
     * @param promptKey             提示词业务键
     * @param captchaVerificationId 验证码校验凭证
     * @return 是否删除成功
     */
    boolean deletePromptConfig(String promptKey, String captchaVerificationId);

    /**
     * 查询提示词键选项列表。
     *
     * @return 提示词键选项列表
     */
    List<AgentPromptKeyOptionVo> listPromptKeys();

    /**
     * 新增或更新提示词业务键。
     *
     * @param request 业务键新增/更新请求
     * @return 是否保存成功
     */
    boolean savePromptKey(AgentPromptKeyUpsertRequest request);

}
