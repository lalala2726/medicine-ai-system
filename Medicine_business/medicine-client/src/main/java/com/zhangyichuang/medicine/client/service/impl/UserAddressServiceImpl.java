package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.UserAddressMapper;
import com.zhangyichuang.medicine.client.service.UserAddressService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.UserAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户收货地址服务实现类
 *
 * @author Chuang
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService, BaseService {

    /**
     * 用户地址数量上限
     */
    private static final int MAX_ADDRESS_COUNT = 20;

    /**
     * 获取当前用户地址列表
     *
     * @return 地址列表
     */
    @Override
    public List<UserAddress> getUserAddressList() {
        Long userId = getUserId();
        return lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime)
                .list();
    }

    /**
     * 根据ID获取地址详情
     *
     * @param id 地址ID
     * @return 地址信息
     */
    @Override
    public UserAddress getAddressById(Long id) {
        Assert.notNull(id, "地址ID不能为空");
        Long userId = getUserId();
        UserAddress address = getById(id);
        Assert.notNull(address, "地址不存在");
        Assert.isTrue(address.getUserId().equals(userId), "无权访问该地址");
        return address;
    }

    /**
     * 新增地址
     *
     * @param address 地址信息
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addAddress(UserAddress address) {
        Long userId = getUserId();

        // 校验地址数量是否超过上限
        long count = lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .count();
        Assert.isTrue(count < MAX_ADDRESS_COUNT, "地址数量已达上限，最多支持" + MAX_ADDRESS_COUNT + "个地址");

        // 设置用户ID和创建时间
        address.setUserId(userId);
        address.setCreateTime(new Date());
        address.setUpdateTime(new Date());

        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        } else {
            // 如果是第一个地址，自动设为默认
            if (count == 0) {
                address.setIsDefault(1);
            } else {
                address.setIsDefault(0);
            }
        }

        return save(address);
    }

    /**
     * 更新地址
     *
     * @param address 地址信息
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAddress(UserAddress address) {
        Assert.notNull(address.getId(), "地址ID不能为空");
        Long userId = getUserId();

        // 校验地址是否存在且属于当前用户
        UserAddress existAddress = getById(address.getId());
        Assert.notNull(existAddress, "地址不存在");
        Assert.isTrue(existAddress.getUserId().equals(userId), "无权修改该地址");

        // 如果设置为默认地址，先取消其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(userId);
        }

        // 设置更新时间
        address.setUpdateTime(new Date());
        address.setUserId(userId);

        return updateById(address);
    }

    /**
     * 删除地址
     *
     * @param id 地址ID
     * @return 是否成功
     */
    @Override
    public boolean deleteAddress(Long id) {
        Assert.notNull(id, "地址ID不能为空");
        Long userId = getUserId();

        // 校验地址是否存在且属于当前用户
        UserAddress address = getById(id);
        Assert.notNull(address, "地址不存在");
        Assert.isTrue(address.getUserId().equals(userId), "无权删除该地址");

        return removeById(id);
    }

    /**
     * 设置默认地址
     *
     * @param id 地址ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultAddress(Long id) {
        Assert.notNull(id, "地址ID不能为空");
        Long userId = getUserId();

        // 校验地址是否存在且属于当前用户
        UserAddress address = getById(id);
        Assert.notNull(address, "地址不存在");
        Assert.isTrue(address.getUserId().equals(userId), "无权操作该地址");

        // 先取消该用户所有默认地址
        clearDefaultAddress(userId);

        // 设置当前地址为默认
        address.setIsDefault(1);
        address.setUpdateTime(new Date());
        return updateById(address);
    }

    /**
     * 获取默认地址
     *
     * @return 默认地址
     */
    @Override
    public UserAddress getDefaultAddress() {
        Long userId = getUserId();
        return lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .one();
    }

    /**
     * 取消用户所有默认地址
     *
     * @param userId 用户ID
     */
    private void clearDefaultAddress(Long userId) {
        lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0)
                .update();
    }
}

