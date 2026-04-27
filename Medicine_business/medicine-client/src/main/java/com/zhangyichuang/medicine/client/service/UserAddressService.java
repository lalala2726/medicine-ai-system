package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.model.entity.UserAddress;

import java.util.List;

/**
 * 用户收货地址服务接口
 *
 * @author Chuang
 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 获取当前用户地址列表
     *
     * @return 地址列表
     */
    List<UserAddress> getUserAddressList();

    /**
     * 根据ID获取地址详情
     *
     * @param id 地址ID
     * @return 地址信息
     */
    UserAddress getAddressById(Long id);

    /**
     * 新增地址
     *
     * @param address 地址信息
     * @return 是否成功
     */
    boolean addAddress(UserAddress address);

    /**
     * 更新地址
     *
     * @param address 地址信息
     * @return 是否成功
     */
    boolean updateAddress(UserAddress address);

    /**
     * 删除地址
     *
     * @param id 地址ID
     * @return 是否成功
     */
    boolean deleteAddress(Long id);

    /**
     * 设置默认地址
     *
     * @param id 地址ID
     * @return 是否成功
     */
    boolean setDefaultAddress(Long id);

    /**
     * 获取默认地址
     *
     * @return 默认地址
     */
    UserAddress getDefaultAddress();
}

