package com.zhangyichuang.medicine.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangyichuang.medicine.model.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Chuang
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户角色编码
     */
    List<String> listRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色编码查询角色ID
     */
    Long selectRoleIdByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 写入用户角色关联
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}



