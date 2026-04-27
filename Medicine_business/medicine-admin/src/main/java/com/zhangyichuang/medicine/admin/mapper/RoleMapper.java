package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.RoleListRequest;
import com.zhangyichuang.medicine.model.entity.Role;
import org.apache.ibatis.annotations.Param;

public interface RoleMapper extends BaseMapper<Role> {

    Page<Role> roleList(Page<Role> page, @Param("query") RoleListRequest query);
}
