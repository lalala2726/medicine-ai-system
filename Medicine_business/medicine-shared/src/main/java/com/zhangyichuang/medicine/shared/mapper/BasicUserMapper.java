package com.zhangyichuang.medicine.shared.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基础用户 Mapper。
 */
@Mapper
public interface BasicUserMapper extends BaseMapper<User> {
}
