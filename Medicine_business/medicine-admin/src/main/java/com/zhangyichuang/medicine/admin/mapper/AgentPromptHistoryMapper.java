package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.AgentPromptHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 提示词历史版本 Mapper。
 */
@Mapper
public interface AgentPromptHistoryMapper extends BaseMapper<AgentPromptHistory> {
}
