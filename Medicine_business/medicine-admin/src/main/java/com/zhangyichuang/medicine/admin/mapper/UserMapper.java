package com.zhangyichuang.medicine.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.dto.UserOrderStatistics;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Chuang
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 用户列表
     *
     * @param userPage 分页对象
     * @param request  用户列表查询请求参数
     * @return 用户列表
     */
    Page<User> listUser(Page<User> userPage, @Param("request") UserListQueryRequest request);

    /**
     * 获取用户订单统计信息
     *
     * @param userId 用户ID
     * @return 用户订单统计信息
     */
    UserOrderStatistics getOrderStatisticsByUserId(@Param("userId") Long userId);
}




